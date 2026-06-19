package com.fs.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fs.model.Currency;
import com.fs.model.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestTemplate;

import com.fs.dto.FigiesDto;
import com.fs.dto.OrderBookDto;
import com.fs.dto.OrderBookEntryDto;
import com.fs.dto.StockPrice;
import com.fs.dto.StocksPricesDto;
import com.fs.exception.StockNotFoundException;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoexIssDataAdapterTest {

    @RegisterExtension
    static com.github.tomakehurst.wiremock.junit5.WireMockExtension wireMock =
            com.github.tomakehurst.wiremock.junit5.WireMockExtension.newInstance()
                    .options(wireMockConfig().dynamicPort())
                    .build();

    private MoexIssDataAdapter adapter;

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        adapter = new MoexIssDataAdapter(
                new RestTemplate(),
                new ObjectMapper(),
                wireMock.baseUrl(),
                "TQBR");
    }

    @Test
    void getBrokerName_returnsMoexIss() {
        assertThat(adapter.getBrokerName()).isEqualTo("MOEX_ISS");
    }

    @Test
    void getStockByTicker_returnsMappedStock() {
        stubSecurity("SBER", "Сбербанк", "RU0009029540", "SUR");

        Stock stock = adapter.getStockByTicker("SBER");

        assertThat(stock.getTicker()).isEqualTo("SBER");
        assertThat(stock.getFigi()).isEqualTo("MOEX:SBER");
        assertThat(stock.getName()).isEqualTo("Сбербанк");
        assertThat(stock.getCurrency()).isEqualTo(Currency.RUB);
        assertThat(stock.getSource()).isEqualTo("MOEX_ISS");
    }

    @Test
    void getStockByTicker_unknownTicker_throwsNotFound() {
        wireMock.stubFor(get(urlPathEqualTo(
                "/iss/engines/stock/markets/shares/boards/TQBR/securities/UNKNOWN.json"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "securities": {
                                    "columns": ["SECID","SHORTNAME","ISIN","SECTYPE","FACEUNIT"],
                                    "data": []
                                  }
                                }
                                """)));

        assertThatThrownBy(() -> adapter.getStockByTicker("UNKNOWN"))
                .isInstanceOf(StockNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
    }

    @Test
    void getPrices_returnsLastPriceForMoexFigi() {
        stubMarketData("GAZP", "165.42");

        StocksPricesDto prices = adapter.getPrices(new FigiesDto(List.of("MOEX:GAZP")));

        assertThat(prices.getPrices()).containsExactly(new StockPrice("MOEX:GAZP", new BigDecimal("165.42")));
    }

    @Test
    void getPrices_nonMoexFigi_throwsNotFound() {
        assertThatThrownBy(() -> adapter.getPrices(new FigiesDto(List.of("BBG004730N88"))))
                .isInstanceOf(StockNotFoundException.class);
    }

    @Test
    void getOrderBook_returnsBestBidOfferFromMarketData() {
        stubMarketDataOrderBook("VTBR", "78.80", "1500", "78.82", "2000", "78.81");

        OrderBookDto orderBook = adapter.getOrderBook("MOEX:VTBR", 20);

        assertThat(orderBook.getFigi()).isEqualTo("MOEX:VTBR");
        assertThat(orderBook.getBids()).hasSize(1);
        assertThat(orderBook.getBids().get(0).getPrice()).isEqualByComparingTo("78.80");
        assertThat(orderBook.getBids().get(0).getQuantity()).isEqualTo(1500L);
        assertThat(orderBook.getAsks()).hasSize(1);
        assertThat(orderBook.getAsks().get(0).getPrice()).isEqualByComparingTo("78.82");
        assertThat(orderBook.getAsks().get(0).getQuantity()).isEqualTo(2000L);
        assertThat(orderBook.getLastPrice()).isEqualByComparingTo("78.81");
    }

    @Test
    void getOrderBook_outsideTradingHours_returnsLastPriceWithEmptyLevels() {
        stubMarketDataOrderBook("VTBR", null, null, null, null, "78.81");

        OrderBookDto orderBook = adapter.getOrderBook("MOEX:VTBR", 20);

        assertThat(orderBook.getBids()).isEmpty();
        assertThat(orderBook.getAsks()).isEmpty();
        assertThat(orderBook.getLastPrice()).isEqualByComparingTo("78.81");
    }

    @Test
    void getOrderBook_nonMoexFigi_throwsNotFound() {
        assertThatThrownBy(() -> adapter.getOrderBook("BBG004730N88", 20))
                .isInstanceOf(StockNotFoundException.class);
    }

    @Test
    void placeOrder_throwsUnsupported() {
        assertThatThrownBy(() -> adapter.placeOrder("acc", null))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("data-only");
    }

    private void stubSecurity(String ticker, String shortName, String isin, String faceUnit) {
        wireMock.stubFor(get(urlPathEqualTo(
                "/iss/engines/stock/markets/shares/boards/TQBR/securities/" + ticker + ".json"))
                .withQueryParam("iss.only", equalTo("securities"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(String.format("""
                                {
                                  "securities": {
                                    "columns": ["SECID","SHORTNAME","ISIN","SECTYPE","FACEUNIT"],
                                    "data": [["%s","%s","%s","1","%s"]]
                                  }
                                }
                                """, ticker, shortName, isin, faceUnit))));
    }

    private void stubMarketData(String ticker, String lastPrice) {
        wireMock.stubFor(get(urlPathEqualTo(
                "/iss/engines/stock/markets/shares/boards/TQBR/securities/" + ticker + ".json"))
                .withQueryParam("iss.only", equalTo("marketdata"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(String.format("""
                                {
                                  "marketdata": {
                                    "columns": ["SECID","LAST"],
                                    "data": [["%s", %s]]
                                  }
                                }
                                """, ticker, lastPrice))));
    }

    private void stubMarketDataOrderBook(String ticker, String bid, String bidDepth, String offer,
            String offerDepth, String lastPrice) {
        wireMock.stubFor(get(urlPathEqualTo(
                "/iss/engines/stock/markets/shares/boards/TQBR/securities/" + ticker + ".json"))
                .withQueryParam("iss.only", equalTo("marketdata"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(String.format("""
                                {
                                  "marketdata": {
                                    "columns": ["SECID","BID","BIDDEPTH","OFFER","OFFERDEPTH","LAST"],
                                    "data": [["%s", %s, %s, %s, %s, %s]]
                                  }
                                }
                                """,
                                ticker,
                                jsonValue(bid),
                                jsonValue(bidDepth),
                                jsonValue(offer),
                                jsonValue(offerDepth),
                                jsonValue(lastPrice)))));
    }

    private static String jsonValue(String value) {
        return value == null ? "null" : value;
    }
}

package com.fs.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fs.adapter.moex.MoexFigi;
import com.fs.adapter.moex.MoexIssJsonParser;
import com.fs.config.MoexConfig;
import com.fs.dto.AmendBrokerOrderDto;
import com.fs.dto.BrokerCandleDto;
import com.fs.dto.BrokerOrderDto;
import com.fs.dto.CreateBrokerOrderDto;
import com.fs.dto.FigiesDto;
import com.fs.dto.OrderBookDto;
import com.fs.dto.OrderBookEntryDto;
import com.fs.dto.StockPrice;
import com.fs.dto.StocksDto;
import com.fs.dto.StocksPricesDto;
import com.fs.dto.TickersDto;
import com.fs.exception.StockNotFoundException;
import com.fs.model.Currency;
import com.fs.model.Stock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Read-only MOEX ISS data provider (instruments and last prices).
 */
@Component
@Profile("!test & !mock-broker")
@Slf4j
public class MoexIssDataAdapter implements BrokerAdapter {

    private static final String BROKER_NAME = "MOEX_ISS";

    private static final String DATA_ONLY_MSG = "MOEX ISS adapter is data-only";

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper;

    private final String baseUrl;

    private final String board;

    @Autowired
    public MoexIssDataAdapter(RestTemplate restTemplate, ObjectMapper objectMapper, MoexConfig moexConfig) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.baseUrl = trimTrailingSlash(moexConfig.getBaseUrl());
        this.board = moexConfig.getBoard();
    }

    MoexIssDataAdapter(RestTemplate restTemplate, ObjectMapper objectMapper, String baseUrl, String board) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.board = board;
    }

    @Override
    public String getBrokerName() {
        return BROKER_NAME;
    }

    @Override
    public boolean isAvailable() {
        try {
            restTemplate.getForObject(baseUrl + "/iss/index.json?iss.meta=off&iss.only=version", String.class);
            return true;
        } catch (RestClientException ex) {
            log.warn("MOEX ISS is not available: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public Stock getStockByTicker(String ticker) {
        Map<String, String> row = fetchSecurityRow(ticker)
                .orElseThrow(() -> new StockNotFoundException(
                        String.format("Stock %s not found in MOEX ISS", ticker)));
        return mapSecurityRow(row);
    }

    @Override
    public StocksDto getStocksByTickers(TickersDto tickersDto) {
        if (tickersDto.getTickers() == null || tickersDto.getTickers().isEmpty()) {
            return new StocksDto(Collections.emptyList());
        }
        List<Stock> stocks = tickersDto.getTickers().stream()
                .map(ticker -> fetchSecurityRow(ticker).map(this::mapSecurityRow).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new StocksDto(stocks);
    }

    @Override
    public StocksPricesDto getPrices(FigiesDto figiesDto) {
        if (figiesDto.getFigies() == null || figiesDto.getFigies().isEmpty()) {
            return new StocksPricesDto(Collections.emptyList());
        }

        List<StockPrice> prices = new ArrayList<>();
        List<String> failedFigies = new ArrayList<>();
        collectMoexPrices(figiesDto.getFigies(), prices, failedFigies);
        handleMissingMoexPrices(prices, failedFigies);
        return new StocksPricesDto(prices);
    }

    private void collectMoexPrices(List<String> figies, List<StockPrice> prices, List<String> failedFigies) {
        for (String figi : figies) {
            Optional<String> ticker = MoexFigi.toTicker(figi);
            if (ticker.isEmpty()) {
                failedFigies.add(figi);
                continue;
            }
            fetchLastPrice(ticker.get())
                    .ifPresentOrElse(
                            price -> prices.add(new StockPrice(figi, price)),
                            () -> failedFigies.add(figi));
        }
    }

    private void handleMissingMoexPrices(List<StockPrice> prices, List<String> failedFigies) {
        if (!failedFigies.isEmpty() && prices.isEmpty()) {
            throw new StockNotFoundException(
                    String.format("Stocks %s not found in MOEX ISS", failedFigies));
        }
        if (!failedFigies.isEmpty()) {
            log.warn("Failed to get MOEX prices for {} figies: {}", failedFigies.size(), failedFigies);
        }
    }

    @Override
    public OrderBookDto getOrderBook(String figi, int depth) {
        String ticker = MoexFigi.toTicker(figi)
                .orElseThrow(() -> new StockNotFoundException(
                        String.format("Order book for figi %s requires MOEX:{TICKER} format", figi)));
        return fetchOrderBook(ticker, figi, depth);
    }

    private OrderBookDto fetchOrderBook(String ticker, String figi, int depth) {
        Optional<OrderBookDto> fullBook = tryFetchIssOrderBook(ticker, figi, depth);
        if (fullBook.isPresent()) {
            return fullBook.get();
        }
        return fetchShallowOrderBookFromMarketData(ticker, figi, depth);
    }

    private Optional<OrderBookDto> tryFetchIssOrderBook(String ticker, String figi, int depth) {
        String url = buildIssOrderBookUrl(ticker);
        try {
            String body = restTemplate.getForObject(url, String.class);
            if (!isJsonResponse(body)) {
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(body);
            List<Map<String, String>> rows = MoexIssJsonParser.parseAllRows(root, "orderbook");
            return buildOrderBookFromIssRows(rows, figi, depth);
        } catch (Exception ex) {
            log.debug("MOEX ISS orderbook unavailable for {}: {}", ticker, ex.getMessage());
            return Optional.empty();
        }
    }

    private String buildIssOrderBookUrl(String ticker) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/iss/engines/stock/markets/shares/boards/{board}/securities/{ticker}/orderbook.json")
                .queryParam("iss.meta", "off")
                .queryParam("iss.only", "orderbook")
                .queryParam("orderbook.columns", "BOARDID,SECID,BUYSELL,PRICE,QUANTITY")
                .buildAndExpand(board, ticker.toUpperCase())
                .toUriString();
    }

    private Optional<OrderBookDto> buildOrderBookFromIssRows(
            List<Map<String, String>> rows, String figi, int depth) {
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        List<OrderBookEntryDto> bids = new ArrayList<>();
        List<OrderBookEntryDto> asks = new ArrayList<>();
        for (Map<String, String> row : rows) {
            mapOrderBookRow(row).ifPresent(entry -> appendSideEntry(entry, bids, asks));
        }
        if (bids.isEmpty() && asks.isEmpty()) {
            return Optional.empty();
        }
        int effectiveDepth = Math.max(1, depth);
        return Optional.of(new OrderBookDto(
                figi,
                bids.stream().limit(effectiveDepth).toList(),
                asks.stream().limit(effectiveDepth).toList(),
                null,
                Instant.now(),
                depth
        ));
    }

    private void appendSideEntry(SideEntry entry, List<OrderBookEntryDto> bids, List<OrderBookEntryDto> asks) {
        if ("B".equalsIgnoreCase(entry.side())) {
            bids.add(entry.entry());
            return;
        }
        if ("S".equalsIgnoreCase(entry.side())) {
            asks.add(entry.entry());
        }
    }

    private OrderBookDto fetchShallowOrderBookFromMarketData(String ticker, String figi, int depth) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/iss/engines/stock/markets/shares/boards/{board}/securities/{ticker}.json")
                .queryParam("iss.meta", "off")
                .queryParam("iss.only", "marketdata")
                .queryParam("marketdata.columns", "SECID,BID,BIDDEPTH,OFFER,OFFERDEPTH,LAST")
                .buildAndExpand(board, ticker.toUpperCase())
                .toUriString();
        Optional<Map<String, String>> row = fetchIssBlock(url, "marketdata");
        if (row.isEmpty()) {
            throw new StockNotFoundException(String.format("Order book not found for figi: %s", figi));
        }

        Map<String, String> values = row.get();
        List<OrderBookEntryDto> bids = new ArrayList<>();
        List<OrderBookEntryDto> asks = new ArrayList<>();
        parseDecimal(values.get("BID")).ifPresent(price ->
                bids.add(new OrderBookEntryDto(price, parseLongQuantity(values.get("BIDDEPTH")))));
        parseDecimal(values.get("OFFER")).ifPresent(price ->
                asks.add(new OrderBookEntryDto(price, parseLongQuantity(values.get("OFFERDEPTH")))));
        BigDecimal lastPrice = parseDecimal(values.get("LAST")).orElse(null);

        return new OrderBookDto(figi, bids, asks, lastPrice, Instant.now(), depth);
    }

    private Optional<SideEntry> mapOrderBookRow(Map<String, String> row) {
        String side = row.get("BUYSELL");
        Optional<BigDecimal> price = parseDecimal(row.get("PRICE"));
        if (side == null || price.isEmpty()) {
            return Optional.empty();
        }
        OrderBookEntryDto entryDto = new OrderBookEntryDto(
                price.get(), parseLongQuantity(row.get("QUANTITY")));
        return Optional.of(new SideEntry(side, entryDto));
    }

    private static Long parseLongQuantity(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private static boolean isJsonResponse(String body) {
        if (body == null || body.isBlank()) {
            return false;
        }
        String trimmed = body.stripLeading();
        return trimmed.startsWith("{") || trimmed.startsWith("[");
    }

    private record SideEntry(String side, OrderBookEntryDto entry) {
    }

    @Override
    public StocksDto getAvailableTickers() {
        return new StocksDto(Collections.emptyList());
    }

    @Override
    public BrokerOrderDto placeOrder(String accountId, CreateBrokerOrderDto createOrderDto) {
        throw new UnsupportedOperationException(DATA_ONLY_MSG);
    }

    @Override
    public void cancelOrder(String accountId, String orderId) {
        throw new UnsupportedOperationException(DATA_ONLY_MSG);
    }

    @Override
    public BrokerOrderDto amendOrder(String accountId, String orderId, AmendBrokerOrderDto amendDto) {
        throw new UnsupportedOperationException(DATA_ONLY_MSG);
    }

    @Override
    public BrokerOrderDto getOrderStatus(String accountId, String orderId) {
        throw new UnsupportedOperationException(DATA_ONLY_MSG);
    }

    @Override
    public List<BrokerOrderDto> getActiveOrders(String accountId) {
        throw new UnsupportedOperationException(DATA_ONLY_MSG);
    }

    @Override
    public List<BrokerCandleDto> getHistoricCandles(String figi, Instant from, Instant to, String interval) {
        throw new UnsupportedOperationException(DATA_ONLY_MSG);
    }

    private Optional<Map<String, String>> fetchSecurityRow(String ticker) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/iss/engines/stock/markets/shares/boards/{board}/securities/{ticker}.json")
                .queryParam("iss.meta", "off")
                .queryParam("iss.only", "securities")
                .queryParam("securities.columns", "SECID,SHORTNAME,ISIN,SECTYPE,FACEUNIT")
                .buildAndExpand(board, ticker.toUpperCase())
                .toUriString();
        return fetchIssBlock(url, "securities");
    }

    private Optional<BigDecimal> fetchLastPrice(String ticker) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/iss/engines/stock/markets/shares/boards/{board}/securities/{ticker}.json")
                .queryParam("iss.meta", "off")
                .queryParam("iss.only", "marketdata")
                .queryParam("marketdata.columns", "SECID,LAST")
                .buildAndExpand(board, ticker.toUpperCase())
                .toUriString();
        Optional<Map<String, String>> row = fetchIssBlock(url, "marketdata");
        return row.flatMap(values -> parseDecimal(values.get("LAST")));
    }

    private Optional<Map<String, String>> fetchIssBlock(String url, String blockName) {
        try {
            String body = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(body);
            return MoexIssJsonParser.parseFirstRow(root, blockName);
        } catch (Exception ex) {
            log.warn("MOEX ISS request failed for {}: {}", url, ex.getMessage());
            return Optional.empty();
        }
    }

    private Stock mapSecurityRow(Map<String, String> row) {
        String secId = row.get("SECID");
        String shortName = row.getOrDefault("SHORTNAME", secId);
        Currency currency = mapCurrency(row.get("FACEUNIT"));
        return new Stock(
                secId,
                MoexFigi.toFigi(secId),
                shortName,
                "SHARE",
                currency,
                BROKER_NAME
        );
    }

    private Currency mapCurrency(String faceUnit) {
        if (faceUnit == null || faceUnit.isBlank()) {
            return Currency.RUB;
        }
        String normalized = "SUR".equalsIgnoreCase(faceUnit) ? "RUB" : faceUnit.toUpperCase();
        try {
            return Currency.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            log.warn("Unsupported MOEX currency {}, defaulting to RUB", faceUnit);
            return Currency.RUB;
        }
    }

    private Optional<BigDecimal> parseDecimal(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "https://iss.moex.com";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}

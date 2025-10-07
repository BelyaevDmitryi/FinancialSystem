package ru.otus.hw.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.otus.hw.dto.FigiesDto;
import ru.otus.hw.dto.StockPrice;
import ru.otus.hw.dto.StocksDto;
import ru.otus.hw.dto.StocksPricesDto;
import ru.otus.hw.dto.TickersDto;
import ru.otus.hw.exception.StockNotFoundException;
import ru.otus.hw.mapper.StockPriceMapper;
import ru.otus.hw.model.Currency;
import ru.otus.hw.model.Stock;
import ru.tinkoff.piapi.contract.v1.GetOrderBookResponse;
import ru.tinkoff.piapi.contract.v1.InstrumentShort;
import ru.tinkoff.piapi.contract.v1.Share;
import ru.tinkoff.piapi.core.InvestApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TinkoffStockService implements StockService {
    private final InvestApi api;
    private final StockPriceMapper stockPriceMapper;

    @Async
    public CompletableFuture<List<InstrumentShort>> getMarketInstrumentsTicker(String ticker) {
        var service = api.getInstrumentsService();
        return service.findInstrument(ticker);
    }

    @Async
    public CompletableFuture<Share> getShareTicker(String ticker, String classcode) {
        var service = api.getInstrumentsService();
        return service.getShareByTicker(ticker, classcode);
    }

    @Async
    public CompletableFuture<List<InstrumentShort>> getInstrumentsTicker(String ticker) {
        var cf = getMarketInstrumentsTicker(ticker);
        var list = cf.join();
        var l = list.stream()
                .map(obj -> {
                    if (obj.getApiTradeAvailableFlag() && obj.getInstrumentKindValue() == 2 && obj.getTicker().equals(ticker)) {
                        return obj;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
        return CompletableFuture.completedFuture(l);
    }

    @Override
    public Stock getStockByTicker(String ticker) {
        var stock = getInstrumentsTicker(ticker);
        var listItem = stock.join();
        if (listItem.isEmpty()) {
            throw new StockNotFoundException(String.format("Stock %S not found ", ticker));
        }
        var item = listItem.get(0);
        var shareCompletableFuture = getShareTicker(item.getTicker(), item.getClassCode());
        var share = shareCompletableFuture.join();
        return new Stock(
                share.getTicker(),
                share.getFigi(),
                share.getName(),
                share.getShareType().name(),
                Currency.valueOf(share.getCurrency().toUpperCase()),
                "TINKOFF");
    }

    @Override
    public StocksDto getStocksByTickers(TickersDto tickers) {
        List<CompletableFuture<List<InstrumentShort>>> instruments = new ArrayList<>();
        tickers.getTickers().forEach(ticker -> instruments.add(getInstrumentsTicker(ticker)));
        List<Stock> stocks = instruments.stream()
                .map(CompletableFuture::join)
                .map(instrumentShorts -> {
                    if (!instrumentShorts.isEmpty()) {
                        return instrumentShorts.get(0);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .map(share -> getShareTicker(share.getTicker(), share.getClassCode()))
                .map(CompletableFuture::join)
                .map(share -> new Stock(
                        share.getTicker(),
                        share.getFigi(),
                        share.getName(),
                        share.getShareType().name(),
                        Currency.valueOf(share.getCurrency().toUpperCase()),
                        "TINKOFF"
                ))
                .collect(Collectors.toList());
        return new StocksDto(stocks);
    }

    @Async
    public CompletableFuture<GetOrderBookResponse> getOrderBookByFigi(String figi) {
        var orderBook = api.getMarketDataService().getOrderBook(figi, 1);
        log.info("Getting prise {} from Tinkoff", figi);
        return orderBook;
    }

    @Override
    public StocksPricesDto getPrices(FigiesDto figiesDto) {
        long start = System.currentTimeMillis();
        List<CompletableFuture<GetOrderBookResponse>> orderBooks = new ArrayList<>();
        List<String> listOrdersFailed = new ArrayList<>();
        figiesDto.getFigies().forEach(figi -> orderBooks.add(getOrderBookByFigi(figi)
                        .handle((result, ex) -> {
                            if (ex != null) {
                                log.error(String.format("Stock %S not found ", figi));
                                listOrdersFailed.add(figi);
                            }
                            return result;
                        })
        ));
        var listPrices = orderBooks.stream()
                .map(CompletableFuture::join)
                .map(orderBook -> {
                    if (!listOrdersFailed.isEmpty()) {
                        throw new StockNotFoundException(String.format("Stock %S not found ", listOrdersFailed.remove(listOrdersFailed.size() - 1)));
                    }
                    return new StockPrice(orderBook.getFigi(), stockPriceMapper.toBigDecimal(orderBook.getLastPrice()));
                })
                .collect(Collectors.toList());

        log.info("Time {}", System.currentTimeMillis() - start);
        return new StocksPricesDto(listPrices);
    }
}

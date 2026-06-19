package com.fs.adapter;

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
import com.fs.mapper.StockPriceMapper;
import com.fs.model.Currency;
import com.fs.model.Stock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.GetOrderBookResponse;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;
import ru.tinkoff.piapi.contract.v1.InstrumentShort;
import ru.tinkoff.piapi.contract.v1.OrderDirection;
import ru.tinkoff.piapi.contract.v1.OrderType;
import ru.tinkoff.piapi.contract.v1.PostOrderResponse;
import ru.tinkoff.piapi.contract.v1.MoneyValue;
import ru.tinkoff.piapi.contract.v1.Quotation;
import ru.tinkoff.piapi.contract.v1.Share;
import ru.tinkoff.piapi.contract.v1.OrderState;
import ru.tinkoff.piapi.contract.v1.PriceType;
import ru.tinkoff.piapi.contract.v1.StopOrder;
import ru.tinkoff.piapi.contract.v1.StopOrderDirection;
import ru.tinkoff.piapi.contract.v1.StopOrderType;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Адаптер для работы с Tinkoff Invest API
 */
@Component("tinkoffBrokerAdapter")
@Profile("!test & !mock-broker")
@RequiredArgsConstructor
@Slf4j
public class TinkoffBrokerAdapter implements BrokerAdapter {
    
    private final InvestApi investApi;
    private final StockPriceMapper stockPriceMapper;

    /** ID стоп-заявок, выставленных через этот адаптер (для cancel/getStatus). */
    private final Set<String> stopOrderIds = ConcurrentHashMap.newKeySet();
    
    private static final String BROKER_NAME = "TINKOFF";
    private static final int INSTRUMENT_KIND_SHARE = 2;

    @Override
    public Stock getStockByTicker(String ticker) {
        log.debug("Getting stock by ticker: {}", ticker);
        
        try {
            var instrumentsFuture = findInstrumentsByTicker(ticker);
            var instruments = instrumentsFuture.join();
            
            if (instruments.isEmpty()) {
                throw new StockNotFoundException(String.format("Stock %s not found in Tinkoff", ticker));
            }
            
            var instrument = instruments.get(0);
            var shareFuture = investApi.getInstrumentsService()
                    .getShareByTicker(instrument.getTicker(), instrument.getClassCode());
            var share = shareFuture.join();
            
            return mapShareToStock(share);
        } catch (StockNotFoundException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("Invalid currency for ticker {}: {}", ticker, e.getMessage());
            throw new StockNotFoundException(String.format("Stock %s has unsupported currency", ticker), e.getCause());
        } catch (Exception e) {
            log.error("Error getting stock by ticker {}: {}", ticker, e.getMessage(), e);
            throw new RuntimeException("Не удалось получить информацию об акции: " + e.getMessage(), e);
        }
    }

    @Override
    public StocksDto getStocksByTickers(TickersDto tickers) {
        log.debug("Getting stocks by tickers: {}", tickers.getTickers());
        
        if (tickers.getTickers() == null || tickers.getTickers().isEmpty()) {
            return new StocksDto(Collections.emptyList());
        }
        
        try {
            // Параллельно ищем все инструменты
            List<CompletableFuture<List<InstrumentShort>>> instrumentFutures = tickers.getTickers().stream()
                    .map(this::findInstrumentsByTicker)
                    .collect(Collectors.toList());
            
            // Получаем первый найденный инструмент для каждого тикера
            List<CompletableFuture<Share>> shareFutures = instrumentFutures.stream()
                    .map(CompletableFuture::join)
                    .filter(instruments -> !instruments.isEmpty())
                    .map(instruments -> instruments.get(0))
                    .map(instrument -> investApi.getInstrumentsService()
                            .getShareByTicker(instrument.getTicker(), instrument.getClassCode()))
                    .collect(Collectors.toList());
            
            // Преобразуем в Stock, обрабатывая ошибки валют
            List<Stock> stocks = shareFutures.stream()
                    .map(CompletableFuture::join)
                    .map(share -> {
                        try {
                            return mapShareToStock(share);
                        } catch (IllegalArgumentException e) {
                            log.warn("Skipping stock {} due to unsupported currency: {}", 
                                    share.getTicker(), e.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            log.debug("Found {} stocks out of {} requested", stocks.size(), tickers.getTickers().size());
            return new StocksDto(stocks);
        } catch (Exception e) {
            log.error("Error getting stocks by tickers: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось получить информацию об акциях: " + e.getMessage(), e);
        }
    }

    @Override
    public StocksPricesDto getPrices(FigiesDto figiesDto) {
        long start = System.currentTimeMillis();
        log.debug("Getting prices for {} figies", figiesDto.getFigies().size());
        
        if (figiesDto.getFigies() == null || figiesDto.getFigies().isEmpty()) {
            return new StocksPricesDto(Collections.emptyList());
        }
        
        // Параллельно запрашиваем стаканы для всех FIGI
        List<CompletableFuture<GetOrderBookResponse>> orderBookFutures = figiesDto.getFigies().stream()
                .map(figi -> investApi.getMarketDataService()
                        .getOrderBook(figi, 1)
                        .handle((result, ex) -> {
                            if (ex != null) {
                                log.warn("Failed to get order book for figi {}: {}", figi, ex.getMessage());
                                return null;
                            }
                            return result;
                        }))
                .collect(Collectors.toList());
        
        // Обрабатываем результаты
        List<String> failedFigies = new ArrayList<>();
        List<StockPrice> prices = new ArrayList<>();
        
        for (int i = 0; i < orderBookFutures.size(); i++) {
            var future = orderBookFutures.get(i);
            var figi = figiesDto.getFigies().get(i);
            
            try {
                var orderBook = future.join();
                if (orderBook != null && orderBook.hasLastPrice()) {
                    BigDecimal price = stockPriceMapper.toBigDecimal(orderBook.getLastPrice());
                    prices.add(new StockPrice(figi, price));
                } else {
                    failedFigies.add(figi);
                }
            } catch (Exception e) {
                log.warn("Error processing order book for figi {}: {}", figi, e.getMessage());
                failedFigies.add(figi);
            }
        }
        
        // Если все запросы провалились, выбрасываем исключение
        if (!failedFigies.isEmpty() && prices.isEmpty()) {
            throw new StockNotFoundException(
                    String.format("Stocks %s not found in Tinkoff", failedFigies));
        }
        
        // Если часть запросов провалилась, логируем предупреждение
        if (!failedFigies.isEmpty()) {
            log.warn("Failed to get prices for {} figies: {}", failedFigies.size(), failedFigies);
        }

        log.info("Tinkoff API call time: {} ms, retrieved {} prices", 
                System.currentTimeMillis() - start, prices.size());
        return new StocksPricesDto(prices);
    }

    @Override
    public OrderBookDto getOrderBook(String figi, int depth) {
        log.debug("Getting order book for figi: {} with depth: {}", figi, depth);
        
        try {
            var orderBookFuture = investApi.getMarketDataService().getOrderBook(figi, depth);
            var orderBookResponse = orderBookFuture.join();
            
            if (orderBookResponse == null) {
                throw new StockNotFoundException("Order book not found for figi: " + figi);
            }
            
            // Преобразуем bids (заявки на покупку) - отсортированы по убыванию цены
            List<OrderBookEntryDto> bids = orderBookResponse.getBidsList().stream()
                    .filter(order -> order != null && order.getQuantity() > 0)
                    .map(order -> {
                        try {
                            return new OrderBookEntryDto(
                                    stockPriceMapper.toBigDecimal(order.getPrice()),
                                    order.getQuantity()
                            );
                        } catch (Exception e) {
                            log.warn("Error mapping bid order: {}", e.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            // Преобразуем asks (заявки на продажу) - отсортированы по возрастанию цены
            List<OrderBookEntryDto> asks = orderBookResponse.getAsksList().stream()
                    .filter(order -> order != null && order.getQuantity() > 0)
                    .map(order -> {
                        try {
                            return new OrderBookEntryDto(
                                    stockPriceMapper.toBigDecimal(order.getPrice()),
                                    order.getQuantity()
                            );
                        } catch (Exception e) {
                            log.warn("Error mapping ask order: {}", e.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            // Получаем последнюю цену
            BigDecimal lastPrice = null;
            if (orderBookResponse.hasLastPrice()) {
                lastPrice = stockPriceMapper.toBigDecimal(orderBookResponse.getLastPrice());
            }
            
            // Получаем время обновления
            // Используем текущее время как время получения данных со стакана
            Instant timestamp = Instant.now();
            
            log.debug("Retrieved order book for figi: {}, bids: {}, asks: {}", 
                    figi, bids.size(), asks.size());
            
            return new OrderBookDto(
                    figi,
                    bids,
                    asks,
                    lastPrice,
                    timestamp,
                    depth
            );
        } catch (Exception e) {
            log.error("Error getting order book for figi {}: {}", figi, e.getMessage(), e);
            throw new RuntimeException("Не удалось получить стакан заявок: " + e.getMessage(), e);
        }
    }

    @Override
    public String getBrokerName() {
        return BROKER_NAME;
    }

    @Override
    public boolean isAvailable() {
        try {
            Instant now = Instant.now();
            investApi.getInstrumentsService()
                    .getTradingSchedule("MOEX", now, now.plusSeconds(86_400))
                    .join();
            return true;
        } catch (Exception e) {
            log.warn("Tinkoff API is not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Находит инструменты по тикеру и фильтрует только доступные для торговли акции
     */
    private CompletableFuture<List<InstrumentShort>> findInstrumentsByTicker(String ticker) {
        return investApi.getInstrumentsService()
                .findInstrument(ticker)
                .thenApply(instruments -> instruments.stream()
                        .filter(instrument -> instrument.getApiTradeAvailableFlag()
                                && instrument.getInstrumentKindValue() == INSTRUMENT_KIND_SHARE
                                && ticker.equalsIgnoreCase(instrument.getTicker()))
                        .collect(Collectors.toList()));
    }
    
    /**
     * Преобразует Share в Stock с обработкой ошибок валют
     */
    private Stock mapShareToStock(Share share) {
        Currency currency;
        try {
            currency = Currency.valueOf(share.getCurrency().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Unsupported currency: {} for ticker {}", share.getCurrency(), share.getTicker());
            throw new IllegalArgumentException(
                    String.format("Unsupported currency: %s", share.getCurrency()), e);
        }
        
        return new Stock(
                share.getTicker(),
                share.getFigi(),
                share.getName(),
                share.getShareType().name(),
                currency,
                BROKER_NAME
        );
    }

    @Override
    public StocksDto getAvailableTickers() {
        log.info("Getting available tickers from Tinkoff");
        try {
            var instrumentsService = investApi.getInstrumentsService();
            var shares = instrumentsService.getTradableShares().join();
            
            List<Stock> stocks = shares.stream()
                    .map(share -> {
                        try {
                            return mapShareToStock(share);
                        } catch (IllegalArgumentException e) {
                            log.debug("Skipping stock {} due to unsupported currency: {}", 
                                    share.getTicker(), share.getCurrency());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            log.info("Found {} available tickers (filtered from {} total)", 
                    stocks.size(), shares.size());
            return new StocksDto(stocks);
        } catch (Exception e) {
            log.error("Error getting available tickers from Tinkoff: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось получить список доступных тикеров: " + e.getMessage(), e);
        }
    }

    @Override
    public BrokerOrderDto placeOrder(String accountId, CreateBrokerOrderDto createOrderDto) {
        log.info("Placing order on Tinkoff: accountId={}, figi={}, direction={}, quantity={}, price={}", 
                accountId, createOrderDto.getFigi(), createOrderDto.getDirection(), 
                createOrderDto.getQuantity(), createOrderDto.getPrice());

        if ("STOP".equalsIgnoreCase(createOrderDto.getOrderType())) {
            return placeStopOrder(accountId, createOrderDto);
        }
        
        try {
            var ordersService = investApi.getOrdersService();
            
            // Преобразуем направление
            OrderDirection direction = "BUY".equalsIgnoreCase(createOrderDto.getDirection()) 
                    ? OrderDirection.ORDER_DIRECTION_BUY 
                    : OrderDirection.ORDER_DIRECTION_SELL;
            
            OrderType orderType = resolveExchangeOrderType(createOrderDto.getOrderType());
            
            // Преобразуем цену в Quotation (если указана)
            Quotation price = null;
            if (orderType == OrderType.ORDER_TYPE_LIMIT) {
                if (createOrderDto.getPrice() == null) {
                    throw new IllegalArgumentException("Цена обязательна для лимитной заявки");
                }
                price = MapperUtils.bigDecimalToQuotation(createOrderDto.getPrice());
            }
            
            // Генерируем уникальный идентификатор заявки на стороне клиента
            String clientOrderId = UUID.randomUUID().toString();
            
            // Выставляем заявку (для рыночной заявки price может быть null)
            CompletableFuture<PostOrderResponse> orderFuture = ordersService.postOrder(
                    createOrderDto.getFigi(),
                    createOrderDto.getQuantity(),
                    price,
                    direction,
                    accountId,
                    orderType,
                    clientOrderId
            );
            
            PostOrderResponse response = orderFuture.join();
            
            // Создаём DTO на основе ответа от API
            BrokerOrderDto orderDto = new BrokerOrderDto();
            orderDto.setOrderId(response.getOrderId());
            orderDto.setFigi(createOrderDto.getFigi());
            orderDto.setQuantity(createOrderDto.getQuantity());
            orderDto.setExecutedQuantity(0L); // При создании заявка ещё не исполнена
            orderDto.setPrice(createOrderDto.getPrice());
            orderDto.setDirection(createOrderDto.getDirection());
            orderDto.setOrderType(createOrderDto.getOrderType());
            orderDto.setStatus("NEW"); // Новая заявка
            orderDto.setCreatedAt(Instant.now());
            
            // Если в ответе есть сообщение, сохраняем его
            if (response.getMessage() != null && !response.getMessage().isEmpty()) {
                orderDto.setMessage(response.getMessage());
            }
            
            log.info("Order placed successfully: orderId={}, figi={}", 
                    response.getOrderId(), createOrderDto.getFigi());
            return orderDto;
            
        } catch (Exception e) {
            log.error("Error placing order on Tinkoff: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось выставить заявку: " + e.getMessage(), e);
        }
    }

    @Override
    public BrokerOrderDto amendOrder(String accountId, String orderId, AmendBrokerOrderDto amendDto) {
        if (stopOrderIds.contains(orderId)) {
            throw new UnsupportedOperationException("Изменение стоп-заявок не поддерживается");
        }
        log.info("Amending order on Tinkoff: accountId={}, orderId={}", accountId, orderId);

        try {
            OrderState current = investApi.getOrdersService().getOrderState(accountId, orderId).join();
            if (current.getOrderType() != OrderType.ORDER_TYPE_LIMIT) {
                throw new IllegalStateException("Изменение поддерживается только для LIMIT-заявок");
            }

            long quantity = amendDto.getQuantity() != null
                    ? amendDto.getQuantity()
                    : current.getLotsRequested();
            BigDecimal priceDecimal = resolveAmendPrice(current, amendDto);
            Quotation price = MapperUtils.bigDecimalToQuotation(priceDecimal);
            String idempotencyKey = UUID.randomUUID().toString();

            investApi.getOrdersService().replaceOrder(
                    orderId,
                    quantity,
                    price,
                    accountId,
                    idempotencyKey,
                    PriceType.PRICE_TYPE_CURRENCY
            ).join();

            return getOrderStatus(accountId, orderId);
        } catch (IllegalStateException | IllegalArgumentException | UnsupportedOperationException ex) {
            throw ex;
        } catch (Exception e) {
            log.error("Error amending order on Tinkoff: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось изменить заявку: " + e.getMessage(), e);
        }
    }

    private BigDecimal resolveAmendPrice(OrderState current, AmendBrokerOrderDto amendDto) {
        if (amendDto.getPrice() != null) {
            return amendDto.getPrice();
        }
        if (current.hasInitialSecurityPrice()) {
            return convertMoneyValueToBigDecimal(current.getInitialSecurityPrice());
        }
        throw new IllegalArgumentException("Цена обязательна для изменения лимитной заявки");
    }

    @Override
    public void cancelOrder(String accountId, String orderId) {
        log.info("Cancelling order on Tinkoff: accountId={}, orderId={}", accountId, orderId);

        if (stopOrderIds.contains(orderId)) {
            cancelStopOrderOnTinkoff(accountId, orderId);
            return;
        }

        try {
            var ordersService = investApi.getOrdersService();
            ordersService.cancelOrder(accountId, orderId).join();
            log.info("Exchange order cancelled successfully: orderId={}", orderId);
        } catch (Exception exchangeError) {
            log.debug("Exchange cancel failed for orderId={}, trying stop order API: {}",
                    orderId, exchangeError.getMessage());
            try {
                cancelStopOrderOnTinkoff(accountId, orderId);
            } catch (Exception stopError) {
                log.error("Error cancelling order on Tinkoff: {}", stopError.getMessage(), stopError);
                throw new RuntimeException("Не удалось отменить заявку: " + stopError.getMessage(), stopError);
            }
        }
    }

    private void cancelStopOrderOnTinkoff(String accountId, String stopOrderId) {
        investApi.getStopOrdersService().cancelStopOrder(accountId, stopOrderId).join();
        stopOrderIds.remove(stopOrderId);
        log.info("Stop order cancelled successfully: orderId={}", stopOrderId);
    }

    @Override
    public BrokerOrderDto getOrderStatus(String accountId, String orderId) {
        log.debug("Getting order status from Tinkoff: accountId={}, orderId={}", accountId, orderId);

        if (stopOrderIds.contains(orderId)) {
            return getStopOrderStatus(accountId, orderId);
        }

        try {
            var ordersService = investApi.getOrdersService();
            var orderState = ordersService.getOrderState(accountId, orderId).join();
            return convertOrderStateToDto(orderState);
        } catch (Exception exchangeError) {
            log.debug("Exchange order state unavailable for orderId={}, trying stop orders: {}",
                    orderId, exchangeError.getMessage());
            return getStopOrderStatus(accountId, orderId);
        }
    }

    private BrokerOrderDto getStopOrderStatus(String accountId, String stopOrderId) {
        try {
            var stopOrders = investApi.getStopOrdersService().getStopOrders(accountId).join();
            return stopOrders.stream()
                    .filter(stop -> stopOrderId.equals(stop.getStopOrderId()))
                    .findFirst()
                    .map(this::convertStopOrderToDto)
                    .orElseGet(() -> buildCancelledStopOrderDto(stopOrderId));
        } catch (Exception e) {
            log.error("Error getting stop order status from Tinkoff: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось получить статус стоп-заявки: " + e.getMessage(), e);
        }
    }

    private BrokerOrderDto buildCancelledStopOrderDto(String stopOrderId) {
        BrokerOrderDto dto = new BrokerOrderDto();
        dto.setOrderId(stopOrderId);
        dto.setOrderType("STOP");
        dto.setStatus("CANCELLED");
        dto.setCreatedAt(Instant.now());
        dto.setMessage("Стоп-заявка отменена или исполнена");
        return dto;
    }

    private BrokerOrderDto convertStopOrderToDto(StopOrder stopOrder) {
        BrokerOrderDto dto = new BrokerOrderDto();
        dto.setOrderId(stopOrder.getStopOrderId());
        dto.setFigi(stopOrder.getFigi());
        dto.setQuantity(stopOrder.getLotsRequested());
        dto.setExecutedQuantity(0L);
        if (stopOrder.hasPrice()) {
            dto.setPrice(convertMoneyValueToBigDecimal(stopOrder.getPrice()));
        }
        dto.setDirection(convertStopOrderDirection(stopOrder.getDirection()));
        dto.setOrderType("STOP");
        // getStopOrders возвращает только активные стоп-заявки (SDK 1.5 без поля status).
        dto.setStatus("NEW");
        if (stopOrder.hasCreateDate()) {
            var createDate = stopOrder.getCreateDate();
            dto.setCreatedAt(Instant.ofEpochSecond(createDate.getSeconds(), createDate.getNanos()));
        } else {
            dto.setCreatedAt(Instant.now());
        }
        dto.setMessage("Стоп-заявка");
        return dto;
    }

    private String convertStopOrderDirection(StopOrderDirection direction) {
        return switch (direction) {
            case STOP_ORDER_DIRECTION_BUY -> "BUY";
            case STOP_ORDER_DIRECTION_SELL -> "SELL";
            default -> direction.name();
        };
    }

    @Override
    public List<BrokerOrderDto> getActiveOrders(String accountId) {
        log.debug("Getting active orders from Tinkoff: accountId={}", accountId);
        
        try {
            var ordersService = investApi.getOrdersService();
            var orderStates = ordersService.getOrders(accountId).join();
            
            return orderStates.stream()
                    .map(this::convertOrderStateToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting active orders from Tinkoff: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось получить список активных заявок: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BrokerCandleDto> getHistoricCandles(String figi, Instant from, Instant to, String interval) {
        CandleInterval candleInterval = resolveCandleInterval(interval);
        try {
            List<HistoricCandle> list = investApi.getMarketDataService()
                    .getCandlesSync(figi, from, to, candleInterval);
            if (list == null || list.isEmpty()) {
                return Collections.emptyList();
            }
            List<BrokerCandleDto> out = new ArrayList<>(list.size());
            for (HistoricCandle c : list) {
                var ts = c.getTime();
                Instant t = Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos());
                out.add(new BrokerCandleDto(
                        t,
                        MapperUtils.quotationToBigDecimal(c.getOpen()),
                        MapperUtils.quotationToBigDecimal(c.getHigh()),
                        MapperUtils.quotationToBigDecimal(c.getLow()),
                        MapperUtils.quotationToBigDecimal(c.getClose()),
                        c.getVolume()
                ));
            }
            return out;
        } catch (Exception e) {
            log.error("Error loading historic candles for {}: {}", figi, e.getMessage(), e);
            throw new RuntimeException("Не удалось загрузить исторические свечи: " + e.getMessage(), e);
        }
    }

    private BrokerOrderDto placeStopOrder(String accountId, CreateBrokerOrderDto createOrderDto) {
        if (createOrderDto.getStopPrice() == null) {
            throw new IllegalArgumentException("stopPrice обязателен для стоп-заявки");
        }
        try {
            StopOrderDirection direction = "BUY".equalsIgnoreCase(createOrderDto.getDirection())
                    ? StopOrderDirection.STOP_ORDER_DIRECTION_BUY
                    : StopOrderDirection.STOP_ORDER_DIRECTION_SELL;

            Quotation stopPrice = MapperUtils.bigDecimalToQuotation(createOrderDto.getStopPrice());
            Quotation price = createOrderDto.getPrice() != null
                    ? MapperUtils.bigDecimalToQuotation(createOrderDto.getPrice())
                    : stopPrice;

            String stopOrderId = investApi.getStopOrdersService().postStopOrderGoodTillDate(
                    createOrderDto.getFigi(),
                    createOrderDto.getQuantity(),
                    price,
                    stopPrice,
                    direction,
                    accountId,
                    StopOrderType.STOP_ORDER_TYPE_STOP_LOSS,
                    Instant.now().plusSeconds(86_400)
            ).join();

            stopOrderIds.add(stopOrderId);

            BrokerOrderDto orderDto = new BrokerOrderDto();
            orderDto.setOrderId(stopOrderId);
            orderDto.setFigi(createOrderDto.getFigi());
            orderDto.setQuantity(createOrderDto.getQuantity());
            orderDto.setExecutedQuantity(0L);
            orderDto.setPrice(createOrderDto.getPrice());
            orderDto.setDirection(createOrderDto.getDirection());
            orderDto.setOrderType("STOP");
            orderDto.setStatus("NEW");
            orderDto.setCreatedAt(Instant.now());
            orderDto.setMessage("Стоп-заявка создана");
            return orderDto;
        } catch (Exception e) {
            log.error("Error placing stop order on Tinkoff: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось выставить стоп-заявку: " + e.getMessage(), e);
        }
    }

    private OrderType resolveExchangeOrderType(String orderType) {
        if (orderType == null || orderType.isBlank() || "LIMIT".equalsIgnoreCase(orderType)) {
            return OrderType.ORDER_TYPE_LIMIT;
        }
        if ("MARKET".equalsIgnoreCase(orderType)) {
            return OrderType.ORDER_TYPE_MARKET;
        }
        throw new IllegalArgumentException("Неподдерживаемый тип заявки для биржевого API: " + orderType);
    }

    private CandleInterval resolveCandleInterval(String interval) {
        if (interval == null || interval.isBlank()) {
            return CandleInterval.CANDLE_INTERVAL_DAY;
        }
        return switch (interval.trim().toUpperCase()) {
            case "MIN_1", "CANDLE_INTERVAL_1_MIN", "1M" -> CandleInterval.CANDLE_INTERVAL_1_MIN;
            case "MIN_5", "CANDLE_INTERVAL_5_MIN", "5M" -> CandleInterval.CANDLE_INTERVAL_5_MIN;
            case "MIN_15", "CANDLE_INTERVAL_15_MIN", "15M" -> CandleInterval.CANDLE_INTERVAL_15_MIN;
            case "HOUR", "CANDLE_INTERVAL_HOUR", "1H" -> CandleInterval.CANDLE_INTERVAL_HOUR;
            case "DAY", "CANDLE_INTERVAL_DAY", "1D", "D" -> CandleInterval.CANDLE_INTERVAL_DAY;
            default -> throw new IllegalArgumentException("Неподдерживаемый интервал свечей: " + interval);
        };
    }
    
    /**
     * Преобразует OrderState в BrokerOrderDto
     */
    private BrokerOrderDto convertOrderStateToDto(OrderState orderState) {
        BrokerOrderDto dto = new BrokerOrderDto();
        dto.setOrderId(orderState.getOrderId());
        dto.setFigi(orderState.getFigi());
        dto.setQuantity(orderState.getLotsRequested());
        dto.setExecutedQuantity(orderState.getLotsExecuted());
        
        // Преобразуем цены из MoneyValue в BigDecimal
        try {
            if (orderState.hasInitialSecurityPrice()) {
                MoneyValue initialPrice = orderState.getInitialSecurityPrice();
                dto.setPrice(convertMoneyValueToBigDecimal(initialPrice));
            }
        } catch (Exception e) {
            log.debug("Initial security price not available for order {}", orderState.getOrderId());
        }
        
        try {
            if (orderState.hasExecutedOrderPrice()) {
                MoneyValue executedPrice = orderState.getExecutedOrderPrice();
                dto.setAverageExecutionPrice(convertMoneyValueToBigDecimal(executedPrice));
            }
        } catch (Exception e) {
            log.debug("Executed order price not available for order {}", orderState.getOrderId());
        }
        
        // Преобразуем направление и тип заявки
        dto.setDirection(convertOrderDirection(orderState.getDirection()));
        dto.setOrderType(convertOrderType(orderState.getOrderType()));
        
        // Преобразуем статус исполнения
        String statusName = orderState.getExecutionReportStatus().name();
        dto.setStatus(convertExecutionReportStatus(statusName));
        
        // Устанавливаем время создания
        try {
            if (orderState.hasOrderDate()) {
                dto.setCreatedAt(Instant.ofEpochSecond(
                        orderState.getOrderDate().getSeconds(),
                        orderState.getOrderDate().getNanos()));
            }
        } catch (Exception e) {
            log.debug("Order date not available for order {}", orderState.getOrderId());
            dto.setCreatedAt(Instant.now());
        }
        
        // Если заявка исполнена (полностью или частично), устанавливаем время исполнения
        if ("EXECUTION_REPORT_STATUS_FILL".equals(statusName) 
                || "EXECUTION_REPORT_STATUS_PARTIALLYFILL".equals(statusName)) {
            // Используем текущее время, так как в OrderState нет поля executedAt
            dto.setExecutedAt(Instant.now());
        }
        
        // Формируем сообщение на основе статуса заявки
        // В OrderState нет поля message, поэтому создаем информативное сообщение из статуса
        String message = generateMessageFromStatus(statusName, orderState);
        if (message != null && !message.isEmpty()) {
            dto.setMessage(message);
        }
        
        return dto;
    }
    
    /**
     * Преобразует OrderDirection в строку
     */
    private String convertOrderDirection(OrderDirection direction) {
        return switch (direction) {
            case ORDER_DIRECTION_BUY -> "BUY";
            case ORDER_DIRECTION_SELL -> "SELL";
            default -> direction.name();
        };
    }
    
    /**
     * Преобразует OrderType в строку
     */
    private String convertOrderType(OrderType orderType) {
        return switch (orderType) {
            case ORDER_TYPE_MARKET -> "MARKET";
            case ORDER_TYPE_LIMIT -> "LIMIT";
            case ORDER_TYPE_BESTPRICE -> "BESTPRICE";
            default -> orderType.name();
        };
    }
    
    /**
     * Преобразует статус исполнения заявки из формата Tinkoff API в читаемый формат
     */
    private String convertExecutionReportStatus(String statusName) {
        if (statusName == null) {
            return "UNKNOWN";
        }
        
        return switch (statusName) {
            case "EXECUTION_REPORT_STATUS_NEW" -> "NEW";
            case "EXECUTION_REPORT_STATUS_FILL" -> "FILL";
            case "EXECUTION_REPORT_STATUS_PARTIALLYFILL" -> "PARTIALLY_FILLED";
            case "EXECUTION_REPORT_STATUS_CANCELLED" -> "CANCELLED";
            case "EXECUTION_REPORT_STATUS_REJECTED" -> "REJECTED";
            default -> statusName.replace("EXECUTION_REPORT_STATUS_", "");
        };
    }
    
    /**
     * Преобразует MoneyValue в BigDecimal
     */
    private BigDecimal convertMoneyValueToBigDecimal(MoneyValue moneyValue) {
        if (moneyValue == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(moneyValue.getUnits())
                .add(BigDecimal.valueOf(moneyValue.getNano(), 9));
    }
    
    /**
     * Генерирует сообщение на основе статуса заявки
     * В OrderState нет поля message, поэтому создаем информативное сообщение из статуса
     */
    private String generateMessageFromStatus(String statusName, OrderState orderState) {
        if (statusName == null) {
            return null;
        }
        
        return switch (statusName) {
            case "EXECUTION_REPORT_STATUS_NEW" -> "Заявка создана и ожидает исполнения";
            case "EXECUTION_REPORT_STATUS_FILL" -> {
                if (orderState.getLotsExecuted() > 0) {
                    yield String.format("Заявка полностью исполнена. Исполнено лотов: %d", 
                            orderState.getLotsExecuted());
                }
                yield "Заявка полностью исполнена";
            }
            case "EXECUTION_REPORT_STATUS_PARTIALLYFILL" -> {
                if (orderState.getLotsRequested() > 0 && orderState.getLotsExecuted() > 0) {
                    yield String.format("Заявка частично исполнена. Запрошено: %d, исполнено: %d", 
                            orderState.getLotsRequested(), orderState.getLotsExecuted());
                }
                yield "Заявка частично исполнена";
            }
            case "EXECUTION_REPORT_STATUS_CANCELLED" -> "Заявка отменена";
            case "EXECUTION_REPORT_STATUS_REJECTED" -> "Заявка отклонена брокером или биржей";
            default -> null;
        };
    }
}

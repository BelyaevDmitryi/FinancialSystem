package com.fs.adapter;

import com.fs.dto.BrokerCandleDto;
import com.fs.dto.AmendBrokerOrderDto;
import com.fs.dto.BrokerOrderDto;
import com.fs.dto.CreateBrokerOrderDto;
import com.fs.dto.FigiesDto;
import com.fs.dto.OrderBookDto;
import com.fs.dto.StockPrice;
import com.fs.dto.StocksDto;
import com.fs.dto.StocksPricesDto;
import com.fs.dto.TickersDto;
import com.fs.model.Currency;
import com.fs.model.Stock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Мок-адаптер брокера для тестирования.
 * MARKET исполняется сразу; LIMIT и STOP остаются NEW до пересечения цены.
 */
@Component
@Primary
@Profile({"test", "mock-broker"})
@Slf4j
public class MockBrokerAdapter implements BrokerAdapter {

    private static final BigDecimal DEFAULT_MARKET_PRICE = BigDecimal.valueOf(100);
    private static final long SYNTHETIC_CANDLE_VOLUME = 1_000L;
    private static final int PRICE_SCALE = 4;

    private final Map<String, MockOrder> orders = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> marketPrices = new ConcurrentHashMap<>();

    @Override
    public String getBrokerName() {
        return "MOCK";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public BrokerOrderDto placeOrder(String accountId, CreateBrokerOrderDto dto) {
        log.info("Mock broker: placing order figi={}, accountId={}, direction={}, qty={}, type={}",
                dto.getFigi(), accountId, dto.getDirection(), dto.getQuantity(), dto.getOrderType());
        String orderType = normalizeOrderType(dto.getOrderType());
        validateOrderRequest(dto, orderType);

        String orderId = "MOCK-" + UUID.randomUUID();
        BigDecimal marketPrice = marketPriceFor(dto.getFigi());
        MockOrder order = new MockOrder(
                orderId,
                dto.getFigi(),
                dto.getQuantity(),
                dto.getPrice(),
                dto.getStopPrice(),
                dto.getDirection(),
                orderType,
                "NEW",
                Instant.now(),
                null,
                "mock order accepted"
        );
        orders.put(orderId, order);

        if ("MARKET".equals(orderType)) {
            fillAtMarket(order, marketPrice);
        } else {
            tryFillPendingOrder(order, marketPrice);
        }
        return toDto(order);
    }

    @Override
    public BrokerOrderDto amendOrder(String accountId, String orderId, AmendBrokerOrderDto amendDto) {
        log.info("Mock broker: amending order {}", orderId);
        MockOrder order = orders.get(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Заявка не найдена: " + orderId);
        }
        if (!"LIMIT".equals(order.orderType)) {
            throw new IllegalStateException("Изменение поддерживается только для LIMIT-заявок");
        }
        if (!"NEW".equals(order.status)) {
            throw new IllegalStateException("Нельзя изменить заявку в статусе " + order.status);
        }
        if (amendDto.getPrice() != null) {
            order.price = amendDto.getPrice();
        }
        if (amendDto.getQuantity() != null) {
            order.quantity = amendDto.getQuantity();
        }
        order.message = "mock order amended";
        return toDto(order);
    }

    @Override
    public void cancelOrder(String accountId, String orderId) {
        log.info("Mock broker: cancelling order {}", orderId);
        MockOrder order = orders.get(orderId);
        if (order == null) {
            return;
        }
        if ("FILL".equals(order.status) || "CANCELLED".equals(order.status)) {
            return;
        }
        order.status = "CANCELLED";
        order.message = "mock order cancelled";
    }

    @Override
    public BrokerOrderDto getOrderStatus(String accountId, String orderId) {
        MockOrder order = orders.get(orderId);
        if (order == null) {
            return new BrokerOrderDto(
                    orderId,
                    null,
                    null,
                    null,
                    DEFAULT_MARKET_PRICE,
                    DEFAULT_MARKET_PRICE,
                    null,
                    "NEW",
                    null,
                    Instant.now(),
                    null,
                    "mock order not found"
            );
        }
        return toDto(order);
    }

    @Override
    public List<BrokerOrderDto> getActiveOrders(String accountId) {
        return orders.values().stream()
                .filter(order -> "NEW".equals(order.status) || "PARTIALLY_FILLED".equals(order.status))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Обновляет рыночную цену по FIGI и проверяет исполнение отложенных заявок (для тестов и симуляции).
     */
    public void setMarketPrice(String figi, BigDecimal price) {
        marketPrices.put(figi, price);
        orders.values().stream()
                .filter(order -> figi.equals(order.figi))
                .filter(order -> "NEW".equals(order.status))
                .forEach(order -> tryFillPendingOrder(order, price));
    }

    @Override
    public StocksPricesDto getPrices(FigiesDto figiesDto) {
        List<StockPrice> prices = figiesDto.getFigies().stream()
                .map(figi -> new StockPrice(figi, marketPriceFor(figi)))
                .collect(Collectors.toList());
        return new StocksPricesDto(prices);
    }

    @Override
    public Stock getStockByTicker(String ticker) {
        return new Stock(ticker, "MOCK-FIGI-" + ticker, "MOCK " + ticker, "STOCK",
                Currency.RUB, "MOCK");
    }

    @Override
    public StocksDto getStocksByTickers(TickersDto tickersDto) {
        List<Stock> stocks = tickersDto.getTickers().stream()
                .map(this::getStockByTicker)
                .collect(Collectors.toList());
        return new StocksDto(stocks);
    }

    @Override
    public OrderBookDto getOrderBook(String figi, int depth) {
        BigDecimal price = marketPriceFor(figi);
        return new OrderBookDto(figi, new ArrayList<>(), new ArrayList<>(),
                price, Instant.now(), depth);
    }

    @Override
    public StocksDto getAvailableTickers() {
        return new StocksDto(Collections.emptyList());
    }

    @Override
    public List<BrokerCandleDto> getHistoricCandles(String figi, Instant from, Instant to,
            String interval) {
        if (from == null || to == null || from.isAfter(to)) {
            return Collections.emptyList();
        }
        BigDecimal base = marketPriceFor(figi);
        ZoneOffset utc = ZoneOffset.UTC;
        LocalDate startDay = LocalDate.ofInstant(from, utc);
        LocalDate endDay = LocalDate.ofInstant(to, utc);
        List<BrokerCandleDto> candles = new ArrayList<>();
        int dayIndex = 0;
        for (LocalDate day = startDay; !day.isAfter(endDay); day = day.plusDays(1), dayIndex++) {
            // Циклические колебания ±2% вокруг base: close < SMA при открытой позиции (ose-02 E2E).
            double variation = ((dayIndex % 5) - 2) * 0.01;
            BigDecimal close = base.multiply(BigDecimal.valueOf(1.0 + variation))
                    .setScale(PRICE_SCALE, RoundingMode.HALF_UP);
            BigDecimal open = close;
            BigDecimal high = close.multiply(new BigDecimal("1.005"))
                    .setScale(PRICE_SCALE, RoundingMode.HALF_UP);
            BigDecimal low = close.multiply(new BigDecimal("0.995"))
                    .setScale(PRICE_SCALE, RoundingMode.HALF_UP);
            Instant barTime = day.atStartOfDay().toInstant(utc);
            candles.add(new BrokerCandleDto(barTime, open, high, low, close, SYNTHETIC_CANDLE_VOLUME));
        }
        log.debug("Mock broker: generated {} synthetic DAY candles for figi={} {}..{}",
                candles.size(), figi, startDay, endDay);
        return candles;
    }

    private void validateOrderRequest(CreateBrokerOrderDto dto, String orderType) {
        if ("LIMIT".equals(orderType) && dto.getPrice() == null) {
            throw new IllegalArgumentException("Цена обязательна для лимитной заявки");
        }
        if ("STOP".equals(orderType) && dto.getStopPrice() == null) {
            throw new IllegalArgumentException("stopPrice обязателен для стоп-заявки");
        }
    }

    private String normalizeOrderType(String orderType) {
        if (orderType == null || orderType.isBlank()) {
            return "LIMIT";
        }
        return orderType.trim().toUpperCase();
    }

    private BigDecimal marketPriceFor(String figi) {
        return marketPrices.getOrDefault(figi, DEFAULT_MARKET_PRICE);
    }

    private void fillAtMarket(MockOrder order, BigDecimal marketPrice) {
        order.status = "FILL";
        order.executedQuantity = order.quantity;
        order.averageExecutionPrice = marketPrice;
        order.executedAt = Instant.now();
        order.message = "mock market order executed";
    }

    private void tryFillPendingOrder(MockOrder order, BigDecimal marketPrice) {
        if (!"NEW".equals(order.status)) {
            return;
        }
        boolean triggered = switch (order.orderType) {
            case "LIMIT" -> isLimitFillable(order, marketPrice);
            case "STOP" -> isStopTriggered(order, marketPrice);
            default -> false;
        };
        if (triggered) {
            fillAtMarket(order, marketPrice);
        }
    }

    private boolean isLimitFillable(MockOrder order, BigDecimal marketPrice) {
        if (order.price == null) {
            return false;
        }
        if ("BUY".equalsIgnoreCase(order.direction)) {
            return marketPrice.compareTo(order.price) <= 0;
        }
        return marketPrice.compareTo(order.price) >= 0;
    }

    private boolean isStopTriggered(MockOrder order, BigDecimal marketPrice) {
        if (order.stopPrice == null) {
            return false;
        }
        if ("BUY".equalsIgnoreCase(order.direction)) {
            return marketPrice.compareTo(order.stopPrice) >= 0;
        }
        return marketPrice.compareTo(order.stopPrice) <= 0;
    }

    private BrokerOrderDto toDto(MockOrder order) {
        return new BrokerOrderDto(
                order.orderId,
                order.figi,
                order.quantity,
                order.executedQuantity != null ? order.executedQuantity : 0L,
                order.price,
                order.averageExecutionPrice,
                order.direction,
                order.status,
                order.orderType,
                order.createdAt,
                order.executedAt,
                order.message
        );
    }

    private static final class MockOrder {
        private final String orderId;
        private final String figi;
        private Long quantity;
        private BigDecimal price;
        private final BigDecimal stopPrice;
        private final String direction;
        private final String orderType;
        private String status;
        private final Instant createdAt;
        private Instant executedAt;
        private String message;
        private Long executedQuantity;
        private BigDecimal averageExecutionPrice;

        private MockOrder(String orderId, String figi, Long quantity, BigDecimal price, BigDecimal stopPrice,
                String direction, String orderType, String status, Instant createdAt, Instant executedAt,
                String message) {
            this.orderId = orderId;
            this.figi = figi;
            this.quantity = quantity;
            this.price = price;
            this.stopPrice = stopPrice;
            this.direction = direction;
            this.orderType = orderType;
            this.status = status;
            this.createdAt = createdAt;
            this.executedAt = executedAt;
            this.message = message;
        }
    }
}

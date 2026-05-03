package com.fs.controller;

import com.fs.dto.BrokerOrderDto;
import com.fs.dto.CreateBrokerOrderDto;
import com.fs.dto.FigiesDto;
import com.fs.dto.HistoricCandlesDto;
import com.fs.dto.OrderBookDto;
import com.fs.dto.StocksDto;
import com.fs.dto.StocksPricesDto;
import com.fs.dto.TickersDto;
import com.fs.model.Stock;
import com.fs.service.BrokerIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/broker")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Broker Integration Controller", description = "API для интеграции с различными брокерами")
public class StockController {
    
    private final BrokerIntegrationService brokerIntegrationService;

    @GetMapping("/stocks/{ticker}")
    @Operation(summary = "Получить информацию об акции по тикеру")
    public Stock getStock(
            @PathVariable String ticker,
            @Parameter(description = "Название брокера (TINKOFF, VTB, SBER и т.д.)")
            @RequestParam(required = false) String broker) {
        log.info("Getting stock {} from broker {}", ticker, broker != null ? broker : "default");
        if (broker != null) {
            return brokerIntegrationService.getStockByTicker(ticker, broker);
        }
        return brokerIntegrationService.getStockByTicker(ticker);
    }

    @PostMapping("/stocks/getStocksByTickers")
    @Operation(summary = "Получить информацию об акциях по списку тикеров")
    public StocksDto getStocksByTickers(
            @RequestBody TickersDto tickersDto,
            @Parameter(description = "Название брокера")
            @RequestParam(required = false) String broker) {
        log.info("Getting stocks {} from broker {}", tickersDto.getTickers(), broker != null ? broker : "default");
        if (broker != null) {
            return brokerIntegrationService.getStocksByTickers(tickersDto, broker);
        }
        return brokerIntegrationService.getStocksByTickers(tickersDto);
    }

    @GetMapping("/stocks/available")
    @Operation(summary = "Получить список всех доступных тикеров с биржи")
    public ResponseEntity<StocksDto> getAvailableTickers(
            @Parameter(description = "Название брокера")
            @RequestParam(required = false) String broker) {
        log.info("Getting available tickers from broker {}", broker != null ? broker : "default");
        StocksDto stocks;
        if (broker != null) {
            stocks = brokerIntegrationService.getAvailableTickers(broker);
        } else {
            stocks = brokerIntegrationService.getAvailableTickers();
        }
        return ResponseEntity.ok(stocks);
    }

    @GetMapping("/history/candles")
    @Operation(summary = "Исторические свечи по FIGI (UTC)")
    public HistoricCandlesDto getHistoricCandles(
            @RequestParam String figi,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @Parameter(description = "DAY, HOUR, MIN_15, MIN_5, MIN_1")
            @RequestParam(required = false, defaultValue = "DAY") String interval,
            @RequestParam(required = false) String broker) {
        log.info("Historic candles figi={} from={} to={} interval={}", figi, from, to, interval);
        if (broker != null) {
            return brokerIntegrationService.getHistoricCandles(figi, from, to, interval, broker);
        }
        return brokerIntegrationService.getHistoricCandles(figi, from, to, interval);
    }

    @PostMapping("/prices")
    @Operation(summary = "Получить цены по списку FIGI")
    public StocksPricesDto getPrices(
            @RequestBody FigiesDto figiesDto,
            @Parameter(description = "Название брокера")
            @RequestParam(required = false) String broker) {
        log.info("Getting prices for {} figies from broker {}", figiesDto.getFigies().size(), broker != null ? broker : "default");
        if (broker != null) {
            return brokerIntegrationService.getPrices(figiesDto, broker);
        }
        return brokerIntegrationService.getPrices(figiesDto);
    }
    
    @GetMapping("/orderbook/{figi}")
    @Operation(summary = "Получить стакан заявок (order book) по FIGI")
    public ResponseEntity<OrderBookDto> getOrderBook(
            @PathVariable String figi,
            @Parameter(description = "Глубина стакана (количество уровней)")
            @RequestParam(defaultValue = "20") int depth,
            @Parameter(description = "Название брокера")
            @RequestParam(required = false) String broker) {
        log.info("Getting order book for figi {} with depth {} from broker {}", figi, depth, broker != null ? broker : "default");
        OrderBookDto orderBook;
        if (broker != null) {
            orderBook = brokerIntegrationService.getOrderBook(figi, depth, broker);
        } else {
            orderBook = brokerIntegrationService.getOrderBook(figi, depth);
        }
        return ResponseEntity.ok(orderBook);
    }
    
    @GetMapping("/available")
    @Operation(summary = "Получить список доступных брокеров")
    public List<String> getAvailableBrokers() {
        log.info("Getting available brokers");
        return brokerIntegrationService.getAvailableBrokers();
    }
    
    @PostMapping("/orders")
    @Operation(summary = "Выставить заявку на биржу")
    public ResponseEntity<BrokerOrderDto> placeOrder(
            @RequestHeader("X-Account-Id") String accountId,
            @Valid @RequestBody CreateBrokerOrderDto createOrderDto,
            @Parameter(description = "Название брокера")
            @RequestParam(required = false) String broker) {
        log.info("Placing order for account {} on broker {}", accountId, broker != null ? broker : "default");
        BrokerOrderDto order;
        if (broker != null) {
            order = brokerIntegrationService.placeOrder(accountId, createOrderDto, broker);
        } else {
            order = brokerIntegrationService.placeOrder(accountId, createOrderDto);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
    
    @PostMapping("/orders/{orderId}/cancel")
    @Operation(summary = "Отменить заявку на бирже")
    public ResponseEntity<Void> cancelOrder(
            @RequestHeader("X-Account-Id") String accountId,
            @PathVariable String orderId,
            @Parameter(description = "Название брокера")
            @RequestParam(required = false) String broker) {
        log.info("Cancelling order {} for account {} on broker {}", orderId, accountId, broker != null ? broker : "default");
        if (broker != null) {
            brokerIntegrationService.cancelOrder(accountId, orderId, broker);
        } else {
            brokerIntegrationService.cancelOrder(accountId, orderId);
        }
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Получить статус заявки")
    public ResponseEntity<BrokerOrderDto> getOrderStatus(
            @RequestHeader("X-Account-Id") String accountId,
            @PathVariable String orderId,
            @Parameter(description = "Название брокера")
            @RequestParam(required = false) String broker) {
        log.info("Getting order status {} for account {} on broker {}", orderId, accountId, broker != null ? broker : "default");
        BrokerOrderDto order;
        if (broker != null) {
            order = brokerIntegrationService.getOrderStatus(accountId, orderId, broker);
        } else {
            order = brokerIntegrationService.getOrderStatus(accountId, orderId);
        }
        return ResponseEntity.ok(order);
    }
    
    @GetMapping("/orders")
    @Operation(summary = "Получить все активные заявки пользователя")
    public ResponseEntity<List<BrokerOrderDto>> getActiveOrders(
            @RequestHeader("X-Account-Id") String accountId,
            @Parameter(description = "Название брокера")
            @RequestParam(required = false) String broker) {
        log.info("Getting active orders for account {} on broker {}", accountId, broker != null ? broker : "default");
        List<BrokerOrderDto> orders;
        if (broker != null) {
            orders = brokerIntegrationService.getActiveOrders(accountId, broker);
        } else {
            orders = brokerIntegrationService.getActiveOrders(accountId);
        }
        return ResponseEntity.ok(orders);
    }
}

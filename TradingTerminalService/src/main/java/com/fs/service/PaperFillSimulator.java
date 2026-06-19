package com.fs.service;

import com.fs.domain.Order;
import com.fs.domain.OrderStatus;
import com.fs.dto.PriceDataDto;
import com.fs.feignclient.PriceServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Симулирует исполнение paper-ордера по last price из PriceService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaperFillSimulator {

    private final PriceServiceClient priceServiceClient;

    public BigDecimal resolveFillPrice(String figi, BigDecimal fallbackPrice) {
        try {
            List<PriceDataDto> prices = priceServiceClient.getPrices(List.of(figi));
            if (prices != null && !prices.isEmpty()) {
                PriceDataDto latest = prices.get(prices.size() - 1);
                if (latest.getPrice() != null) {
                    return latest.getPrice();
                }
            }
        } catch (Exception e) {
            log.warn("Не удалось получить last price для FIGI {}: {}", figi, e.getMessage());
        }
        return fallbackPrice;
    }

    public void applyPaperFill(Order order) {
        BigDecimal fillPrice = resolveFillPrice(order.getFigi(), order.getPrice());
        order.setPrice(fillPrice);
        order.setStatus(OrderStatus.EXECUTED);
        order.setExecutedAt(LocalDateTime.now());
        order.setPaper(true);
        log.info("Paper fill: orderId={}, figi={}, price={}", order.getId(), order.getFigi(), fillPrice);
    }
}

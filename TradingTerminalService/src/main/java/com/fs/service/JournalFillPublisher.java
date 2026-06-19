package com.fs.service;

import com.fs.domain.Order;
import com.fs.domain.OrderType;
import com.fs.dto.FillDto;
import com.fs.dto.TradeSide;
import com.fs.feignclient.JournalServiceClient;
import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Публикует fill в JournalService после исполнения ордера.
 * Retry на 5xx/timeout; ошибки ingest не откатывают локальный EXECUTED.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JournalFillPublisher {

    private final JournalServiceClient journalServiceClient;

    @Retryable(
            retryFor = {FeignException.FeignServerException.class, RetryableException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2.0)
    )
    public void publishFill(Order order) {
        FillDto fill = toFillDto(order);
        journalServiceClient.recordFill(order.getUserId(), fill);
        log.info("Fill опубликован в Journal для orderId={}", order.getId());
    }

    @Recover
    public void recoverPublishFill(Exception exception, Order order) {
        log.error("Не удалось опубликовать fill в Journal для orderId={} после повторов: {}",
                order.getId(), exception.getMessage(), exception);
    }

    private FillDto toFillDto(Order order) {
        LocalDateTime executedAt = order.getExecutedAt() != null
                ? order.getExecutedAt()
                : LocalDateTime.now();
        return new FillDto(
                order.getId(),
                order.getFigi(),
                toTradeSide(order.getType()),
                order.getQuantity(),
                order.getPrice(),
                null,
                executedAt
        );
    }

    private TradeSide toTradeSide(OrderType orderType) {
        return orderType == OrderType.SELL ? TradeSide.SELL : TradeSide.BUY;
    }
}

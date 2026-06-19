package com.fs.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fs.domain.TradeSide;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FillDto(
        @JsonIgnore Long userId,
        @NotNull Long orderId,
        @NotBlank String figi,
        @NotNull TradeSide side,
        @NotNull @Positive BigDecimal quantity,
        @NotNull @Positive BigDecimal price,
        BigDecimal commission,
        @NotNull LocalDateTime executedAt
) {

    public FillDto withUserId(Long userId) {
        return new FillDto(userId, orderId, figi, side, quantity, price, commission, executedAt);
    }
}

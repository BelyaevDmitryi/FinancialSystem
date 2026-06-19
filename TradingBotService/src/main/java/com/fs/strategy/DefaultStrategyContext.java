package com.fs.strategy;

import com.fs.dto.PriceDataDto;

import java.math.BigDecimal;
import java.util.List;

public class DefaultStrategyContext implements StrategyContext {

    private final List<PriceDataDto> candles;
    private final BigDecimal positionQuantity;

    public DefaultStrategyContext(List<PriceDataDto> candles, BigDecimal positionQuantity) {
        this.candles = List.copyOf(candles);
        this.positionQuantity = positionQuantity != null ? positionQuantity : BigDecimal.ZERO;
    }

    @Override
    public List<PriceDataDto> getCandles() {
        return candles;
    }

    @Override
    public BigDecimal getPositionQuantity() {
        return positionQuantity;
    }

    @Override
    public BigDecimal getCurrentPrice() {
        if (candles.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return candles.get(candles.size() - 1).getPrice();
    }
}

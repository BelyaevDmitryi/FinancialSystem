package com.fs.strategy;

import com.fs.dto.PriceDataDto;

import java.math.BigDecimal;
import java.util.List;

public interface StrategyContext {

    List<PriceDataDto> getCandles();

    BigDecimal getPositionQuantity();

    BigDecimal getCurrentPrice();
}

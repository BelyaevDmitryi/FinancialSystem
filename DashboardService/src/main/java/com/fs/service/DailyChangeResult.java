package com.fs.service;

import java.math.BigDecimal;

public record DailyChangeResult(BigDecimal dailyChange, BigDecimal dailyChangePercent) {
}

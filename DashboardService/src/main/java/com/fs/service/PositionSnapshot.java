package com.fs.service;

import java.math.BigDecimal;

public record PositionSnapshot(String figi, BigDecimal quantity) {
}

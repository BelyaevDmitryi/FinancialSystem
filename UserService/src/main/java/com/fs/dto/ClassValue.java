package com.fs.dto;

import lombok.Value;
import com.fs.domain.Type;

import java.math.BigDecimal;

@Value
public class ClassValue {
    private Type classActive;
    private BigDecimal value;
}

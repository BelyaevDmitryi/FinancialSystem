package ru.otus.hw.dto;

import lombok.Value;
import ru.otus.hw.domain.Type;

import java.math.BigDecimal;

@Value
public class ClassValue {
    private Type classActive;
    private BigDecimal value;
}

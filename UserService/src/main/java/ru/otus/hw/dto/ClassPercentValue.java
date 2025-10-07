package ru.otus.hw.dto;

import lombok.Value;
import ru.otus.hw.domain.Type;

@Value
public class ClassPercentValue {
    private Type classActive;
    private Integer value;
}

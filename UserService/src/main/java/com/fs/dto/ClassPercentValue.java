package com.fs.dto;

import lombok.Value;
import com.fs.domain.Type;

@Value
public class ClassPercentValue {
    private Type classActive;
    private Integer value;
}

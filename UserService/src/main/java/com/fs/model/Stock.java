package com.fs.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Stock {
    private String ticker;
    private String figi;
    private String name;
    private String type;
    private Currency currency;
    private String source;
}

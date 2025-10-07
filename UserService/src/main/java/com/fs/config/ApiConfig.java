package com.fs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "api")
public class ApiConfig {
    private StockConfig stockConfig;
    private PriceConfig priceServiceConfig;
    private CurrencyConfig currencyConfig;
}

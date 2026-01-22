package com.fs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "api")
public class ApiConfig {
    @Deprecated
    private StockConfig stockConfig; // Для обратной совместимости
    private BrokerConfig brokerConfig;
    private PriceConfig priceServiceConfig;
    private CurrencyConfig currencyConfig;
}

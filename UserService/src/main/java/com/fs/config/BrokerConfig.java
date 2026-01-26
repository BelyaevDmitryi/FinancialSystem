package com.fs.config;

import lombok.Data;

@Data
public class BrokerConfig {
    private String brokerIntegrationService;
    private String getStocksByTickers;
}

package com.fs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ответ для получения счёта по умолчанию (используется Feign-клиентом TradingTerminalService).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefaultBrokerAccountDto {
    private String externalAccountId;
}

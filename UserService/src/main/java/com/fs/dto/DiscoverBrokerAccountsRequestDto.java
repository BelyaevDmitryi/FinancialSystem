package com.fs.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Запрос списка счетов у брокера по API-токену (токен не сохраняется).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscoverBrokerAccountsRequestDto {

    @NotBlank(message = "Код брокера обязателен")
    private String brokerCode;

    @NotBlank(message = "API-токен обязателен")
    private String apiToken;

    /** Токен песочницы Tinkoff Invest (иначе — боевой контур). */
    private boolean sandbox;
}

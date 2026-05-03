package com.fs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ответ UserService: счёт по умолчанию у брокера для пользователя.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefaultBrokerAccountDto {
    private String externalAccountId;
}

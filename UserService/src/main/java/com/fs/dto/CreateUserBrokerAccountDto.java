package com.fs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserBrokerAccountDto {

    @NotBlank(message = "Код брокера обязателен")
    private String brokerCode;

    @NotBlank(message = "ID счёта у брокера обязателен")
    private String externalAccountId;

    private String displayName;

    private boolean isDefault = false;
}

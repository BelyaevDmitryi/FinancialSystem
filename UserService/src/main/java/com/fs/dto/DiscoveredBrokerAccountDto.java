package com.fs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Счёт, полученный от брокера по токену (для выбора при привязке).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscoveredBrokerAccountDto {

    private String externalAccountId;
    private String name;
    private String accountType;
    private String status;
}

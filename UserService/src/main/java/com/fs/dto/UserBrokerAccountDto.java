package com.fs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBrokerAccountDto {
    private Long id;
    private Long userId;
    private BrokerDto broker;
    private String externalAccountId;
    private String displayName;
    private boolean isDefault;
    private LocalDateTime createdAt;
}

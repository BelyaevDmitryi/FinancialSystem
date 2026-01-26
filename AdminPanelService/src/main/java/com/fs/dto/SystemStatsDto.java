package com.fs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatsDto {
    private Long totalUsers;
    private Long totalOrders;
    private Long activeBots;
    private Map<String, Long> ordersByStatus;
    private Map<String, Long> botsByStrategy;
}

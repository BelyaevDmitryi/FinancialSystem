package com.fs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BotStatsDto {
    private Long activeBots;
    private Map<String, Long> botsByStrategy;
}

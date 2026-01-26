package com.fs.feignclient;

import com.fs.dto.BotStatsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "trading-bot-service")
public interface TradingBotServiceClient {

    @GetMapping("/admin/stats/bots")
    BotStatsDto getBotStats();
}

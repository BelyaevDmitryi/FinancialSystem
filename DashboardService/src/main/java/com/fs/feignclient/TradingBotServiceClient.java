package com.fs.feignclient;

import com.fs.dto.BotDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "trading-bot-service")
public interface TradingBotServiceClient {

    @GetMapping("/bots")
    List<BotDto> getUserBots(@RequestHeader("X-User-Id") String userId);
}

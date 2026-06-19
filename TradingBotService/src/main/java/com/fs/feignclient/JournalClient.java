package com.fs.feignclient;

import com.fs.config.FeignClientConfiguration;
import com.fs.dto.JournalPositionDto;
import com.fs.dto.JournalTradeDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "journal-service", configuration = FeignClientConfiguration.class)
public interface JournalClient {

    @GetMapping("/journal/trades")
    List<JournalTradeDto> getTrades(@RequestHeader("X-User-Id") Long userId);

    @GetMapping("/journal/positions/{figi}")
    JournalPositionDto getPosition(@RequestHeader("X-User-Id") Long userId, @PathVariable String figi);
}

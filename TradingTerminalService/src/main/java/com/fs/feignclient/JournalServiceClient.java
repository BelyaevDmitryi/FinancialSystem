package com.fs.feignclient;

import com.fs.config.JournalFeignClientConfiguration;
import com.fs.dto.FillDto;
import com.fs.dto.TradeDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "journal-service", configuration = JournalFeignClientConfiguration.class)
public interface JournalServiceClient {

    @PostMapping("/journal/fills")
    TradeDto recordFill(@RequestHeader("X-User-Id") Long userId, @RequestBody FillDto fill);
}

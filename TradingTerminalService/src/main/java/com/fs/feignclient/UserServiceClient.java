package com.fs.feignclient;

import com.fs.dto.DefaultBrokerAccountDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign-клиент для получения привязанных счетов пользователя у брокеров (UserService).
 */
@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/profile/broker-accounts/default")
    DefaultBrokerAccountDto getDefaultBrokerAccount(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam("broker") String broker
    );
}

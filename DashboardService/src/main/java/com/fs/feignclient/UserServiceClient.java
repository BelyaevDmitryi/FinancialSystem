package com.fs.feignclient;

import com.fs.dto.PositionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/users/{userId}/positions")
    List<PositionDto> getUserPositions(@PathVariable String userId);
}

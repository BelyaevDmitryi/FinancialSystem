package com.fs.feignclient;

import com.fs.dto.FigisRequest;
import com.fs.dto.PositionDto;
import com.fs.dto.StockDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/users/{userId}/positions")
    List<PositionDto> getUserPositions(@PathVariable String userId);

    @PostMapping("/stocks/by-figis")
    List<StockDto> getStocksByFigis(@RequestBody FigisRequest request);
}

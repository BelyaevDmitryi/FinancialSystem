package com.fs.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.fs.domain.FigiWithPrice;
import com.fs.dto.StocksDto;
import com.fs.dto.StocksWithPrices;
import com.fs.service.StockPriceService;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PriceController {
    private final StockPriceService priceService;

    @PostMapping("/prices")
    public StocksWithPrices getStocksWithPrices(@RequestBody @Valid StocksDto stocksDto) {
        log.info("Received request: {}", stocksDto.getStocks());
        StocksWithPrices result = priceService.getPrices(stocksDto);
        log.info("Received response: {}", result.getStocks());
        return result;
    }

    @PostMapping("/add")
    public FigiWithPrice addStock(@RequestBody FigiWithPrice figiWithPrice) {
        return priceService.addStock(figiWithPrice);
    }

}

package ru.otus.hw.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.otus.hw.domain.FigiWithPrice;
import ru.otus.hw.dto.StocksDto;
import ru.otus.hw.dto.StocksWithPrices;
import ru.otus.hw.service.StockPriceService;

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

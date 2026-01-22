package com.fs.controller;

import com.fs.domain.FigiWithPrice;
import com.fs.dto.GetPricesDto;
import com.fs.dto.PriceDataDto;
import com.fs.dto.StocksDto;
import com.fs.dto.StocksWithPrices;
import com.fs.service.StockPriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Price Controller", description = "API для получения цен на акции")
public class PriceController {
    private final StockPriceService priceService;

    @PostMapping("/prices")
    @Operation(summary = "Получить цены по списку акций (legacy)")
    public StocksWithPrices getStocksWithPrices(@RequestBody @Valid StocksDto stocksDto) {
        log.info("Received request: {}", stocksDto.getStocks());
        StocksWithPrices result = priceService.getPrices(stocksDto);
        log.info("Received response: {}", result.getStocks());
        return result;
    }
    
    @GetMapping("/prices")
    @Operation(summary = "Получить цены по списку FIGI")
    public List<PriceDataDto> getPricesByFigies(@RequestParam("figies") List<String> figies) {
        log.info("Getting prices for figies: {}", figies);
        return priceService.getPricesByFigies(figies);
    }

    @PostMapping("/add")
    @Operation(summary = "Добавить цену в кеш")
    public FigiWithPrice addStock(@RequestBody FigiWithPrice figiWithPrice) {
        return priceService.addStock(figiWithPrice);
    }

}

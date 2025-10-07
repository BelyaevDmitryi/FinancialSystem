package ru.otus.hw.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.otus.hw.domain.Currency;
import ru.otus.hw.domain.Position;
import ru.otus.hw.domain.Stock;
import ru.otus.hw.domain.Type;
import ru.otus.hw.domain.User;
import ru.otus.hw.dto.ClassPercentValue;
import ru.otus.hw.dto.ClassesPercentDto;
import ru.otus.hw.dto.CostDto;
import ru.otus.hw.dto.ClassValue;
import ru.otus.hw.exception.CouldntGetPricesException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticService {
    private final UserService userService;
    private final StockService stockService;
    private final PriceService priceService;
    private final CurrencyService currencyService;

    public ClassesPercentDto getStatisticOfClassesByUserId(String userId) {
        long start = System.currentTimeMillis();
        User user = userService.getUserById(userId);
        Map<String, Integer> tickersWithQuantity = getTickersWithQuantity(user);
        List<Stock> stocksInPortfolio = getStocksUser(user);

        if(stocksInPortfolio.isEmpty()) {
            throw new CouldntGetPricesException("User hasn't any stocks.");
        }

        Map<String, BigDecimal> figiesWithPrices = getPricesStocks(stocksInPortfolio);
        Map<Currency, BigDecimal> currencyRates = getRates(stocksInPortfolio);

        Map<Type, BigDecimal> classesWithCost = new HashMap<>();
        stocksInPortfolio.forEach(stock -> {
            BigDecimal cost = figiesWithPrices.get(stock.getFigi()).multiply(new BigDecimal(tickersWithQuantity.get(stock.getTicker())).multiply(currencyRates.get(stock.getCurrency())));
            if(!classesWithCost.containsKey(stock.getType())) {
                classesWithCost.put(stock.getType(), cost);
            } else {
                BigDecimal newCost = classesWithCost.get(stock.getType()).add(cost);
                classesWithCost.put(stock.getType(), newCost);
            }
        });

        BigDecimal result = BigDecimal.valueOf(classesWithCost.values()
                .stream()
                .map(BigDecimal::doubleValue).mapToDouble(Double::doubleValue).sum());

        List<ClassPercentValue> classValues = new ArrayList<>();
        classesWithCost.forEach((k,v) -> classValues.add(new ClassPercentValue(k, (int) Math.round(100 *(v.doubleValue() / result.doubleValue())))));
        log.info("Calculate time for classes stat - {}", System.currentTimeMillis() - start);
        return new ClassesPercentDto(userId, classValues);
    }

    public CostDto getCostPortfolio(String userId) {
        long start = System.currentTimeMillis();
        User user = userService.getUserById(userId);
        Map<String, Integer> tickersWithQuantity = getTickersWithQuantity(user);
        List<Stock> stocksInPortfolio = getStocksUser(user);

        if (stocksInPortfolio.isEmpty()) {
            throw new CouldntGetPricesException("User hasn't any stocks.");
        }

        Map<String, BigDecimal> figiesWithPrices = getPricesStocks(stocksInPortfolio);
        Map<Currency, BigDecimal> currencyRates = getRates(stocksInPortfolio);

        BigDecimal resultCost = getCostByStocks(stocksInPortfolio, figiesWithPrices, tickersWithQuantity, currencyRates);

        log.info("Calculate time for cost - {}", System.currentTimeMillis() - start);
        return new CostDto(resultCost);
    }

    public ClassValue getStatisticOfClassByUserId(String userId, String typeStr) {
        Type type = Type.valueOf(typeStr.toUpperCase());
        User user = userService.getUserById(userId);
        Map<String, Integer> tickersWithQuantity = getTickersWithQuantity(user);
        List<Stock> stocksInPortfolio = getStocksUser(user);

        stocksInPortfolio = stocksInPortfolio.stream().filter(s -> s.getType() == type).collect(Collectors.toList());

        if (stocksInPortfolio.isEmpty()) {
            throw new CouldntGetPricesException(String.format("User hasn't stocks with type: %s", type));
        }

        Map<String, BigDecimal> figiesWithPrices = getPricesStocks(stocksInPortfolio);
        Map<Currency, BigDecimal> currencyRates = getRates(stocksInPortfolio);

        BigDecimal resultCost = getCostByStocks(stocksInPortfolio, figiesWithPrices, tickersWithQuantity, currencyRates);
        return new ClassValue(type, resultCost);
    }

    private Map<String, Integer> getTickersWithQuantity(User user) {
        return user.getPortfolio().stream().collect(Collectors.toMap(Position::getTicker, Position::getQuantity));
    }

    private List<Stock> getStocksUser(User user) {
        return stockService.getStocksByTickers(
                user.getPortfolio().stream()
                        .map(Position::getTicker)
                        .toList());
    }

    private Map<String, BigDecimal> getPricesStocks(List<Stock> stocks) {
        return priceService.getPricesByFigies(stocks);
    }

    private Map<Currency, BigDecimal> getRates(List<Stock> stocksInPortfolio) {
        return currencyService.getRates(
                stocksInPortfolio.stream()
                        .map(Stock::getCurrency)
                        .distinct().toList());
    }

    private BigDecimal getCostByStocks(List<Stock> stocksInPortfolio, Map<String, BigDecimal> figiesWithPrices,
                                   Map<String, Integer> tickersWithQuantity, Map<Currency, BigDecimal> currencyRates) {

        return BigDecimal.valueOf(stocksInPortfolio.stream()
                .map(s -> figiesWithPrices.get(s.getFigi())
                        .multiply(BigDecimal.valueOf(tickersWithQuantity.get(s.getTicker())))
                        .multiply(currencyRates.get(s.getCurrency())))
                .map(BigDecimal::doubleValue).mapToDouble(Double::doubleValue).sum());
    }
}

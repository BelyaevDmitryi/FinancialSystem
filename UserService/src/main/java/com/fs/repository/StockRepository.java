package com.fs.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.fs.domain.Stock;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends MongoRepository<Stock, String> {
    Optional<Stock> findByTicker(String ticker);

    boolean existsByTicker(String ticker);

    List<Stock> findByTickerIn(List<String> tickers);

    Optional<Stock> deleteStockByTicker(String ticker);
}

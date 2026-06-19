package com.fs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.fs.domain.Stock;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByTicker(String ticker);

    boolean existsByTicker(String ticker);

    List<Stock> findByTickerIn(List<String> tickers);

    List<Stock> findByFigiIn(List<String> figis);

    void deleteByTicker(String ticker);
}

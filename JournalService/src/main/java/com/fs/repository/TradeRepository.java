package com.fs.repository;

import com.fs.domain.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {

    Optional<Trade> findByOrderId(Long orderId);

    List<Trade> findByUserIdOrderByExecutedAtDesc(Long userId);

    List<Trade> findByUserIdAndFigiOrderByExecutedAtDesc(Long userId, String figi);
}

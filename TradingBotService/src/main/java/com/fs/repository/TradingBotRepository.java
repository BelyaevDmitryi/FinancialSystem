package com.fs.repository;

import com.fs.domain.BotStatus;
import com.fs.domain.TradingBot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradingBotRepository extends JpaRepository<TradingBot, Long> {
    List<TradingBot> findByUserId(Long userId);
    List<TradingBot> findByUserIdAndStatus(Long userId, BotStatus status);
    List<TradingBot> findByStatus(BotStatus status);
}

package com.fs.repository;

import com.fs.domain.StoredCandle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface CandleRepository extends JpaRepository<StoredCandle, Long> {

    List<StoredCandle> findByFigiAndIntervalAndTimeBetweenOrderByTimeAsc(
            String figi, String interval, Instant from, Instant to);

    boolean existsByFigiAndIntervalAndTime(String figi, String interval, Instant time);

    /**
   * Idempotent upsert: repeated load of the same bar updates OHLCV in place.
   */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO candles (figi, candle_interval, time, open, high, low, close, volume)
            VALUES (:#{#candle.figi}, :#{#candle.interval}, :#{#candle.time},
                    :#{#candle.open}, :#{#candle.high}, :#{#candle.low}, :#{#candle.close}, :#{#candle.volume})
            ON CONFLICT (figi, candle_interval, time)
            DO UPDATE SET
                open = EXCLUDED.open,
                high = EXCLUDED.high,
                low = EXCLUDED.low,
                close = EXCLUDED.close,
                volume = EXCLUDED.volume
            """, nativeQuery = true)
    void upsert(@Param("candle") StoredCandle candle);
}

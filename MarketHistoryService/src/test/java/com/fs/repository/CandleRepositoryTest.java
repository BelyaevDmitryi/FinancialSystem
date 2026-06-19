package com.fs.repository;

import com.fs.domain.StoredCandle;
import com.fs.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** PostgreSQL via Testcontainers for ON CONFLICT upsert semantics. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class CandleRepositoryTest extends PostgresIntegrationTestBase {

    private static final String FIGI = "BBG004730N88";
    private static final String INTERVAL = "DAY";
    private static final Instant BAR_TIME = Instant.parse("2026-05-01T00:00:00Z");

    @Autowired
    private CandleRepository candleRepository;

    @Test
    void upsert_insertsNewCandle() {
        StoredCandle candle = candle(100, 105, 99, 102, 1000);

        candleRepository.upsert(candle);
        candleRepository.flush();

        List<StoredCandle> stored = candleRepository.findByFigiAndIntervalAndTimeBetweenOrderByTimeAsc(
                FIGI, INTERVAL, BAR_TIME, BAR_TIME);
        assertThat(stored).hasSize(1);
        assertThat(stored.get(0).getClose()).isEqualByComparingTo("102");
    }

    @Test
    void upsert_sameBar_isIdempotentAndUpdatesValues() {
        candleRepository.upsert(candle(100, 105, 99, 102, 1000));
        candleRepository.flush();

        candleRepository.upsert(candle(100, 110, 98, 108, 2000));
        candleRepository.flush();

        assertThat(candleRepository.count()).isEqualTo(1);

        StoredCandle updated = candleRepository.findByFigiAndIntervalAndTimeBetweenOrderByTimeAsc(
                FIGI, INTERVAL, BAR_TIME, BAR_TIME).get(0);
        assertThat(updated.getHigh()).isEqualByComparingTo("110");
        assertThat(updated.getClose()).isEqualByComparingTo("108");
        assertThat(updated.getVolume()).isEqualTo(2000L);
    }

    private static StoredCandle candle(
            BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, long volume) {
        return new StoredCandle(null, FIGI, INTERVAL, BAR_TIME, open, high, low, close, volume);
    }

    private static StoredCandle candle(
            double open, double high, double low, double close, long volume) {
        return candle(
                BigDecimal.valueOf(open),
                BigDecimal.valueOf(high),
                BigDecimal.valueOf(low),
                BigDecimal.valueOf(close),
                volume);
    }
}

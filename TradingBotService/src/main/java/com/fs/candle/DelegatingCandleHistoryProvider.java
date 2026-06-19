package com.fs.candle;

import com.fs.config.MarketHistoryCandleProperties;
import com.fs.dto.PriceDataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Primary
@RequiredArgsConstructor
@Slf4j
public class DelegatingCandleHistoryProvider implements CandleHistoryProvider {

    private final MarketHistoryCandleProperties marketHistoryCandleProperties;
    private final MarketHistoryCandleProvider marketHistoryCandleProvider;
    private final PriceServiceCandleProvider priceServiceCandleProvider;

    @Override
    public List<PriceDataDto> getCandles(String figi) {
        if (marketHistoryCandleProperties.isEnabled()) {
            List<PriceDataDto> fromHistory = marketHistoryCandleProvider.getCandles(figi);
            if (fromHistory != null && !fromHistory.isEmpty()) {
                log.debug("Свечи для FIGI {} получены из MarketHistory ({} баров)", figi, fromHistory.size());
                return fromHistory;
            }
            log.info("MarketHistory пуст или недоступен для FIGI {}; fallback на PriceService", figi);
        }
        return priceServiceCandleProvider.getCandles(figi);
    }
}

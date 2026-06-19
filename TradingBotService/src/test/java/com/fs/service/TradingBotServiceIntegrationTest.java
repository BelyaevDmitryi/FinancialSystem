package com.fs.service;

import com.fs.domain.BotStatus;
import com.fs.domain.BotStrategy;
import com.fs.dto.BotDto;
import com.fs.dto.CreateBotDto;
import com.fs.repository.TradingBotRepository;
import com.fs.support.TestFeignConfig;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestFeignConfig.class)
@Transactional
@EnabledIfSystemProperty(named = "docker.tests", matches = "true")
class TradingBotServiceIntegrationTest {

    @Autowired
    private TradingBotService tradingBotService;

    @Autowired
    private TradingBotRepository botRepository;

    @Test
    void createBot_savesActiveBot() {
        CreateBotDto request = new CreateBotDto(
                "BBG004730N88", "TestBot", BotStrategy.SMA_CROSSOVER,
                BigDecimal.valueOf(1000), null, null, 20, null, null);

        BotDto result = tradingBotService.createBot("42", request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(BotStatus.ACTIVE);
        assertThat(result.getFigi()).isEqualTo("BBG004730N88");
        assertThat(result.getUserId()).isEqualTo("42");
    }

    @Test
    void updateBotStatus_changesStatusToPaused() {
        CreateBotDto request = new CreateBotDto(
                "BBG004730N88", "TestBot2", BotStrategy.EMA_TREND,
                BigDecimal.valueOf(500), null, null, null, 10, null);
        BotDto created = tradingBotService.createBot("42", request);

        BotDto updated = tradingBotService.updateBotStatus("42", created.getId(), BotStatus.PAUSED);

        assertThat(updated.getStatus()).isEqualTo(BotStatus.PAUSED);
    }

    @Test
    void getUserBots_returnsCreatedBots() {
        tradingBotService.createBot("99", new CreateBotDto(
                "BBG000000001", "Bot1", BotStrategy.MACD_CROSSOVER,
                BigDecimal.valueOf(200), null, null, null, null, null));

        var bots = tradingBotService.getUserBots("99");

        assertThat(bots).hasSize(1);
        assertThat(bots.get(0).getFigi()).isEqualTo("BBG000000001");
    }
}

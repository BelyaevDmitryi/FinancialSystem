package com.fs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fs.domain.Position;
import com.fs.domain.TradeSide;
import com.fs.dto.FillDto;
import com.fs.repository.PositionRepository;
import com.fs.repository.TradeRepository;
import com.fs.support.InternalJwtTestSupport;
import com.fs.support.JournalServiceIntegrationTestBase;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Полный контур ingest fill → position в PostgreSQL (Testcontainers).
 */
@EnabledIfSystemProperty(named = "docker.tests", matches = "true")
@Transactional
class JournalFillIngestIntegrationTest extends JournalServiceIntegrationTestBase {

    private static final String FIGI = "BBG004730N88";
    private static final long USER_ID = 42L;
    private static final long ORDER_ID = 9001L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Test
    void recordFill_buy_persistsPosition() throws Exception {
        FillDto fill = new FillDto(null, ORDER_ID, FIGI, TradeSide.BUY,
                BigDecimal.TEN, BigDecimal.valueOf(150), null, LocalDateTime.now());

        mockMvc.perform(post("/journal/fills")
                        .header("X-User-Id", Long.toString(USER_ID))
                        .header("X-Gateway-Internal-Jwt", InternalJwtTestSupport.internalToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fill)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(ORDER_ID));

        Position position = positionRepository.findByUserIdAndFigi(USER_ID, FIGI).orElseThrow();
        assertThat(position.getQuantity()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(position.getAvgPrice()).isEqualByComparingTo("150");
    }

    @Test
    void recordFill_duplicateOrderId_isIdempotent() throws Exception {
        FillDto fill = new FillDto(null, ORDER_ID, FIGI, TradeSide.BUY,
                BigDecimal.TEN, BigDecimal.valueOf(150), null, LocalDateTime.now());
        String body = objectMapper.writeValueAsString(fill);
        String token = InternalJwtTestSupport.internalToken();

        mockMvc.perform(post("/journal/fills")
                        .header("X-User-Id", Long.toString(USER_ID))
                        .header("X-Gateway-Internal-Jwt", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/journal/fills")
                        .header("X-User-Id", Long.toString(USER_ID))
                        .header("X-Gateway-Internal-Jwt", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        assertThat(tradeRepository.findByOrderId(ORDER_ID)).isPresent();
        assertThat(tradeRepository.count()).isEqualTo(1);
        assertThat(positionRepository.findByUserIdAndFigi(USER_ID, FIGI).orElseThrow().getQuantity())
                .isEqualByComparingTo(BigDecimal.TEN);
    }
}

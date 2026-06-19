package com.fs.controller;

import com.fs.config.MethodSecurityConfig;
import com.fs.config.TestSecurityConfig;
import com.fs.dto.FillDto;
import com.fs.dto.TradeDto;
import com.fs.domain.TradeSide;
import com.fs.security.JwtAuthenticationFilter;
import com.fs.service.JournalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({JournalController.class, FillIngestController.class})
@Import({TestSecurityConfig.class, MethodSecurityConfig.class})
@ActiveProfiles("test")
class JournalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JournalService journalService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setupJwtFilter() throws Exception {
        doAnswer((InvocationOnMock inv) -> {
            jakarta.servlet.FilterChain chain = inv.getArgument(2);
            chain.doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Nested
    class GetPositions {

        @Test
        void getPositions_withUserId_returns200() throws Exception {
            when(journalService.getPositionsForUser(42L)).thenReturn(List.of(
                    new com.fs.domain.Position(1L, 42L, "BBG004730N88", BigDecimal.TEN, BigDecimal.valueOf(150))
            ));

            mockMvc.perform(get("/journal/positions").header("X-User-Id", "42"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].figi").value("BBG004730N88"))
                    .andExpect(jsonPath("$[0].quantity").value(10))
                    .andExpect(jsonPath("$[0].avgPrice").value(150));
        }

        @Test
        void getPositionByFigi_withUserId_returns200() throws Exception {
            when(journalService.getPositionForUser(42L, "BBG004730N88"))
                    .thenReturn(new com.fs.domain.Position(1L, 42L, "BBG004730N88", BigDecimal.TEN, BigDecimal.valueOf(150)));

            mockMvc.perform(get("/journal/positions/BBG004730N88").header("X-User-Id", "42"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.figi").value("BBG004730N88"))
                    .andExpect(jsonPath("$.quantity").value(10));
        }

        @Test
        void getTrades_withUserId_returns200() throws Exception {
            when(journalService.getTradesForUser(42L)).thenReturn(List.of(
                    new TradeDto(1L, 42L, "BBG004730N88", TradeSide.BUY, BigDecimal.TEN,
                            BigDecimal.valueOf(150), null, 1001L, null, LocalDateTime.now())
            ));

            mockMvc.perform(get("/journal/trades").header("X-User-Id", "42"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].orderId").value(1001))
                    .andExpect(jsonPath("$[0].side").value("BUY"));
        }

        @Test
        void getPositions_missingUserId_returns4xx() throws Exception {
            mockMvc.perform(get("/journal/positions"))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    class RecordFill {

        @Test
        void recordFill_withInternalRole_returns200() throws Exception {
            FillDto request = new FillDto(null, 5001L, "BBG004730N88", TradeSide.BUY,
                    BigDecimal.TEN, BigDecimal.valueOf(150), null, LocalDateTime.now());
            TradeDto response = new TradeDto(1L, 42L, "BBG004730N88", TradeSide.BUY, BigDecimal.TEN,
                    BigDecimal.valueOf(150), null, 5001L, null, LocalDateTime.now());

            when(journalService.recordFill(any(FillDto.class))).thenReturn(response);

            mockMvc.perform(post("/journal/fills")
                            .with(user("trading-terminal-service").roles("INTERNAL"))
                            .header("X-User-Id", "42")
                            .header("X-Gateway-Internal-Jwt", "internal-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(5001));
        }

        @Test
        @WithMockUser(roles = "USER")
        void recordFill_withoutInternalRole_returns403() throws Exception {
            FillDto request = new FillDto(null, 5002L, "BBG004730N88", TradeSide.BUY,
                    BigDecimal.TEN, BigDecimal.valueOf(150), null, LocalDateTime.now());

            mockMvc.perform(post("/journal/fills")
                            .header("X-User-Id", "42")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }
}

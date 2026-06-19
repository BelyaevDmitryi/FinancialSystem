package com.fs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fs.config.TestSecurityConfig;
import com.fs.domain.BotStatus;
import com.fs.domain.BotStrategy;
import com.fs.dto.BotDto;
import com.fs.dto.CreateBotDto;
import com.fs.security.JwtAuthenticationFilter;
import com.fs.service.TradingBotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TradingBotController.class)
@Import(TestSecurityConfig.class)
class TradingBotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TradingBotService botService;

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
    class CreateBot {

        @Test
        void createBot_validRequest_returns201() throws Exception {
            CreateBotDto request = new CreateBotDto("BBG004730N88", "MyBot",
                    BotStrategy.SMA_CROSSOVER, BigDecimal.valueOf(1000),
                    null, null, 20, null, null);
            BotDto response = new BotDto("1", "42", "BBG004730N88", "MyBot",
                    BotStrategy.SMA_CROSSOVER, BotStatus.ACTIVE, BigDecimal.valueOf(1000),
                    null, null, 20, null, LocalDateTime.now(), null, 0, BigDecimal.ZERO, true);
            when(botService.createBot(eq("42"), any(CreateBotDto.class))).thenReturn(response);

            mockMvc.perform(post("/bots")
                            .header("X-User-Id", "42")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value("1"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.strategy").value("SMA_CROSSOVER"));
        }

        @Test
        void createBot_missingFigi_returns400() throws Exception {
            CreateBotDto request = new CreateBotDto("", "MyBot",
                    BotStrategy.SMA_CROSSOVER, BigDecimal.valueOf(1000),
                    null, null, null, null, null);

            mockMvc.perform(post("/bots")
                            .header("X-User-Id", "42")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class GetBots {

        @Test
        void getUserBots_returns200WithList() throws Exception {
            BotDto botDto = new BotDto("1", "42", "BBG004730N88", "MyBot",
                    BotStrategy.SMA_CROSSOVER, BotStatus.ACTIVE, BigDecimal.valueOf(1000),
                    null, null, 20, null, LocalDateTime.now(), null, 0, BigDecimal.ZERO, true);
            when(botService.getUserBots("42")).thenReturn(List.of(botDto));

            mockMvc.perform(get("/bots").header("X-User-Id", "42"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value("1"))
                    .andExpect(jsonPath("$[0].status").value("ACTIVE"));
        }
    }

    @Nested
    class UpdateBotStatus {

        @Test
        void updateBotStatus_validRequest_returns200() throws Exception {
            BotDto updated = new BotDto("1", "42", "BBG004730N88", "MyBot",
                    BotStrategy.SMA_CROSSOVER, BotStatus.PAUSED, BigDecimal.valueOf(1000),
                    null, null, 20, null, LocalDateTime.now(), null, 0, BigDecimal.ZERO, true);
            when(botService.updateBotStatus(eq("42"), eq("1"), eq(BotStatus.PAUSED))).thenReturn(updated);

            mockMvc.perform(put("/bots/1/status")
                            .header("X-User-Id", "42")
                            .param("status", "PAUSED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PAUSED"));
        }
    }

    @Nested
    class DeleteBot {

        @Test
        void deleteBot_returns204() throws Exception {
            mockMvc.perform(delete("/bots/1").header("X-User-Id", "42"))
                    .andExpect(status().isNoContent());
        }
    }
}

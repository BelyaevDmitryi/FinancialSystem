package com.fs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fs.config.TestConfig;
import com.fs.dto.LoginRequestDto;
import org.junit.jupiter.api.Disabled;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Disabled("Requires seeded database with 'admin'/'admin123' and pre-created 'testuser'; use docker-compose.test.yml")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
public class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;

    @BeforeEach
    public void setup() throws Exception {
        // Login as admin
        LoginRequestDto adminLoginRequest = new LoginRequestDto();
        adminLoginRequest.setUsername("admin");
        adminLoginRequest.setPassword("admin123");

        MvcResult adminResult = mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminLoginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String adminResponse = adminResult.getResponse().getContentAsString();
        adminToken = objectMapper.readTree(adminResponse).get("token").asText();

        // Login as regular user (assuming the user was created in previous tests)
        LoginRequestDto userLoginRequest = new LoginRequestDto();
        userLoginRequest.setUsername("testuser");
        userLoginRequest.setPassword("password123");

        MvcResult userResult = mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userLoginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String userResponse = userResult.getResponse().getContentAsString();
        userToken = objectMapper.readTree(userResponse).get("token").asText();
    }

    @Test
    public void testGetStockByTicker_AsAdmin() throws Exception {
        // This test might fail if the stock doesn't exist in the database
        // It's just to test the security aspect
        mockMvc.perform(get("/stocks/AAPL")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetStockByTicker_AsUser() throws Exception {
        mockMvc.perform(get("/stocks/AAPL")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetStockByTicker_Unauthorized() throws Exception {
        mockMvc.perform(get("/stocks/AAPL"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testDeleteStockByTicker_AsAdmin() throws Exception {
        // This test might fail if the stock doesn't exist in the database
        // It's just to test the security aspect
        mockMvc.perform(delete("/stocks/AAPL")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    public void testDeleteStockByTicker_AsUser() throws Exception {
        mockMvc.perform(delete("/stocks/AAPL")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetAllStocks_AsAdmin() throws Exception {
        mockMvc.perform(get("/stocks")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testGetAllStocks_AsUser() throws Exception {
        mockMvc.perform(get("/stocks")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testGetAllStocks_Unauthorized() throws Exception {
        mockMvc.perform(get("/stocks"))
                .andExpect(status().isUnauthorized());
    }
}

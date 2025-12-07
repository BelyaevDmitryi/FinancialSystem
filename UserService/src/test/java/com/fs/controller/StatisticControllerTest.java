package com.fs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fs.config.TestConfig;
import com.fs.dto.LoginRequestDto;
import com.fs.dto.SignupRequestDto;
import com.fs.dto.UserDtoCreate;
import com.fs.domain.Position;
import com.fs.repository.UserRepository;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
public class StatisticControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private String adminToken;
    private String userToken;
    private String testUserId = "statTestUser";

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

        // Create a test user with portfolio
        Set<Position> portfolio = new HashSet<>();
        portfolio.add(new Position("AAPL", 10));
        portfolio.add(new Position("MSFT", 5));

        UserDtoCreate userCreate = new UserDtoCreate(testUserId, "Stat Test User", portfolio);

        mockMvc.perform(post("/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userCreate)))
                .andExpect(status().isOk());

        // Register the user with a password
        SignupRequestDto signupRequest = new SignupRequestDto(testUserId, "Stat Test User", "password123");

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk());

        // Login as the test user
        LoginRequestDto userLoginRequest = new LoginRequestDto();
        userLoginRequest.setUsername(testUserId);
        userLoginRequest.setPassword("password123");

        MvcResult userResult = mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userLoginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String userResponse = userResult.getResponse().getContentAsString();
        userToken = objectMapper.readTree(userResponse).get("token").asText();
    }

    @AfterEach
    public void cleanup() {
        userRepository.deleteById(testUserId);
    }

    @Test
    public void testGetClassStat_AsAdmin() throws Exception {
        mockMvc.perform(get("/statistic/classes/" + testUserId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classes").isArray());
    }

    @Test
    public void testGetClassStat_AsUser() throws Exception {
        mockMvc.perform(get("/statistic/classes/" + testUserId)
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classes").isArray());
    }

    @Test
    public void testGetClassStat_Unauthorized() throws Exception {
        mockMvc.perform(get("/statistic/classes/" + testUserId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetClassStat_Forbidden() throws Exception {
        // Create another user
        String anotherUserId = "anotherStatUser";
        UserDtoCreate anotherUserCreate = new UserDtoCreate(anotherUserId, "Another Stat User", new HashSet<>());

        mockMvc.perform(post("/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(anotherUserCreate)))
                .andExpect(status().isOk());

        // Try to access another user's statistics as a regular user
        mockMvc.perform(get("/statistic/classes/" + anotherUserId)
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // Clean up
        userRepository.deleteById(anotherUserId);
    }

    @Test
    public void testGetCostPortfolio_AsAdmin() throws Exception {
        mockMvc.perform(get("/statistic/cost/" + testUserId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cost").isNumber());
    }

    @Test
    public void testGetCostPortfolio_AsUser() throws Exception {
        mockMvc.perform(get("/statistic/cost/" + testUserId)
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cost").isNumber());
    }

    @Test
    public void testGetCostPortfolio_Unauthorized() throws Exception {
        mockMvc.perform(get("/statistic/cost/" + testUserId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetClassStatByType_AsAdmin() throws Exception {
        mockMvc.perform(get("/statistic/classes/" + testUserId + "/STOCK")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").isNumber());
    }

    @Test
    public void testGetClassStatByType_AsUser() throws Exception {
        mockMvc.perform(get("/statistic/classes/" + testUserId + "/STOCK")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").isNumber());
    }

    @Test
    public void testGetClassStatByType_Unauthorized() throws Exception {
        mockMvc.perform(get("/statistic/classes/" + testUserId + "/STOCK"))
                .andExpect(status().isUnauthorized());
    }
}

package com.fs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fs.config.TestConfig;
import com.fs.dto.LoginRequestDto;
import com.fs.dto.PositionsDto;
import com.fs.dto.UserDtoCreate;
import com.fs.repository.UserRepository;
import com.fs.domain.Position;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

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

        // Create and login as regular user
        // First, create the user
        Set<Position> portfolio = new HashSet<>();
        UserDtoCreate userCreate = new UserDtoCreate("testuser", "Test User", portfolio);

        mockMvc.perform(post("/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userCreate)))
                .andExpect(status().isOk());

        // Now register the user with a password
        LoginRequestDto userLoginRequest = new LoginRequestDto();
        userLoginRequest.setUsername("testuser");
        userLoginRequest.setPassword("password123");

        // First signup to set the password
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new com.fs.dto.SignupRequestDto("testuser", "Test User", "password123"))))
                .andExpect(status().isOk());

        // Then login
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
        // Clean up test user
        userRepository.deleteById("testuser");
    }

    @Test
    public void testGetUserById_AsAdmin() throws Exception {
        mockMvc.perform(get("/users/testuser")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("testuser"))
                .andExpect(jsonPath("$.name").value("Test User"));
    }

    @Test
    public void testGetUserById_AsUser() throws Exception {
        mockMvc.perform(get("/users/testuser")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("testuser"))
                .andExpect(jsonPath("$.name").value("Test User"));
    }

    @Test
    public void testGetUserById_Unauthorized() throws Exception {
        mockMvc.perform(get("/users/testuser"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetUserById_Forbidden() throws Exception {
        // Create another user
        UserDtoCreate anotherUserCreate = new UserDtoCreate("anotheruser", "Another User", new HashSet<>());

        mockMvc.perform(post("/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(anotherUserCreate)))
                .andExpect(status().isOk());

        // Try to access another user's data as a regular user
        mockMvc.perform(get("/users/anotheruser")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // Clean up
        userRepository.deleteById("anotheruser");
    }

    @Test
    public void testAddStocksToUser() throws Exception {
        Set<Position> positions = new HashSet<>();
        positions.add(new Position("AAPL", 10));
        PositionsDto positionsDto = new PositionsDto();
        positionsDto.setPositions(positions);

        mockMvc.perform(put("/users/testuser/stocks")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(positionsDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolio[0].ticker").value("AAPL"))
                .andExpect(jsonPath("$.portfolio[0].quantity").value(10));
    }

    @Test
    public void testDeleteUser() throws Exception {
        // Create a user to delete
        UserDtoCreate userToDelete = new UserDtoCreate("userToDelete", "User To Delete", new HashSet<>());

        mockMvc.perform(post("/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userToDelete)))
                .andExpect(status().isOk());

        // Delete the user
        mockMvc.perform(delete("/users/userToDelete")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Verify the user is deleted
        mockMvc.perform(get("/users/userToDelete")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetAllUsers() throws Exception {
        mockMvc.perform(get("/users")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}

package com.fs.config;

import com.fs.domain.User;
import com.fs.repository.StockRepository;
import com.fs.repository.UserRepository;
import com.fs.service.StockApiService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public UserRepository userRepository() {
        UserRepository mockRepo = mock(UserRepository.class);

        // Mock admin user
        User adminUser = new User();
        adminUser.setId(1L);
        adminUser.setName("Administrator");
        adminUser.setPassword(passwordEncoder().encode("admin123"));
        adminUser.getRoles().add("ROLE_ADMIN");
        adminUser.getRoles().add("ROLE_USER");

        when(mockRepo.findById(1L)).thenReturn(Optional.of(adminUser));
        when(mockRepo.existsById(1L)).thenReturn(true);
        when(mockRepo.findByName("Administrator")).thenReturn(Optional.of(adminUser));
        when(mockRepo.existsByName("Administrator")).thenReturn(true);

        // Mock save method
        when(mockRepo.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return user;
        });

        // Mock findAll method
        List<User> users = new ArrayList<>();
        users.add(adminUser);
        when(mockRepo.findAll()).thenReturn(users);

        return mockRepo;
    }

    @Bean
    @Primary
    public StockRepository stockRepository() {
        return mock(StockRepository.class);
    }

    @Bean
    @Primary
    public StockApiService stockApiService() {
        return mock(StockApiService.class);
    }

    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

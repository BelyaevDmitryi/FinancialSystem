package com.fs.config;

import com.fs.domain.User;
import com.fs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Инициализатор данных для создания администратора и владельца сайта при первом запуске
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        createAdminUser();
        createOwnerUser();
    }

    private void createAdminUser() {
        if (!userRepository.existsByName("admin")) {
            User admin = new User();
            admin.setName("admin");
            admin.setNickname("admin");
            admin.setPassword(passwordEncoder.encode("admin"));
            // Дефолтный аватар не устанавливаем - будет использоваться локальная картинка
            admin.setAvatarUrl(null);
            Set<String> roles = new HashSet<>();
            roles.add("ROLE_ADMIN");
            roles.add("ROLE_USER");
            admin.setRoles(roles);
            admin.setPortfolio(new HashSet<>());
            userRepository.save(admin);
            log.info("Создан пользователь администратора: admin");
        } else {
            log.debug("Пользователь admin уже существует");
        }
    }

    private void createOwnerUser() {
        if (!userRepository.existsByName("beldyu")) {
            User owner = new User();
            owner.setName("beldyu");
            owner.setNickname("beldyu");
            owner.setPassword(passwordEncoder.encode("beldyu"));
            // Дефолтный аватар не устанавливаем - будет использоваться локальная картинка
            owner.setAvatarUrl(null);
            Set<String> roles = new HashSet<>();
            roles.add("ROLE_OWNER");
            roles.add("ROLE_ADMIN");
            roles.add("ROLE_USER");
            owner.setRoles(roles);
            owner.setPortfolio(new HashSet<>());
            userRepository.save(owner);
            log.info("Создан пользователь владельца сайта: beldyu");
        } else {
            log.debug("Пользователь beldyu уже существует");
        }
    }
}

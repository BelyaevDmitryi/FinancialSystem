package com.fs.service;

import com.fs.domain.User;
import com.fs.dto.JwtResponseDto;
import com.fs.dto.LoginRequestDto;
import com.fs.dto.SignupRequestDto;
import com.fs.exception.InvalidRefreshTokenException;
import com.fs.exception.UserAlreadyExistException;
import com.fs.repository.UserRepository;
import com.fs.security.UserDetailsServiceImpl;
import com.fs.security.jwt.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Transactional
    public void registerUser(SignupRequestDto signupRequest) {
        logger.debug("Attempting to register user with username: {}", signupRequest.getUsername());
        
        String username = signupRequest.getUsername() != null && !signupRequest.getUsername().isEmpty() 
                ? signupRequest.getUsername() 
                : signupRequest.getName();
        
        if (username == null || username.isEmpty()) {
            logger.warn("Registration failed: username is required");
            throw new IllegalArgumentException("Username is required");
        }
        
        if (signupRequest.getPassword() == null || signupRequest.getPassword().isEmpty()) {
            logger.warn("Registration failed: password is required");
            throw new IllegalArgumentException("Password is required");
        }
        
        if (userRepository.existsByName(username)) {
            logger.warn("Registration failed: username {} already exists", username);
            throw new UserAlreadyExistException("Error: Username is already taken!");
        }

        try {
            User user = new User();
            user.setName(username);
            user.setNickname(username); // По умолчанию nickname = username
            String encodedPassword = passwordEncoder.encode(signupRequest.getPassword());
            user.setPassword(encodedPassword);
            
            // Дефолтный аватар не устанавливаем - будет использоваться локальная картинка
            user.setAvatarUrl(null);
            
            Set<String> roles = new HashSet<>();
            roles.add("ROLE_USER");
            user.setRoles(roles);
            
            // Portfolio инициализируется автоматически в конструкторе User
            // Убедимся, что portfolio не null для корректной работы Hibernate
            if (user.getPortfolio() == null) {
                user.setPortfolio(new HashSet<>());
            }

            logger.debug("Saving user to database: username={}, hasRoles={}, hasPortfolio={}", 
                    username, !user.getRoles().isEmpty(), user.getPortfolio() != null);
            
            User savedUser = userRepository.save(user);
            logger.info("User {} registered successfully with id: {}", username, savedUser.getId());
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            logger.error("Data integrity violation while registering user {}: {}", username, e.getMessage(), e);
            throw e;
        } catch (jakarta.persistence.PersistenceException e) {
            logger.error("Persistence error while registering user {}: {}", username, e.getMessage(), e);
            throw new RuntimeException("Database error during registration: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error registering user {}: {}", username, e.getMessage(), e);
            logger.error("Exception type: {}, cause: {}", e.getClass().getName(), 
                    e.getCause() != null ? e.getCause().getClass().getName() : "null");
            throw new RuntimeException("Registration failed: " + e.getMessage(), e);
        }
    }

    /**
     * Аутентификация пользователя. Возвращает access-токен.
     */
    public String authenticateUser(LoginRequestDto loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsServiceImpl.UserPrincipal userPrincipal = (UserDetailsServiceImpl.UserPrincipal) authentication.getPrincipal();
        return jwtUtils.generateToken(userPrincipal);
    }

    /**
     * Генерация refresh-токена для пользователя (после успешного логина).
     */
    public String generateRefreshToken(org.springframework.security.core.userdetails.UserDetails userDetails) {
        return jwtUtils.generateRefreshToken(userDetails);
    }

    /**
     * Обмен refresh-токена на новую пару access + refresh (продление сессии без повторного ввода пароля).
     * При каждом успешном вызове выдаётся новая пара; старый refresh остаётся валидным до истечения TTL (stateless JWT).
     *
     * @param refreshToken JWT с claim {@code type=refresh}
     * @return только токены ({@link JwtResponseDto#tokensOnly})
     * @throws InvalidRefreshTokenException если токен null, пустой, невалидный или истёкший
     */
    public JwtResponseDto refreshTokens(String refreshToken) {
        if (refreshToken == null || !jwtUtils.validateRefreshToken(refreshToken)) {
            logger.warn("Refresh token rejected: invalid or expired");
            throw new InvalidRefreshTokenException("Невалидный или истёкший refresh token");
        }
        String userId = jwtUtils.extractUserId(refreshToken);
        UserDetailsServiceImpl.UserPrincipal userPrincipal = (UserDetailsServiceImpl.UserPrincipal)
                userDetailsService.loadUserById(Long.parseLong(userId));
        String newAccess = jwtUtils.generateToken(userPrincipal);
        String newRefresh = jwtUtils.generateRefreshToken(userPrincipal);
        logger.info("Tokens refreshed for user id {}", userId);
        return JwtResponseDto.tokensOnly(newAccess, newRefresh, jwtUtils.getExpirationMs());
    }

    public long getAccessTokenExpirationMs() {
        return jwtUtils.getExpirationMs();
    }
}


package com.fs.service;

import com.fs.domain.User;
import com.fs.dto.LoginRequestDto;
import com.fs.dto.SignupRequestDto;
import com.fs.exception.UserAlreadyExistException;
import com.fs.repository.UserRepository;
import com.fs.security.UserDetailsServiceImpl;
import com.fs.security.jwt.JwtUtils;
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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Transactional
    public void registerUser(SignupRequestDto signupRequest) {
        if (userRepository.existsById(signupRequest.getUsername())) {
            throw new UserAlreadyExistException("Error: Username is already taken!");
        }

        User user = new User();
        user.setId(signupRequest.getUsername());
        user.setName(signupRequest.getName());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        
        Set<String> roles = new HashSet<>();
        roles.add("ROLE_USER");
        user.setRoles(roles);
        
        if (user.getPortfolio() == null) {
            user.setPortfolio(new HashSet<>());
        }

        userRepository.save(user);
    }

    public String authenticateUser(LoginRequestDto loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsServiceImpl.UserPrincipal userPrincipal = (UserDetailsServiceImpl.UserPrincipal) authentication.getPrincipal();
        return jwtUtils.generateToken(userPrincipal);
    }
}


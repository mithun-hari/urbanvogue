package com.urbanvogue.auth_service.service;

import com.urbanvogue.auth_service.dto.LoginResponse;
import com.urbanvogue.auth_service.model.Role;
import com.urbanvogue.auth_service.model.User;
import com.urbanvogue.auth_service.repository.UserRepository;
import com.urbanvogue.auth_service.security.JwtService;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public String register(String email, String password, String username) {

        logger.info("Register request received for email: {} and username: {}", email, username);

        if (userRepository.findByEmail(email).isPresent()) {

            logger.error("Registration failed. User already exists: {}", email);

            return "User already exists";
        }


        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(Role.USER)
                .build();

        userRepository.save(user);

        logger.info("User registered successfully: {}", username);

        return "User registered successfully";
    }

    public LoginResponse login(String email, String password) {

        logger.info("Login attempt for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {

                    logger.error("Login failed. User not found: {}", email);

                    return new RuntimeException("User not found");
                });

        if (!passwordEncoder.matches(password, user.getPassword())) {

            logger.error("Login failed. Invalid credentials for email: {}", email);

            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole());

        logger.info("Login successful for email: {}", email);

        return new LoginResponse(token);
    }
}
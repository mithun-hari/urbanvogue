package com.urbanvogue.auth_service.service;

import com.urbanvogue.auth_service.model.User;
import com.urbanvogue.auth_service.repository.UserRepository;
import com.urbanvogue.auth_service.security.JwtService;
import com.urbanvogue.auth_service.dto.LoginResponse;


import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public String register(String email, String password) {

        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("User already exists");
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .role("USER")
                .build();

        userRepository.save(user);

        return "User registered successfully";
    }

    public LoginResponse login(String email, String password) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        // Generate JWT token after successful login
        String token = jwtService.generateToken(user.getEmail(), user.getRole());

        return new LoginResponse(token);
    }
}
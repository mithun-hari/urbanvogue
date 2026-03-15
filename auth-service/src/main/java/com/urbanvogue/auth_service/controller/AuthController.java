package com.urbanvogue.auth_service.controller;

import com.urbanvogue.auth_service.dto.LoginRequest;
import com.urbanvogue.auth_service.dto.LoginResponse;
import com.urbanvogue.auth_service.dto.RegisterRequest;
import com.urbanvogue.auth_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/test")
    public String test() {
        return "Protected Endpoint";
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        String result = authService.register(request.getEmail(), request.getPassword(), request.getUsername());
        if ("User already exists".equals(result)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.getEmail(), request.getPassword());
    }
}
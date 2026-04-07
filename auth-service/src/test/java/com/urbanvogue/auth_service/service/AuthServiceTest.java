package com.urbanvogue.auth_service.service;

import com.urbanvogue.auth_service.dto.LoginResponse;
import com.urbanvogue.auth_service.model.Role;
import com.urbanvogue.auth_service.model.User;
import com.urbanvogue.auth_service.repository.UserRepository;
import com.urbanvogue.auth_service.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = User.builder()
                .id(1L)
                .username("mithun")
                .email("mithun@example.com")
                .password("encoded_password")
                .role(Role.USER)
                .build();
    }

    @Test
    @DisplayName("register - Success: saves user with encoded password")
    void register_Success() {
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("rawPassword")).thenReturn("encoded_pass");

        String result = authService.register("new@example.com", "rawPassword", "newuser");

        assertEquals("User registered successfully", result);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User savedUser = captor.getValue();
        assertEquals("new@example.com", savedUser.getEmail());
        assertEquals("newuser", savedUser.getUsername());
        assertEquals("encoded_pass", savedUser.getPassword());
        assertEquals(Role.USER, savedUser.getRole());
    }

    @Test
    @DisplayName("register - User already exists: returns error message without saving")
    void register_UserAlreadyExists_ReturnsMessage() {
        when(userRepository.findByEmail("mithun@example.com")).thenReturn(Optional.of(existingUser));

        String result = authService.register("mithun@example.com", "pass", "mithun");

        assertEquals("User already exists", result);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login - Success: returns JWT token")
    void login_Success() {
        when(userRepository.findByEmail("mithun@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("correctPassword", "encoded_password")).thenReturn(true);
        when(jwtService.generateToken("mithun@example.com", Role.USER, 1L)).thenReturn("jwt-token-123");

        LoginResponse response = authService.login("mithun@example.com", "correctPassword");

        assertNotNull(response);
        assertEquals("jwt-token-123", response.getToken());
    }

    @Test
    @DisplayName("login - User not found: throws RuntimeException")
    void login_UserNotFound_Throws() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                authService.login("unknown@example.com", "pass"));

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    @DisplayName("login - Wrong password: throws RuntimeException")
    void login_WrongPassword_Throws() {
        when(userRepository.findByEmail("mithun@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrongPassword", "encoded_password")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                authService.login("mithun@example.com", "wrongPassword"));

        assertEquals("Invalid credentials", ex.getMessage());
    }
}

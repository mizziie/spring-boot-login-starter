package com.example.javalogin.controller;

import com.example.javalogin.config.SecurityConfig;
import com.example.javalogin.dto.LoginRequest;
import com.example.javalogin.dto.SignupRequest;
import com.example.javalogin.dto.UserResponse;
import com.example.javalogin.entity.Role;
import com.example.javalogin.entity.User;
import com.example.javalogin.entity.VerificationToken;
import com.example.javalogin.service.AuditLogService;
import com.example.javalogin.service.EmailVerificationService;
import com.example.javalogin.service.JwtService;
import com.example.javalogin.service.LoginAttemptService;
import com.example.javalogin.service.RefreshTokenService;
import com.example.javalogin.service.UserDetailsServiceImpl;
import com.example.javalogin.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@SuppressWarnings("null")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private UserService userService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private LoginAttemptService loginAttemptService;

    @MockBean
    private AuditLogService auditLogService;

    @MockBean
    private EmailVerificationService emailVerificationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @Test
    void register_shouldReturnCreatedUser() throws Exception {
        SignupRequest request = new SignupRequest("john", "password123", "john@example.com", "John Doe");

        UserResponse response = UserResponse.builder()
                .id(1L)
                .username("john")
                .email("john@example.com")
                .fullName("John Doe")
                .role(Role.USER)
                .build();

        User savedUser = User.builder()
                .id(1L)
                .username("john")
                .email("john@example.com")
                .build();

        VerificationToken token = VerificationToken.builder()
                .token("verify-token")
                .build();

        when(userService.registerUser(any(SignupRequest.class), isNull())).thenReturn(response);
        when(userService.findByUsername("john")).thenReturn(Optional.of(savedUser));
        when(emailVerificationService.createVerificationToken(any())).thenReturn(token);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("john"));
    }

    @Test
    void login_shouldReturnUserWhenCredentialsValid() throws Exception {
        LoginRequest request = new LoginRequest("admin", "admin123");

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                );

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(loginAttemptService.isBlocked(any(), any())).thenReturn(false);

        UserResponse response = UserResponse.builder()
                .id(1L)
                .username("admin")
                .email("admin@example.com")
                .fullName("Administrator")
                .role(Role.ADMIN)
                .emailVerified(true)
                .build();

        when(userService.getCurrentUser()).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    void login_shouldReturnErrorWhenCredentialsInvalid() throws Exception {
        LoginRequest request = new LoginRequest("admin", "wrongpass");

        when(loginAttemptService.isBlocked(any(), any())).thenReturn(false);
        when(authenticationManager.authenticate(any()))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }
}

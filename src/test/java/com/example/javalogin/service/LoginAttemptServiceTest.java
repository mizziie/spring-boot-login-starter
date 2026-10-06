package com.example.javalogin.service;

import com.example.javalogin.config.properties.RateLimitProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HttpServletRequest request;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final RateLimitProperties properties = new RateLimitProperties();
    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        properties.setMaxAttempts(3);
        properties.setAttemptWindowSeconds(300);
        properties.setLockoutDurationSeconds(900);
        Mockito.lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        loginAttemptService = new LoginAttemptService(properties, redisTemplate, objectMapper);
    }

    private void stubRemoteAddr() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Test
    void isBlocked_shouldReturnFalse_whenNoAttempts() {
        stubRemoteAddr();
        when(valueOperations.get(anyString())).thenReturn(null);

        assertThat(loginAttemptService.isBlocked(request, "john")).isFalse();
    }

    @Test
    void isBlocked_shouldReturnFalse_whenAttemptsWithinWindow() throws Exception {
        stubRemoteAddr();
        String infoJson = objectMapper.writeValueAsString(new LoginAttemptInfoData(Instant.now(), 1, null));
        when(valueOperations.get(anyString())).thenReturn(infoJson);

        assertThat(loginAttemptService.isBlocked(request, "john")).isFalse();
    }

    @Test
    void isBlocked_shouldReturnTrue_whenMaxAttemptsExceeded() throws Exception {
        stubRemoteAddr();
        Instant lockedUntil = Instant.now().plusSeconds(900);
        String infoJson = objectMapper.writeValueAsString(new LoginAttemptInfoData(Instant.now(), 3, lockedUntil));
        when(valueOperations.get(anyString())).thenReturn(infoJson);

        assertThat(loginAttemptService.isBlocked(request, "john")).isTrue();
    }

    @Test
    void recordFailure_shouldCreateNewAttempt_whenNoPreviousAttempts() {
        stubRemoteAddr();
        when(valueOperations.get(anyString())).thenReturn(null);

        loginAttemptService.recordFailure(request, "john");

        verify(valueOperations).set(anyString(), anyString());
        verify(redisTemplate).expire(anyString(), any(java.time.Duration.class));
    }

    @Test
    void recordSuccess_shouldRemoveAttemptKey() {
        loginAttemptService.recordSuccess(request, "john");

        verify(redisTemplate).delete(anyString());
    }

    @Test
    void isBlocked_shouldReset_whenWindowExpired() throws Exception {
        stubRemoteAddr();
        String infoJson = objectMapper.writeValueAsString(
                new LoginAttemptInfoData(Instant.now().minusSeconds(400), 5, null)
        );
        when(valueOperations.get(anyString())).thenReturn(infoJson);

        assertThat(loginAttemptService.isBlocked(request, "john")).isFalse();
        verify(redisTemplate).delete(anyString());
    }

    private record LoginAttemptInfoData(Instant firstAttempt, int count, Instant lockedUntil) {
    }
}

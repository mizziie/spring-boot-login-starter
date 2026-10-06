package com.example.javalogin.service;

import com.example.javalogin.config.properties.RateLimitProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService {

    private static final String KEY_PREFIX = "login:attempts:";

    private final RateLimitProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public boolean isBlocked(HttpServletRequest request, String username) {
        String key = getKey(request, username);
        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return false;
        }

        try {
            LoginAttemptInfo info = objectMapper.readValue(value, LoginAttemptInfo.class);

            if (info.isLocked()) {
                if (Instant.now().isAfter(info.getLockedUntil())) {
                    redisTemplate.delete(key);
                    return false;
                }
                log.warn("Login blocked for key {} until {}", key, info.getLockedUntil());
                return true;
            }

            if (info.getFirstAttempt().isBefore(Instant.now().minusSeconds(properties.getAttemptWindowSeconds()))) {
                redisTemplate.delete(key);
                return false;
            }

            return false;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse rate limit data for key {}: {}", key, e.getMessage());
            redisTemplate.delete(key);
            return false;
        }
    }

    public void recordFailure(HttpServletRequest request, String username) {
        String key = getKey(request, username);
        String value = redisTemplate.opsForValue().get(key);

        LoginAttemptInfo info;
        Instant now = Instant.now();

        if (value == null) {
            info = new LoginAttemptInfo(now, 1, null);
        } else {
            try {
                info = objectMapper.readValue(value, LoginAttemptInfo.class);
                if (info.getFirstAttempt().isBefore(now.minusSeconds(properties.getAttemptWindowSeconds()))) {
                    info = new LoginAttemptInfo(now, 1, null);
                } else {
                    info.setCount(info.getCount() + 1);
                }
            } catch (JsonProcessingException e) {
                log.error("Failed to parse rate limit data for key {}: {}", key, e.getMessage());
                info = new LoginAttemptInfo(now, 1, null);
            }
        }

        if (info.getCount() >= properties.getMaxAttempts()) {
            Instant lockedUntil = now.plusSeconds(properties.getLockoutDurationSeconds());
            info.setLockedUntil(lockedUntil);
            log.warn("User {} exceeded max login attempts, locked until {}", username, lockedUntil);
        }

        long ttlSeconds = Math.max(properties.getAttemptWindowSeconds(), properties.getLockoutDurationSeconds());
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(info));
            redisTemplate.expire(key, java.time.Duration.ofSeconds(ttlSeconds));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize rate limit data for key {}: {}", key, e.getMessage());
        }
    }

    public void recordSuccess(HttpServletRequest request, String username) {
        redisTemplate.delete(getKey(request, username));
    }

    private String getKey(HttpServletRequest request, String username) {
        String ip = request != null ? request.getRemoteAddr() : "unknown";
        return KEY_PREFIX + ip + ":" + username;
    }

    @Getter
    @Setter
    private static class LoginAttemptInfo {
        private Instant firstAttempt;
        private int count;
        private Instant lockedUntil;

        LoginAttemptInfo() {
        }

        LoginAttemptInfo(Instant firstAttempt, int count, Instant lockedUntil) {
            this.firstAttempt = firstAttempt;
            this.count = count;
            this.lockedUntil = lockedUntil;
        }

        boolean isLocked() {
            return lockedUntil != null && Instant.now().isBefore(lockedUntil);
        }
    }
}

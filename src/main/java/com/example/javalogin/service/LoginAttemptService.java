package com.example.javalogin.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class LoginAttemptService {

    private final int maxAttempts;
    private final long lockoutDurationSeconds;
    private final long attemptWindowSeconds;
    private final Map<String, LoginAttempts> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService() {
        this(5, 300, 900);
    }

    public LoginAttemptService(int maxAttempts, long attemptWindowSeconds, long lockoutDurationSeconds) {
        this.maxAttempts = maxAttempts;
        this.attemptWindowSeconds = attemptWindowSeconds;
        this.lockoutDurationSeconds = lockoutDurationSeconds;
    }

    public boolean isBlocked(HttpServletRequest request, String username) {
        String key = getKey(request, username);
        LoginAttempts loginAttempts = attempts.get(key);

        if (loginAttempts == null) {
            return false;
        }

        if (loginAttempts.isLocked()) {
            if (Instant.now().isAfter(loginAttempts.lockedUntil)) {
                attempts.remove(key);
                return false;
            }
            log.warn("Login blocked for key {} until {}", key, loginAttempts.lockedUntil);
            return true;
        }

        if (loginAttempts.firstAttempt.isBefore(Instant.now().minusSeconds(attemptWindowSeconds))) {
            attempts.remove(key);
            return false;
        }

        return false;
    }

    public void recordFailure(HttpServletRequest request, String username) {
        String key = getKey(request, username);
        attempts.compute(key, (k, existing) -> {
            LoginAttempts current = existing;
            if (current == null || current.firstAttempt.isBefore(Instant.now().minusSeconds(attemptWindowSeconds))) {
                current = new LoginAttempts(Instant.now(), 1, null);
            } else {
                current.count++;
            }

            if (current.count >= maxAttempts) {
                current.lockedUntil = Instant.now().plusSeconds(lockoutDurationSeconds);
                log.warn("User {} exceeded max login attempts, locked until {}", username, current.lockedUntil);
            }
            return current;
        });
    }

    public void recordSuccess(HttpServletRequest request, String username) {
        attempts.remove(getKey(request, username));
    }

    private String getKey(HttpServletRequest request, String username) {
        String ip = request != null ? request.getRemoteAddr() : "unknown";
        return ip + ":" + username;
    }

    private static class LoginAttempts {
        Instant firstAttempt;
        int count;
        Instant lockedUntil;

        LoginAttempts(Instant firstAttempt, int count, Instant lockedUntil) {
            this.firstAttempt = firstAttempt;
            this.count = count;
            this.lockedUntil = lockedUntil;
        }

        boolean isLocked() {
            return lockedUntil != null && Instant.now().isBefore(lockedUntil);
        }
    }
}

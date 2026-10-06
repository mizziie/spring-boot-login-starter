package com.example.javalogin.service;

import com.example.javalogin.entity.AuditLog;
import com.example.javalogin.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void log(String username, String action, HttpServletRequest request, String details) {
        try {
            AuditLog logEntry = AuditLog.builder()
                    .username(username)
                    .action(action)
                    .ipAddress(getClientIp(request))
                    .userAgent(request != null ? request.getHeader("User-Agent") : null)
                    .details(details)
                    .build();
            auditLogRepository.save(logEntry);
        } catch (Exception ex) {
            log.error("Failed to write audit log: {}", ex.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String xForwarded = request.getHeader("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.isBlank()) {
            return xForwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

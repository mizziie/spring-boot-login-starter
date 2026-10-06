package com.example.javalogin.config;

import com.example.javalogin.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;

    @Value("${ADMIN_USERNAME:admin}")
    private String adminUsername;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Value("${ADMIN_EMAIL:admin@example.com}")
    private String adminEmail;

    @Override
    public void run(String... args) {
        String password = (adminPassword != null && !adminPassword.isBlank())
                ? adminPassword
                : "admin123";

        if (password.equals("admin123")) {
            log.warn("Using default admin password. Please change ADMIN_PASSWORD environment variable in production!");
        }

        userService.createAdminIfAbsent(adminUsername, password, adminEmail);
    }
}

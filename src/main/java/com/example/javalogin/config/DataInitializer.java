package com.example.javalogin.config;

import com.example.javalogin.config.properties.AdminProperties;
import com.example.javalogin.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private static final String DEFAULT_ADMIN_PASSWORD = "admin123";

    private final UserService userService;
    private final AdminProperties adminProperties;

    @Override
    public void run(String... args) {
        if (DEFAULT_ADMIN_PASSWORD.equals(adminProperties.getPassword())) {
            log.warn("Using default admin password '{}'. Please change ADMIN_PASSWORD environment variable in production!", DEFAULT_ADMIN_PASSWORD);
        }

        userService.createAdminIfAbsent(
                adminProperties.getUsername(),
                adminProperties.getPassword(),
                adminProperties.getEmail()
        );
    }
}

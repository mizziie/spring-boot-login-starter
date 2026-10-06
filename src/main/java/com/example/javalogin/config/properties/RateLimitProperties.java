package com.example.javalogin.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.rate-limit")
@Validated
@Getter
@Setter
public class RateLimitProperties {

    private int maxAttempts = 5;
    private long attemptWindowSeconds = 300;
    private long lockoutDurationSeconds = 900;
}

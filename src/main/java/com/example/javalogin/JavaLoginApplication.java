package com.example.javalogin;

import com.example.javalogin.config.properties.AdminProperties;
import com.example.javalogin.config.properties.RateLimitProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({AdminProperties.class, RateLimitProperties.class})
public class JavaLoginApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaLoginApplication.class, args);
    }
}

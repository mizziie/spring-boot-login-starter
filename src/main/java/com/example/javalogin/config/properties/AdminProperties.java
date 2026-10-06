package com.example.javalogin.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.admin")
@Validated
@Getter
@Setter
public class AdminProperties {

    private String username;
    private String password;
    private String email;
    private String fullName;
}

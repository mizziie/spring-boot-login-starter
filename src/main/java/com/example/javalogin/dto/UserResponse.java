package com.example.javalogin.dto;

import com.example.javalogin.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private Role role;
    private boolean emailVerified;
    private Instant createdAt;
    private Instant updatedAt;
}

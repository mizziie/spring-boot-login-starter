package com.example.javalogin.service;

import com.example.javalogin.dto.SignupRequest;
import com.example.javalogin.dto.UserResponse;
import com.example.javalogin.entity.Role;
import com.example.javalogin.entity.User;
import com.example.javalogin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse registerUser(SignupRequest request, Role role) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .fullName(request.getFullName())
                .role(role != null ? role : Role.USER)
                .emailVerified(role != null && role == Role.ADMIN)
                .build();

        User saved = userRepository.save(user);
        log.info("Registered new user: {}", saved.getUsername());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public User createAdminIfAbsent(String username, String password, String email) {
        if (userRepository.existsByUsername(username)) {
            log.info("Admin user '{}' already exists, skipping seed.", username);
            return userRepository.findByUsername(username).orElseThrow();
        }

        User admin = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .fullName("Administrator")
                .role(Role.ADMIN)
                .emailVerified(true)
                .build();

        User saved = userRepository.save(admin);
        log.info("Created default admin user: {}", saved.getUsername());
        return saved;
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

package com.example.javalogin.service;

import com.example.javalogin.dto.SignupRequest;
import com.example.javalogin.dto.UserResponse;
import com.example.javalogin.entity.Role;
import com.example.javalogin.entity.User;
import com.example.javalogin.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void registerUser_shouldCreateNewUser() {
        SignupRequest request = new SignupRequest("john", "password123", "john@example.com", "John Doe");

        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            user.setId(1L);
            user.setCreatedAt(Instant.now());
            user.setUpdatedAt(Instant.now());
            return user;
        });

        UserResponse response = userService.registerUser(request, Role.USER);

        assertThat(response.getUsername()).isEqualTo("john");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void registerUser_shouldThrow_whenUsernameExists() {
        SignupRequest request = new SignupRequest("john", "password123", "john@example.com", "John Doe");
        when(userRepository.existsByUsername("john")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(request, Role.USER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    void registerUser_shouldThrow_whenEmailExists() {
        SignupRequest request = new SignupRequest("john", "password123", "john@example.com", "John Doe");
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(request, Role.USER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void getCurrentUser_shouldReturnAuthenticatedUser() {
        setAuthentication("john");
        User user = createUser(1L, "john");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        UserResponse response = userService.getCurrentUser();

        assertThat(response.getUsername()).isEqualTo("john");
    }

    @Test
    void getAllUsers_shouldReturnAllUsers() {
        User user1 = createUser(1L, "john");
        User user2 = createUser(2L, "jane");
        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        List<UserResponse> users = userService.getAllUsers();

        assertThat(users).hasSize(2);
    }

    @Test
    void findByUsername_shouldReturnUser() {
        User user = createUser(1L, "john");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByUsername("john");

        assertThat(result).isPresent();
    }

    @Test
    void createAdminIfAbsent_shouldCreateAdmin_whenNotExists() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode("admin123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            user.setId(1L);
            return user;
        });

        User admin = userService.createAdminIfAbsent("admin", "admin123", "admin@example.com");

        assertThat(admin.getUsername()).isEqualTo("admin");
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.isEmailVerified()).isTrue();
    }

    @Test
    void createAdminIfAbsent_shouldSkip_whenAdminExists() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(createUser(1L, "admin")));

        User admin = userService.createAdminIfAbsent("admin", "admin123", "admin@example.com");

        assertThat(admin.getUsername()).isEqualTo("admin");
        verify(userRepository, never()).save(any());
    }

    private User createUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword("encoded");
        user.setEmail(username + "@example.com");
        user.setFullName(username);
        user.setRole(Role.USER);
        user.setEmailVerified(true);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return user;
    }

    private void setAuthentication(String username) {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                username, null, List.of()
        ));
        SecurityContextHolder.setContext(context);
    }
}

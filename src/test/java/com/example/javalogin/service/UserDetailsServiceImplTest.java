package com.example.javalogin.service;

import com.example.javalogin.entity.Role;
import com.example.javalogin.entity.User;
import com.example.javalogin.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsername_shouldReturnUserDetails_whenUserExists() {
        User user = createUser("john", Role.USER);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("john");

        assertThat(details.getUsername()).isEqualTo("john");
        assertThat(details.getPassword()).isEqualTo("encoded-password");
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    void loadUserByUsername_shouldThrow_whenUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: unknown");
    }

    @Test
    void loadUserByUsername_shouldMapAdminRoleCorrectly() {
        User user = createUser("admin", Role.ADMIN);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("admin");

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    private User createUser(String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("encoded-password");
        user.setEmail(username + "@example.com");
        user.setRole(role);
        user.setEmailVerified(true);
        return user;
    }
}

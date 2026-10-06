package com.example.javalogin.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "test-secret-key-must-be-at-least-32-characters-long");
        ReflectionTestUtils.setField(jwtService, "accessExpirationMs", 900_000L);
    }

    @Test
    void generateToken_shouldCreateValidToken() {
        UserDetails userDetails = createUserDetails("john");

        String token = jwtService.generateAccessToken(userDetails);

        assertThat(token).isNotBlank();
        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("john");
    }

    @Test
    void isTokenValid_shouldReturnFalse_forWrongUser() {
        UserDetails userDetails = createUserDetails("john");
        String token = jwtService.generateAccessToken(userDetails);

        UserDetails otherUser = createUserDetails("jane");

        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalse_forMalformedToken() {
        assertThat(jwtService.isTokenValid("not-a-jwt", createUserDetails("john"))).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnTrue_whenUserDetailsNotProvided() {
        UserDetails userDetails = createUserDetails("john");
        String token = jwtService.generateAccessToken(userDetails);

        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void getAccessExpirationMs_shouldReturnConfiguredValue() {
        assertThat(jwtService.getAccessExpirationMs()).isEqualTo(900_000L);
    }

    private UserDetails createUserDetails(String username) {
        return new User(username, "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}

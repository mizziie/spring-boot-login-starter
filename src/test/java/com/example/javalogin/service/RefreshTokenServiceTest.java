package com.example.javalogin.service;

import com.example.javalogin.entity.RefreshToken;
import com.example.javalogin.entity.User;
import com.example.javalogin.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void createRefreshToken_shouldDeleteExistingAndCreateNewToken() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", 604_800_000L);
        User user = createUser(1L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken token = refreshTokenService.createRefreshToken(user);

        assertThat(token.getToken()).isNotBlank();
        assertThat(token.getUser()).isEqualTo(user);
        assertThat(token.getExpiryDate()).isAfter(Instant.now());
        verify(refreshTokenRepository).deleteByUserId(1L);
    }

    @Test
    void findByToken_shouldReturnToken_whenValid() {
        RefreshToken token = createRefreshToken(UUID.randomUUID().toString(), Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        Optional<RefreshToken> result = refreshTokenService.findByToken(token.getToken());

        assertThat(result).isPresent();
    }

    @Test
    void findByToken_shouldReturnEmpty_whenExpired() {
        RefreshToken token = createRefreshToken(UUID.randomUUID().toString(), Instant.now().minusSeconds(3600));
        when(refreshTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        Optional<RefreshToken> result = refreshTokenService.findByToken(token.getToken());

        assertThat(result).isEmpty();
    }

    @Test
    void deleteByUserId_shouldCallRepository() {
        refreshTokenService.deleteByUserId(1L);
        verify(refreshTokenRepository).deleteByUserId(1L);
    }

    @Test
    void isExpired_shouldReturnTrue_forExpiredToken() {
        RefreshToken token = createRefreshToken("token", Instant.now().minusSeconds(10));

        assertThat(refreshTokenService.isExpired(token)).isTrue();
    }

    @Test
    void isExpired_shouldReturnFalse_forValidToken() {
        RefreshToken token = createRefreshToken("token", Instant.now().plusSeconds(3600));

        assertThat(refreshTokenService.isExpired(token)).isFalse();
    }

    private User createUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("john");
        return user;
    }

    private RefreshToken createRefreshToken(String token, Instant expiryDate) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setExpiryDate(expiryDate);
        refreshToken.setUser(createUser(1L));
        return refreshToken;
    }
}

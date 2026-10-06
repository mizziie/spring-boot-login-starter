package com.example.javalogin.service;

import com.example.javalogin.entity.User;
import com.example.javalogin.entity.VerificationToken;
import com.example.javalogin.repository.UserRepository;
import com.example.javalogin.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${APP_BASE_URL:http://localhost:8080}")
    private String baseUrl;

    @Value("${MAIL_FROM:noreply@example.com}")
    private String fromAddress;

    @Transactional
    public VerificationToken createVerificationToken(User user) {
        tokenRepository.findByToken(user.getUsername()).ifPresent(tokenRepository::delete);

        VerificationToken token = VerificationToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(Instant.now().plusSeconds(24 * 60 * 60))
                .build();

        return tokenRepository.save(token);
    }

    @Transactional
    public Optional<User> verifyEmail(String token) {
        Optional<VerificationToken> optional = tokenRepository.findByToken(token);
        if (optional.isEmpty() || optional.get().getExpiryDate().isBefore(Instant.now())) {
            return Optional.empty();
        }

        VerificationToken verificationToken = optional.get();
        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        tokenRepository.delete(verificationToken);
        return Optional.of(user);
    }

    public void sendVerificationEmail(User user, String token) {
        String verificationUrl = baseUrl + "/api/auth/verify-email?token=" + token;
        String subject = "Please verify your email";
        String body = String.format(
                "Hi %s,%n%nPlease verify your email by clicking the link below:%n%s%n%nThis link will expire in 24 hours.",
                user.getUsername(), verificationUrl
        );

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(user.getEmail());
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Verification email sent to: {}", user.getEmail());
        } catch (MailException e) {
            log.warn("Could not send verification email to {}: {}", user.getEmail(), e.getMessage());
        }

        log.info("Verification link for user {}: {}", user.getUsername(), verificationUrl);
    }
}

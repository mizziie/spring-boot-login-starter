package com.example.javalogin.controller;

import com.example.javalogin.dto.ApiResponse;
import com.example.javalogin.dto.LoginRequest;
import com.example.javalogin.dto.SignupRequest;
import com.example.javalogin.dto.TokenRefreshRequest;
import com.example.javalogin.dto.TokenResponse;
import com.example.javalogin.dto.UserResponse;
import com.example.javalogin.entity.RefreshToken;
import com.example.javalogin.entity.User;
import com.example.javalogin.entity.VerificationToken;
import com.example.javalogin.service.AuditLogService;
import com.example.javalogin.service.EmailVerificationService;
import com.example.javalogin.service.JwtService;
import com.example.javalogin.service.LoginAttemptService;
import com.example.javalogin.service.RefreshTokenService;
import com.example.javalogin.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final LoginAttemptService loginAttemptService;
    private final AuditLogService auditLogService;
    private final EmailVerificationService emailVerificationService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Value("${EMAIL_VERIFICATION_REQUIRED:false}")
    private boolean emailVerificationRequired;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        if (loginAttemptService.isBlocked(httpRequest, request.getUsername())) {
            auditLogService.log(request.getUsername(), "LOGIN_BLOCKED", httpRequest, "Too many failed attempts");
            return ResponseEntity.status(429)
                    .body(ApiResponse.error("Too many failed login attempts. Please try again later."));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            UserResponse user = userService.getCurrentUser();

            if (emailVerificationRequired && !user.isEmailVerified()) {
                auditLogService.log(user.getUsername(), "LOGIN_DENIED", httpRequest, "Email not verified");
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Please verify your email before logging in."));
            }

            SecurityContextHolder.getContext().setAuthentication(authentication);
            httpRequest.getSession(true);
            loginAttemptService.recordSuccess(httpRequest, request.getUsername());
            auditLogService.log(user.getUsername(), "LOGIN_SUCCESS", httpRequest, null);
            log.info("User logged in: {}", user.getUsername());
            return ResponseEntity.ok(ApiResponse.success("Login successful", user));
        } catch (BadCredentialsException e) {
            loginAttemptService.recordFailure(httpRequest, request.getUsername());
            auditLogService.log(request.getUsername(), "LOGIN_FAILURE", httpRequest, "Bad credentials");
            log.warn("Failed login attempt for user: {}", request.getUsername());
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid username or password"));
        }
    }

    @PostMapping("/token")
    public ResponseEntity<ApiResponse<TokenResponse>> token(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        if (loginAttemptService.isBlocked(httpRequest, request.getUsername())) {
            auditLogService.log(request.getUsername(), "TOKEN_BLOCKED", httpRequest, "Too many failed attempts");
            return ResponseEntity.status(429)
                    .body(ApiResponse.error("Too many failed login attempts. Please try again later."));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            UserResponse userResponse = userService.getCurrentUser();
            if (emailVerificationRequired && !userResponse.isEmailVerified()) {
                auditLogService.log(userResponse.getUsername(), "TOKEN_DENIED", httpRequest, "Email not verified");
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Please verify your email before logging in."));
            }

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String accessToken = jwtService.generateAccessToken(userDetails);
            User user = userService.findByUsername(userResponse.getUsername())
                    .orElseThrow(() -> new BadCredentialsException("User not found"));
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            loginAttemptService.recordSuccess(httpRequest, request.getUsername());
            auditLogService.log(userResponse.getUsername(), "TOKEN_SUCCESS", httpRequest, null);

            TokenResponse response = TokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken.getToken())
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getAccessExpirationMs() / 1000)
                    .build();

            return ResponseEntity.ok(ApiResponse.success("Token generated", response));
        } catch (BadCredentialsException e) {
            loginAttemptService.recordFailure(httpRequest, request.getUsername());
            auditLogService.log(request.getUsername(), "TOKEN_FAILURE", httpRequest, "Bad credentials");
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid username or password"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request) {
        Optional<RefreshToken> optionalToken = refreshTokenService.findByToken(request.getRefreshToken());
        if (optionalToken.isEmpty()) {
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid refresh token"));
        }

        RefreshToken refreshToken = optionalToken.get();
        if (refreshTokenService.isExpired(refreshToken)) {
            return ResponseEntity.status(401).body(ApiResponse.error("Refresh token expired"));
        }

        User user = refreshToken.getUser();
        String accessToken = jwtService.generateAccessToken(
                new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPassword(),
                        org.springframework.security.core.authority.AuthorityUtils
                                .createAuthorityList("ROLE_" + user.getRole().name())
                )
        );

        TokenResponse response = TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessExpirationMs() / 1000)
                .build();

        return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody SignupRequest request,
            HttpServletRequest httpRequest) {
        UserResponse user = userService.registerUser(request, null);

        User savedUser = userService.findByUsername(user.getUsername()).orElseThrow();
        VerificationToken verificationToken = emailVerificationService.createVerificationToken(savedUser);
        emailVerificationService.sendVerificationEmail(savedUser, verificationToken.getToken());

        auditLogService.log(user.getUsername(), "REGISTER", httpRequest, "Email verification sent");
        return ResponseEntity.ok(ApiResponse.success("Registration successful. Please verify your email.", user));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@RequestParam String token) {
        Optional<User> optionalUser = emailVerificationService.verifyEmail(token);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid or expired verification token"));
        }
        auditLogService.log(optionalUser.get().getUsername(), "EMAIL_VERIFIED", null, "Email verified via token");
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", null));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "anonymous";
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        auditLogService.log(username, "LOGOUT", request, null);
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }
}

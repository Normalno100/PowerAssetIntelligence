package com.powerassetintelligence.infrastructure.web;

import com.powerassetintelligence.infrastructure.security.JwtTokenProvider;
import com.powerassetintelligence.infrastructure.security.dto.LoginRequest;
import com.powerassetintelligence.infrastructure.security.dto.LoginResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${security.jwt.dev-username:admin}")
    private String devUsername;

    @Value("${security.jwt.dev-password:admin123}")
    private String devPassword;

    public AuthController(JwtTokenProvider tokenProvider,
                          PasswordEncoder passwordEncoder) {
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        // Dev mode: hardcoded credentials
        if (devUsername.equals(request.username()) && devPassword.equals(request.password())) {
            String token = tokenProvider.generateToken(request.username());
            return ResponseEntity.ok(new LoginResponse(token));
        }

        // Production mode: check against BCrypt-encoded password in DB
        // For MVP, we'll use a simple lookup - in production this would query a User entity
        if (passwordEncoder.matches(request.password(), getStoredPassword(request.username()))) {
            String token = tokenProvider.generateToken(request.username());
            return ResponseEntity.ok(new LoginResponse(token));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    /**
     * MVP: returns a dummy BCrypt hash. In production, this queries the User entity.
     * For dev, the hardcoded path above handles authentication.
     */
    private String getStoredPassword(String username) {
        // Placeholder for production user lookup
        // return userRepository.findByUsername(username).map(u -> u.getPasswordHash()).orElse(null);
        return passwordEncoder.encode("placeholder");
    }
}

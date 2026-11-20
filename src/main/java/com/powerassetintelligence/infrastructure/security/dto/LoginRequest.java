package com.powerassetintelligence.infrastructure.security.dto;

public record LoginRequest(
        String username,
        String password
) {
}

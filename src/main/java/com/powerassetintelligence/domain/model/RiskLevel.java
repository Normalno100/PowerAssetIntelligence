package com.powerassetintelligence.domain.model;

public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    /**
     * Parse a risk level from a string code (case-insensitive).
     * Used when mapping ML service responses.
     */
    public static RiskLevel fromCode(String code) {
        if (code == null || code.isBlank()) {
            return MEDIUM;
        }
        try {
            return valueOf(code.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return MEDIUM;
        }
    }
}

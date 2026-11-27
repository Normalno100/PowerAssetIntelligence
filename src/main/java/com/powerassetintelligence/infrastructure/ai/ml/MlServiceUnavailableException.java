package com.powerassetintelligence.infrastructure.ai.ml;

/**
 * Thrown when the ML prediction service is unavailable — circuit breaker
 * open, timeout, or persistent 5xx errors.
 */
public class MlServiceUnavailableException extends RuntimeException {

    public MlServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

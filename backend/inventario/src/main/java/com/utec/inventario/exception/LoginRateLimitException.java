package com.utec.inventario.exception;

public class LoginRateLimitException extends RuntimeException {
    public static final String MESSAGE = "Demasiados intentos de inicio de sesión. Espera antes de intentarlo nuevamente.";
    private final long retryAfterSeconds;

    public LoginRateLimitException(long retryAfterSeconds) {
        super(MESSAGE);
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}

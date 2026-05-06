package com.marcura.exchange.web.error;

/**
 * Raised when no rate exists for the requested currency / date. Mapped to HTTP 404
 * by {@code GlobalExceptionHandler}.
 */
public class RateNotFoundException extends RuntimeException {
    public RateNotFoundException(String message) {
        super(message);
    }
}

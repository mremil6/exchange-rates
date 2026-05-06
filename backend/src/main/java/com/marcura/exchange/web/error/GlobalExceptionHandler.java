package com.marcura.exchange.web.error;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;

/**
 * Centralised error mapping. Every error response is RFC 7807
 * {@link ProblemDetail} JSON, so clients can branch on {@code type}/{@code status}
 * rather than parsing message strings.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RateNotFoundException.class)
    public ProblemDetail rateNotFound(RateNotFoundException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        pd.setType(URI.create("https://marcura.example/errors/rate-not-found"));
        pd.setTitle("Exchange rate not found");
        return pd;
    }

    @ExceptionHandler({IllegalArgumentException.class,
                        MethodArgumentTypeMismatchException.class,
                        ConstraintViolationException.class,
                        MethodArgumentNotValidException.class})
    public ProblemDetail badRequest(Exception e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        pd.setTitle("Invalid request");
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail internal(Exception e) {
        log.error("Unhandled error", e);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
        pd.setTitle("Internal server error");
        return pd;
    }
}

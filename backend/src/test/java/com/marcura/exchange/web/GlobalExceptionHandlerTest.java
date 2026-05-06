package com.marcura.exchange.web;

import com.marcura.exchange.web.error.GlobalExceptionHandler;
import com.marcura.exchange.web.error.RateNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void rateNotFound_returns404ProblemDetail() {
        ProblemDetail pd = handler.rateNotFound(new RateNotFoundException("No rate for EUR on 2024-03-15"));

        assertThat(pd.getStatus()).isEqualTo(404);
        assertThat(pd.getDetail()).contains("EUR");
        assertThat(pd.getTitle()).isEqualTo("Exchange rate not found");
    }

    @Test
    void badRequest_returns400ProblemDetail() {
        ProblemDetail pd = handler.badRequest(new IllegalArgumentException("currency code must be 3 letters"));

        assertThat(pd.getStatus()).isEqualTo(400);
        assertThat(pd.getTitle()).isEqualTo("Invalid request");
    }

    @Test
    void internal_returns500ProblemDetail() {
        ProblemDetail pd = handler.internal(new RuntimeException("something went wrong"));

        assertThat(pd.getStatus()).isEqualTo(500);
        assertThat(pd.getTitle()).isEqualTo("Internal server error");
    }
}

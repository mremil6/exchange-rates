package com.marcura.exchange.web;

import com.marcura.exchange.domain.ExchangeRate;
import com.marcura.exchange.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end happy-path + 404 for the {@code /api/exchange} endpoint, against
 * a real Postgres container. Uses a small in-method seed instead of running
 * the full {@link com.marcura.exchange.service.ingestion.SeedDataService} so the
 * test is deterministic.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ExchangeControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("app.ingestion.seed-days", () -> "0");
        r.add("app.fixer.api-key", () -> "");
    }

    @Autowired private WebApplicationContext context;
    @Autowired private ExchangeRateRepository rateRepository;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        rateRepository.deleteAll();
    }

    @Test
    void exchangeHappyPathReturnsAdjustedRate() throws Exception {
        LocalDate d = LocalDate.parse("2024-03-15");
        rateRepository.save(rate("EUR", d, "0.8"));
        rateRepository.save(rate("PLN", d, "3.7"));

        mvc.perform(get("/exchange").param("from", "EUR").param("to", "PLN").param("date", d.toString()))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.from").value("EUR"))
           .andExpect(jsonPath("$.to").value("PLN"))
           .andExpect(jsonPath("$.date").value("2024-03-15"))
           .andExpect(jsonPath("$.exchange").value(org.hamcrest.Matchers.closeTo(4.50, 0.01)));
    }

    @Test
    void exchangeReturns404WhenDateHasNoRate() throws Exception {
        mvc.perform(get("/exchange")
                .param("from", "EUR").param("to", "PLN").param("date", "1999-01-01"))
           .andExpect(status().isNotFound());
    }

    @Test
    void exchangeReturns400ForBadCurrency() throws Exception {
        mvc.perform(get("/exchange").param("from", "E").param("to", "PLN"))
           .andExpect(status().isBadRequest());
    }

    private ExchangeRate rate(String ccy, LocalDate d, String r) {
        return ExchangeRate.builder()
                .currency(ccy).rateDate(d).rateToBase(new BigDecimal(r))
                .baseCurrency("EUR").fetchedAt(Instant.now()).build();
    }
}

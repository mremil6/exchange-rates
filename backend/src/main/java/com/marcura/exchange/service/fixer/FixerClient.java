package com.marcura.exchange.service.fixer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Thin HTTP client for Fixer.io. Returns {@link Optional#empty()} when the API key
 * is unset or the response is unsuccessful, allowing callers to fall back gracefully.
 */
@Component
public class FixerClient {

    private static final Logger log = LoggerFactory.getLogger(FixerClient.class);

    private final RestClient restClient;
    private final String apiKey;

    public FixerClient(@Value("${app.fixer.base-url}") String baseUrl,
                       @Value("${app.fixer.api-key:}") String apiKey) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    /** Package-private constructor for unit tests — avoids making a real HTTP client. */
    FixerClient(RestClient restClient, String apiKey) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public Optional<FixerLatestResponse> fetchLatest() {
        return call("/latest");
    }

    public Optional<FixerLatestResponse> fetchForDate(LocalDate date) {
        return call("/" + date);
    }

    private Optional<FixerLatestResponse> call(String path) {
        if (!isConfigured()) {
            log.debug("Fixer API key not configured; skipping {} call", path);
            return Optional.empty();
        }
        try {
            FixerLatestResponse body = restClient.get()
                    .uri(uri -> uri.path(path).queryParam("access_key", apiKey).build())
                    .retrieve()
                    .body(FixerLatestResponse.class);
            if (body == null || !body.isOk()) {
                log.warn("Fixer call {} returned non-OK body: {}", path, body);
                return Optional.empty();
            }
            return Optional.of(body);
        } catch (RestClientException e) {
            log.warn("Fixer call {} failed: {}", path, e.getMessage());
            return Optional.empty();
        }
    }
}

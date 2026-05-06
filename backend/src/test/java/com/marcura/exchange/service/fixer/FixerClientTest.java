package com.marcura.exchange.service.fixer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FixerClientTest {

    @Test
    void isConfigured_trueWhenKeyPresent() {
        assertThat(new FixerClient(mock(RestClient.class), "my-key").isConfigured()).isTrue();
    }

    @Test
    void isConfigured_falseWhenKeyBlank() {
        assertThat(new FixerClient(mock(RestClient.class), "  ").isConfigured()).isFalse();
        assertThat(new FixerClient(mock(RestClient.class), "").isConfigured()).isFalse();
    }

    @Test
    void fetchLatest_returnsEmptyWhenNotConfigured() {
        RestClient restClient = mock(RestClient.class);
        FixerClient client = new FixerClient(restClient, "");

        assertThat(client.fetchLatest()).isEmpty();
        verifyNoInteractions(restClient);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void fetchLatest_returnsResponseWhenOk() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec reqSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec respSpec = mock(RestClient.ResponseSpec.class);
        FixerLatestResponse body = new FixerLatestResponse(true, 123L, "EUR",
                LocalDate.parse("2024-03-15"), Map.of("USD", new BigDecimal("1.08")), null);

        when(restClient.get()).thenReturn(reqSpec);
        when(reqSpec.uri(any(Function.class))).thenReturn(reqSpec);
        when(reqSpec.retrieve()).thenReturn(respSpec);
        when(respSpec.body(FixerLatestResponse.class)).thenReturn(body);

        Optional<FixerLatestResponse> result = new FixerClient(restClient, "key").fetchLatest();

        assertThat(result).isPresent();
        assertThat(result.get().base()).isEqualTo("EUR");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void fetchLatest_returnsEmptyWhenBodyNull() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec reqSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec respSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(reqSpec);
        when(reqSpec.uri(any(Function.class))).thenReturn(reqSpec);
        when(reqSpec.retrieve()).thenReturn(respSpec);
        when(respSpec.body(FixerLatestResponse.class)).thenReturn(null);

        assertThat(new FixerClient(restClient, "key").fetchLatest()).isEmpty();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void fetchLatest_returnsEmptyWhenBodyNotOk() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec reqSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec respSpec = mock(RestClient.ResponseSpec.class);
        FixerLatestResponse notOk = new FixerLatestResponse(false, null, null, null, null, null);

        when(restClient.get()).thenReturn(reqSpec);
        when(reqSpec.uri(any(Function.class))).thenReturn(reqSpec);
        when(reqSpec.retrieve()).thenReturn(respSpec);
        when(respSpec.body(FixerLatestResponse.class)).thenReturn(notOk);

        assertThat(new FixerClient(restClient, "key").fetchLatest()).isEmpty();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void fetchLatest_returnsEmptyOnRestClientException() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec reqSpec = mock(RestClient.RequestHeadersUriSpec.class);

        when(restClient.get()).thenReturn(reqSpec);
        when(reqSpec.uri(any(Function.class))).thenThrow(new RestClientException("timeout"));

        assertThat(new FixerClient(restClient, "key").fetchLatest()).isEmpty();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void fetchForDate_delegatesToDatePath() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec reqSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec respSpec = mock(RestClient.ResponseSpec.class);
        LocalDate date = LocalDate.parse("2024-03-15");
        FixerLatestResponse body = new FixerLatestResponse(true, 123L, "EUR", date,
                Map.of("USD", new BigDecimal("1.08")), null);

        when(restClient.get()).thenReturn(reqSpec);
        when(reqSpec.uri(any(Function.class))).thenReturn(reqSpec);
        when(reqSpec.retrieve()).thenReturn(respSpec);
        when(respSpec.body(FixerLatestResponse.class)).thenReturn(body);

        Optional<FixerLatestResponse> result = new FixerClient(restClient, "key").fetchForDate(date);

        assertThat(result).isPresent();
    }

    @Test
    void fixerLatestResponse_isOkReturnsFalseForVariousBadStates() {
        assertThat(new FixerLatestResponse(false, null, null, null, null, null).isOk()).isFalse();
        assertThat(new FixerLatestResponse(true, null, null, null, null, null).isOk()).isFalse();
        assertThat(new FixerLatestResponse(true, null, null, null, Map.of(), null).isOk()).isFalse();
        assertThat(new FixerLatestResponse(true, 1L, "EUR", null, Map.of("USD", BigDecimal.ONE), null).isOk()).isFalse();
        assertThat(new FixerLatestResponse(true, 1L, "EUR",
                LocalDate.parse("2024-03-15"), Map.of("USD", BigDecimal.ONE), null).isOk()).isTrue();
    }
}

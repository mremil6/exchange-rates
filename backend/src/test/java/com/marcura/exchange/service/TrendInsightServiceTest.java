package com.marcura.exchange.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcura.exchange.service.insight.TrendInsightService;
import com.marcura.exchange.web.dto.HistoricalRatesResponse;
import com.marcura.exchange.web.error.RateNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrendInsightServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ObjectProvider<ChatClient> chatClientProvider;
    private TrendInsightService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        chatClientProvider = mock(ObjectProvider.class);
        service = new TrendInsightService(objectMapper, chatClientProvider);
    }

    @Test
    void generate_throwsWhenDataIsNull() {
        assertThatThrownBy(() -> service.generate(null))
                .isInstanceOf(RateNotFoundException.class);
    }

    @Test
    void generate_throwsWhenPointsEmpty() {
        HistoricalRatesResponse empty = new HistoricalRatesResponse(
                "EUR", "USD",
                LocalDate.parse("2024-03-01"), LocalDate.parse("2024-03-07"),
                List.of());

        assertThatThrownBy(() -> service.generate(empty))
                .isInstanceOf(RateNotFoundException.class);
    }

    @Test
    void generate_returnsFallbackWhenChatClientUnavailable() {
        when(chatClientProvider.getIfAvailable()).thenReturn(null);

        String result = service.generate(sampleData("1.00", "1.05"));

        assertThat(result).contains("EUR/USD");
        assertThat(result).contains("deterministic fallback");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void generate_returnsLlmInsightWhenChatClientAvailable() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClientProvider.getIfAvailable()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("EUR/USD rose by 5% this week.");

        String result = service.generate(sampleData("1.00", "1.05"));

        assertThat(result).isEqualTo("EUR/USD rose by 5% this week.");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void generate_returnsFallbackWhenLlmThrows() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);

        when(chatClientProvider.getIfAvailable()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("Ollama not reachable"));

        String result = service.generate(sampleData("1.00", "1.05"));

        assertThat(result).contains("deterministic fallback");
    }

    @Test
    void generate_fallbackUsesRoseWhenRateIncreased() {
        when(chatClientProvider.getIfAvailable()).thenReturn(null);

        String result = service.generate(sampleData("1.00", "1.05"));

        assertThat(result).contains("rose");
    }

    @Test
    void generate_fallbackUsesFellWhenRateDecreased() {
        when(chatClientProvider.getIfAvailable()).thenReturn(null);

        String result = service.generate(sampleData("1.05", "1.00"));

        assertThat(result).contains("fell");
    }

    @Test
    void generate_fallbackUsesFlatWhenRateUnchanged() {
        when(chatClientProvider.getIfAvailable()).thenReturn(null);

        String result = service.generate(sampleData("1.00", "1.00"));

        assertThat(result).contains("flat");
    }

    private HistoricalRatesResponse sampleData(String firstRate, String lastRate) {
        LocalDate d1 = LocalDate.parse("2024-03-01");
        LocalDate d2 = LocalDate.parse("2024-03-07");
        return new HistoricalRatesResponse("EUR", "USD", d1, d2, List.of(
                new HistoricalRatesResponse.Point(d1, new BigDecimal(firstRate)),
                new HistoricalRatesResponse.Point(d2, new BigDecimal(lastRate))
        ));
    }
}

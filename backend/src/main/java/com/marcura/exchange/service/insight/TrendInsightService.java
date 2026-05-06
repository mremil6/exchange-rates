package com.marcura.exchange.service.insight;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcura.exchange.web.dto.HistoricalRatesResponse;
import com.marcura.exchange.web.error.RateNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calls the configured LLM to produce a short, factual trend summary for a currency pair.
 * Falls back to a deterministic one-liner when the model is unavailable.
 */
@Service
public class TrendInsightService {

    private static final Logger log = LoggerFactory.getLogger(TrendInsightService.class);

    private static final String SYSTEM_PROMPT = """
        You are a concise financial-data analyst for an internal currency dashboard.

        Strict rules:
        - Reply with at most 2 sentences. Plain prose. No markdown, no bullet points, no headers.
        - Reference the actual numbers you see — direction (rose / fell / steady),
          magnitude as a percentage to 1 decimal place, and *when* the largest move occurred.
        - Do not give financial advice, recommendations, predictions, or speculation.
        - Do not invent data not present in the input.
        - If the input has fewer than 3 points or values are constant, say
          "Not enough data to characterise a trend over this period." and stop.
        """;

    private static final String USER_PROMPT_TEMPLATE = """
        Pair: %s/%s
        Range: %s to %s
        Spread-adjusted daily rates as JSON: %s
        Write the insight now.
        """;

    private final ObjectMapper objectMapper;
    private final ObjectProvider<ChatClient> chatClientProvider;

    public TrendInsightService(ObjectMapper objectMapper,
                               ObjectProvider<ChatClient> chatClientProvider) {
        this.objectMapper = objectMapper;
        this.chatClientProvider = chatClientProvider;
    }

    public String generate(HistoricalRatesResponse data) {
        if (data == null || data.points().isEmpty()) {
            throw new RateNotFoundException("No rate data to summarise");
        }
        ChatClient chatClient = chatClientProvider.getIfAvailable();
        if (chatClient == null) {
            log.warn("No ChatClient bean available; returning deterministic fallback insight");
            return fallbackInsight(data);
        }
        try {
            String userMessage = USER_PROMPT_TEMPLATE.formatted(
                    data.from(),
                    data.to(),
                    data.fromDate(),
                    data.toDate(),
                    serialise(data.points())
            );
            return chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userMessage)
                    .call()
                    .content()
                    .trim();
        } catch (Exception e) {
            log.warn("LLM call failed: {}. Falling back to deterministic insight.", e.toString());
            return fallbackInsight(data);
        }
    }

    private String serialise(List<HistoricalRatesResponse.Point> points) throws JsonProcessingException {
        List<Map<String, Object>> compact = points.stream()
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("date", p.date().toString());
                    m.put("rate", p.rate());
                    return m;
                })
                .toList();
        return objectMapper.writeValueAsString(compact);
    }

    /** Deterministic fallback used when the LLM is unreachable. */
    private String fallbackInsight(HistoricalRatesResponse data) {
        List<HistoricalRatesResponse.Point> pts = data.points();
        BigDecimal first = pts.get(0).rate();
        BigDecimal last = pts.get(pts.size() - 1).rate();
        BigDecimal pct = last.subtract(first)
                .divide(first, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
        String direction = pct.signum() > 0 ? "rose" : pct.signum() < 0 ? "fell" : "was flat";
        LocalDate firstDate = pts.get(0).date();
        LocalDate lastDate = pts.get(pts.size() - 1).date();
        return "%s/%s %s by %s%% between %s and %s (deterministic fallback; LLM unavailable)."
                .formatted(data.from(), data.to(), direction, pct.abs(), firstDate, lastDate);
    }
}

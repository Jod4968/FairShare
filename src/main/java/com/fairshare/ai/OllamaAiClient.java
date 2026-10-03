package com.fairshare.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "ollama", matchIfMissing = true)
public class OllamaAiClient implements AiClient {
    private final RestClient client;
    private final ObjectMapper mapper;
    private final String model;

    public OllamaAiClient(ObjectMapper mapper, @Value("${ai.base-url}") String baseUrl,
                          @Value("${ai.model}") String model) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.mapper = mapper; this.model = model;
    }

    @Override
    public AiDtos.Intent interpret(String message, List<String> memberNames) {
        String prompt = """
                You are the FairShare expense assistant. You have no database access and do not calculate finances.
                Return ONLY valid JSON with fields intent, amountRupees, description, category, splitType,
                participantNames, customShares, targetUserName, period, payerName.
                amountRupees is the user-facing rupee amount as a decimal STRING, never paise and never a
                floating-point calculation. The backend converts it to integer paise and is the final financial authority.
                Examples: "₹200" -> amountRupees "200"; "200 rupees" -> "200"; "200rs" -> "200";
                "₹900" -> "900"; "900 rupees" -> "900"; "₹125.50" -> "125.50".
                If an amountMinor field is ever present, it means PAISA (minor currency units), not rupees:
                ₹200 -> amountMinor 20000, 200 rupees -> 20000, 200rs -> 20000,
                ₹900 -> 90000, 900 rupees -> 90000, ₹125.50 -> 12550.
                Supported intents: CREATE_EXPENSE, QUERY_BALANCES, QUERY_GROUP_SPENDING, QUERY_PERSON_SPENDING,
                QUERY_CATEGORY_EXPENSES, QUERY_MONTHLY_SPENDING, UNKNOWN.
                Categories: FOOD, GROCERIES, RENT, UTILITIES, TRANSPORT, ENTERTAINMENT, OTHER.
                Split types: EQUAL, CUSTOM. Periods: ALL_TIME, CURRENT_MONTH, LAST_MONTH.
                Use participantNames for names or "me"; never invent IDs. Use null for unknown fields.
                For expense creation, payerName must be "me" unless the message explicitly names another payer.
                Group members: %s
                User message: %s
                """.formatted(String.join(", ", memberNames), message);
        try {
            JsonNode response = client.post().uri("/api/generate").contentType(MediaType.APPLICATION_JSON)
                    .body(java.util.Map.of("model", model, "prompt", prompt, "stream", false, "format", "json"))
                    .retrieve().body(JsonNode.class);
            if (response == null || response.get("response") == null) throw new AiException("Invalid AI response");
            return mapper.readValue(response.get("response").asText(), AiDtos.Intent.class);
        } catch (RestClientException exception) {
            throw new AiUnavailableException();
        } catch (Exception exception) {
            throw new AiException("The AI assistant returned an invalid response.", exception);
        }
    }
}

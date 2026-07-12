package com.ptn.strategy.news.translation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptn.strategy.config.LlmProperties;
import java.util.List;
import java.util.Map;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class OpenAiCompatibleLlmClient implements LlmClient {

    private static final String TRANSLATION_SYSTEM_PROMPT = """
            You are a professional English-to-Chinese news translator. Translate faithfully into
            natural Simplified Chinese. Preserve names, numbers, quotations, paragraph meaning and
            factual uncertainty. Do not add commentary or facts. Return only JSON in this exact
            shape: {"translation":"..."}.
            """;
    private static final String SUMMARY_SYSTEM_PROMPT = """
            Summarize the supplied Chinese news article in concise Simplified Chinese. Include only
            facts present in the text and do not speculate. Return only JSON in this exact shape:
            {"summary":"..."}.
            """;

    private final LlmProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public OpenAiCompatibleLlmClient(
            LlmProperties properties,
            ObjectMapper objectMapper,
            RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
                .connectTimeout(properties.requestTimeout())
                .readTimeout(properties.requestTimeout())
                .build();
    }

    @Override
    public String translateToChinese(String englishText) {
        return call(TRANSLATION_SYSTEM_PROMPT, englishText, "translation");
    }

    @Override
    public String summarizeInChinese(String chineseText) {
        return call(SUMMARY_SYSTEM_PROMPT, chineseText, "summary");
    }

    private String call(String systemPrompt, String userContent, String outputField) {
        Map<String, Object> request = Map.of(
                "model", properties.chatModel(),
                "temperature", 0,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userContent)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.apiKey());

        try {
            JsonNode response = restTemplate.postForObject(
                    endpoint(), new HttpEntity<>(request, headers), JsonNode.class);
            String content = response == null
                    ? null
                    : response.path("choices").path(0).path("message").path("content").asText(null);
            return parseStructuredContent(content, outputField);
        } catch (LlmResponseException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new LlmResponseException("LLM request failed: " + exception.getMessage(), exception);
        }
    }

    private String endpoint() {
        return properties.baseUrl().replaceAll("/+$", "") + "/v1/chat/completions";
    }

    private String parseStructuredContent(String content, String outputField) {
        if (content == null || content.isBlank()) {
            throw new LlmResponseException("LLM returned an empty completion");
        }
        String json = content.trim();
        if (json.startsWith("```")) {
            json = json.replaceFirst("^```(?:json)?\\s*", "")
                    .replaceFirst("\\s*```$", "");
        }
        try {
            JsonNode result = objectMapper.readTree(json);
            JsonNode value = result.get(outputField);
            if (value == null || !value.isTextual() || value.asText().isBlank()) {
                throw new LlmResponseException("LLM JSON is missing non-empty field: " + outputField);
            }
            return value.asText().trim();
        } catch (LlmResponseException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new LlmResponseException("LLM returned invalid JSON", exception);
        }
    }
}

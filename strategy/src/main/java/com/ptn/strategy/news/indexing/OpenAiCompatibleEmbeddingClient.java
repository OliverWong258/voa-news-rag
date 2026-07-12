package com.ptn.strategy.news.indexing;

import com.fasterxml.jackson.databind.JsonNode;
import com.ptn.strategy.config.LlmProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class OpenAiCompatibleEmbeddingClient implements EmbeddingClient {

    private final LlmProperties properties;
    private final RestTemplate restTemplate;

    public OpenAiCompatibleEmbeddingClient(
            LlmProperties properties, RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.restTemplate = restTemplateBuilder
                .connectTimeout(properties.requestTimeout())
                .readTimeout(properties.requestTimeout())
                .build();
    }

    @Override
    public List<Float> embed(String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.apiKey());
        Map<String, Object> body = Map.of(
                "model", properties.embeddingModel(),
                "input", text);
        JsonNode response = restTemplate.postForObject(
                endpoint(), new HttpEntity<>(body, headers), JsonNode.class);
        JsonNode embedding = response == null
                ? null
                : response.path("data").path(0).path("embedding");
        if (embedding == null || !embedding.isArray() || embedding.isEmpty()) {
            throw new IllegalStateException("Embedding API returned no vector");
        }
        List<Float> vector = new ArrayList<>(embedding.size());
        embedding.forEach(value -> vector.add(value.floatValue()));
        return List.copyOf(vector);
    }

    private String endpoint() {
        return properties.baseUrl().replaceAll("/+$", "") + "/v1/embeddings";
    }
}

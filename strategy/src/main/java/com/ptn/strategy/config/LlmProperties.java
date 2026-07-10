package com.ptn.strategy.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "llm")
public record LlmProperties(
        @NotBlank String baseUrl,
        @NotBlank String apiKey,
        @NotBlank String chatModel,
        @NotBlank String embeddingModel,
        @NotNull Duration requestTimeout) {
}

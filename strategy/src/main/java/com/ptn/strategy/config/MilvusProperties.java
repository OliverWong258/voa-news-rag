package com.ptn.strategy.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "milvus")
public record MilvusProperties(
        @NotBlank String uri,
        String token,
        @NotBlank String collection,
        @Positive int embeddingDimension,
        @Positive int chunkChars,
        @Positive int chunkOverlapChars,
        @Positive int indexingMaxAttempts) {
}

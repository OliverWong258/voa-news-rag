package com.ptn.strategy.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "crawler")
public record CrawlerProperties(
        boolean schedulerEnabled,
        @NotBlank String discoveryCron,
        @NotEmpty List<@NotBlank String> seedUrls,
        @Positive int maxLinksPerRun,
        @NotBlank String recoveryCron,
        @NotNull Duration staleTaskThreshold,
        @Positive int recoveryBatchSize) {
}

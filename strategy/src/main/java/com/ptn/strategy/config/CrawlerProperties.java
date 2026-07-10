package com.ptn.strategy.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "crawler")
public record CrawlerProperties(boolean schedulerEnabled, @NotBlank String discoveryCron) {
}

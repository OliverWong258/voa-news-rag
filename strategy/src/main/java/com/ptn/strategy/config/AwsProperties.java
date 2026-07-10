package com.ptn.strategy.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "aws")
public record AwsProperties(@NotBlank String region, @Valid Sqs sqs, @Valid S3 s3) {

    public record Sqs(
            @NotBlank String crawlQueue,
            @NotBlank String processQueue,
            @NotBlank String crawlDlq,
            @NotBlank String processDlq) {
    }

    public record S3(@NotBlank String rawContentBucket) {
    }
}

package com.ptn.strategy.news.task;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CrawlTask {
    private Long id;
    private String url;
    private String canonicalUrl;
    private String taskType;
    private TaskStatus status;
    private Integer retryCount;
    private Integer maxRetries;
    private String workerId;
    private String errorMessage;
    private String rawS3Key;
    private LocalDateTime nextRetryAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

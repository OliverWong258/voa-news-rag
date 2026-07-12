package com.ptn.strategy.news.article;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class NewsArticle {
    private Long id;
    private String source;
    private String sourceArticleId;
    private String url;
    private String canonicalUrl;
    private String titleEn;
    private String titleZh;
    private String contentEn;
    private String contentZh;
    private String summaryZh;
    private String author;
    private LocalDateTime publishedAt;
    private String contentHash;
    private String language;
    private String rawS3Key;
    private String processingStatus;
    private Integer translationRetryCount;
    private String translationError;
    private String translationModel;
    private LocalDateTime translatedAt;
    private Integer indexingRetryCount;
    private String indexingError;
    private LocalDateTime indexedAt;
}

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
    private String contentEn;
    private String author;
    private LocalDateTime publishedAt;
    private String contentHash;
    private String language;
    private String rawS3Key;
    private String processingStatus;
}

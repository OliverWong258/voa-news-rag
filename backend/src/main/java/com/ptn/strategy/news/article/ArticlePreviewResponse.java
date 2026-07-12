package com.ptn.strategy.news.article;

import java.time.LocalDateTime;

public record ArticlePreviewResponse(
        long id,
        String title,
        String summary,
        String category,
        String source,
        String sourceUrl,
        LocalDateTime publishedAt) {
}

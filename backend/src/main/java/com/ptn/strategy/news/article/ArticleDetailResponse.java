package com.ptn.strategy.news.article;

import java.time.LocalDateTime;

public record ArticleDetailResponse(
        long id,
        String title,
        String summary,
        String content,
        String category,
        String source,
        String sourceUrl,
        String author,
        LocalDateTime publishedAt) {
}

package com.ptn.strategy.news.article;

import java.time.LocalDateTime;

public record ParsedArticle(
        String title,
        String content,
        String author,
        LocalDateTime publishedAt) {
}

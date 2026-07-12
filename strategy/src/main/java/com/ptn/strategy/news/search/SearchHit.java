package com.ptn.strategy.news.search;

import java.time.Instant;

public record SearchHit(
        String vectorId,
        long articleId,
        long chunkId,
        int chunkIndex,
        double score,
        String title,
        String excerpt,
        String sourceUrl,
        Instant publishedAt) {
}

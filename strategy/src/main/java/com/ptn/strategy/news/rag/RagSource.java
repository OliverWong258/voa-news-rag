package com.ptn.strategy.news.rag;

import java.time.Instant;

public record RagSource(
        int citation,
        long articleId,
        long chunkId,
        String title,
        String url,
        Instant publishedAt,
        double score,
        String excerpt) {
}

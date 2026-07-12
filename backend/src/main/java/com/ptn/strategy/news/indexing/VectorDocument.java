package com.ptn.strategy.news.indexing;

import java.time.LocalDateTime;
import java.util.List;

public record VectorDocument(
        String vectorId,
        long articleId,
        long chunkId,
        int chunkIndex,
        String titleZh,
        String contentZh,
        String category,
        String sourceUrl,
        LocalDateTime publishedAt,
        String contentHash,
        List<Float> embedding) {
}

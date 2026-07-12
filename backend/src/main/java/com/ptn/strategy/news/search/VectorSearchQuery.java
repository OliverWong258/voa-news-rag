package com.ptn.strategy.news.search;

import java.util.List;

public record VectorSearchQuery(
        List<Float> embedding,
        int topK,
        String category,
        Long publishedFromEpochMillis,
        Long publishedToEpochMillis) {
}

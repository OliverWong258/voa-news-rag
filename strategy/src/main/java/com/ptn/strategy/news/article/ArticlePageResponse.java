package com.ptn.strategy.news.article;

import java.util.List;

public record ArticlePageResponse(
        List<ArticlePreviewResponse> items,
        int page,
        int size,
        long total) {
}

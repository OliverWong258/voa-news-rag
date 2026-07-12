package com.ptn.strategy.news.article;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ArticleQueryService {

    private static final int MAX_PAGE_SIZE = 50;
    private final NewsArticleMapper articleMapper;

    public ArticleQueryService(NewsArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    public ArticlePageResponse list(
            String category, LocalDate startDate, LocalDate endDate, int page, int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String normalizedCategory = normalize(category);
        LocalDateTime publishedFrom = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime publishedTo = endDate == null ? null : endDate.plusDays(1).atStartOfDay();
        long offset = (long) normalizedPage * normalizedSize;

        List<ArticlePreviewResponse> items = articleMapper.findPublishedArticles(
                        normalizedCategory, publishedFrom, publishedTo, offset, normalizedSize)
                .stream()
                .map(this::toPreview)
                .toList();
        long total = articleMapper.countPublishedArticles(
                normalizedCategory, publishedFrom, publishedTo);
        return new ArticlePageResponse(items, normalizedPage, normalizedSize, total);
    }

    public ArticleDetailResponse detail(long articleId) {
        NewsArticle article = articleMapper.findById(articleId);
        if (article == null || !"INDEXED".equals(article.getProcessingStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found");
        }
        return new ArticleDetailResponse(
                article.getId(),
                preferred(article.getTitleZh(), article.getTitleEn()),
                article.getSummaryZh(),
                preferred(article.getContentZh(), article.getContentEn()),
                article.getCategory(),
                article.getSource(),
                article.getCanonicalUrl(),
                article.getAuthor(),
                article.getPublishedAt());
    }

    private ArticlePreviewResponse toPreview(NewsArticle article) {
        return new ArticlePreviewResponse(
                article.getId(),
                preferred(article.getTitleZh(), article.getTitleEn()),
                article.getSummaryZh(),
                article.getCategory(),
                article.getSource(),
                article.getCanonicalUrl(),
                article.getPublishedAt());
    }

    private String preferred(String translated, String original) {
        return translated == null || translated.isBlank() ? original : translated;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

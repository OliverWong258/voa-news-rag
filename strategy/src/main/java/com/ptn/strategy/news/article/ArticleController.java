package com.ptn.strategy.news.article;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleQueryService queryService;

    public ArticleController(ArticleQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public ArticlePageResponse list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return queryService.list(category, startDate, endDate, page, size);
    }

    @GetMapping("/{articleId}")
    public ArticleDetailResponse detail(@PathVariable long articleId) {
        return queryService.detail(articleId);
    }
}

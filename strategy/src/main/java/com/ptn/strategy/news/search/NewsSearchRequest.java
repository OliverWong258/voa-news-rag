package com.ptn.strategy.news.search;

import java.time.LocalDate;

public record NewsSearchRequest(
        String query,
        Integer topK,
        String category,
        LocalDate startDate,
        LocalDate endDate) {
}

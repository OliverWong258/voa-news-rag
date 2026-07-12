package com.ptn.strategy.news.search;

import java.util.List;

public record NewsSearchResponse(String query, int total, List<SearchHit> hits) {
}

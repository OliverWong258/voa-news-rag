package com.ptn.strategy.news.discovery;

public record DiscoveryResult(
        int pagesVisited,
        int linksFound,
        int redisDuplicates,
        int databaseDuplicates,
        int tasksDispatched,
        int pagesFailed) {
}

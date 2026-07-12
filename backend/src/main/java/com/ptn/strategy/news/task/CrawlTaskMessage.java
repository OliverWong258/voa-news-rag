package com.ptn.strategy.news.task;

public record CrawlTaskMessage(long taskId, String url, String canonicalUrl) {
}

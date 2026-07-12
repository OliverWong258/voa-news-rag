package com.ptn.strategy.news.ingestion;

public interface RawHtmlStorage {
    String store(long taskId, String html);
}

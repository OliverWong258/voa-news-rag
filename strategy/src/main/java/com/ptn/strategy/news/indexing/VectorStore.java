package com.ptn.strategy.news.indexing;

public interface VectorStore {
    void upsert(VectorDocument document);
}

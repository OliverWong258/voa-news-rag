package com.ptn.strategy.news.indexing;

import com.ptn.strategy.news.search.SearchHit;
import com.ptn.strategy.news.search.VectorSearchQuery;
import java.util.List;

public interface VectorStore {
    void upsert(VectorDocument document);

    List<SearchHit> search(VectorSearchQuery query);
}

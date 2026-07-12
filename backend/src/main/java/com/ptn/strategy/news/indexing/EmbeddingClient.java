package com.ptn.strategy.news.indexing;

import java.util.List;

public interface EmbeddingClient {
    List<Float> embed(String text);
}

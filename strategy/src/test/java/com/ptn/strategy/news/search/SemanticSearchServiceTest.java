package com.ptn.strategy.news.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ptn.strategy.config.MilvusProperties;
import com.ptn.strategy.news.article.ContentHasher;
import com.ptn.strategy.news.indexing.CachingEmbeddingService;
import com.ptn.strategy.news.indexing.VectorStore;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class SemanticSearchServiceTest {

    @Test
    void filtersLowScoresAndHonorsTopK() {
        CachingEmbeddingService embeddings = mock(CachingEmbeddingService.class);
        VectorStore store = mock(VectorStore.class);
        MilvusProperties properties = new MilvusProperties(
                "http://localhost:19530", "", "chunks", 3,
                100, 20, 3, 5, 20, 0.5);
        SemanticSearchService service = new SemanticSearchService(
                embeddings, new ContentHasher(), store, properties);
        when(embeddings.embed(anyString(), anyString())).thenReturn(List.of(0.1f, 0.2f, 0.3f));
        when(store.search(any())).thenReturn(List.of(
                hit(0.91, 1), hit(0.72, 2), hit(0.30, 3)));

        NewsSearchResponse response = service.search(new NewsSearchRequest(
                "国际新闻", 2, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 11)));

        assertThat(response.total()).isEqualTo(2);
        assertThat(response.hits()).extracting(SearchHit::score).containsExactly(0.91, 0.72);
    }

    @Test
    void rejectsInvalidDateRange() {
        MilvusProperties properties = new MilvusProperties(
                "http://localhost:19530", "", "chunks", 3,
                100, 20, 3, 5, 20, 0.5);
        SemanticSearchService service = new SemanticSearchService(
                mock(CachingEmbeddingService.class),
                new ContentHasher(),
                mock(VectorStore.class),
                properties);

        assertThatThrownBy(() -> service.search(new NewsSearchRequest(
                "query", 5, LocalDate.of(2026, 7, 11), LocalDate.of(2026, 7, 1))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private SearchHit hit(double score, long id) {
        return new SearchHit(
                "chunk-" + id, id, id, 0, score, "标题", "片段", "https://example.com/" + id,
                Instant.parse("2026-07-10T00:00:00Z"));
    }
}

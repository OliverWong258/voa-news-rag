package com.ptn.strategy.news.search;

import com.ptn.strategy.config.MilvusProperties;
import com.ptn.strategy.news.article.ContentHasher;
import com.ptn.strategy.news.indexing.CachingEmbeddingService;
import com.ptn.strategy.news.indexing.VectorStore;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SemanticSearchService {

    private final CachingEmbeddingService embeddingService;
    private final ContentHasher contentHasher;
    private final VectorStore vectorStore;
    private final MilvusProperties properties;

    public SemanticSearchService(
            CachingEmbeddingService embeddingService,
            ContentHasher contentHasher,
            VectorStore vectorStore,
            MilvusProperties properties) {
        this.embeddingService = embeddingService;
        this.contentHasher = contentHasher;
        this.vectorStore = vectorStore;
        this.properties = properties;
    }

    public NewsSearchResponse search(NewsSearchRequest request) {
        String query = request.query() == null ? "" : request.query().trim();
        if (query.isBlank()) {
            throw new IllegalArgumentException("Search query must not be blank");
        }
        if (request.startDate() != null && request.endDate() != null
                && request.startDate().isAfter(request.endDate())) {
            throw new IllegalArgumentException("startDate must not be after endDate");
        }
        int topK = request.topK() == null ? properties.defaultTopK() : request.topK();
        if (topK < 1 || topK > properties.maxTopK()) {
            throw new IllegalArgumentException("topK must be between 1 and " + properties.maxTopK());
        }

        List<Float> queryVector = embeddingService.embed(contentHasher.sha256(query), query);
        int candidateCount = Math.min(properties.maxTopK(), Math.max(topK, topK * 2));
        List<SearchHit> hits = vectorStore.search(new VectorSearchQuery(
                        queryVector,
                        candidateCount,
                        startEpoch(request.startDate()),
                        endEpoch(request.endDate())))
                .stream()
                .filter(hit -> hit.score() >= properties.minimumScore())
                .limit(topK)
                .toList();
        return new NewsSearchResponse(query, hits.size(), hits);
    }

    private Long startEpoch(LocalDate date) {
        return date == null ? null : date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    private Long endEpoch(LocalDate date) {
        return date == null
                ? null
                : date.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}

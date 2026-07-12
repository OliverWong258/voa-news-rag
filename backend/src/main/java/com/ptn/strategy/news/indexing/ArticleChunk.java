package com.ptn.strategy.news.indexing;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ArticleChunk {
    private Long id;
    private Long articleId;
    private Integer chunkIndex;
    private String contentEn;
    private String contentZh;
    private Integer tokenCount;
    private String contentHash;
    private String milvusVectorId;
    private String embeddingModel;
    private String embeddingStatus;
    private Integer embeddingRetryCount;
    private String embeddingError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

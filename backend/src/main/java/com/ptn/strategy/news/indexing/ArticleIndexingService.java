package com.ptn.strategy.news.indexing;

import com.ptn.strategy.config.LlmProperties;
import com.ptn.strategy.config.MilvusProperties;
import com.ptn.strategy.news.article.ContentHasher;
import com.ptn.strategy.news.article.NewsArticle;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ArticleIndexingService {

    private final ArticleChunkMapper chunkMapper;
    private final SemanticChunker chunker;
    private final ContentHasher contentHasher;
    private final CachingEmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final MilvusProperties milvusProperties;
    private final LlmProperties llmProperties;

    public ArticleIndexingService(
            ArticleChunkMapper chunkMapper,
            SemanticChunker chunker,
            ContentHasher contentHasher,
            CachingEmbeddingService embeddingService,
            VectorStore vectorStore,
            MilvusProperties milvusProperties,
            LlmProperties llmProperties) {
        this.chunkMapper = chunkMapper;
        this.chunker = chunker;
        this.contentHasher = contentHasher;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.milvusProperties = milvusProperties;
        this.llmProperties = llmProperties;
    }

    public int index(NewsArticle article, Runnable heartbeat) {
        if (article.getContentZh() == null || article.getContentZh().isBlank()) {
            throw new IllegalArgumentException("Translated article content is required for indexing");
        }

        List<ArticleChunk> chunks = chunkMapper.findByArticleId(article.getId());
        if (chunks.isEmpty()) {
            persistChunks(article);
            chunks = chunkMapper.findByArticleId(article.getId());
        }
        if (chunks.isEmpty()) {
            throw new IllegalStateException("No chunks were persisted for article " + article.getId());
        }

        int indexed = 0;
        for (ArticleChunk chunk : chunks) {
            if ("INDEXED".equals(chunk.getEmbeddingStatus())) {
                continue;
            }
            if (chunkMapper.claimForEmbedding(chunk.getId()) == 0) {
                continue;
            }
            try {
                List<Float> vector = embeddingService.embed(
                        chunk.getContentHash(), chunk.getContentZh());
                if (vector.size() != milvusProperties.embeddingDimension()) {
                    throw new IllegalStateException(
                            "Embedding dimension mismatch: expected "
                                    + milvusProperties.embeddingDimension() + ", got " + vector.size());
                }
                String vectorId = "chunk-" + chunk.getId();
                vectorStore.upsert(new VectorDocument(
                        vectorId,
                        article.getId(),
                        chunk.getId(),
                        chunk.getChunkIndex(),
                        article.getTitleZh(),
                        chunk.getContentZh(),
                        article.getCategory(),
                        article.getCanonicalUrl(),
                        article.getPublishedAt(),
                        chunk.getContentHash(),
                        vector));
                if (chunkMapper.markIndexed(
                        chunk.getId(), vectorId, llmProperties.embeddingModel()) != 1) {
                    throw new IllegalStateException("Chunk state changed concurrently: " + chunk.getId());
                }
                indexed++;
                heartbeat.run();
            } catch (RuntimeException exception) {
                chunkMapper.markFailed(chunk.getId(), errorMessage(exception));
                throw exception;
            }
        }
        return indexed;
    }

    private void persistChunks(NewsArticle article) {
        List<String> chineseChunks = chunker.chunk(
                article.getContentZh(),
                milvusProperties.chunkChars(),
                milvusProperties.chunkOverlapChars());
        List<String> englishChunks = chunker.chunk(
                article.getContentEn(),
                milvusProperties.chunkChars(),
                milvusProperties.chunkOverlapChars());

        for (int index = 0; index < chineseChunks.size(); index++) {
            String contentZh = chineseChunks.get(index);
            ArticleChunk chunk = new ArticleChunk();
            chunk.setArticleId(article.getId());
            chunk.setChunkIndex(index);
            chunk.setContentZh(contentZh);
            chunk.setContentEn(index < englishChunks.size() ? englishChunks.get(index) : null);
            chunk.setTokenCount(Math.max(1, (contentZh.length() + 1) / 2));
            chunk.setContentHash(contentHasher.sha256(contentZh));
            chunkMapper.insertIfAbsent(chunk);
        }
    }

    private String errorMessage(Exception exception) {
        return exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }
}

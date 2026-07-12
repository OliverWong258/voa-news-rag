package com.ptn.strategy.news.indexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

import com.ptn.strategy.config.LlmProperties;
import com.ptn.strategy.config.MilvusProperties;
import com.ptn.strategy.news.article.ContentHasher;
import com.ptn.strategy.news.article.NewsArticle;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArticleIndexingServiceTest {

    @Test
    void embedsAndIndexesPersistedChunks() {
        ArticleChunkMapper mapper = mock(ArticleChunkMapper.class);
        CachingEmbeddingService embeddings = mock(CachingEmbeddingService.class);
        VectorStore vectorStore = mock(VectorStore.class);
        MilvusProperties milvus = new MilvusProperties(
                "http://localhost:19530", "", "chunks", 4, 100, 20, 3, 5, 20, 0.45);
        LlmProperties llm = new LlmProperties(
                "http://localhost", "key", "chat", "embed",
                Duration.ofSeconds(5), 3, 100, 200, 1000);
        ArticleIndexingService service = new ArticleIndexingService(
                mapper,
                new SemanticChunker(),
                new ContentHasher(),
                embeddings,
                vectorStore,
                milvus,
                llm);

        NewsArticle article = new NewsArticle();
        article.setId(7L);
        article.setTitleZh("中文标题");
        article.setContentZh("这是用于向量索引的中文新闻正文。");
        article.setContentEn("English article body.");
        article.setCategory("Politics");
        article.setCanonicalUrl("https://www.voanews.com/a/story/123.html");

        ArticleChunk persisted = new ArticleChunk();
        persisted.setId(11L);
        persisted.setArticleId(7L);
        persisted.setChunkIndex(0);
        persisted.setContentZh(article.getContentZh());
        persisted.setContentHash(new ContentHasher().sha256(article.getContentZh()));
        persisted.setEmbeddingStatus("PENDING");
        when(mapper.findByArticleId(7L)).thenReturn(List.of(), List.of(persisted));
        when(mapper.claimForEmbedding(11L)).thenReturn(1);
        when(mapper.markIndexed(11L, "chunk-11", "embed")).thenReturn(1);
        when(embeddings.embed(any(), any())).thenReturn(List.of(0.1f, 0.2f, 0.3f, 0.4f));

        int indexed = service.index(article, () -> { });

        assertThat(indexed).isEqualTo(1);
        ArgumentCaptor<VectorDocument> documentCaptor = ArgumentCaptor.forClass(VectorDocument.class);
        verify(vectorStore).upsert(documentCaptor.capture());
        assertThat(documentCaptor.getValue().category()).isEqualTo("Politics");
        verify(mapper).markIndexed(11L, "chunk-11", "embed");
    }
}

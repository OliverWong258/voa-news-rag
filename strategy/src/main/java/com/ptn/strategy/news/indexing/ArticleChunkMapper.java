package com.ptn.strategy.news.indexing;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ArticleChunkMapper {

    @Insert("""
            INSERT IGNORE INTO article_chunk
                (article_id, chunk_index, content_en, content_zh, token_count, content_hash,
                 embedding_status)
            VALUES
                (#{articleId}, #{chunkIndex}, #{contentEn}, #{contentZh}, #{tokenCount},
                 #{contentHash}, 'PENDING')
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertIfAbsent(ArticleChunk chunk);

    @Select("SELECT * FROM article_chunk WHERE article_id = #{articleId} ORDER BY chunk_index")
    List<ArticleChunk> findByArticleId(@Param("articleId") long articleId);

    @Update("""
            UPDATE article_chunk
            SET embedding_status = 'EMBEDDING', embedding_error = NULL
            WHERE id = #{id} AND embedding_status IN ('PENDING', 'FAILED')
            """)
    int claimForEmbedding(@Param("id") long id);

    @Update("""
            UPDATE article_chunk
            SET embedding_status = 'INDEXED', milvus_vector_id = #{vectorId},
                embedding_model = #{model}, embedding_error = NULL
            WHERE id = #{id} AND embedding_status = 'EMBEDDING'
            """)
    int markIndexed(
            @Param("id") long id,
            @Param("vectorId") String vectorId,
            @Param("model") String model);

    @Update("""
            UPDATE article_chunk
            SET embedding_status = 'FAILED', embedding_retry_count = embedding_retry_count + 1,
                embedding_error = LEFT(#{error}, 4000)
            WHERE id = #{id} AND embedding_status = 'EMBEDDING'
            """)
    int markFailed(@Param("id") long id, @Param("error") String error);
}

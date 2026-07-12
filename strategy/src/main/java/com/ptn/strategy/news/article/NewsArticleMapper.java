package com.ptn.strategy.news.article;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NewsArticleMapper {

    @Insert("""
            INSERT IGNORE INTO news_article
                (source, source_article_id, url, canonical_url, title_en, content_en, author,
                 published_at, content_hash, language, raw_s3_key, processing_status)
            VALUES
                (#{source}, #{sourceArticleId}, #{url}, #{canonicalUrl}, #{titleEn}, #{contentEn},
                 #{author}, #{publishedAt}, #{contentHash}, #{language}, #{rawS3Key}, 'CRAWLED')
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertIfAbsent(NewsArticle article);

    @Select("SELECT * FROM news_article WHERE id = #{id}")
    NewsArticle findById(@Param("id") long id);

    @Select("SELECT * FROM news_article WHERE canonical_url = #{canonicalUrl} LIMIT 1")
    NewsArticle findByCanonicalUrl(@Param("canonicalUrl") String canonicalUrl);

    @Select("SELECT * FROM news_article WHERE content_hash = #{contentHash} LIMIT 1")
    NewsArticle findByContentHash(@Param("contentHash") String contentHash);

    @Update("""
            UPDATE news_article
            SET processing_status = 'TRANSLATING', translation_error = NULL
            WHERE id = #{id}
              AND processing_status IN ('CRAWLED', 'TRANSLATION_FAILED')
              AND translation_retry_count < #{maxAttempts}
            """)
    int claimForTranslation(@Param("id") long id, @Param("maxAttempts") int maxAttempts);

    @Update("""
            UPDATE news_article
            SET title_zh = #{titleZh}, content_zh = #{contentZh}, summary_zh = #{summaryZh},
                translation_model = #{model}, translated_at = CURRENT_TIMESTAMP(6),
                translation_error = NULL, processing_status = 'TRANSLATED'
            WHERE id = #{id} AND processing_status = 'TRANSLATING'
            """)
    int markTranslated(
            @Param("id") long id,
            @Param("titleZh") String titleZh,
            @Param("contentZh") String contentZh,
            @Param("summaryZh") String summaryZh,
            @Param("model") String model);

    @Update("""
            UPDATE news_article
            SET translation_retry_count = translation_retry_count + 1,
                translation_error = LEFT(#{error}, 4000),
                processing_status = CASE
                    WHEN translation_retry_count + 1 >= #{maxAttempts}
                    THEN 'TRANSLATION_DEAD' ELSE 'TRANSLATION_FAILED' END
            WHERE id = #{id} AND processing_status = 'TRANSLATING'
            """)
    int markTranslationFailed(
            @Param("id") long id,
            @Param("error") String error,
            @Param("maxAttempts") int maxAttempts);

    @Update("""
            UPDATE news_article
            SET processing_status = 'CRAWLED', translation_retry_count = 0, translation_error = NULL
            WHERE id = #{id} AND processing_status IN ('TRANSLATION_FAILED', 'TRANSLATION_DEAD')
            """)
    int resetTranslation(@Param("id") long id);

    @Update("""
            UPDATE news_article
            SET processing_status = 'TRANSLATION_FAILED', translation_error = LEFT(#{error}, 4000)
            WHERE id = #{id} AND processing_status = 'CRAWLED'
            """)
    int markTranslationDispatchFailed(@Param("id") long id, @Param("error") String error);
}

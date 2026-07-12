package com.ptn.strategy.news.article;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

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
}

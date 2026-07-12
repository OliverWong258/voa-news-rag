ALTER TABLE news_article
    ADD COLUMN indexing_retry_count INT NOT NULL DEFAULT 0 AFTER translated_at,
    ADD COLUMN indexing_error TEXT NULL AFTER indexing_retry_count,
    ADD COLUMN indexed_at DATETIME(6) NULL AFTER indexing_error;

ALTER TABLE article_chunk
    DROP INDEX uk_article_chunk_hash,
    ADD INDEX idx_article_chunk_hash (content_hash),
    ADD COLUMN embedding_retry_count INT NOT NULL DEFAULT 0 AFTER embedding_status,
    ADD COLUMN embedding_error TEXT NULL AFTER embedding_retry_count,
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6) AFTER created_at;

CREATE INDEX idx_news_article_indexing_state
    ON news_article (processing_status, indexing_retry_count);

UPDATE news_article
SET processing_status = 'INDEX_PENDING'
WHERE processing_status = 'TRANSLATED';

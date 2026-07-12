ALTER TABLE news_article
    ADD COLUMN translation_retry_count INT NOT NULL DEFAULT 0 AFTER processing_status,
    ADD COLUMN translation_error TEXT NULL AFTER translation_retry_count,
    ADD COLUMN translation_model VARCHAR(255) NULL AFTER translation_error,
    ADD COLUMN translated_at DATETIME(6) NULL AFTER translation_model;

CREATE INDEX idx_news_article_translation_state
    ON news_article (processing_status, translation_retry_count);

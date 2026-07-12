ALTER TABLE news_article
    ADD COLUMN category VARCHAR(128) NULL AFTER source;

CREATE INDEX idx_news_article_category_published
    ON news_article (category, published_at);

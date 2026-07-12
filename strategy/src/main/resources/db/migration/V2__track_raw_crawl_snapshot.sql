ALTER TABLE crawl_task
    ADD COLUMN raw_s3_key VARCHAR(1024) NULL AFTER error_message;

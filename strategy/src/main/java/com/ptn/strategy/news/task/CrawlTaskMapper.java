package com.ptn.strategy.news.task;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CrawlTaskMapper {

    @Insert("""
            INSERT IGNORE INTO crawl_task
                (url, canonical_url, task_type, status, retry_count, max_retries)
            VALUES
                (#{url}, #{canonicalUrl}, 'ARTICLE', 'PENDING', 0, #{maxRetries})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertIfAbsent(CrawlTask task);

    @Update("""
            UPDATE crawl_task
            SET status = 'DISPATCHED'
            WHERE id = #{id} AND status = 'PENDING'
            """)
    int markDispatched(@Param("id") long id);

    @Update("""
            UPDATE crawl_task
            SET status = 'CRAWLING', worker_id = #{workerId}, started_at = CURRENT_TIMESTAMP(6),
                error_message = NULL
            WHERE id = #{id}
              AND status IN ('DISPATCHED', 'FAILED', 'RETRYING')
              AND retry_count < max_retries
            """)
    int claimForCrawling(@Param("id") long id, @Param("workerId") String workerId);

    @Update("""
            UPDATE crawl_task
            SET status = 'CRAWLED', finished_at = CURRENT_TIMESTAMP(6), error_message = NULL
            WHERE id = #{id} AND status = 'CRAWLING'
            """)
    int markCrawled(@Param("id") long id);

    @Update("""
            UPDATE crawl_task SET raw_s3_key = #{rawS3Key}
            WHERE id = #{id} AND status = 'CRAWLING'
            """)
    int recordRawSnapshot(@Param("id") long id, @Param("rawS3Key") String rawS3Key);

    @Update("""
            UPDATE crawl_task
            SET status = CASE WHEN retry_count + 1 >= max_retries THEN 'DEAD' ELSE 'FAILED' END,
                retry_count = retry_count + 1,
                error_message = LEFT(#{errorMessage}, 4000),
                finished_at = CURRENT_TIMESTAMP(6)
            WHERE id = #{id} AND status = 'CRAWLING'
            """)
    int markFailed(@Param("id") long id, @Param("errorMessage") String errorMessage);

    @Select("SELECT * FROM crawl_task WHERE id = #{id}")
    CrawlTask findById(@Param("id") long id);

    @Select("""
            SELECT * FROM crawl_task
            WHERE status = 'CRAWLING' AND started_at < #{cutoff}
            ORDER BY started_at
            LIMIT #{limit}
            """)
    List<CrawlTask> findStaleCrawling(
            @Param("cutoff") LocalDateTime cutoff, @Param("limit") int limit);

    @Update("""
            UPDATE crawl_task
            SET status = 'RETRYING', worker_id = NULL,
                error_message = 'Recovered stale CRAWLING task'
            WHERE id = #{id} AND status = 'CRAWLING' AND started_at < #{cutoff}
            """)
    int markRetryingIfStale(@Param("id") long id, @Param("cutoff") LocalDateTime cutoff);

    @Update("""
            UPDATE crawl_task
            SET status = 'RETRYING', worker_id = NULL, error_message = NULL, retry_count = 0
            WHERE id = #{id} AND status IN ('FAILED', 'DEAD')
            """)
    int markRetrying(@Param("id") long id);

    @Update("""
            UPDATE crawl_task
            SET status = 'FAILED', error_message = LEFT(#{errorMessage}, 4000)
            WHERE id = #{id} AND status = 'RETRYING'
            """)
    int markRedispatchFailed(@Param("id") long id, @Param("errorMessage") String errorMessage);
}

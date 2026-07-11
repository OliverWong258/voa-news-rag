package com.ptn.strategy.news.task;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CrawlTaskMapper {

    @Insert("""
            INSERT IGNORE INTO crawl_task
                (url, canonical_url, task_type, status, retry_count, max_retries)
            VALUES
                (#{url}, #{canonicalUrl}, 'ARTICLE', 'PENDING', 0, 5)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertIfAbsent(CrawlTask task);

    @Update("""
            UPDATE crawl_task
            SET status = 'DISPATCHED'
            WHERE id = #{id} AND status = 'PENDING'
            """)
    int markDispatched(@Param("id") long id);
}

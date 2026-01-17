package com.ptn.strategy.mapper;

import com.ptn.strategy.entity.SinnerTask;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface SinnerTaskMapper {
    /**
     * 批量初始化任务。使用 INSERT IGNORE 确保如果名字已存在则跳过，不会报错。
     */
    @Insert("INSERT IGNORE INTO sinner_task (name_en, status, retry_count) VALUES (#{nameEn}, 0, 0)")
    int insertTask(String nameEn);

    /**
     * 获取所有待处理的任务名单
     */
    @Select("SELECT * FROM sinner_task WHERE status = 0")
    List<SinnerTask> findPendingTasks();

    /**
     * 获取所有爬取失败的任务名单
     */
    @Select("SELECT * FROM sinner_task WHERE status = 2")
    List<SinnerTask> findFailedTasks();

    /**
     * 更新任务状态（爬取成功后改为 1，失败改为 2）
     */
    @Update("UPDATE sinner_task SET status = #{status} WHERE name_en = #{nameEn}")
    void updateStatus(@Param("nameEn") String nameEn, @Param("status") Integer status);

    /**
     * 失败重试计数
     */
    @Update("UPDATE sinner_task SET retry_count = retry_count + 1 WHERE name_en = #{nameEn}")
    void incrementRetry(String nameEn);
}

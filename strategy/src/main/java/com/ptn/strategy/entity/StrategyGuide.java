package com.ptn.strategy.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StrategyGuide {
    private Long id;
    private String sourceId;      // B 站专栏 ID (cv_id)
    private String titleCn;       // 中文原标题
    private String sourceUrl;     // 原始链接
    private Integer status;       // 0-待处理, 1-处理中, 2-已完成
    private LocalDateTime createdAt;
}

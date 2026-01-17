package com.ptn.strategy.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SinnerTask {
    private Long id;
    private String nameEn;     // 角色英文名
    private Integer status;    // 状态: 0-待处理, 1-已完成, 2-失败
    private Integer retryCount;// 重试次数
    private LocalDateTime updatedAt;    
}

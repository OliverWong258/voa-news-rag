package com.ptn.strategy.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SinnerInfo {
    private Long id;
    private String nameEn;
    private String assessment; // 对应表中的稀有度
    private String roles;      // 对应表中的职业
    private String imageUrl;
    private String description;
    private String criminalRecord;
    private String performanceInServingTeam;
    private LocalDateTime createdAt;    
}

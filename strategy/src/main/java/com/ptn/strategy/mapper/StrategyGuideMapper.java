package com.ptn.strategy.mapper;

import com.ptn.strategy.entity.StrategyGuide;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface StrategyGuideMapper {
    @Insert("INSERT IGNORE INTO strategy_guide (source_id, title_cn, source_url, status) " +
            "VALUES (#{sourceId}, #{titleCn}, #{sourceUrl}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(StrategyGuide guide);
}

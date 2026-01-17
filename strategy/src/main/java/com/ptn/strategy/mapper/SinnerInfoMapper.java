package com.ptn.strategy.mapper;

import com.ptn.strategy.entity.SinnerInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SinnerInfoMapper {
    // 插入数据，并自动获取自增的 ID
    @Insert("INSERT INTO sinner_info (name_en, assessment, roles, image_url, description, criminal_record, performance_in_serving_team) " +
            "VALUES (#{nameEn}, #{assessment}, #{roles}, #{imageUrl}, #{description}, #{criminalRecord}, #{performanceInServingTeam})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SinnerInfo sinnerInfo);

    // 根据英文名查询，防止重复抓取
    @Select("SELECT * FROM sinner_info WHERE name_en = #{nameEn} LIMIT 1")
    SinnerInfo findByNameEn(String nameEn);
}

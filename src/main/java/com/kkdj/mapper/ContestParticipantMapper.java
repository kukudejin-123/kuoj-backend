package com.kkdj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kkdj.model.entity.ContestParticipant;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 比赛参赛者 Mapper
 */
public interface ContestParticipantMapper extends BaseMapper<ContestParticipant> {

    /**
     * 检查用户是否已报名比赛
     */
    @Select("SELECT COUNT(*) FROM contest_participant WHERE contestId = #{contestId} AND userId = #{userId} AND isDelete = 0")
    int countByContestIdAndUserId(@Param("contestId") Long contestId, @Param("userId") Long userId);

    /**
     * 统计比赛参赛人数
     */
    @Select("SELECT COUNT(*) FROM contest_participant WHERE contestId = #{contestId} AND isDelete = 0")
    int countByContestId(@Param("contestId") Long contestId);
}
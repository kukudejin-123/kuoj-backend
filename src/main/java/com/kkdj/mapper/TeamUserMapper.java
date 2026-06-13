package com.kkdj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kkdj.model.entity.TeamUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 团队用户 Mapper
 */
public interface TeamUserMapper extends BaseMapper<TeamUser> {

    /**
     * 查询用户所在的团队ID列表
     */
    @Select("SELECT teamId FROM team_user WHERE userId = #{userId} AND isDelete = 0")
    List<Long> listTeamIdsByUserId(@Param("userId") Long userId);

    /**
     * 查询团队中的用户ID列表
     */
    @Select("SELECT userId FROM team_user WHERE teamId = #{teamId} AND isDelete = 0")
    List<Long> listUserIdsByTeamId(@Param("teamId") Long teamId);

    /**
     * 检查用户是否在指定团队中
     */
    @Select("SELECT COUNT(*) FROM team_user WHERE teamId = #{teamId} AND userId = #{userId} AND isDelete = 0")
    int countByTeamIdAndUserId(@Param("teamId") Long teamId, @Param("userId") Long userId);

    /**
     * 检查用户是否在任意指定团队中
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM team_user WHERE userId = #{userId} AND isDelete = 0 " +
            "AND teamId IN " +
            "<foreach collection='teamIds' item='teamId' open='(' separator=',' close=')'>" +
            "#{teamId}" +
            "</foreach>" +
            "</script>")
    int countByTeamIdsAndUserId(@Param("teamIds") List<Long> teamIds, @Param("userId") Long userId);

    /**
     * 恢复已删除的团队成员记录
     */
    @Update("UPDATE team_user SET isDelete = 0, userRole = #{userRole}, joinTime = #{joinTime} WHERE teamId = #{teamId} AND userId = #{userId}")
    int restoreTeamUser(@Param("teamId") Long teamId, @Param("userId") Long userId, @Param("userRole") Integer userRole, @Param("joinTime") java.util.Date joinTime);

    /**
     * 检查用户是否曾经加入过团队（包括已退出的）
     */
    @Select("SELECT COUNT(*) FROM team_user WHERE teamId = #{teamId} AND userId = #{userId}")
    int countByTeamIdAndUserIdIncludeDeleted(@Param("teamId") Long teamId, @Param("userId") Long userId);
}
package com.kkdj.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kkdj.model.entity.TeamUser;

import java.util.Date;
import java.util.List;

/**
 * 团队用户服务
 */
public interface TeamUserService extends IService<TeamUser> {

    /**
     * 查询用户所在的团队ID列表
     */
    List<Long> listTeamIdsByUserId(Long userId);

    /**
     * 检查用户是否在指定团队中
     */
    boolean isUserInTeam(Long teamId, Long userId);

    /**
     * 检查用户是否在任意指定团队中
     */
    boolean isUserInTeams(List<Long> teamIds, Long userId);

    /**
     * 获取用户在团队中的角色
     */
    Integer getUserRoleInTeam(Long teamId, Long userId);

    /**
     * 检查用户是否曾经加入过团队（包括已退出的）
     */
    boolean hasUserEverJoinedTeam(Long teamId, Long userId);

    /**
     * 恢复已删除的团队成员记录
     */
    boolean restoreTeamUser(Long teamId, Long userId, Integer userRole, Date joinTime);
}
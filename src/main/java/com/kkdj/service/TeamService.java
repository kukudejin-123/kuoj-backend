package com.kkdj.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kkdj.model.dto.team.*;
import com.kkdj.model.entity.Team;
import com.kkdj.model.entity.TeamUser;
import com.kkdj.model.entity.User;
import com.kkdj.model.vo.TeamVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import javax.servlet.http.HttpServletRequest;

/**
 * 团队服务
 */
public interface TeamService extends IService<Team> {

    /**
     * 校验团队数据
     */
    void validTeam(Team team, boolean add);

    /**
     * 获取查询条件
     */
    QueryWrapper<Team> getQueryWrapper(TeamQueryRequest teamQueryRequest);

    /**
     * 获取团队VO
     */
    TeamVO getTeamVO(Team team, HttpServletRequest request);

    /**
     * 分页获取团队VO
     */
    Page<TeamVO> getTeamVOPage(Page<Team> teamPage, HttpServletRequest request);

    /**
     * 创建团队
     */
    Long addTeam(TeamAddRequest teamAddRequest, User loginUser);

    /**
     * 更新团队
     */
    Boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

    /**
     * 退出团队
     */
    Boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);

    /**
     * 邀请成员
     */
    Boolean inviteUser(TeamInviteRequest teamInviteRequest, User loginUser);

    /**
     * 移除成员
     */
    Boolean kickUser(TeamKickRequest teamKickRequest, User loginUser);

    /**
     * 检查用户是否在团队中
     */
    boolean isUserInTeam(Long teamId, Long userId);

    /**
     * 获取用户在团队中的角色
     */
    Integer getUserRoleInTeam(Long teamId, Long userId);

    /**
     * 获取团队成员列表
     */
    Page<TeamUser> listTeamMembers(Long teamId, long current, long pageSize);
}
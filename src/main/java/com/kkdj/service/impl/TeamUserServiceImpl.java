package com.kkdj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kkdj.mapper.TeamUserMapper;
import com.kkdj.model.entity.TeamUser;
import com.kkdj.service.TeamUserService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 团队用户服务实现
 */
@Service
public class TeamUserServiceImpl extends ServiceImpl<TeamUserMapper, TeamUser> implements TeamUserService {

    @Override
    public List<Long> listTeamIdsByUserId(Long userId) {
        return baseMapper.listTeamIdsByUserId(userId);
    }

    @Override
    public boolean isUserInTeam(Long teamId, Long userId) {
        return baseMapper.countByTeamIdAndUserId(teamId, userId) > 0;
    }

    @Override
    public boolean isUserInTeams(List<Long> teamIds, Long userId) {
        if (teamIds == null || teamIds.isEmpty()) {
            return false;
        }
        return baseMapper.countByTeamIdsAndUserId(teamIds, userId) > 0;
    }

    @Override
    public Integer getUserRoleInTeam(Long teamId, Long userId) {
        QueryWrapper<TeamUser> wrapper = new QueryWrapper<>();
        wrapper.eq("teamId", teamId);
        wrapper.eq("userId", userId);
        wrapper.eq("isDelete", 0);
        TeamUser teamUser = this.getOne(wrapper);
        return teamUser != null ? teamUser.getUserRole() : null;
    }

    @Override
    public boolean hasUserEverJoinedTeam(Long teamId, Long userId) {
        return baseMapper.countByTeamIdAndUserIdIncludeDeleted(teamId, userId) > 0;
    }

    @Override
    public boolean restoreTeamUser(Long teamId, Long userId, Integer userRole, Date joinTime) {
        return baseMapper.restoreTeamUser(teamId, userId, userRole, joinTime) > 0;
    }
}
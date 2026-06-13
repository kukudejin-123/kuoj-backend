package com.kkdj.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kkdj.common.ErrorCode;
import com.kkdj.constant.CommonConstant;
import com.kkdj.exception.BusinessException;
import com.kkdj.exception.ThrowUtils;
import com.kkdj.mapper.TeamMapper;
import com.kkdj.mapper.TeamUserMapper;
import com.kkdj.model.dto.team.*;
import com.kkdj.model.entity.Team;
import com.kkdj.model.entity.TeamUser;
import com.kkdj.model.entity.User;
import com.kkdj.model.enums.TeamUserRoleEnum;
import com.kkdj.model.vo.TeamUserVO;
import com.kkdj.model.vo.TeamVO;
import com.kkdj.model.vo.UserVO;
import com.kkdj.service.TeamService;
import com.kkdj.service.TeamUserService;
import com.kkdj.service.UserService;
import com.kkdj.utils.SqlUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 团队服务实现
 */
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamService {

    @Resource
    private TeamUserMapper teamUserMapper;

    @Resource
    private TeamUserService teamUserService;

    @Resource
    private UserService userService;

    @Override
    public void validTeam(Team team, boolean add) {
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String teamName = team.getTeamName();
        String teamDesc = team.getTeamDesc();
        Integer maxMemberCount = team.getMaxMemberCount();
        // 创建时，参数不能为空
        if (add) {
            ThrowUtils.throwIf(StringUtils.isBlank(teamName), ErrorCode.PARAMS_ERROR, "团队名称不能为空");
        }
        // 有参数则校验
        if (StringUtils.isNotBlank(teamName) && teamName.length() > 256) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "团队名称过长");
        }
        if (StringUtils.isNotBlank(teamDesc) && teamDesc.length() > 1024) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "团队描述过长");
        }
        if (maxMemberCount != null && maxMemberCount < 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "最大成员数量不能小于1");
        }
    }

    @Override
    public QueryWrapper<Team> getQueryWrapper(TeamQueryRequest teamQueryRequest) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        if (teamQueryRequest == null) {
            return queryWrapper;
        }
        Long id = teamQueryRequest.getId();
        String teamName = teamQueryRequest.getTeamName();
        String teamDesc = teamQueryRequest.getTeamDesc();
        Long userId = teamQueryRequest.getUserId();
        Long memberId = teamQueryRequest.getMemberId();
        Integer status = teamQueryRequest.getStatus();
        String sortField = teamQueryRequest.getSortField();
        String sortOrder = teamQueryRequest.getSortOrder();

        // 拼接查询条件（使用驼峰字段名）
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.like(StringUtils.isNotBlank(teamName), "teamName", teamName);
        queryWrapper.like(StringUtils.isNotBlank(teamDesc), "teamDesc", teamDesc);
        queryWrapper.eq(userId != null, "userId", userId);
        queryWrapper.eq(status != null, "status", status);
        // 排序
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
            CommonConstant.SORT_ORDER_ASC.equals(sortOrder), sortField);
        return queryWrapper;
    }

    @Override
    public TeamVO getTeamVO(Team team, HttpServletRequest request) {
        TeamVO teamVO = TeamVO.objToVo(team);
        // 1. 关联查询创建者信息
        Long userId = team.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        teamVO.setCreateUser(userVO);

        // 2. 查询当前用户是否已加入团队
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser != null) {
            boolean hasJoin = teamUserService.isUserInTeam(team.getId(), loginUser.getId());
            teamVO.setHasJoin(hasJoin);
            if (hasJoin) {
                Integer userRole = teamUserService.getUserRoleInTeam(team.getId(), loginUser.getId());
                teamVO.setUserRole(userRole);
            }
        } else {
            teamVO.setHasJoin(false);
        }
        return teamVO;
    }

    @Override
    public Page<TeamVO> getTeamVOPage(Page<Team> teamPage, HttpServletRequest request) {
        List<Team> teamList = teamPage.getRecords();
        Page<TeamVO> teamVOPage = new Page<>(teamPage.getCurrent(), teamPage.getSize(), teamPage.getTotal());
        if (CollUtil.isEmpty(teamList)) {
            return teamVOPage;
        }
        // 1. 关联查询创建者信息
        Set<Long> userIdSet = teamList.stream().map(Team::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        // 2. 查询当前用户已加入的团队
        User loginUser = userService.getLoginUserPermitNull(request);
        Set<Long> joinedTeamIds = new HashSet<>();
        Map<Long, Integer> teamUserRoleMap = new HashMap<>();
        if (loginUser != null) {
            List<Long> joinedTeamIdList = teamUserMapper.listTeamIdsByUserId(loginUser.getId());
            joinedTeamIds.addAll(joinedTeamIdList);
            // 查询用户在各团队的角色
            if (!joinedTeamIds.isEmpty()) {
                QueryWrapper<TeamUser> wrapper = new QueryWrapper<>();
                wrapper.in("teamId", joinedTeamIds);
                wrapper.eq("userId", loginUser.getId());
                List<TeamUser> teamUsers = teamUserService.list(wrapper);
                teamUserRoleMap = teamUsers.stream()
                        .collect(Collectors.toMap(TeamUser::getTeamId, TeamUser::getUserRole));
            }
        }

        // 3. 填充信息
        Map<Long, Integer> finalTeamUserRoleMap = teamUserRoleMap;
        List<TeamVO> teamVOList = teamList.stream().map(team -> {
            TeamVO teamVO = TeamVO.objToVo(team);
            Long userId = team.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            teamVO.setCreateUser(userService.getUserVO(user));
            teamVO.setHasJoin(joinedTeamIds.contains(team.getId()));
            teamVO.setUserRole(finalTeamUserRoleMap.get(team.getId()));
            return teamVO;
        }).collect(Collectors.toList());
        teamVOPage.setRecords(teamVOList);
        return teamVOPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addTeam(TeamAddRequest teamAddRequest, User loginUser) {
        // 1. 校验参数
        String teamName = teamAddRequest.getTeamName();
        if (StringUtils.isBlank(teamName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "团队名称不能为空");
        }
        // 2. 确定队长（如果指定了队长则使用指定的，否则使用当前创建者）
        Long captainId = teamAddRequest.getCaptainUserId();
        if (captainId == null) {
            captainId = loginUser.getId();
        } else {
            // 验证指定的队长用户是否存在
            User captainUser = userService.getById(captainId);
            ThrowUtils.throwIf(captainUser == null, ErrorCode.NOT_FOUND_ERROR, "指定的队长用户不存在");
        }
        // 3. 创建团队
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest, team);
        team.setUserId(captainId); // 设置队长
        team.setMemberCount(1);
        if (team.getMaxMemberCount() == null) {
            team.setMaxMemberCount(50);
        }
        this.validTeam(team, true);
        boolean result = this.save(team);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建团队失败");
        // 4. 队长加入团队
        TeamUser teamUser = new TeamUser();
        teamUser.setTeamId(team.getId());
        teamUser.setUserId(captainId);
        teamUser.setUserRole(TeamUserRoleEnum.CAPTAIN.getValue());
        teamUser.setJoinTime(new Date());
        result = teamUserService.save(teamUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "加入团队失败");
        return team.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        if (teamUpdateRequest == null || teamUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = teamUpdateRequest.getId();
        Team oldTeam = this.getById(id);
        ThrowUtils.throwIf(oldTeam == null, ErrorCode.NOT_FOUND_ERROR, "团队不存在");
        // 只有队长或管理员可以更新
        if (!oldTeam.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamUpdateRequest, team);
        this.validTeam(team, false);
        return this.updateById(team);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
        if (teamQuitRequest == null || teamQuitRequest.getTeamId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamQuitRequest.getTeamId();
        Team team = this.getById(teamId);
        ThrowUtils.throwIf(team == null, ErrorCode.NOT_FOUND_ERROR, "团队不存在");
        // 检查是否已加入
        if (!teamUserService.isUserInTeam(teamId, loginUser.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未加入该团队");
        }
        Integer userRole = teamUserService.getUserRoleInTeam(teamId, loginUser.getId());
        // 如果是队长，需要先转让队长或解散团队
        if (TeamUserRoleEnum.CAPTAIN.getValue().equals(userRole)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队长不能退出团队，请先转让队长或解散团队");
        }
        // 退出团队
        QueryWrapper<TeamUser> wrapper = new QueryWrapper<>();
        wrapper.eq("teamId", teamId);
        wrapper.eq("userId", loginUser.getId());
        boolean result = teamUserService.remove(wrapper);
        if (result) {
            // 更新团队成员数量
            team.setMemberCount(team.getMemberCount() - 1);
            this.updateById(team);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean inviteUser(TeamInviteRequest teamInviteRequest, User loginUser) {
        if (teamInviteRequest == null || teamInviteRequest.getTeamId() == null || teamInviteRequest.getUserId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamInviteRequest.getTeamId();
        // 处理字符串类型的 userId，避免前端大数精度丢失
        Long userId;
        try {
            userId = Long.parseLong(teamInviteRequest.getUserId());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID格式错误");
        }
        Team team = this.getById(teamId);
        ThrowUtils.throwIf(team == null, ErrorCode.NOT_FOUND_ERROR, "团队不存在");
        // 只有队长可以邀请成员
        if (!team.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只有队长可以邀请成员");
        }
        // 检查被邀请用户是否存在
        User inviteUser = userService.getById(userId);
        ThrowUtils.throwIf(inviteUser == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        // 检查是否已加入
        if (teamUserService.isUserInTeam(teamId, userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已加入该团队");
        }
        // 检查人数是否已满
        if (team.getMemberCount() >= team.getMaxMemberCount()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "团队人数已满");
        }

        boolean result;
        Date joinTime = new Date();
        Integer userRole = TeamUserRoleEnum.MEMBER.getValue();

        // 检查用户是否曾经加入过（被移除后重新邀请）
        if (teamUserService.hasUserEverJoinedTeam(teamId, userId)) {
            // 恢复已删除的记录
            result = teamUserService.restoreTeamUser(teamId, userId, userRole, joinTime);
        } else {
            // 新用户加入团队
            TeamUser teamUser = new TeamUser();
            teamUser.setTeamId(teamId);
            teamUser.setUserId(userId);
            teamUser.setUserRole(userRole);
            teamUser.setJoinTime(joinTime);
            result = teamUserService.save(teamUser);
        }

        if (result) {
            // 更新团队成员数量
            team.setMemberCount(team.getMemberCount() + 1);
            this.updateById(team);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean kickUser(TeamKickRequest teamKickRequest, User loginUser) {
        if (teamKickRequest == null || teamKickRequest.getTeamId() == null || teamKickRequest.getUserId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamKickRequest.getTeamId();
        Long userId = teamKickRequest.getUserId();
        Team team = this.getById(teamId);
        ThrowUtils.throwIf(team == null, ErrorCode.NOT_FOUND_ERROR, "团队不存在");
        // 只有队长可以移除成员
        if (!team.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只有队长可以移除成员");
        }
        // 不能移除自己
        if (userId.equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能移除自己");
        }
        // 检查被移除用户是否在团队中
        if (!teamUserService.isUserInTeam(teamId, userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不在该团队中");
        }
        // 移除成员
        QueryWrapper<TeamUser> wrapper = new QueryWrapper<>();
        wrapper.eq("teamId", teamId);
        wrapper.eq("userId", userId);
        boolean result = teamUserService.remove(wrapper);
        if (result) {
            // 更新团队成员数量
            team.setMemberCount(team.getMemberCount() - 1);
            this.updateById(team);
        }
        return result;
    }

    @Override
    public boolean isUserInTeam(Long teamId, Long userId) {
        return teamUserService.isUserInTeam(teamId, userId);
    }

    @Override
    public Integer getUserRoleInTeam(Long teamId, Long userId) {
        return teamUserService.getUserRoleInTeam(teamId, userId);
    }

    @Override
    public Page<TeamUser> listTeamMembers(Long teamId, long current, long pageSize) {
        QueryWrapper<TeamUser> wrapper = new QueryWrapper<>();
        wrapper.eq("teamId", teamId);
        wrapper.orderByDesc("userRole");
        return teamUserService.page(new Page<>(current, pageSize), wrapper);
    }
}
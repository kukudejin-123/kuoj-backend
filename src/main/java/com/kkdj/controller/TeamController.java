package com.kkdj.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kkdj.annotation.AuthCheck;
import com.kkdj.common.BaseResponse;
import com.kkdj.common.DeleteRequest;
import com.kkdj.common.ErrorCode;
import com.kkdj.common.ResultUtils;
import com.kkdj.constant.UserConstant;
import com.kkdj.exception.BusinessException;
import com.kkdj.exception.ThrowUtils;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 团队接口
 */
@RestController
@RequestMapping("/team")
@Slf4j
public class TeamController {

    @Resource
    private TeamService teamService;

    @Resource
    private TeamUserService teamUserService;

    @Resource
    private UserService userService;

    // region 增删改查

    /**
     * 创建团队
     * 仅管理员可创建
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request) {
        if (teamAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long teamId = teamService.addTeam(teamAddRequest, loginUser);
        return ResultUtils.success(teamId);
    }

    /**
     * 删除团队（解散）
     * 仅队长或管理员可删除
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        // 判断是否存在
        Team oldTeam = teamService.getById(id);
        ThrowUtils.throwIf(oldTeam == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅队长或管理员可删除
        User loginUser = userService.getLoginUser(request);
        if (!oldTeam.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 删除团队成员关联
        QueryWrapper<TeamUser> wrapper = new QueryWrapper<>();
        wrapper.eq("teamId", id);
        teamUserService.remove(wrapper);
        // 删除团队
        boolean b = teamService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新团队
     * 仅队长或管理员可更新
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request) {
        if (teamUpdateRequest == null || teamUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.updateTeam(teamUpdateRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取团队
     */
    @GetMapping("/get")
    public BaseResponse<TeamVO> getTeamById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamService.getById(id);
        if (team == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(teamService.getTeamVO(team, request));
    }

    /**
     * 分页获取团队列表
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<TeamVO>> listTeamByPage(@RequestBody TeamQueryRequest teamQueryRequest, HttpServletRequest request) {
        long current = teamQueryRequest.getCurrent();
        long size = teamQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Team> teamPage = teamService.page(new Page<>(current, size),
                teamService.getQueryWrapper(teamQueryRequest));
        return ResultUtils.success(teamService.getTeamVOPage(teamPage, request));
    }

    /**
     * 获取我的团队列表
     */
    @PostMapping("/my/list")
    public BaseResponse<Page<TeamVO>> listMyTeam(@RequestBody TeamQueryRequest teamQueryRequest, HttpServletRequest request) {
        if (teamQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        // 查询用户所在的团队ID列表
        List<Long> teamIds = teamUserService.listTeamIdsByUserId(loginUser.getId());
        if (teamIds.isEmpty()) {
            Page<TeamVO> emptyPage = new Page<>(teamQueryRequest.getCurrent(), teamQueryRequest.getPageSize(), 0);
            return ResultUtils.success(emptyPage);
        }
        // 查询团队列表
        QueryWrapper<Team> wrapper = teamService.getQueryWrapper(teamQueryRequest);
        wrapper.in("id", teamIds);
        long current = teamQueryRequest.getCurrent();
        long size = teamQueryRequest.getPageSize();
        Page<Team> teamPage = teamService.page(new Page<>(current, size), wrapper);
        return ResultUtils.success(teamService.getTeamVOPage(teamPage, request));
    }

    // endregion

    // region 团队成员操作

    /**
     * 退出团队
     */
    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
        if (teamQuitRequest == null || teamQuitRequest.getTeamId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.quitTeam(teamQuitRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 邀请成员
     * 仅队长可邀请
     */
    @PostMapping("/invite")
    public BaseResponse<Boolean> inviteUser(@RequestBody TeamInviteRequest teamInviteRequest, HttpServletRequest request) {
        if (teamInviteRequest == null || teamInviteRequest.getTeamId() == null || teamInviteRequest.getUserId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.inviteUser(teamInviteRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 移除成员
     * 仅队长可操作
     */
    @PostMapping("/kick")
    public BaseResponse<Boolean> kickUser(@RequestBody TeamKickRequest teamKickRequest, HttpServletRequest request) {
        if (teamKickRequest == null || teamKickRequest.getTeamId() == null || teamKickRequest.getUserId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.kickUser(teamKickRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 转让队长
     */
    @PostMapping("/transfer")
    public BaseResponse<Boolean> transferCaptain(@RequestBody TeamInviteRequest transferRequest, HttpServletRequest request) {
        if (transferRequest == null || transferRequest.getTeamId() == null || transferRequest.getUserId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = transferRequest.getTeamId();
        // 处理字符串类型的 userId
        Long newCaptainId;
        try {
            newCaptainId = Long.parseLong(transferRequest.getUserId());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID格式错误");
        }
        Team team = teamService.getById(teamId);
        ThrowUtils.throwIf(team == null, ErrorCode.NOT_FOUND_ERROR, "团队不存在");

        User loginUser = userService.getLoginUser(request);
        // 只有队长可以转让
        if (!team.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只有队长可以转让");
        }
        // 检查新队长是否在团队中
        if (!teamUserService.isUserInTeam(teamId, newCaptainId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不在该团队中");
        }
        // 更新原队长的角色
        QueryWrapper<TeamUser> oldCaptainWrapper = new QueryWrapper<>();
        oldCaptainWrapper.eq("teamId", teamId);
        oldCaptainWrapper.eq("userId", loginUser.getId());
        TeamUser oldCaptainTeamUser = teamUserService.getOne(oldCaptainWrapper);
        if (oldCaptainTeamUser != null) {
            oldCaptainTeamUser.setUserRole(TeamUserRoleEnum.MEMBER.getValue());
            teamUserService.updateById(oldCaptainTeamUser);
        }
        // 更新新队长的角色
        QueryWrapper<TeamUser> newCaptainWrapper = new QueryWrapper<>();
        newCaptainWrapper.eq("teamId", teamId);
        newCaptainWrapper.eq("userId", newCaptainId);
        TeamUser newCaptainTeamUser = teamUserService.getOne(newCaptainWrapper);
        if (newCaptainTeamUser != null) {
            newCaptainTeamUser.setUserRole(TeamUserRoleEnum.CAPTAIN.getValue());
            teamUserService.updateById(newCaptainTeamUser);
        }
        // 更新团队的队长
        team.setUserId(newCaptainId);
        boolean result = teamService.updateById(team);
        return ResultUtils.success(result);
    }

    /**
     * 获取团队成员列表
     */
    @PostMapping("/members")
    public BaseResponse<Page<TeamUserVO>> listTeamMembers(@RequestBody TeamUserQueryRequest teamUserQueryRequest, HttpServletRequest request) {
        if (teamUserQueryRequest == null || teamUserQueryRequest.getTeamId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamUserQueryRequest.getTeamId();
        Team team = teamService.getById(teamId);
        ThrowUtils.throwIf(team == null, ErrorCode.NOT_FOUND_ERROR, "团队不存在");

        long current = teamUserQueryRequest.getCurrent();
        long size = teamUserQueryRequest.getPageSize();
        Page<TeamUser> teamUserPage = teamService.listTeamMembers(teamId, current, size);

        // 转换为VO
        Page<TeamUserVO> teamUserVOPage = new Page<>(teamUserPage.getCurrent(), teamUserPage.getSize(), teamUserPage.getTotal());
        List<TeamUser> teamUserList = teamUserPage.getRecords();
        if (teamUserList.isEmpty()) {
            return ResultUtils.success(teamUserVOPage);
        }
        // 关联用户信息
        Set<Long> userIdSet = teamUserList.stream().map(TeamUser::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        List<TeamUserVO> teamUserVOList = teamUserList.stream().map(teamUser -> {
            TeamUserVO teamUserVO = TeamUserVO.objToVo(teamUser);
            Long userId = teamUser.getUserId();
            if (userIdUserListMap.containsKey(userId)) {
                User user = userIdUserListMap.get(userId).get(0);
                teamUserVO.setUser(userService.getUserVO(user));
            }
            return teamUserVO;
        }).collect(Collectors.toList());
        teamUserVOPage.setRecords(teamUserVOList);
        return ResultUtils.success(teamUserVOPage);
    }

    // endregion
}
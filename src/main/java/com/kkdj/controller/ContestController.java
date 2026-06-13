package com.kkdj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kkdj.annotation.AuthCheck;
import com.kkdj.common.BaseResponse;
import com.kkdj.common.DeleteRequest;
import com.kkdj.common.ErrorCode;
import com.kkdj.common.ResultUtils;
import com.kkdj.constant.UserConstant;
import com.kkdj.exception.BusinessException;
import com.kkdj.exception.ThrowUtils;
import com.kkdj.model.dto.contest.ContestAddRequest;
import com.kkdj.model.dto.contest.ContestQueryRequest;
import com.kkdj.model.dto.contest.ContestUpdateRequest;
import com.kkdj.model.entity.Contest;
import com.kkdj.model.entity.User;
import com.kkdj.model.vo.ContestVO;
import com.kkdj.service.ContestParticipantService;
import com.kkdj.service.ContestService;
import com.kkdj.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 比赛接口
 */
@RestController
@RequestMapping("/contest")
@Slf4j
public class ContestController {

    @Resource
    private ContestService contestService;

    @Resource
    private ContestParticipantService contestParticipantService;

    @Resource
    private UserService userService;

    // region 增删改查

    /**
     * 创建比赛
     * 仅管理员可创建
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addContest(@RequestBody ContestAddRequest contestAddRequest, HttpServletRequest request) {
        if (contestAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long contestId = contestService.addContest(contestAddRequest, loginUser);
        return ResultUtils.success(contestId);
    }

    /**
     * 删除比赛
     * 仅管理员可删除
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteContest(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        // 判断是否存在
        Contest oldContest = contestService.getById(id);
        ThrowUtils.throwIf(oldContest == null, ErrorCode.NOT_FOUND_ERROR, "比赛不存在");
        // 仅管理员可删除
        boolean result = contestService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 更新比赛
     * 仅管理员可更新
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateContest(@RequestBody ContestUpdateRequest contestUpdateRequest, HttpServletRequest request) {
        if (contestUpdateRequest == null || contestUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = contestService.updateContest(contestUpdateRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 根据id获取比赛
     */
    @GetMapping("/get")
    public BaseResponse<ContestVO> getContestById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Contest contest = contestService.getById(id);
        ThrowUtils.throwIf(contest == null, ErrorCode.NOT_FOUND_ERROR, "比赛不存在");
        ContestVO contestVO = contestService.getContestVO(contest, request);
        return ResultUtils.success(contestVO);
    }

    /**
     * 分页获取比赛列表
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<ContestVO>> listContestByPage(@RequestBody ContestQueryRequest contestQueryRequest, HttpServletRequest request) {
        long current = contestQueryRequest.getCurrent();
        long size = contestQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Contest> contestPage = contestService.page(new Page<>(current, size),
                contestService.getQueryWrapper(contestQueryRequest));
        return ResultUtils.success(contestService.getContestVOPage(contestPage, request));
    }

    /**
     * 获取比赛题目列表
     */
    @GetMapping("/questions")
    public BaseResponse<List<ContestVO>> getContestQuestions(long contestId, HttpServletRequest request) {
        if (contestId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 检查比赛是否存在
        Contest contest = contestService.getById(contestId);
        ThrowUtils.throwIf(contest == null, ErrorCode.NOT_FOUND_ERROR, "比赛不存在");
        // 检查用户是否已报名（比赛进行中才能看到题目）
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser != null) {
            boolean hasJoin = contestParticipantService.isUserJoined(contestId, loginUser.getId());
            ThrowUtils.throwIf(!hasJoin && !userService.isAdmin(loginUser),
                    ErrorCode.NO_AUTH_ERROR, "请先报名参加比赛");
        }
        List<ContestVO> contestQuestions = contestService.getContestQuestions(contestId, request);
        return ResultUtils.success(contestQuestions);
    }

    // endregion

    // region 报名相关

    /**
     * 报名比赛
     */
    @PostMapping("/join")
    public BaseResponse<Boolean> joinContest(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Object contestIdObj = body.get("contestId");
        if (contestIdObj == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long contestId = Long.parseLong(contestIdObj.toString());
        if (contestId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Contest contest = contestService.getById(contestId);
        ThrowUtils.throwIf(contest == null, ErrorCode.NOT_FOUND_ERROR, "比赛不存在");
        // 检查比赛是否已结束
        ThrowUtils.throwIf(contest.getStatus() == 2, ErrorCode.PARAMS_ERROR, "比赛已结束，无法报名");
        User loginUser = userService.getLoginUser(request);
        // 检查是否有报名资格（加入任意团队即可）
        ThrowUtils.throwIf(!contestService.canJoinContest(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "请先加入一个团队");
        boolean result = contestParticipantService.joinContest(contestId, loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 取消报名
     */
    @PostMapping("/quit")
    public BaseResponse<Boolean> quitContest(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Object contestIdObj = body.get("contestId");
        if (contestIdObj == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long contestId = Long.parseLong(contestIdObj.toString());
        if (contestId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Contest contest = contestService.getById(contestId);
        ThrowUtils.throwIf(contest == null, ErrorCode.NOT_FOUND_ERROR, "比赛不存在");
        // 检查比赛是否已开始
        ThrowUtils.throwIf(contest.getStatus() == 1, ErrorCode.PARAMS_ERROR, "比赛进行中，无法取消报名");
        ThrowUtils.throwIf(contest.getStatus() == 2, ErrorCode.PARAMS_ERROR, "比赛已结束，无法取消报名");
        User loginUser = userService.getLoginUser(request);
        boolean result = contestParticipantService.quitContest(contestId, loginUser.getId());
        return ResultUtils.success(result);
    }

    // endregion
}
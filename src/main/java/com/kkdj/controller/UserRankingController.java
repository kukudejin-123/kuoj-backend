package com.kkdj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kkdj.common.BaseResponse;
import com.kkdj.common.ErrorCode;
import com.kkdj.common.ResultUtils;
import com.kkdj.exception.BusinessException;
import com.kkdj.exception.ThrowUtils;
import com.kkdj.model.dto.ranking.UserRankingQueryRequest;
import com.kkdj.model.entity.User;
import com.kkdj.model.vo.ContestRankingVO;
import com.kkdj.model.vo.UserRankingVO;
import com.kkdj.service.ContestRankingService;
import com.kkdj.service.ContestService;
import com.kkdj.service.UserRankingService;
import com.kkdj.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 排行榜接口
 */
@RestController
@RequestMapping("/ranking")
@Slf4j
public class UserRankingController {

    @Resource
    private UserRankingService userRankingService;

    @Resource
    private ContestRankingService contestRankingService;

    @Resource
    private ContestService contestService;

    @Resource
    private UserService userService;

    /**
     * 获取总排行榜（分页）
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<UserRankingVO>> listUserRankingByPage(@RequestBody UserRankingQueryRequest queryRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 50, ErrorCode.PARAMS_ERROR);
        Page<UserRankingVO> rankingPage = userRankingService.getUserRankingVOPage(
                new Page<>(current, size), queryRequest);
        return ResultUtils.success(rankingPage);
    }

    /**
     * 获取当前用户排名信息
     */
    @GetMapping("/user")
    public BaseResponse<UserRankingVO> getUserRanking(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        UserRankingVO userRanking = userRankingService.getUserRanking(loginUser.getId());
        return ResultUtils.success(userRanking);
    }

    /**
     * 获取指定用户排名信息
     */
    @GetMapping("/user/{userId}")
    public BaseResponse<UserRankingVO> getUserRankingById(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserRankingVO userRanking = userRankingService.getUserRanking(userId);
        return ResultUtils.success(userRanking);
    }

    /**
     * 获取比赛排行榜
     */
    @GetMapping("/contest/{contestId}")
    public BaseResponse<List<ContestRankingVO>> getContestRanking(@PathVariable Long contestId) {
        if (contestId == null || contestId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 检查比赛是否存在
        ThrowUtils.throwIf(contestService.getById(contestId) == null, ErrorCode.NOT_FOUND_ERROR, "比赛不存在");
        List<ContestRankingVO> ranking = contestRankingService.getRanking(contestId);
        return ResultUtils.success(ranking);
    }
}
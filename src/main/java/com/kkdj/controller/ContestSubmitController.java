package com.kkdj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kkdj.common.BaseResponse;
import com.kkdj.common.ErrorCode;
import com.kkdj.common.ResultUtils;
import com.kkdj.exception.BusinessException;
import com.kkdj.exception.ThrowUtils;
import com.kkdj.model.dto.contestsubmit.ContestSubmitAddRequest;
import com.kkdj.model.dto.contestsubmit.ContestSubmitQueryRequest;
import com.kkdj.model.entity.ContestSubmit;
import com.kkdj.model.entity.User;
import com.kkdj.model.vo.ContestSubmitVO;
import com.kkdj.service.ContestSubmitService;
import com.kkdj.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 比赛提交接口
 */
@RestController
@RequestMapping("/contest_submit")
@Slf4j
public class ContestSubmitController {

    @Resource
    private ContestSubmitService contestSubmitService;

    @Resource
    private UserService userService;

    /**
     * 提交代码
     */
    @PostMapping("/do")
    public BaseResponse<Long> doContestSubmit(@RequestBody ContestSubmitAddRequest contestSubmitAddRequest, HttpServletRequest request) {
        if (contestSubmitAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long submitId = contestSubmitService.doContestSubmit(contestSubmitAddRequest, loginUser);
        return ResultUtils.success(submitId);
    }

    /**
     * 根据id获取提交详情
     */
    @GetMapping("/get")
    public BaseResponse<ContestSubmitVO> getContestSubmitById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ContestSubmit contestSubmit = contestSubmitService.getById(id);
        ThrowUtils.throwIf(contestSubmit == null, ErrorCode.NOT_FOUND_ERROR, "提交不存在");
        User loginUser = userService.getLoginUser(request);
        ContestSubmitVO contestSubmitVO = contestSubmitService.getContestSubmitVOWithCode(contestSubmit, loginUser);
        return ResultUtils.success(contestSubmitVO);
    }

    /**
     * 分页获取提交列表
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<ContestSubmitVO>> listContestSubmitByPage(@RequestBody ContestSubmitQueryRequest contestSubmitQueryRequest, HttpServletRequest request) {
        long current = contestSubmitQueryRequest.getCurrent();
        long size = contestSubmitQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<ContestSubmit> contestSubmitPage = contestSubmitService.page(new Page<>(current, size),
                contestSubmitService.getQueryWrapper(contestSubmitQueryRequest));
        return ResultUtils.success(contestSubmitService.getContestSubmitVOPage(contestSubmitPage, request));
    }
}
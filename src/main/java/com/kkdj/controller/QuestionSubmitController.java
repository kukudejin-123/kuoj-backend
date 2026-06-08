package com.kkdj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kkdj.common.BaseResponse;
import com.kkdj.common.ErrorCode;
import com.kkdj.common.ResultUtils;
import com.kkdj.exception.BusinessException;
import com.kkdj.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.kkdj.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.kkdj.model.entity.QuestionSubmit;
import com.kkdj.model.entity.User;
import com.kkdj.model.vo.QuestionSubmitVO;
import com.kkdj.service.QuestionSubmitService;
import com.kkdj.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 题目提交接口
*/
@RestController
@RequestMapping("/question_submit")
@Slf4j
@Deprecated
public class QuestionSubmitController {

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private UserService userService;

    /**
     * 提交题目
     *
     * @param questionSubmitAddRequest
     * @param request
     * @return 提交记录的id
     */
    @PostMapping("/do")
    public BaseResponse<Long> doQuestionSubmit(@RequestBody QuestionSubmitAddRequest questionSubmitAddRequest,
            HttpServletRequest request) {
        if (questionSubmitAddRequest == null || questionSubmitAddRequest.getQuestionId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 登录才能提交记录
        final User loginUser = userService.getLoginUser(request);
        long resultId = questionSubmitService.doQuestionSubmit(questionSubmitAddRequest, loginUser);
        return ResultUtils.success(resultId);
    }

    /**
     * 分页获取题目提交列表（除管理员外，普通用户只能看到公开信息，比如语言、题目标签等）
     *
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<QuestionSubmitVO>> listQuestionSubmitByPage(@RequestBody QuestionSubmitQueryRequest questionSubmitQueryRequest,
                                                                         HttpServletRequest request) {
        long current = questionSubmitQueryRequest.getCurrent();
        long size = questionSubmitQueryRequest.getPageSize();
        Page<QuestionSubmit> questionPage = questionSubmitService.page(new Page<>(current, size),
                // 获取的是所有的列表
                questionSubmitService.getQueryWrapper(questionSubmitQueryRequest));
        // 获取用户信息
        final User loginUser = userService.getLoginUser(request);
        // 进行过滤，只获取公开信息
        return ResultUtils.success(questionSubmitService.getQuestionSubmitVOPage(questionPage, loginUser));
    }

}

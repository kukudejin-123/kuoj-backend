package com.kkdj.controller;

import cn.hutool.json.JSONUtil;
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
import com.kkdj.mapper.UserMapper;
import com.kkdj.model.dto.question.*;
import com.kkdj.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.kkdj.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.kkdj.model.entity.Question;
import com.kkdj.model.entity.QuestionSubmit;
import com.kkdj.model.entity.User;
import com.kkdj.model.entity.Contest;
import com.kkdj.model.vo.QuestionAdminVo;
import com.kkdj.model.vo.QuestionSubmitVO;
import com.kkdj.model.vo.QuestionVO;
import com.kkdj.service.QuestionService;
import com.kkdj.service.QuestionSubmitService;
import com.kkdj.service.UserService;
import com.kkdj.service.ContestService;
import com.kkdj.service.ContestParticipantService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 题目接口
*/
@RestController
@RequestMapping("/question")
@Slf4j
public class QuestionController {

    @Resource
    private QuestionService questionService;

    @Resource
    private UserService userService;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private ContestService contestService;

    @Resource
    private ContestParticipantService contestParticipantService;

    // region 增删改查

    /**
     * 创建（仅管理员）
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addQuestion(@RequestBody QuestionAddRequest questionAddRequest, HttpServletRequest request) {
        if (questionAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionAddRequest, question);
        List<String> tags = questionAddRequest.getTags();
        if (tags != null) {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        List<JudgeCase> judgeCase = questionAddRequest.getJudgeCase();
        if (judgeCase != null) {
            question.setJudgeCase(JSONUtil.toJsonStr(judgeCase));
        }
        JudgeConfig judgeConfig = questionAddRequest.getJudgeConfig();
        if (judgeConfig != null) {
            question.setJudgeConfig(JSONUtil.toJsonStr(judgeConfig));
        }
        // 设置默认公开状态
        if (question.getIsPublic() == null) {
            question.setIsPublic(1);
        }
        // 管理员创建的题目默认审核通过
        if (question.getReviewStatus() == null) {
            question.setReviewStatus(1);
        }
        questionService.validQuestion(question, true);
        User loginUser = userService.getLoginUser(request);
        question.setUserId(loginUser.getId());
        question.setFavourNum(0);
        question.setThumbNum(0);

        // 自动生成题号：查询当前最大题号，+1
        // 使用 MAX 聚合查询获取最大题号
        QueryWrapper<Question> maxQueryWrapper = new QueryWrapper<>();
        maxQueryWrapper.select("MAX(questionNumber) as maxNumber");
        Map<String, Object> maxResult = questionService.getMap(maxQueryWrapper);
        int nextNumber = 1001; // 默认从1001开始
        if (maxResult != null && maxResult.get("maxNumber") != null) {
            Object maxNumberObj = maxResult.get("maxNumber");
            if (maxNumberObj instanceof Number) {
                nextNumber = ((Number) maxNumberObj).intValue() + 1;
            } else if (maxNumberObj instanceof String) {
                nextNumber = Integer.parseInt((String) maxNumberObj) + 1;
            }
        }

        // 检查题号是否已存在，如果存在则递增直到找到可用的题号
        while (true) {
            QueryWrapper<Question> existQuery = new QueryWrapper<>();
            existQuery.eq("questionNumber", nextNumber);
            long count = questionService.count(existQuery);
            if (count == 0) {
                break; // 题号可用
            }
            nextNumber++;
        }
        question.setQuestionNumber(nextNumber);

        boolean result = questionService.save(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newQuestionId = question.getId();
        return ResultUtils.success(newQuestionId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteQuestion(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldQuestion.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = questionService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param questionUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateQuestion(@RequestBody QuestionUpdateRequest questionUpdateRequest) {
        if (questionUpdateRequest == null || questionUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionUpdateRequest, question);
        // 标签
        List<String> tags = questionUpdateRequest.getTags();
        if (tags != null) {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        // 判题用例
        List<JudgeCase> judgeCase = questionUpdateRequest.getJudgeCase();
        if (judgeCase != null) {
            question.setJudgeCase(JSONUtil.toJsonStr(judgeCase));
        }
        // 判题配置
        JudgeConfig judgeConfig = questionUpdateRequest.getJudgeConfig();
        if (judgeConfig != null) {
            question.setJudgeConfig(JSONUtil.toJsonStr(judgeConfig));
        }
        // 参数校验
        questionService.validQuestion(question, false);
        long id = questionUpdateRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = questionService.updateById(question);
        return ResultUtils.success(result);
    }
    /**
     * 根据 id 获取（全部信息）
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Question> getQuestionById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = questionService.getById(id);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 是否为管理员
        User loginUser = userService.getLoginUser(request);
        // 不是本人或管理员不允许访问
        if (!question.getUserId().equals(loginUser.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return ResultUtils.success(question);
    }

    /**
     * 根据 id 获取（脱敏后）
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<QuestionVO> getQuestionVOById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = questionService.getById(id);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 检查题目是否公开
        if (question.getIsPublic() != null && question.getIsPublic() == 0) {
            User loginUser = userService.getLoginUserPermitNull(request);
            // 管理员可以查看所有题目
            if (!userService.isAdmin(loginUser)) {
                // 非公开题目，检查是否是比赛题目
                Long contestId = question.getContestId();
                if (contestId != null && contestId > 0) {
                    // 是比赛题目，检查用户是否已报名
                    if (loginUser == null) {
                        throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "请先报名参加比赛");
                    }
                    boolean joined = contestParticipantService.isUserJoined(contestId, loginUser.getId());
                    if (!joined) {
                        throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "请先报名参加比赛");
                    }
                    // 已报名，允许查看
                } else {
                    // 不是比赛题目，非公开则拒绝
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该题目未公开，无法查看");
                }
            }
        }
        return ResultUtils.success(questionService.getQuestionVO(question, request));
    }

    /**
     * 分页获取列表（仅管理员）
     *
     * @param questionQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<QuestionAdminVo>> listQuestionByPage(@RequestBody QuestionQueryRequest questionQueryRequest, HttpServletRequest request) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 管理员可以查看所有题目（包括不公开的）
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getAdminQueryWrapper(questionQueryRequest));
        return ResultUtils.success(questionService.getQuestionAdminVOPage(questionPage, request));
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
            HttpServletRequest request) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 判断是否为管理员，管理员可以看到所有题目
        User loginUser = userService.getLoginUserPermitNull(request);
        Page<Question> questionPage;
        if (userService.isAdmin(loginUser)) {
            questionPage = questionService.page(new Page<>(current, size),
                    questionService.getAdminQueryWrapper(questionQueryRequest));
        } else {
            questionPage = questionService.page(new Page<>(current, size),
                    questionService.getQueryWrapper(questionQueryRequest));
        }
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listMyQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
            HttpServletRequest request) {
        if (questionQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        questionQueryRequest.setUserId(loginUser.getId());
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    // endregion


    /**
     * 编辑（用户）
     *
     * @param questionEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editQuestion(@RequestBody QuestionEditRequest questionEditRequest, HttpServletRequest request) {
        if (questionEditRequest == null || questionEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionEditRequest, question);
        List<String> tags = questionEditRequest.getTags();
        if (tags != null) {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        // 判题用例
        List<JudgeCase> judgeCase = questionEditRequest.getJudgeCase();
        if (judgeCase != null) {
            question.setJudgeCase(JSONUtil.toJsonStr(judgeCase));
        }
        // 判题配置
        JudgeConfig judgeConfig = questionEditRequest.getJudgeConfig();
        if (judgeConfig != null) {
            question.setJudgeConfig(JSONUtil.toJsonStr(judgeConfig));
        }
        // 参数校验
        questionService.validQuestion(question, false);
        User loginUser = userService.getLoginUser(request);
        long id = questionEditRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldQuestion.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = questionService.updateById(question);
        return ResultUtils.success(result);
    }


    /**
     * 提交题目
     *
     * @param questionSubmitAddRequest
     * @param request
     * @return 提交记录的id
     */
    @PostMapping("/question_submit/do")
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
    @PostMapping("/question_submit/list/page")
    public BaseResponse<Page<QuestionSubmitVO>> listQuestionSubmitByPage(@RequestBody QuestionSubmitQueryRequest questionSubmitQueryRequest,
                                                                         HttpServletRequest request) {
        long current = questionSubmitQueryRequest.getCurrent();
        long size = questionSubmitQueryRequest.getPageSize();

        // 用户名筛选：先查询用户ID
        String userName = questionSubmitQueryRequest.getUserName();
        if (StringUtils.isNotBlank(userName)) {
            QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
            userQueryWrapper.like("userName", userName);
            List<User> users = userService.list(userQueryWrapper);
            if (users.isEmpty()) {
                // 没有匹配的用户，返回空结果
                Page<QuestionSubmitVO> emptyPage = new Page<>(current, size, 0);
                emptyPage.setRecords(new ArrayList<>());
                return ResultUtils.success(emptyPage);
            }
            // 设置用户ID列表作为查询条件
            List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());
            // 这里简化处理，只取第一个匹配的用户ID（如果需要支持多用户，需要修改查询逻辑）
            questionSubmitQueryRequest.setUserId(userIds.get(0));
            questionSubmitQueryRequest.setUserName(null); // 清空userName，避免后续处理
        }

        Page<QuestionSubmit> questionPage = questionSubmitService.page(new Page<>(current, size),
                // 获取的是所有的列表
                questionSubmitService.getQueryWrapper(questionSubmitQueryRequest));
        // 获取用户信息
        final User loginUser = userService.getLoginUser(request);
        // 进行过滤，只获取公开信息
        return ResultUtils.success(questionSubmitService.getQuestionSubmitVOPage(questionPage, loginUser));
    }


    /**
     * 根据 id 获取题目提交记录（脱敏）
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/question_submit/get/vo")
    public BaseResponse<QuestionSubmitVO> getQuestionSubmitVOById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QuestionSubmit questionSubmit = questionSubmitService.getById(id);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(questionSubmitService.getQuestionSubmitVO(questionSubmit, loginUser));
    }

    /**
     * 获取所有标签列表
     *
     * @return 标签列表
     */
    @GetMapping("/tags")
    public BaseResponse<List<String>> getAllTags() {
        // 查询所有题目的标签字段
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("tags");
        queryWrapper.isNotNull("tags");
        List<Question> questions = questionService.list(queryWrapper);

        // 提取所有标签并去重
        Set<String> tagSet = new HashSet<>();
        for (Question question : questions) {
            String tagsStr = question.getTags();
            if (StringUtils.isNotBlank(tagsStr)) {
                List<String> tags = JSONUtil.toList(tagsStr, String.class);
                tagSet.addAll(tags);
            }
        }

        // 转换为列表并排序
        List<String> tagList = new ArrayList<>(tagSet);
        Collections.sort(tagList);
        return ResultUtils.success(tagList);
    }

    /**
     * 获取用户提交统计信息
     *
     * @param userId 可选，指定要查询的用户ID；不传则查询当前登录用户
     * @return 统计数据 {submitCount, acceptCount}
     */
    @GetMapping("/question_submit/statistics")
    public BaseResponse<Map<String, Long>> getUserSubmitStatistics(
            @RequestParam(required = false) Long userId,
            HttpServletRequest request) {
        Long targetUserId;
        if (userId != null && userId > 0) {
            // 查询指定用户的统计（公开接口，用于查看他人主页）
            targetUserId = userId;
        } else {
            User loginUser = userService.getLoginUser(request);
            targetUserId = loginUser.getId();
        }
        long[] statistics = questionSubmitService.getUserSubmitStatistics(targetUserId);
        Map<String, Long> result = new HashMap<>();
        result.put("submitCount", statistics[0]);
        result.put("acceptCount", statistics[1]);
        return ResultUtils.success(result);
    }

    /**
     * 获取全局统计信息（首页展示用）
     *
     * @return 统计数据 {totalQuestions, totalSubmissions, totalUsers, totalContests}
     */
    @GetMapping("/statistics")
    public BaseResponse<Map<String, Long>> getGlobalStatistics() {
        Map<String, Long> result = new HashMap<>();

        // 题目总数
        long totalQuestions = questionService.count();
        result.put("totalQuestions", totalQuestions);

        // 总提交数 - 从题目表累加submitNum
        QueryWrapper<Question> submitQueryWrapper = new QueryWrapper<>();
        submitQueryWrapper.select("submitNum");
        List<Question> questions = questionService.list(submitQueryWrapper);
        long totalSubmissions = questions.stream()
                .mapToLong(q -> q.getSubmitNum() == null ? 0 : q.getSubmitNum())
                .sum();
        result.put("totalSubmissions", totalSubmissions);

        // 用户总数
        long totalUsers = userService.count();
        result.put("totalUsers", totalUsers);

        // 比赛总数
        long totalContests = contestService.count();
        result.put("totalContests", totalContests);

        return ResultUtils.success(result);
    }

    // endregion

    // region 用户出题与审核

    /**
     * 用户提交出题（需要管理员审核）
     */
    @PostMapping("/user/add")
    public BaseResponse<Long> addUserQuestion(@RequestBody QuestionAddRequest questionAddRequest, HttpServletRequest request) {
        if (questionAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        // 管理员不能出题（管理员直接用 /question/add）
        if (userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "管理员请使用管理后台创建题目");
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionAddRequest, question);
        List<String> tags = questionAddRequest.getTags();
        if (tags != null) {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        List<JudgeCase> judgeCase = questionAddRequest.getJudgeCase();
        if (judgeCase != null) {
            question.setJudgeCase(JSONUtil.toJsonStr(judgeCase));
        }
        JudgeConfig judgeConfig = questionAddRequest.getJudgeConfig();
        if (judgeConfig != null) {
            question.setJudgeConfig(JSONUtil.toJsonStr(judgeConfig));
        }
        // 用户出题：默认不公开，审核状态为待审核
        question.setIsPublic(0);
        question.setReviewStatus(0);
        question.setUserId(loginUser.getId());
        question.setFavourNum(0);
        question.setThumbNum(0);
        question.setSubmitNum(0);
        question.setAcceptedNum(0);
        questionService.validQuestion(question, true);

        // 自动生成题号
        QueryWrapper<Question> maxQueryWrapper = new QueryWrapper<>();
        maxQueryWrapper.select("MAX(questionNumber) as maxNumber");
        Map<String, Object> maxResult = questionService.getMap(maxQueryWrapper);
        int nextNumber = 1001;
        if (maxResult != null && maxResult.get("maxNumber") != null) {
            Object maxNumberObj = maxResult.get("maxNumber");
            if (maxNumberObj instanceof Number) {
                nextNumber = ((Number) maxNumberObj).intValue() + 1;
            } else if (maxNumberObj instanceof String) {
                nextNumber = Integer.parseInt((String) maxNumberObj) + 1;
            }
        }
        while (true) {
            QueryWrapper<Question> existQuery = new QueryWrapper<>();
            existQuery.eq("questionNumber", nextNumber);
            long count = questionService.count(existQuery);
            if (count == 0) {
                break;
            }
            nextNumber++;
        }
        question.setQuestionNumber(nextNumber);

        boolean result = questionService.save(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "出题失败");
        return ResultUtils.success(question.getId());
    }

    /**
     * 获取我的出题列表
     */
    @PostMapping("/my/questions")
    public BaseResponse<Page<QuestionVO>> listMyQuestions(@RequestBody QuestionQueryRequest questionQueryRequest,
            HttpServletRequest request) {
        if (questionQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        questionQueryRequest.setUserId(loginUser.getId());
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 获取待审核题目列表（仅管理员）
     */
    @PostMapping("/review/list")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<QuestionAdminVo>> listReviewQuestions(@RequestBody QuestionQueryRequest questionQueryRequest) {
        if (questionQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("review_status", 0); // 待审核
        String title = questionQueryRequest.getTitle();
        if (StringUtils.isNotBlank(title)) {
            queryWrapper.like("title", title);
        }
        queryWrapper.orderByDesc("createTime");
        Page<Question> questionPage = questionService.page(new Page<>(current, size), queryWrapper);
        return ResultUtils.success(questionService.getQuestionAdminVOPage(questionPage, null));
    }

    /**
     * 审核题目（仅管理员）
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> reviewQuestion(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Object idObj = body.get("id");
        Object statusObj = body.get("reviewStatus");
        if (idObj == null || statusObj == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id;
        Integer reviewStatus;
        try {
            id = Long.parseLong(idObj.toString());
            reviewStatus = Integer.parseInt(statusObj.toString());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数格式错误");
        }
        if (reviewStatus != 1 && reviewStatus != 2) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "审核状态只能是1（通过）或2（拒绝）");
        }
        Question question = questionService.getById(id);
        ThrowUtils.throwIf(question == null, ErrorCode.NOT_FOUND_ERROR, "题目不存在");

        User loginUser = userService.getLoginUser(request);
        Question update = new Question();
        update.setId(id);
        update.setReviewStatus(reviewStatus);
        update.setReviewTime(new Date());
        update.setReviewUserId(loginUser.getId());
        String remark = body.get("reviewRemark") != null ? body.get("reviewRemark").toString() : null;
        update.setReviewRemark(remark);
        // 审核通过时自动公开，审核拒绝时设为不公开
        if (reviewStatus == 1) {
            update.setIsPublic(1);
        } else {
            update.setIsPublic(0);
        }
        boolean result = questionService.updateById(update);
        return ResultUtils.success(result);
    }

    // endregion

}

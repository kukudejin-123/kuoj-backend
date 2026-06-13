package com.kkdj.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kkdj.common.ErrorCode;
import com.kkdj.constant.CommonConstant;
import com.kkdj.exception.BusinessException;
import com.kkdj.exception.ThrowUtils;
import com.kkdj.mapper.ContestSubmitMapper;
import com.kkdj.model.dto.contestsubmit.ContestSubmitAddRequest;
import com.kkdj.model.dto.contestsubmit.ContestSubmitQueryRequest;
import com.kkdj.model.entity.Contest;
import com.kkdj.model.entity.ContestQuestion;
import com.kkdj.model.entity.ContestSubmit;
import com.kkdj.model.entity.Question;
import com.kkdj.model.entity.User;
import com.kkdj.model.enums.ContestStatusEnum;
import com.kkdj.model.enums.QuestionSubmitStatusEnum;
import com.kkdj.model.vo.ContestQuestionVO;
import com.kkdj.model.vo.ContestSubmitVO;
import com.kkdj.judge.service.JudgeService;
import com.kkdj.model.entity.QuestionSubmit;
import com.kkdj.service.QuestionSubmitService;
import com.kkdj.model.vo.UserVO;
import com.kkdj.service.ContestParticipantService;
import com.kkdj.service.ContestQuestionService;
import com.kkdj.service.ContestRankingService;
import com.kkdj.service.ContestService;
import com.kkdj.service.ContestSubmitService;
import com.kkdj.service.QuestionService;
import com.kkdj.service.UserService;
import com.kkdj.utils.SqlUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 比赛提交服务实现
 */
@Service
public class ContestSubmitServiceImpl extends ServiceImpl<ContestSubmitMapper, ContestSubmit> implements ContestSubmitService {

    @Resource
    private ContestService contestService;

    @Resource
    private ContestQuestionService contestQuestionService;

    @Resource
    private ContestParticipantService contestParticipantService;

    @Resource
    private QuestionService questionService;

    @Resource
    private UserService userService;

    @Resource
    private JudgeService judgeService;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private ContestRankingService contestRankingService;

    @Override
    public QueryWrapper<ContestSubmit> getQueryWrapper(ContestSubmitQueryRequest contestSubmitQueryRequest) {
        QueryWrapper<ContestSubmit> queryWrapper = new QueryWrapper<>();
        if (contestSubmitQueryRequest == null) {
            return queryWrapper;
        }
        Long contestId = contestSubmitQueryRequest.getContestId();
        Long questionId = contestSubmitQueryRequest.getQuestionId();
        Long userId = contestSubmitQueryRequest.getUserId();
        String status = contestSubmitQueryRequest.getStatus();
        String sortField = contestSubmitQueryRequest.getSortField();
        String sortOrder = contestSubmitQueryRequest.getSortOrder();

        queryWrapper.eq(contestId != null, "contestId", contestId);
        queryWrapper.eq(questionId != null, "questionId", questionId);
        queryWrapper.eq(userId != null, "userId", userId);
        queryWrapper.eq(StringUtils.isNotBlank(status), "status", status);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
            CommonConstant.SORT_ORDER_ASC.equals(sortOrder), sortField);
        return queryWrapper;
    }

    @Override
    public ContestSubmitVO getContestSubmitVO(ContestSubmit contestSubmit, HttpServletRequest request) {
        ContestSubmitVO vo = ContestSubmitVO.objToVo(contestSubmit);
        // 1. 关联查询用户信息
        Long userId = contestSubmit.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        vo.setUser(userVO);

        // 2. 获取题目顺序字母
        ContestQuestion contestQuestion = contestQuestionService.getByContestAndQuestion(
                contestSubmit.getContestId(), contestSubmit.getQuestionId());
        if (contestQuestion != null) {
            vo.setQuestionLabel(ContestQuestionVO.getOrderLabel(contestQuestion.getQuestionOrder()));
        }
        return vo;
    }

    @Override
    public Page<ContestSubmitVO> getContestSubmitVOPage(Page<ContestSubmit> contestSubmitPage, HttpServletRequest request) {
        List<ContestSubmit> contestSubmitList = contestSubmitPage.getRecords();
        Page<ContestSubmitVO> contestSubmitVOPage = new Page<>(contestSubmitPage.getCurrent(), contestSubmitPage.getSize(), contestSubmitPage.getTotal());
        if (CollUtil.isEmpty(contestSubmitList)) {
            return contestSubmitVOPage;
        }
        // 1. 关联查询用户信息
        Set<Long> userIdSet = contestSubmitList.stream().map(ContestSubmit::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        // 2. 获取题目顺序映射
        Long contestId = contestSubmitList.get(0).getContestId();
        QueryWrapper<ContestQuestion> wrapper = new QueryWrapper<>();
        wrapper.eq("contestId", contestId);
        List<ContestQuestion> contestQuestions = contestQuestionService.list(wrapper);
        Map<Long, ContestQuestion> questionOrderMap = contestQuestions.stream()
                .collect(Collectors.toMap(ContestQuestion::getQuestionId, cq -> cq));

        // 3. 填充信息
        User loginUser = userService.getLoginUserPermitNull(request);
        List<ContestSubmitVO> contestSubmitVOList = contestSubmitList.stream().map(contestSubmit -> {
            ContestSubmitVO vo = ContestSubmitVO.objToVo(contestSubmit);
            Long userId = contestSubmit.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            vo.setUser(userService.getUserVO(user));
            // 设置题目顺序字母
            ContestQuestion cq = questionOrderMap.get(contestSubmit.getQuestionId());
            if (cq != null) {
                vo.setQuestionLabel(ContestQuestionVO.getOrderLabel(cq.getQuestionOrder()));
            }
            // 代码隔离：非本人不可见（无论比赛是否结束），管理员可见所有
            if (loginUser == null || (!contestSubmit.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser))) {
                vo.setCode(null);
            }
            return vo;
        }).collect(Collectors.toList());
        contestSubmitVOPage.setRecords(contestSubmitVOList);
        return contestSubmitVOPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long doContestSubmit(ContestSubmitAddRequest contestSubmitAddRequest, User loginUser) {
        Long contestId = contestSubmitAddRequest.getContestId();
        Long questionId = contestSubmitAddRequest.getQuestionId();
        String language = contestSubmitAddRequest.getLanguage();
        String code = contestSubmitAddRequest.getCode();

        // 1. 校验参数
        ThrowUtils.throwIf(contestId == null || questionId == null, ErrorCode.PARAMS_ERROR, "比赛或题目ID不能为空");
        ThrowUtils.throwIf(StringUtils.isBlank(language), ErrorCode.PARAMS_ERROR, "编程语言不能为空");
        ThrowUtils.throwIf(StringUtils.isBlank(code), ErrorCode.PARAMS_ERROR, "代码不能为空");

        // 2. 校验比赛是否存在且进行中
        Contest contest = contestService.getById(contestId);
        ThrowUtils.throwIf(contest == null, ErrorCode.NOT_FOUND_ERROR, "比赛不存在");
        ThrowUtils.throwIf(!ContestStatusEnum.RUNNING.getValue().equals(contest.getStatus()),
                ErrorCode.OPERATION_ERROR, "比赛未开始或已结束");

        // 3. 校验用户是否已报名
        ThrowUtils.throwIf(!contestParticipantService.isUserJoined(contestId, loginUser.getId()),
                ErrorCode.OPERATION_ERROR, "请先报名参加比赛");

        // 4. 校验题目是否属于该比赛
        ContestQuestion contestQuestion = contestQuestionService.getByContestAndQuestion(contestId, questionId);
        ThrowUtils.throwIf(contestQuestion == null, ErrorCode.PARAMS_ERROR, "题目不属于该比赛");

        // 5. 校验题目是否存在
        Question question = questionService.getById(questionId);
        ThrowUtils.throwIf(question == null, ErrorCode.NOT_FOUND_ERROR, "题目不存在");

        // 6. 创建提交记录
        ContestSubmit submit = new ContestSubmit();
        submit.setContestId(contestId);
        submit.setQuestionId(questionId);
        submit.setUserId(loginUser.getId());
        submit.setLanguage(language);
        submit.setCode(code);
        submit.setStatus(QuestionSubmitStatusEnum.WAITING.getValue());
        submit.setSubmitTime(new Date());
        submit.setIsFirstAccept(0);

        boolean saved = this.save(submit);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "提交失败");

        // 7. 异步判题（通过创建QuestionSubmit来复用现有判题逻辑）
        Long submitId = submit.getId();
        final Long contestIdFinal = contestId;
        final Long questionIdFinal = questionId;
        final Long userIdFinal = loginUser.getId();
        final int questionOrder = contestQuestion.getQuestionOrder();
        final Date contestStartTime = contest.getStartTime();
        CompletableFuture.runAsync(() -> {
            try {
                // 创建QuestionSubmit记录用于判题
                QuestionSubmit questionSubmit = new QuestionSubmit();
                questionSubmit.setLanguage(language);
                questionSubmit.setCode(code);
                questionSubmit.setQuestionId(questionIdFinal);
                questionSubmit.setUserId(userIdFinal);
                questionSubmit.setStatus(QuestionSubmitStatusEnum.WAITING.getValue());
                questionSubmit.setCreateTime(new Date());
                questionSubmit.setUpdateTime(new Date());
                questionSubmit.setIsDelete(0);
                questionSubmitService.save(questionSubmit);

                // 调用判题
                judgeService.doJudge(questionSubmit.getId());

                // 获取判题结果并更新ContestSubmit
                QuestionSubmit result = questionSubmitService.getById(questionSubmit.getId());
                ContestSubmit update = new ContestSubmit();
                update.setId(submitId);
                update.setStatus(result.getStatus());
                update.setJudgeInfo(result.getJudgeInfo());
                this.updateById(update);

                // 更新排行榜（ACM规则）
                boolean isAccepted = QuestionSubmitStatusEnum.SUCCEED.getValue().equals(result.getStatus());
                if (isAccepted) {
                    // 计算提交时间（分钟，从比赛开始计算）
                    long submitTimeMinutes = (submit.getSubmitTime().getTime() - contestStartTime.getTime()) / (60 * 1000);
                    // 统计该题的错误次数
                    QueryWrapper<ContestSubmit> wrapper = new QueryWrapper<>();
                    wrapper.eq("contestId", contestIdFinal);
                    wrapper.eq("questionId", questionIdFinal);
                    wrapper.eq("userId", userIdFinal);
                    wrapper.ne("status", QuestionSubmitStatusEnum.SUCCEED.getValue());
                    long wrongCount = ContestSubmitServiceImpl.this.count(wrapper);
                    contestRankingService.updateRanking(contestIdFinal, userIdFinal, questionOrder, true, (int) submitTimeMinutes, (int) wrongCount);
                } else {
                    // 记录错误提交（不计入通过）
                    QueryWrapper<ContestSubmit> wrapper = new QueryWrapper<>();
                    wrapper.eq("contestId", contestIdFinal);
                    wrapper.eq("questionId", questionIdFinal);
                    wrapper.eq("userId", userIdFinal);
                    wrapper.ne("status", QuestionSubmitStatusEnum.SUCCEED.getValue());
                    long wrongCount = ContestSubmitServiceImpl.this.count(wrapper);
                    contestRankingService.updateRanking(contestIdFinal, userIdFinal, questionOrder, false, 0, (int) wrongCount);
                }
            } catch (Exception e) {
                // 判题失败，更新状态
                ContestSubmit update = new ContestSubmit();
                update.setId(submitId);
                update.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
                this.updateById(update);
            }
        });

        return submitId;
    }

    @Override
    public ContestSubmitVO getContestSubmitVOWithCode(ContestSubmit contestSubmit, User loginUser) {
        ContestSubmitVO vo = ContestSubmitVO.objToVo(contestSubmit);
        // 代码隔离：非本人不可见（无论比赛是否结束），管理员可见所有
        if (!contestSubmit.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            vo.setCode(null);
        }
        // 关联用户信息
        User user = userService.getById(contestSubmit.getUserId());
        vo.setUser(userService.getUserVO(user));
        // 获取题目顺序
        ContestQuestion contestQuestion = contestQuestionService.getByContestAndQuestion(
                contestSubmit.getContestId(), contestSubmit.getQuestionId());
        if (contestQuestion != null) {
            vo.setQuestionLabel(ContestQuestionVO.getOrderLabel(contestQuestion.getQuestionOrder()));
        }
        return vo;
    }
}
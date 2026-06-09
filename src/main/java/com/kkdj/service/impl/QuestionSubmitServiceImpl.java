package com.kkdj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kkdj.common.ErrorCode;
import com.kkdj.constant.CommonConstant;
import com.kkdj.exception.BusinessException;
import com.kkdj.judge.service.JudgeService;
import com.kkdj.mapper.QuestionMapper;
import com.kkdj.mapper.QuestionSubmitMapper;
import com.kkdj.mapper.UserMapper;
import com.kkdj.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.kkdj.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.kkdj.model.entity.Question;
import com.kkdj.model.entity.QuestionSubmit;
import com.kkdj.model.entity.User;
import com.kkdj.model.enums.QuestionSubmitLanguageEnum;
import com.kkdj.model.enums.QuestionSubmitStatusEnum;
import com.kkdj.model.vo.QuestionSubmitVO;
import com.kkdj.model.vo.QuestionVO;
import com.kkdj.model.vo.UserVO;
import com.kkdj.service.QuestionService;
import com.kkdj.service.QuestionSubmitService;
import com.kkdj.utils.SqlUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
* @author 19457
* @description 针对表【qustion_submit(题目提交表)】的数据库操作Service实现
* @createDate 2024-05-20 11:30:37
*/
@Service
public class QuestionSubmitServiceImpl extends ServiceImpl<QuestionSubmitMapper, QuestionSubmit>
    implements QuestionSubmitService {

    @Resource
    private QuestionService questionService;

    @Resource
    private UserServiceImpl userService;

    @Resource
    @Lazy
    private JudgeService judgeService;

    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private UserMapper userMapper;

    /**
     * 提交题目
     *
     * @param questionSubmitAddRequest（题目提交信息）
     * @param loginUser
     * @return
     */
    @Override
    public long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, User loginUser) {

        // 校验编程语言是否合法
        String language = questionSubmitAddRequest.getLanguage();
        QuestionSubmitLanguageEnum languageEnum = QuestionSubmitLanguageEnum.getEnumByValue(language);
        if (languageEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编程语言错误");
        }

        Long questionId = questionSubmitAddRequest.getQuestionId();
        // 判断实体是否存在，根据类别获取实体
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 是否已提交题目
        long userId = loginUser.getId();
        // 每个用户串行提交题目
        QuestionSubmit questionSubmit = new QuestionSubmit();
        questionSubmit.setUserId(userId);
        questionSubmit.setQuestionId(questionId);
        questionSubmit.setCode(questionSubmitAddRequest.getCode());
        questionSubmit.setLanguage(language);

        // 设置初始状态
        questionSubmit.setStatus(QuestionSubmitStatusEnum.WAITING.getValue());
        questionSubmit.setJudgeInfo("{}");
        boolean save = this.save(questionSubmit);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据插入失败");
        }
        // 执行判题服务
        Long questionSubmitId = questionSubmit.getId();
        System.out.println("========== 开始执行判题服务，提交ID: " + questionSubmitId + " ==========");
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("========== 异步任务开始执行 ==========");
                judgeService.doJudge(questionSubmitId);
                System.out.println("========== 异步任务执行完成 ==========");
            } catch (Exception e) {
                System.out.println("========== 判题异常: " + e.getMessage() + " ==========");
                e.printStackTrace();
                // 更新提交状态为失败
                QuestionSubmit failUpdate = new QuestionSubmit();
                failUpdate.setId(questionSubmitId);
                failUpdate.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
                failUpdate.setJudgeInfo("{\"message\":\"判题异常: " + e.getMessage() + "\"}");
                this.updateById(failUpdate);
            }
        });
        return questionSubmitId;

    }

    /**
     * 获取查询包装类
     *
     * @param questionSubmitQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitQueryRequest) {
        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
        if (questionSubmitQueryRequest == null) {
            return queryWrapper;
        }

        Long id = questionSubmitQueryRequest.getId();
        String language = questionSubmitQueryRequest.getLanguage();
        String status = questionSubmitQueryRequest.getStatus();
        Long questionId = questionSubmitQueryRequest.getQuestionId();
        Long userId = questionSubmitQueryRequest.getUserId();
        String sortField = questionSubmitQueryRequest.getSortField();
        String sortOrder = questionSubmitQueryRequest.getSortOrder();


        // 拼接查询条件
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(language), "language", language);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionId), "questionId", questionId);
        queryWrapper.eq(QuestionSubmitStatusEnum.getEnumByValue(status)!=null, "status", status);
        queryWrapper.eq("isDelete", 0);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }


    /**
     * 获取题目封装(连表查询)
     *
     * @param questionSubmit
     * @param loginUser
     * @return
     */
    @Override
    public QuestionSubmitVO getQuestionSubmitVO(QuestionSubmit questionSubmit, User loginUser) {
        QuestionSubmitVO questionSubmitVO = QuestionSubmitVO.objToVo(questionSubmit);
        // 1. 关联查询用户信息
        long userId = loginUser.getId();

        // 处理脱敏
        // 不是当前用户或者不是管理员，不能获取提交的代码
        if (!questionSubmit.getUserId().equals(userId) && !userService.isAdmin(loginUser)){
            questionSubmitVO.setCode(null);
        }
        return questionSubmitVO;
    }

    @Override
    public Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> questionSubmitPage, User loginUser) {
        // 获取当前分页中的 QuestionSubmit 记录
        List<QuestionSubmit> questionSubmitList = questionSubmitPage.getRecords();
        Page<QuestionSubmitVO> questionSubmitVOPage = new Page<>(questionSubmitPage.getCurrent(), questionSubmitPage.getSize(), questionSubmitPage.getTotal());

        // 如果记录为空，直接返回空的 VO 页
        if (CollectionUtils.isEmpty(questionSubmitList)) {
            return questionSubmitVOPage;
        }

        // 收集所有 questionId 和 userId
        Set<Long> questionIds = questionSubmitList.stream()
                .map(QuestionSubmit::getQuestionId)
                .collect(Collectors.toSet());
        Set<Long> userIds = questionSubmitList.stream()
                .map(QuestionSubmit::getUserId)
                .collect(Collectors.toSet());

        // 使用 MyBatis Plus 批量查询 question 信息
        Map<Long, QuestionVO> questionMap = questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, this::convertToQuestionVO));

        // 使用 MyBatis Plus 批量查询 user 信息
        Map<Long, UserVO> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, this::convertToUserVO));

        // 构建 QuestionSubmitVO 列表
        List<QuestionSubmitVO> questionSubmitVOList = questionSubmitList.stream()
                .map(questionSubmit -> {
                    QuestionSubmitVO vo = getQuestionSubmitVO(questionSubmit, loginUser);
                    vo.setQuestionVO(questionMap.get(questionSubmit.getQuestionId()));
                    vo.setUserVO(userMap.get(questionSubmit.getUserId()));
                    return vo;
                })
                .collect(Collectors.toList());

        // 设置结果到分页对象
        questionSubmitVOPage.setRecords(questionSubmitVOList);
        return questionSubmitVOPage;
    }

    private QuestionVO convertToQuestionVO(Question question) {
        QuestionVO vo = new QuestionVO();
        BeanUtils.copyProperties(question, vo);
        return vo;
    }

    private UserVO convertToUserVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }




}





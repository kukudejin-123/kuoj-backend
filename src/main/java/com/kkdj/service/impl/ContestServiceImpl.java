package com.kkdj.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kkdj.common.ErrorCode;
import com.kkdj.constant.CommonConstant;
import com.kkdj.exception.BusinessException;
import com.kkdj.exception.ThrowUtils;
import com.kkdj.mapper.ContestMapper;
import com.kkdj.mapper.ContestParticipantMapper;
import com.kkdj.mapper.ContestQuestionMapper;
import com.kkdj.mapper.ContestSubmitMapper;
import com.kkdj.mapper.TeamUserMapper;
import com.kkdj.model.dto.contest.ContestAddRequest;
import com.kkdj.model.dto.contest.ContestQueryRequest;
import com.kkdj.model.dto.contest.ContestUpdateRequest;
import com.kkdj.model.entity.Contest;
import com.kkdj.model.entity.ContestParticipant;
import com.kkdj.model.entity.ContestQuestion;
import com.kkdj.model.entity.ContestSubmit;
import com.kkdj.model.entity.Question;
import com.kkdj.model.entity.User;
import com.kkdj.model.enums.ContestStatusEnum;
import com.kkdj.model.enums.QuestionSubmitStatusEnum;
import com.kkdj.model.vo.ContestQuestionVO;
import com.kkdj.model.vo.ContestVO;
import com.kkdj.model.vo.UserVO;
import com.kkdj.service.ContestParticipantService;
import com.kkdj.service.ContestQuestionService;
import com.kkdj.service.ContestService;
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
import java.util.stream.Collectors;

/**
 * 比赛服务实现
 */
@Service
public class ContestServiceImpl extends ServiceImpl<ContestMapper, Contest> implements ContestService {

    @Resource
    private TeamUserMapper teamUserMapper;

    @Resource
    private ContestQuestionMapper contestQuestionMapper;

    @Resource
    private ContestSubmitMapper contestSubmitMapper;

    @Resource
    private ContestParticipantMapper contestParticipantMapper;

    @Resource
    private ContestQuestionService contestQuestionService;

    @Resource
    private ContestParticipantService contestParticipantService;

    @Resource
    private UserService userService;

    @Resource
    private QuestionService questionService;

    @Override
    public void validContest(Contest contest, boolean add) {
        if (contest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String contestName = contest.getContestName();
        String contestDesc = contest.getContestDesc();
        Date startTime = contest.getStartTime();
        Date endTime = contest.getEndTime();
        // 创建时，参数不能为空
        if (add) {
            ThrowUtils.throwIf(StringUtils.isBlank(contestName), ErrorCode.PARAMS_ERROR, "比赛名称不能为空");
            ThrowUtils.throwIf(startTime == null, ErrorCode.PARAMS_ERROR, "开始时间不能为空");
            ThrowUtils.throwIf(endTime == null, ErrorCode.PARAMS_ERROR, "结束时间不能为空");
        }
        // 有参数则校验
        if (StringUtils.isNotBlank(contestName) && contestName.length() > 256) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "比赛名称过长");
        }
        if (StringUtils.isNotBlank(contestDesc) && contestDesc.length() > 2048) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "比赛描述过长");
        }
        if (startTime != null && endTime != null && startTime.after(endTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "开始时间不能晚于结束时间");
        }
    }

    @Override
    public QueryWrapper<Contest> getQueryWrapper(ContestQueryRequest contestQueryRequest) {
        QueryWrapper<Contest> queryWrapper = new QueryWrapper<>();
        if (contestQueryRequest == null) {
            return queryWrapper;
        }
        Long id = contestQueryRequest.getId();
        String contestName = contestQueryRequest.getContestName();
        Long userId = contestQueryRequest.getUserId();
        Integer contestType = contestQueryRequest.getContestType();
        Integer status = contestQueryRequest.getStatus();
        String sortField = contestQueryRequest.getSortField();
        String sortOrder = contestQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.like(StringUtils.isNotBlank(contestName), "contestName", contestName);
        queryWrapper.eq(userId != null, "userId", userId);
        queryWrapper.eq(contestType != null, "contestType", contestType);
        queryWrapper.eq(status != null, "status", status);
        // 排序
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
            CommonConstant.SORT_ORDER_ASC.equals(sortOrder), sortField);
        return queryWrapper;
    }

    @Override
    public ContestVO getContestVO(Contest contest, HttpServletRequest request) {
        ContestVO contestVO = ContestVO.objToVo(contest);
        // 1. 关联查询创建者信息
        Long userId = contest.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        contestVO.setCreateUser(userVO);

        // 2. 查询当前用户是否已报名比赛
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser != null) {
            boolean hasJoin = contestParticipantMapper.countByContestIdAndUserId(contest.getId(), loginUser.getId()) > 0;
            contestVO.setHasJoin(hasJoin);
        } else {
            contestVO.setHasJoin(false);
        }
        return contestVO;
    }

    @Override
    public Page<ContestVO> getContestVOPage(Page<Contest> contestPage, HttpServletRequest request) {
        List<Contest> contestList = contestPage.getRecords();
        Page<ContestVO> contestVOPage = new Page<>(contestPage.getCurrent(), contestPage.getSize(), contestPage.getTotal());
        if (CollUtil.isEmpty(contestList)) {
            return contestVOPage;
        }
        // 1. 关联查询创建者信息
        Set<Long> userIdSet = contestList.stream().map(Contest::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        // 2. 查询当前用户已报名的比赛
        User loginUser = userService.getLoginUserPermitNull(request);
        Set<Long> joinedContestIds;
        if (loginUser != null) {
            QueryWrapper<ContestParticipant> wrapper = new QueryWrapper<>();
            wrapper.eq("userId", loginUser.getId());
            List<ContestParticipant> participants = contestParticipantService.list(wrapper);
            joinedContestIds = participants.stream()
                    .map(ContestParticipant::getContestId)
                    .collect(Collectors.toSet());
        } else {
            joinedContestIds = new HashSet<>();
        }

        // 3. 填充信息
        List<ContestVO> contestVOList = contestList.stream().map(contest -> {
            ContestVO contestVO = ContestVO.objToVo(contest);
            Long userId = contest.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            contestVO.setCreateUser(userService.getUserVO(user));
            contestVO.setHasJoin(joinedContestIds.contains(contest.getId()));
            return contestVO;
        }).collect(Collectors.toList());
        contestVOPage.setRecords(contestVOList);
        return contestVOPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addContest(ContestAddRequest contestAddRequest, User loginUser) {
        // 1. 校验参数
        String contestName = contestAddRequest.getContestName();
        if (StringUtils.isBlank(contestName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "比赛名称不能为空");
        }
        if (contestAddRequest.getStartTime() == null || contestAddRequest.getEndTime() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "比赛时间不能为空");
        }
        if (contestAddRequest.getStartTime().after(contestAddRequest.getEndTime())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "开始时间不能晚于结束时间");
        }
        List<Long> questionIds = contestAddRequest.getQuestionIds();
        if (CollUtil.isEmpty(questionIds)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目列表不能为空");
        }
        // 2. 校验题目是否存在
        List<Question> questions = questionService.listByIds(questionIds);
        if (questions.size() != questionIds.size()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "部分题目不存在");
        }

        // 3. 创建比赛
        Contest contest = new Contest();
        BeanUtils.copyProperties(contestAddRequest, contest);
        contest.setUserId(loginUser.getId());
        contest.setQuestionCount(questionIds.size());
        contest.setParticipantCount(0);
        // 根据时间设置状态
        contest.setStatus(getContestStatus(contest.getStartTime(), contest.getEndTime()));
        this.validContest(contest, true);
        boolean result = this.save(contest);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建比赛失败");

        // 4. 关联题目，并更新题目的比赛关联
        for (int i = 0; i < questionIds.size(); i++) {
            Long questionId = questionIds.get(i);
            ContestQuestion contestQuestion = new ContestQuestion();
            contestQuestion.setContestId(contest.getId());
            contestQuestion.setQuestionId(questionId);
            contestQuestion.setQuestionOrder(i);
            contestQuestion.setScore(100); // 默认分值100
            contestQuestionService.save(contestQuestion);

            // 更新题目的contestId字段（如果题目是非公开的）
            Question questionUpdate = new Question();
            questionUpdate.setId(questionId);
            questionUpdate.setContestId(contest.getId());
            questionService.updateById(questionUpdate);
        }
        return contest.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateContest(ContestUpdateRequest contestUpdateRequest, User loginUser) {
        if (contestUpdateRequest == null || contestUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = contestUpdateRequest.getId();
        Contest oldContest = this.getById(id);
        ThrowUtils.throwIf(oldContest == null, ErrorCode.NOT_FOUND_ERROR, "比赛不存在");
        // 只有管理员可以更新
        if (!userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 比赛已结束不能修改
        if (ContestStatusEnum.ENDED.getValue().equals(oldContest.getStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "比赛已结束，无法修改");
        }

        Contest contest = new Contest();
        BeanUtils.copyProperties(contestUpdateRequest, contest);
        // 更新状态
        if (contest.getStartTime() != null && contest.getEndTime() != null) {
            contest.setStatus(getContestStatus(contest.getStartTime(), contest.getEndTime()));
        }
        this.validContest(contest, false);
        boolean result = this.updateById(contest);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新比赛失败");

        // 更新题目关联
        List<Long> questionIds = contestUpdateRequest.getQuestionIds();
        if (questionIds != null) {
            // 删除原有关联
            QueryWrapper<ContestQuestion> wrapper = new QueryWrapper<>();
            wrapper.eq("contestId", id);
            contestQuestionService.remove(wrapper);
            // 新增关联
            for (int i = 0; i < questionIds.size(); i++) {
                ContestQuestion contestQuestion = new ContestQuestion();
                contestQuestion.setContestId(id);
                contestQuestion.setQuestionId(questionIds.get(i));
                contestQuestion.setQuestionOrder(i);
                contestQuestion.setScore(100);
                contestQuestionService.save(contestQuestion);
            }
            // 更新题目数量
            contest.setQuestionCount(questionIds.size());
            this.updateById(contest);
        }
        return true;
    }

    @Override
    public boolean canJoinContest(Long userId) {
        // 管理员可直接参赛
        User user = userService.getById(userId);
        if (userService.isAdmin(user)) {
            return true;
        }
        // 检查是否加入任意团队
        return teamUserMapper.listTeamIdsByUserId(userId).size() > 0;
    }

    @Override
    public boolean isContestRunning(Long contestId) {
        Contest contest = this.getById(contestId);
        if (contest == null) {
            return false;
        }
        return ContestStatusEnum.RUNNING.getValue().equals(contest.getStatus());
    }

    @Override
    public void updateContestStatus() {
        Date now = new Date();
        QueryWrapper<Contest> wrapper = new QueryWrapper<>();
        wrapper.ne("status", ContestStatusEnum.ENDED.getValue());
        List<Contest> contests = this.list(wrapper);
        for (Contest contest : contests) {
            Integer newStatus = getContestStatus(contest.getStartTime(), contest.getEndTime());
            if (!newStatus.equals(contest.getStatus())) {
                contest.setStatus(newStatus);
                this.updateById(contest);
            }
        }
    }

    @Override
    public List<ContestVO> getContestQuestions(Long contestId, HttpServletRequest request) {
        Contest contest = this.getById(contestId);
        ThrowUtils.throwIf(contest == null, ErrorCode.NOT_FOUND_ERROR, "比赛不存在");

        // 查询比赛题目关联
        QueryWrapper<ContestQuestion> wrapper = new QueryWrapper<>();
        wrapper.eq("contestId", contestId);
        wrapper.orderByAsc("questionOrder");
        List<ContestQuestion> contestQuestions = contestQuestionService.list(wrapper);

        // 获取题目详情
        List<Long> questionIds = contestQuestions.stream()
                .map(ContestQuestion::getQuestionId)
                .collect(Collectors.toList());
        List<Question> questions = questionService.listByIds(questionIds);
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        // 统计比赛中每道题的提交数和通过数
        Map<Long, int[]> contestQuestionStats = new HashMap<>();
        for (Long questionId : questionIds) {
            // 统计该题目在比赛中的总提交数
            QueryWrapper<ContestSubmit> submitWrapper = new QueryWrapper<>();
            submitWrapper.eq("contestId", contestId);
            submitWrapper.eq("questionId", questionId);
            int submitNum = Math.toIntExact(contestSubmitMapper.selectCount(submitWrapper));

            // 统计该题目在比赛中的通过数（状态为2表示成功）
            QueryWrapper<ContestSubmit> acceptedWrapper = new QueryWrapper<>();
            acceptedWrapper.eq("contestId", contestId);
            acceptedWrapper.eq("questionId", questionId);
            acceptedWrapper.eq("status", QuestionSubmitStatusEnum.SUCCEED.getValue());
            int acceptedNum = Math.toIntExact(contestSubmitMapper.selectCount(acceptedWrapper));

            contestQuestionStats.put(questionId, new int[]{submitNum, acceptedNum});
        }

        // 组装VO
        List<ContestQuestionVO> questionVOList = contestQuestions.stream().map(cq -> {
            ContestQuestionVO vo = ContestQuestionVO.objToVo(cq);
            Question question = questionMap.get(cq.getQuestionId());
            if (question != null) {
                vo.setTitle(question.getTitle());
                vo.setDifficulty(question.getDifficulty());
            }
            // 使用比赛中的统计数据
            int[] stats = contestQuestionStats.get(cq.getQuestionId());
            if (stats != null) {
                vo.setSubmitNum(stats[0]);
                vo.setAcceptedNum(stats[1]);
            } else {
                vo.setSubmitNum(0);
                vo.setAcceptedNum(0);
            }
            return vo;
        }).collect(Collectors.toList());

        ContestVO contestVO = ContestVO.objToVo(contest);
        contestVO.setQuestionList(questionVOList);
        return Collections.singletonList(contestVO);
    }

    /**
     * 根据时间计算比赛状态
     */
    private Integer getContestStatus(Date startTime, Date endTime) {
        Date now = new Date();
        if (now.before(startTime)) {
            return ContestStatusEnum.NOT_STARTED.getValue();
        } else if (now.after(endTime)) {
            return ContestStatusEnum.ENDED.getValue();
        } else {
            return ContestStatusEnum.RUNNING.getValue();
        }
    }
}
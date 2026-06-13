package com.kkdj.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kkdj.common.ErrorCode;
import com.kkdj.constant.CommonConstant;
import com.kkdj.exception.BusinessException;
import com.kkdj.exception.ThrowUtils;
import com.kkdj.mapper.ContestParticipantMapper;
import com.kkdj.mapper.ContestSubmitMapper;
import com.kkdj.mapper.QuestionSubmitMapper;
import com.kkdj.mapper.UserRankingMapper;
import com.kkdj.model.dto.ranking.UserRankingQueryRequest;
import com.kkdj.model.entity.ContestParticipant;
import com.kkdj.model.entity.ContestSubmit;
import com.kkdj.model.entity.QuestionSubmit;
import com.kkdj.model.entity.User;
import com.kkdj.model.entity.UserRanking;
import com.kkdj.model.enums.QuestionSubmitStatusEnum;
import com.kkdj.model.vo.UserRankingVO;
import com.kkdj.model.vo.UserVO;
import com.kkdj.service.UserRankingService;
import com.kkdj.service.UserService;
import com.kkdj.utils.SqlUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户排行榜服务实现
 */
@Service
public class UserRankingServiceImpl extends ServiceImpl<UserRankingMapper, UserRanking> implements UserRankingService {

    @Resource
    private UserService userService;

    @Resource
    private ContestParticipantMapper contestParticipantMapper;

    @Resource
    private ContestSubmitMapper contestSubmitMapper;

    @Resource
    private QuestionSubmitMapper questionSubmitMapper;

    @Override
    public QueryWrapper<UserRanking> getQueryWrapper(UserRankingQueryRequest queryRequest) {
        QueryWrapper<UserRanking> queryWrapper = new QueryWrapper<>();
        if (queryRequest == null) {
            return queryWrapper;
        }
        Long userId = queryRequest.getUserId();
        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();

        queryWrapper.eq(userId != null, "userId", userId);
        // 默认按积分降序排序
        if (StringUtils.isBlank(sortField)) {
            queryWrapper.orderByDesc("rating");
        } else {
            queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                    CommonConstant.SORT_ORDER_ASC.equals(sortOrder), sortField);
        }
        return queryWrapper;
    }

    @Override
    public UserRankingVO getUserRankingVO(UserRanking userRanking) {
        if (userRanking == null) {
            return null;
        }
        UserRankingVO vo = UserRankingVO.objToVo(userRanking);
        // 关联用户信息
        User user = userService.getById(userRanking.getUserId());
        if (user != null) {
            vo.setUser(userService.getUserVO(user));
        }
        return vo;
    }

    @Override
    public Page<UserRankingVO> getUserRankingVOPage(Page<UserRanking> page, UserRankingQueryRequest queryRequest) {
        // 直接从数据库实时计算排行榜，不依赖user_ranking表
        return calculateRankingFromDb(page, queryRequest);
    }

    /**
     * 从数据库实时计算排行榜（当user_ranking表无数据时）
     */
    private Page<UserRankingVO> calculateRankingFromDb(Page<UserRanking> page, UserRankingQueryRequest queryRequest) {
        Set<Long> userIds = new HashSet<>();

        // 1. 获取所有有比赛提交记录的用户
        QueryWrapper<ContestSubmit> contestSubmitWrapper = new QueryWrapper<>();
        contestSubmitWrapper.select("DISTINCT userId");
        List<ContestSubmit> contestSubmits = contestSubmitMapper.selectList(contestSubmitWrapper);
        userIds.addAll(contestSubmits.stream().map(ContestSubmit::getUserId).collect(Collectors.toSet()));

        // 2. 获取所有有普通题目提交记录的用户
        QueryWrapper<QuestionSubmit> questionSubmitWrapper = new QueryWrapper<>();
        questionSubmitWrapper.select("DISTINCT userId");
        List<QuestionSubmit> questionSubmits = questionSubmitMapper.selectList(questionSubmitWrapper);
        userIds.addAll(questionSubmits.stream().map(QuestionSubmit::getUserId).collect(Collectors.toSet()));

        // 3. 获取所有报名参赛的用户
        QueryWrapper<ContestParticipant> participantWrapper = new QueryWrapper<>();
        participantWrapper.select("DISTINCT userId");
        List<ContestParticipant> participants = contestParticipantMapper.selectList(participantWrapper);
        userIds.addAll(participants.stream().map(ContestParticipant::getUserId).collect(Collectors.toSet()));

        if (CollUtil.isEmpty(userIds)) {
            return new Page<>(page.getCurrent(), page.getSize(), 0);
        }

        // 4. 获取用户信息
        List<User> users = userService.listByIds(userIds);

        // 搜索用户名过滤
        if (StringUtils.isNotBlank(queryRequest.getUserName())) {
            users = users.stream()
                    .filter(u -> u.getUserName() != null && u.getUserName().contains(queryRequest.getUserName()))
                    .collect(Collectors.toList());
        }

        // 5. 计算每个用户的统计数据
        List<UserRankingVO> voList = new ArrayList<>();
        for (User user : users) {
            UserRankingVO vo = new UserRankingVO();
            vo.setUserId(user.getId());
            vo.setUser(userService.getUserVO(user));

            // 统计参赛次数
            QueryWrapper<ContestParticipant> cpWrapper = new QueryWrapper<>();
            cpWrapper.eq("userId", user.getId());
            vo.setTotalContests(Math.toIntExact(contestParticipantMapper.selectCount(cpWrapper)));

            // 统计比赛提交数
            QueryWrapper<ContestSubmit> csWrapper = new QueryWrapper<>();
            csWrapper.eq("userId", user.getId());
            int contestSubmitCount = Math.toIntExact(contestSubmitMapper.selectCount(csWrapper));

            // 统计普通题目提交数
            QueryWrapper<QuestionSubmit> qsWrapper = new QueryWrapper<>();
            qsWrapper.eq("userId", user.getId());
            int questionSubmitCount = Math.toIntExact(questionSubmitMapper.selectCount(qsWrapper));

            vo.setTotalSubmissions(contestSubmitCount + questionSubmitCount);

            // 统计比赛通过题目数（去重）
            QueryWrapper<ContestSubmit> contestAcceptedWrapper = new QueryWrapper<>();
            contestAcceptedWrapper.eq("userId", user.getId());
            contestAcceptedWrapper.eq("status", QuestionSubmitStatusEnum.SUCCEED.getValue());
            contestAcceptedWrapper.select("DISTINCT questionId");
            int contestAcceptedCount = Math.toIntExact(contestSubmitMapper.selectCount(contestAcceptedWrapper));

            // 统计普通题目通过数（去重）
            QueryWrapper<QuestionSubmit> questionAcceptedWrapper = new QueryWrapper<>();
            questionAcceptedWrapper.eq("userId", user.getId());
            questionAcceptedWrapper.eq("status", QuestionSubmitStatusEnum.SUCCEED.getValue());
            questionAcceptedWrapper.select("DISTINCT questionId");
            int questionAcceptedCount = Math.toIntExact(questionSubmitMapper.selectCount(questionAcceptedWrapper));

            vo.setTotalAccepted(contestAcceptedCount + questionAcceptedCount);

            // 计算积分（基础1500 + 通过题目数 * 10）
            vo.setRating(1500 + vo.getTotalAccepted() * 10);

            voList.add(vo);
        }

        // 6. 按通过题目数降序排序
        voList.sort((a, b) -> {
            if (b.getTotalAccepted() != a.getTotalAccepted()) {
                return b.getTotalAccepted() - a.getTotalAccepted();
            }
            if (b.getTotalSubmissions() != a.getTotalSubmissions()) {
                return a.getTotalSubmissions() - b.getTotalSubmissions();
            }
            return b.getTotalContests() - a.getTotalContests();
        });

        // 7. 设置排名
        for (int i = 0; i < voList.size(); i++) {
            voList.get(i).setRank(i + 1);
        }

        // 8. 分页处理
        int total = voList.size();
        int fromIndex = (int) ((page.getCurrent() - 1) * page.getSize());
        int toIndex = Math.min(fromIndex + (int) page.getSize(), total);

        List<UserRankingVO> pageRecords = fromIndex < total ? voList.subList(fromIndex, toIndex) : new ArrayList<>();

        Page<UserRankingVO> resultPage = new Page<>(page.getCurrent(), page.getSize(), total);
        resultPage.setRecords(pageRecords);
        return resultPage;
    }

    @Override
    public UserRankingVO getUserRanking(Long userId) {
        if (userId == null || userId <= 0) {
            return null;
        }

        User user = userService.getById(userId);
        if (user == null) {
            return null;
        }

        UserRankingVO vo = new UserRankingVO();
        vo.setUserId(userId);
        vo.setUser(userService.getUserVO(user));

        // 实时计算统计数据
        // 统计参赛次数
        QueryWrapper<ContestParticipant> cpWrapper = new QueryWrapper<>();
        cpWrapper.eq("userId", userId);
        vo.setTotalContests(Math.toIntExact(contestParticipantMapper.selectCount(cpWrapper)));

        // 统计比赛提交数
        QueryWrapper<ContestSubmit> csWrapper = new QueryWrapper<>();
        csWrapper.eq("userId", userId);
        int contestSubmitCount = Math.toIntExact(contestSubmitMapper.selectCount(csWrapper));

        // 统计普通题目提交数
        QueryWrapper<QuestionSubmit> qsWrapper = new QueryWrapper<>();
        qsWrapper.eq("userId", userId);
        int questionSubmitCount = Math.toIntExact(questionSubmitMapper.selectCount(qsWrapper));

        vo.setTotalSubmissions(contestSubmitCount + questionSubmitCount);

        // 统计比赛通过题目数（去重）
        QueryWrapper<ContestSubmit> contestAcceptedWrapper = new QueryWrapper<>();
        contestAcceptedWrapper.eq("userId", userId);
        contestAcceptedWrapper.eq("status", QuestionSubmitStatusEnum.SUCCEED.getValue());
        contestAcceptedWrapper.select("DISTINCT questionId");
        int contestAcceptedCount = Math.toIntExact(contestSubmitMapper.selectCount(contestAcceptedWrapper));

        // 统计普通题目通过数（去重）
        QueryWrapper<QuestionSubmit> questionAcceptedWrapper = new QueryWrapper<>();
        questionAcceptedWrapper.eq("userId", userId);
        questionAcceptedWrapper.eq("status", QuestionSubmitStatusEnum.SUCCEED.getValue());
        questionAcceptedWrapper.select("DISTINCT questionId");
        int questionAcceptedCount = Math.toIntExact(questionSubmitMapper.selectCount(questionAcceptedWrapper));

        vo.setTotalAccepted(contestAcceptedCount + questionAcceptedCount);

        // 计算积分（简单规则：基础积分1500 + 通过题目数 * 10）
        vo.setRating(1500 + vo.getTotalAccepted() * 10);

        // 计算排名：查询有多少用户的积分比当前用户高
        // 获取所有有提交记录的用户
        Set<Long> allUserIds = new HashSet<>();

        QueryWrapper<ContestSubmit> contestSubmitWrapper = new QueryWrapper<>();
        contestSubmitWrapper.select("DISTINCT userId");
        List<ContestSubmit> contestSubmits = contestSubmitMapper.selectList(contestSubmitWrapper);
        allUserIds.addAll(contestSubmits.stream().map(ContestSubmit::getUserId).collect(Collectors.toSet()));

        QueryWrapper<QuestionSubmit> questionSubmitWrapper = new QueryWrapper<>();
        questionSubmitWrapper.select("DISTINCT userId");
        List<QuestionSubmit> questionSubmits = questionSubmitMapper.selectList(questionSubmitWrapper);
        allUserIds.addAll(questionSubmits.stream().map(QuestionSubmit::getUserId).collect(Collectors.toSet()));

        QueryWrapper<ContestParticipant> participantWrapper = new QueryWrapper<>();
        participantWrapper.select("DISTINCT userId");
        List<ContestParticipant> participants = contestParticipantMapper.selectList(participantWrapper);
        allUserIds.addAll(participants.stream().map(ContestParticipant::getUserId).collect(Collectors.toSet()));

        // 计算每个用户的积分，统计比当前用户积分高的数量
        int higherCount = 0;
        for (Long uid : allUserIds) {
            if (uid.equals(userId)) continue;

            // 计算该用户的通过数
            QueryWrapper<ContestSubmit> caWrapper = new QueryWrapper<>();
            caWrapper.eq("userId", uid);
            caWrapper.eq("status", QuestionSubmitStatusEnum.SUCCEED.getValue());
            caWrapper.select("DISTINCT questionId");
            int caCount = Math.toIntExact(contestSubmitMapper.selectCount(caWrapper));

            QueryWrapper<QuestionSubmit> qaWrapper = new QueryWrapper<>();
            qaWrapper.eq("userId", uid);
            qaWrapper.eq("status", QuestionSubmitStatusEnum.SUCCEED.getValue());
            qaWrapper.select("DISTINCT questionId");
            int qaCount = Math.toIntExact(questionSubmitMapper.selectCount(qaWrapper));

            int otherAcceptedCount = caCount + qaCount;
            int otherRating = 1500 + otherAcceptedCount * 10;

            if (otherRating > vo.getRating()) {
                higherCount++;
            }
        }

        vo.setRank(higherCount + 1);

        return vo;
    }

    @Override
    public void updateUserRanking(Long userId) {
        if (userId == null || userId <= 0) {
            return;
        }
        // 获取用户的排行榜记录
        QueryWrapper<UserRanking> wrapper = new QueryWrapper<>();
        wrapper.eq("userId", userId);
        UserRanking userRanking = this.getOne(wrapper);

        if (userRanking == null) {
            userRanking = new UserRanking();
            userRanking.setUserId(userId);
            userRanking.setTotalContests(0);
            userRanking.setTotalAccepted(0);
            userRanking.setTotalSubmissions(0);
            userRanking.setRating(1500);
        }

        // 统计参赛次数
        QueryWrapper<ContestParticipant> participantWrapper = new QueryWrapper<>();
        participantWrapper.eq("userId", userId);
        long contestCount = contestParticipantMapper.selectCount(participantWrapper);
        userRanking.setTotalContests((int) contestCount);

        // 统计提交数和通过数
        QueryWrapper<ContestSubmit> submitWrapper = new QueryWrapper<>();
        submitWrapper.eq("userId", userId);
        long totalSubmissions = contestSubmitMapper.selectCount(submitWrapper);
        userRanking.setTotalSubmissions((int) totalSubmissions);

        // 统计通过题目数（去重）
        QueryWrapper<ContestSubmit> acceptedWrapper = new QueryWrapper<>();
        acceptedWrapper.eq("userId", userId);
        acceptedWrapper.eq("status", QuestionSubmitStatusEnum.SUCCEED.getValue());
        acceptedWrapper.select("DISTINCT questionId");
        long acceptedCount = contestSubmitMapper.selectCount(acceptedWrapper);
        userRanking.setTotalAccepted((int) acceptedCount);

        // 更新或保存
        this.saveOrUpdate(userRanking);
    }

    @Override
    public void batchUpdateUserRanking(Long contestId) {
        if (contestId == null || contestId <= 0) {
            return;
        }
        // 获取比赛所有参赛者
        QueryWrapper<ContestParticipant> wrapper = new QueryWrapper<>();
        wrapper.eq("contestId", contestId);
        List<ContestParticipant> participants = contestParticipantMapper.selectList(wrapper);

        if (CollUtil.isEmpty(participants)) {
            return;
        }

        // 批量更新每个参赛者的排行榜数据
        for (ContestParticipant participant : participants) {
            updateUserRanking(participant.getUserId());
        }
    }

    /**
     * 初始化用户排行榜记录
     */
    private UserRanking initUserRanking(Long userId) {
        UserRanking userRanking = new UserRanking();
        userRanking.setUserId(userId);
        userRanking.setTotalContests(0);
        userRanking.setTotalAccepted(0);
        userRanking.setTotalSubmissions(0);
        userRanking.setRating(1500);
        this.save(userRanking);
        return userRanking;
    }
}
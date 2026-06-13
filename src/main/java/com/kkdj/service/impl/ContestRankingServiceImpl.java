package com.kkdj.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kkdj.mapper.ContestSubmitMapper;
import com.kkdj.model.entity.Contest;
import com.kkdj.model.entity.ContestQuestion;
import com.kkdj.model.entity.ContestSubmit;
import com.kkdj.model.entity.User;
import com.kkdj.model.enums.QuestionSubmitStatusEnum;
import com.kkdj.model.vo.ContestRankingVO;
import com.kkdj.model.vo.UserVO;
import com.kkdj.service.ContestQuestionService;
import com.kkdj.service.ContestRankingService;
import com.kkdj.service.ContestService;
import com.kkdj.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 比赛排行榜服务实现（ACM规则，使用Redis有序集合）
 */
@Service
@Slf4j
public class ContestRankingServiceImpl implements ContestRankingService {

    /**
     * Redis key前缀
     */
    private static final String RANKING_KEY_PREFIX = "contest:ranking:";

    /**
     * 用户题目状态key前缀
     */
    private static final String USER_PROBLEM_KEY_PREFIX = "contest:problem:";

    /**
     * 排行榜过期时间（7天）
     */
    private static final long EXPIRE_DAYS = 7;

    @Resource
    private UserService userService;

    @Resource
    private ContestSubmitMapper contestSubmitMapper;

    @Resource
    private ContestQuestionService contestQuestionService;

    @Resource
    private ContestService contestService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void updateRanking(Long contestId, Long userId, int questionOrder, boolean isAccepted, int submitTimeMinutes, int wrongCount) {
        if (contestId == null || userId == null) {
            return;
        }

        String rankingKey = RANKING_KEY_PREFIX + contestId;
        String problemKey = USER_PROBLEM_KEY_PREFIX + contestId + ":" + userId;

        // 检查用户是否已经通过该题目
        String problemField = String.valueOf(questionOrder);
        Object existingStatus = redisTemplate.opsForHash().get(problemKey, problemField);

        // 如果已经通过该题目，不再更新
        if (existingStatus != null && existingStatus.toString().startsWith("1:")) {
            return;
        }

        if (isAccepted) {
            // 通过题目，存储通过时间和错误次数
            String value = "1:" + submitTimeMinutes + ":" + wrongCount;
            redisTemplate.opsForHash().put(problemKey, problemField, value);
            // 更新排行榜分数
            updateScore(contestId, userId);
        } else {
            // 未通过，记录错误次数
            String value = "0:" + wrongCount;
            redisTemplate.opsForHash().put(problemKey, problemField, value);
        }

        // 设置过期时间
        redisTemplate.expire(rankingKey, EXPIRE_DAYS, TimeUnit.DAYS);
        redisTemplate.expire(problemKey, EXPIRE_DAYS, TimeUnit.DAYS);
    }

    /**
     * 更新用户排行榜分数
     * ACM规则：分数 = 通过题数 * 1000000 - 总时间（分钟）
     * 这样可以通过分数降序直接得到正确排名
     */
    private void updateScore(Long contestId, Long userId) {
        String problemKey = USER_PROBLEM_KEY_PREFIX + contestId + ":" + userId;
        String rankingKey = RANKING_KEY_PREFIX + contestId;

        Map<Object, Object> problemStats = redisTemplate.opsForHash().entries(problemKey);

        int acceptedCount = 0;
        int totalTime = 0;

        for (Map.Entry<Object, Object> entry : problemStats.entrySet()) {
            String value = entry.getValue().toString();
            if (value.startsWith("1:")) {
                acceptedCount++;
                String[] parts = value.split(":");
                int submitTime = Integer.parseInt(parts[1]);
                int wrongCount = Integer.parseInt(parts[2]);
                // ACM规则：罚时 = 通过时间 + 错误次数 * 20分钟
                totalTime += submitTime + wrongCount * 20;
            }
        }

        // 计算分数：通过题数多排前面，时间少排前面
        // 使用 1000000 作为基数，确保题目数优先
        double score = acceptedCount * 1000000 - totalTime;
        redisTemplate.opsForZSet().add(rankingKey, userId.toString(), score);
    }

    @Override
    public List<ContestRankingVO> getRanking(Long contestId) {
        if (contestId == null) {
            return new ArrayList<>();
        }

        String rankingKey = RANKING_KEY_PREFIX + contestId;
        // 获取所有用户，按分数降序
        Set<Object> userIds = redisTemplate.opsForZSet().reverseRange(rankingKey, 0, -1);

        if (CollUtil.isEmpty(userIds)) {
            // 如果Redis中没有数据，从数据库计算
            return calculateRankingFromDb(contestId);
        }

        List<ContestRankingVO> rankingList = new ArrayList<>();
        int rank = 1;

        // 获取所有题目序号
        List<ContestQuestion> questions = contestQuestionService.getQuestionsByContestId(contestId);

        for (Object userIdObj : userIds) {
            Long userId = Long.parseLong(userIdObj.toString());
            ContestRankingVO vo = buildUserRankingVO(contestId, userId, rank++, questions);
            rankingList.add(vo);
        }

        return rankingList;
    }

    /**
     * 构建用户排行榜VO
     */
    private ContestRankingVO buildUserRankingVO(Long contestId, Long userId, int rank, List<ContestQuestion> questions) {
        ContestRankingVO vo = new ContestRankingVO();
        vo.setRank(rank);
        vo.setUserId(userId);

        // 获取用户信息
        User user = userService.getById(userId);
        if (user != null) {
            UserVO userVO = userService.getUserVO(user);
            vo.setUser(userVO);
        }

        String problemKey = USER_PROBLEM_KEY_PREFIX + contestId + ":" + userId;
        Map<Object, Object> problemStats = redisTemplate.opsForHash().entries(problemKey);

        int acceptedCount = 0;
        int totalTime = 0;
        Map<String, int[]> stats = new HashMap<>();

        for (ContestQuestion question : questions) {
            String orderStr = String.valueOf(question.getQuestionOrder());
            Object status = problemStats.get(orderStr);
            String label = getOrderLabel(question.getQuestionOrder());

            if (status != null) {
                String value = status.toString();
                String[] parts = value.split(":");
                boolean isAccepted = "1".equals(parts[0]);
                if (isAccepted) {
                    acceptedCount++;
                    int submitTime = Integer.parseInt(parts[1]);
                    int wrongCount = Integer.parseInt(parts[2]);
                    totalTime += submitTime + wrongCount * 20;
                    stats.put(label, new int[]{submitTime, wrongCount});
                } else {
                    int wrongCount = Integer.parseInt(parts[1]);
                    stats.put(label, new int[]{-1, wrongCount});
                }
            }
        }

        vo.setAcceptedCount(acceptedCount);
        vo.setTotalTime(totalTime);
        vo.setProblemStats(stats);

        return vo;
    }

    /**
     * 从数据库计算排行榜（当Redis无数据时）
     */
    private List<ContestRankingVO> calculateRankingFromDb(Long contestId) {
        // 获取比赛信息
        Contest contest = contestService.getById(contestId);
        if (contest == null) {
            return new ArrayList<>();
        }

        // 获取比赛的所有题目
        List<ContestQuestion> questions = contestQuestionService.getQuestionsByContestId(contestId);
        if (CollUtil.isEmpty(questions)) {
            return new ArrayList<>();
        }

        // 获取比赛的所有提交记录
        QueryWrapper<ContestSubmit> wrapper = new QueryWrapper<>();
        wrapper.eq("contestId", contestId);
        wrapper.orderByAsc("submitTime");
        List<ContestSubmit> submits = contestSubmitMapper.selectList(wrapper);

        if (CollUtil.isEmpty(submits)) {
            return new ArrayList<>();
        }

        // 按用户分组计算
        Map<Long, UserContestStats> userStatsMap = new HashMap<>();
        for (ContestSubmit submit : submits) {
            Long userId = submit.getUserId();
            UserContestStats stats = userStatsMap.computeIfAbsent(userId, k -> new UserContestStats());

            // 找到题目序号
            int questionOrder = -1;
            for (ContestQuestion q : questions) {
                if (q.getQuestionId().equals(submit.getQuestionId())) {
                    questionOrder = q.getQuestionOrder();
                    break;
                }
            }

            if (questionOrder < 0) continue;

            String status = submit.getStatus();
            boolean isAccepted = QuestionSubmitStatusEnum.SUCCEED.getValue().equals(status);

            // 计算提交时间（分钟）
            long submitTimeMinutes = 0;
            if (submit.getSubmitTime() != null && contest.getStartTime() != null) {
                submitTimeMinutes = (submit.getSubmitTime().getTime() - contest.getStartTime().getTime()) / (60 * 1000);
            }

            // 更新题目状态
            ProblemStats problemStats = stats.problemStats.computeIfAbsent(questionOrder, k -> new ProblemStats());
            if (isAccepted && !problemStats.accepted) {
                problemStats.accepted = true;
                problemStats.acceptTime = (int) submitTimeMinutes;
                stats.acceptedCount++;
                stats.totalTime += problemStats.acceptTime + problemStats.wrongCount * 20;
            } else if (!isAccepted) {
                problemStats.wrongCount++;
            }
        }

        // 转换为排行榜VO并排序
        List<ContestRankingVO> rankingList = new ArrayList<>();
        for (Map.Entry<Long, UserContestStats> entry : userStatsMap.entrySet()) {
            Long userId = entry.getKey();
            UserContestStats stats = entry.getValue();

            ContestRankingVO vo = new ContestRankingVO();
            vo.setUserId(userId);

            User user = userService.getById(userId);
            if (user != null) {
                vo.setUser(userService.getUserVO(user));
            }

            vo.setAcceptedCount(stats.acceptedCount);
            vo.setTotalTime(stats.totalTime);

            // 构建题目状态Map
            Map<String, int[]> problemStatsVo = new HashMap<>();
            for (Map.Entry<Integer, ProblemStats> ps : stats.problemStats.entrySet()) {
                String label = getOrderLabel(ps.getKey());
                ProblemStats psValue = ps.getValue();
                if (psValue.accepted) {
                    problemStatsVo.put(label, new int[]{psValue.acceptTime, psValue.wrongCount});
                } else {
                    problemStatsVo.put(label, new int[]{-1, psValue.wrongCount});
                }
            }
            vo.setProblemStats(problemStatsVo);

            rankingList.add(vo);
        }

        // 按ACM规则排序：通过题数降序，罚时升序
        rankingList.sort((a, b) -> {
            if (b.getAcceptedCount() != a.getAcceptedCount()) {
                return b.getAcceptedCount() - a.getAcceptedCount();
            }
            return a.getTotalTime() - b.getTotalTime();
        });

        // 设置排名
        for (int i = 0; i < rankingList.size(); i++) {
            rankingList.get(i).setRank(i + 1);
        }

        return rankingList;
    }

    /**
     * 用户比赛统计数据内部类
     */
    private static class UserContestStats {
        int acceptedCount = 0;
        int totalTime = 0;
        Map<Integer, ProblemStats> problemStats = new HashMap<>();
    }

    /**
     * 题目统计数据内部类
     */
    private static class ProblemStats {
        boolean accepted = false;
        int acceptTime = 0;
        int wrongCount = 0;
    }

    @Override
    public Integer getUserRank(Long contestId, Long userId) {
        if (contestId == null || userId == null) {
            return null;
        }

        String rankingKey = RANKING_KEY_PREFIX + contestId;
        Long rank = redisTemplate.opsForZSet().reverseRank(rankingKey, userId.toString());

        return rank != null ? rank.intValue() + 1 : null;
    }

    @Override
    public void deleteRankingCache(Long contestId) {
        if (contestId == null) {
            return;
        }

        String rankingKey = RANKING_KEY_PREFIX + contestId;
        redisTemplate.delete(rankingKey);

        // 删除所有用户的题目状态
        Set<String> keys = redisTemplate.keys(USER_PROBLEM_KEY_PREFIX + contestId + ":*");
        if (CollUtil.isNotEmpty(keys)) {
            redisTemplate.delete(keys);
        }
    }

    @Override
    public void persistRanking(Long contestId) {
        // 比赛结束后，可以将排行榜持久化到数据库
        // 当前实现已经在UserRankingService中处理
    }

    /**
     * 获取题目序号标签（A, B, C...）
     */
    private String getOrderLabel(int order) {
        if (order < 26) {
            return String.valueOf((char) ('A' + order));
        }
        return String.valueOf((char) ('A' + order / 26 - 1)) + (char) ('A' + order % 26);
    }
}
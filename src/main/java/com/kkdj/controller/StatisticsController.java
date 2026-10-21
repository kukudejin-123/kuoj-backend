package com.kkdj.controller;

import com.kkdj.annotation.AuthCheck;
import com.kkdj.common.BaseResponse;
import com.kkdj.common.ErrorCode;
import com.kkdj.common.ResultUtils;
import com.kkdj.constant.UserConstant;
import com.kkdj.exception.BusinessException;
import com.kkdj.mapper.ContestSubmitMapper;
import com.kkdj.mapper.ContestParticipantMapper;
import com.kkdj.mapper.QuestionSubmitMapper;
import com.kkdj.mapper.UserMapper;
import com.kkdj.model.entity.ContestSubmit;
import com.kkdj.model.entity.ContestParticipant;
import com.kkdj.model.entity.QuestionSubmit;
import com.kkdj.model.entity.User;
import com.kkdj.model.enums.QuestionSubmitStatusEnum;
import com.kkdj.service.QuestionService;
import com.kkdj.service.ContestService;
import com.kkdj.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.*;

/**
 * 数据统计接口（仅管理员）
 */
@RestController
@RequestMapping("/statistics")
@Slf4j
public class StatisticsController {

    @Resource
    private UserService userService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private QuestionService questionService;

    @Resource
    private QuestionSubmitMapper questionSubmitMapper;

    @Resource
    private ContestSubmitMapper contestSubmitMapper;

    @Resource
    private ContestParticipantMapper contestParticipantMapper;

    @Resource
    private ContestService contestService;

    /**
     * 平台总览统计
     */
    @GetMapping("/overview")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Map<String, Object>> getOverview() {
        Map<String, Object> result = new HashMap<>();

        // 用户统计
        long totalUsers = userService.count();
        long activeUsers = userService.count(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .ne("userRole", "ban"));
        result.put("totalUsers", totalUsers);
        result.put("activeUsers", activeUsers);

        // 题目统计
        long totalQuestions = questionService.count();
        long publicQuestions = questionService.count(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.kkdj.model.entity.Question>()
                .eq("isPublic", 1).eq("review_status", 1));
        long pendingReview = questionService.count(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.kkdj.model.entity.Question>()
                .eq("review_status", 0));
        result.put("totalQuestions", totalQuestions);
        result.put("publicQuestions", publicQuestions);
        result.put("pendingReview", pendingReview);

        // 提交统计
        long totalSubmissions = questionSubmitMapper.selectCount(null);
        long acceptedSubmissions = questionSubmitMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<QuestionSubmit>()
                .eq("status", QuestionSubmitStatusEnum.SUCCEED.getValue()));
        result.put("totalSubmissions", totalSubmissions);
        result.put("acceptedSubmissions", acceptedSubmissions);
        result.put("acceptRate", totalSubmissions > 0
                ? Math.round((double) acceptedSubmissions / totalSubmissions * 10000) / 100.0 : 0);

        // 比赛统计
        long totalContests = contestService.count();
        result.put("totalContests", totalContests);

        return ResultUtils.success(result);
    }

    /**
     * 用户活跃度统计（近30天）
     */
    @GetMapping("/user-activity")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Map<String, Object>> getUserActivity() {
        Map<String, Object> result = new HashMap<>();

        // 近30天注册用户数
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -30);
        Date thirtyDaysAgo = cal.getTime();
        long newUsers = userService.count(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .ge("createTime", thirtyDaysAgo));
        result.put("newUsersLast30Days", newUsers);

        // 近30天提交数
        long submissionsLast30Days = questionSubmitMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<QuestionSubmit>()
                        .ge("createTime", thirtyDaysAgo));
        result.put("submissionsLast30Days", submissionsLast30Days);

        // 近30天活跃用户数（有提交记录的去重用户）
        List<QuestionSubmit> recentSubmits = questionSubmitMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<QuestionSubmit>()
                        .ge("createTime", thirtyDaysAgo)
                        .select("DISTINCT userId"));
        Set<Long> activeUserIds = new HashSet<>();
        for (QuestionSubmit s : recentSubmits) {
            activeUserIds.add(s.getUserId());
        }
        result.put("activeUsersLast30Days", activeUserIds.size());

        // 每日提交趋势（近7天）
        List<Map<String, Object>> dailyTrend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            Calendar dayCal = Calendar.getInstance();
            dayCal.add(Calendar.DAY_OF_MONTH, -i);
            dayCal.set(Calendar.HOUR_OF_DAY, 0);
            dayCal.set(Calendar.MINUTE, 0);
            dayCal.set(Calendar.SECOND, 0);
            Date dayStart = dayCal.getTime();
            dayCal.set(Calendar.HOUR_OF_DAY, 23);
            dayCal.set(Calendar.MINUTE, 59);
            dayCal.set(Calendar.SECOND, 59);
            Date dayEnd = dayCal.getTime();

            long count = questionSubmitMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<QuestionSubmit>()
                            .between("createTime", dayStart, dayEnd));
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", new java.text.SimpleDateFormat("MM-dd").format(dayStart));
            dayData.put("count", count);
            dailyTrend.add(dayData);
        }
        result.put("dailyTrend", dailyTrend);

        return ResultUtils.success(result);
    }

    /**
     * 题目通过率分布
     */
    @GetMapping("/question-pass-rate")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Map<String, Object>> getQuestionPassRate() {
        Map<String, Object> result = new HashMap<>();

        // 获取所有公开且审核通过的题目
        List<com.kkdj.model.entity.Question> questions = questionService.list(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.kkdj.model.entity.Question>()
                        .eq("isPublic", 1).eq("review_status", 1)
                        .select("id", "title", "questionNumber", "submitNum", "acceptedNum", "difficulty"));

        // 通过率分布：0-20%, 20-40%, 40-60%, 60-80%, 80-100%
        int[] distribution = new int[5]; // [0-20, 20-40, 40-60, 60-80, 80-100]
        List<Map<String, Object>> topQuestions = new ArrayList<>();

        for (com.kkdj.model.entity.Question q : questions) {
            int submitNum = q.getSubmitNum() == null ? 0 : q.getSubmitNum();
            int acceptedNum = q.getAcceptedNum() == null ? 0 : q.getAcceptedNum();
            if (submitNum > 0) {
                double rate = (double) acceptedNum / submitNum * 100;
                int index = (int) (rate / 20);
                if (index >= 5) index = 4;
                distribution[index]++;

                Map<String, Object> qData = new HashMap<>();
                qData.put("questionNumber", q.getQuestionNumber());
                qData.put("title", q.getTitle());
                qData.put("submitNum", submitNum);
                qData.put("acceptedNum", acceptedNum);
                qData.put("passRate", Math.round(rate * 100) / 100.0);
                topQuestions.add(qData);
            }
        }

        // 按提交数排序，取前10
        topQuestions.sort((a, b) -> ((Integer) b.get("submitNum")) - ((Integer) a.get("submitNum")));
        if (topQuestions.size() > 10) {
            topQuestions = topQuestions.subList(0, 10);
        }

        result.put("distribution", distribution);
        result.put("topQuestions", topQuestions);

        return ResultUtils.success(result);
    }

    /**
     * 提交次数分布（按用户）
     */
    @GetMapping("/submit-distribution")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Map<String, Object>> getSubmitDistribution() {
        Map<String, Object> result = new HashMap<>();

        // 获取所有用户的提交次数
        List<QuestionSubmit> allSubmits = questionSubmitMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<QuestionSubmit>()
                        .select("userId"));
        Map<Long, Integer> userSubmitCount = new HashMap<>();
        for (QuestionSubmit s : allSubmits) {
            userSubmitCount.merge(s.getUserId(), 1, Integer::sum);
        }

        // 分布：0次, 1-5次, 6-20次, 21-50次, 50次以上
        int[] distribution = new int[5];
        // 0次档 = 总用户数 - 有提交记录的用户数
        long totalUsers = userService.count();
        distribution[0] = (int) Math.max(0, totalUsers - userSubmitCount.size());
        for (int count : userSubmitCount.values()) {
            if (count <= 5) distribution[1]++;
            else if (count <= 20) distribution[2]++;
            else if (count <= 50) distribution[3]++;
            else distribution[4]++;
        }

        result.put("distribution", distribution);
        result.put("totalSubmitters", userSubmitCount.size());

        return ResultUtils.success(result);
    }

    /**
     * 编程语言使用分布
     */
    @GetMapping("/language-distribution")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Map<String, Object>> getLanguageDistribution() {
        Map<String, Object> result = new HashMap<>();

        List<QuestionSubmit> allSubmits = questionSubmitMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<QuestionSubmit>()
                        .select("language"));
        Map<String, Integer> langCount = new HashMap<>();
        for (QuestionSubmit s : allSubmits) {
            String lang = s.getLanguage() == null ? "unknown" : s.getLanguage();
            langCount.merge(lang, 1, Integer::sum);
        }

        // 按使用次数排序
        List<Map<String, Object>> langList = new ArrayList<>();
        langCount.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .forEach(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("language", entry.getKey());
                    item.put("count", entry.getValue());
                    langList.add(item);
                });

        result.put("languages", langList);

        return ResultUtils.success(result);
    }
}

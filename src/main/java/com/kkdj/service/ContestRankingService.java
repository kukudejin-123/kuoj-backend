package com.kkdj.service;

import com.kkdj.model.vo.ContestRankingVO;

import java.util.List;

/**
 * 比赛排行榜服务（ACM规则）
 */
public interface ContestRankingService {

    /**
     * 更新比赛排行榜（提交成功后调用）
     * @param contestId 比赛ID
     * @param userId 用户ID
     * @param questionOrder 题目序号
     * @param isAccepted 是否通过
     * @param submitTimeMinutes 提交时间（分钟，从比赛开始计算）
     * @param wrongCount 错误次数
     */
    void updateRanking(Long contestId, Long userId, int questionOrder, boolean isAccepted, int submitTimeMinutes, int wrongCount);

    /**
     * 获取比赛排行榜
     * @param contestId 比赛ID
     * @return 排行榜列表
     */
    List<ContestRankingVO> getRanking(Long contestId);

    /**
     * 获取用户在比赛中的排名
     * @param contestId 比赛ID
     * @param userId 用户ID
     * @return 排名（从1开始）
     */
    Integer getUserRank(Long contestId, Long userId);

    /**
     * 删除比赛排行榜缓存
     * @param contestId 比赛ID
     */
    void deleteRankingCache(Long contestId);

    /**
     * 比赛结束后持久化排行榜数据
     * @param contestId 比赛ID
     */
    void persistRanking(Long contestId);
}
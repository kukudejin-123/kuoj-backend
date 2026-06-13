package com.kkdj.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 比赛排行榜视图对象（ACM规则）
 */
@Data
public class ContestRankingVO implements Serializable {

    /**
     * 排名
     */
    private Integer rank;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 用户信息
     */
    private UserVO user;

    /**
     * 通过题目数
     */
    private Integer acceptedCount;

    /**
     * 总罚时（分钟）
     */
    private Integer totalTime;

    /**
     * 各题目解题情况
     * key: 题目序号（A, B, C...）
     * value: [通过时间(分钟), 错误次数]
     */
    private Map<String, int[]> problemStats;

    /**
     * 最后提交时间
     */
    private Date lastSubmitTime;

    private static final long serialVersionUID = 1L;
}
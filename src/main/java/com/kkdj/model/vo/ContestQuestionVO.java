package com.kkdj.model.vo;

import com.kkdj.model.entity.ContestQuestion;
import lombok.Data;

import java.io.Serializable;

/**
 * 比赛题目视图对象
 */
@Data
public class ContestQuestionVO implements Serializable {

    /**
     * 题目id
     */
    private Long questionId;

    /**
     * 题目顺序（A=0, B=1...）
     */
    private Integer questionOrder;

    /**
     * 题目顺序字母（A, B, C...）
     */
    private String questionLabel;

    /**
     * 题目分值
     */
    private Integer score;

    /**
     * 题目标题
     */
    private String title;

    /**
     * 题目难度
     */
    private Integer difficulty;

    /**
     * 通过数
     */
    private Integer acceptedNum;

    /**
     * 提交数
     */
    private Integer submitNum;

    private static final long serialVersionUID = 1L;

    /**
     * 获取题目顺序字母
     */
    public static String getOrderLabel(Integer order) {
        if (order == null || order < 0) {
            return "A";
        }
        return String.valueOf((char) ('A' + order % 26));
    }

    /**
     * 对象转包装类
     */
    public static ContestQuestionVO objToVo(ContestQuestion contestQuestion) {
        if (contestQuestion == null) {
            return null;
        }
        ContestQuestionVO vo = new ContestQuestionVO();
        vo.setQuestionId(contestQuestion.getQuestionId());
        vo.setQuestionOrder(contestQuestion.getQuestionOrder());
        vo.setQuestionLabel(getOrderLabel(contestQuestion.getQuestionOrder()));
        vo.setScore(contestQuestion.getScore());
        return vo;
    }
}
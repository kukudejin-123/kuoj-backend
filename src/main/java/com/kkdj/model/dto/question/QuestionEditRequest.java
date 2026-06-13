package com.kkdj.model.dto.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 编辑请求
*/
@Data
public class QuestionEditRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 标签列表（json 数组）
     */
    private List<String> tags;

    /**
     * 题目难度（0-简单，1-中等，2-困难）
     */
    private Integer difficulty;

    /**
     * 题目标案答案
     */
    private String answer;


    /**
     * 判题用例（json 数组）
     */
    private List<JudgeCase> judgeCase;

    /**
     * 判题配置（json 对象）
     */
    private JudgeConfig judgeConfig;

    /**
     * 是否公开：1-公开，0-仅比赛可见
     */
    private Integer isPublic;

    private static final long serialVersionUID = 1L;
}
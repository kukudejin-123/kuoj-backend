package com.kkdj.model.dto.contestsubmit;

import lombok.Data;

import java.io.Serializable;

/**
 * 比赛提交请求
 */
@Data
public class ContestSubmitAddRequest implements Serializable {

    /**
     * 比赛id
     */
    private Long contestId;

    /**
     * 题目id
     */
    private Long questionId;

    /**
     * 编程语言
     */
    private String language;

    /**
     * 提交代码
     */
    private String code;

    private static final long serialVersionUID = 1L;
}
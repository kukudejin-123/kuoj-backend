package com.kkdj.model.dto.contestsubmit;

import com.kkdj.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 比赛提交查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContestSubmitQueryRequest extends PageRequest implements Serializable {

    /**
     * 比赛id
     */
    private Long contestId;

    /**
     * 题目id
     */
    private Long questionId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 判题状态
     */
    private String status;

    private static final long serialVersionUID = 1L;
}
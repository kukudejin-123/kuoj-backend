package com.kkdj.model.dto.contest;

import com.kkdj.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 比赛查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContestQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 比赛名称
     */
    private String contestName;

    /**
     * 创建者用户id
     */
    private Long userId;

    /**
     * 比赛类型：0-ACM，1-IOI
     */
    private Integer contestType;

    /**
     * 状态：0-未开始，1-进行中，2-已结束
     */
    private Integer status;

    private static final long serialVersionUID = 1L;
}
package com.kkdj.model.dto.contest;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kkdj.common.PageRequest;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 创建比赛请求
 */
@Data
public class ContestAddRequest implements Serializable {

    /**
     * 比赛名称
     */
    private String contestName;

    /**
     * 比赛描述
     */
    private String contestDesc;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

    /**
     * 比赛类型：0-ACM，1-IOI
     */
    private Integer contestType;

    /**
     * 题目ID列表
     */
    private List<Long> questionIds;

    private static final long serialVersionUID = 1L;
}
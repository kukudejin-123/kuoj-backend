package com.kkdj.model.dto.ranking;

import com.kkdj.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 用户排行榜查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserRankingQueryRequest extends PageRequest implements Serializable {

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 用户名（搜索）
     */
    private String userName;

    private static final long serialVersionUID = 1L;
}
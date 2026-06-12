package com.kkdj.model.dto.team;

import lombok.Data;

import java.io.Serializable;

/**
 * 移除成员请求
 */
@Data
public class TeamKickRequest implements Serializable {

    /**
     * 团队id
     */
    private Long teamId;

    /**
     * 被移除用户id
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}
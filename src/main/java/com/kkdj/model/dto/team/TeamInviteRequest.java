package com.kkdj.model.dto.team;

import lombok.Data;

import java.io.Serializable;

/**
 * 邀请成员请求
 */
@Data
public class TeamInviteRequest implements Serializable {

    /**
     * 团队id
     */
    private Long teamId;

    /**
     * 被邀请用户id（支持字符串传递，避免前端大数精度丢失）
     */
    private String userId;

    private static final long serialVersionUID = 1L;
}
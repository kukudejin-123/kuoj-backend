package com.kkdj.model.dto.team;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建团队请求
 */
@Data
public class TeamAddRequest implements Serializable {

    /**
     * 团队名称
     */
    private String teamName;

    /**
     * 团队描述
     */
    private String teamDesc;

    /**
     * 团队头像
     */
    private String teamAvatar;

    /**
     * 最大成员数量
     */
    private Integer maxMemberCount;

    /**
     * 队长用户ID（可选，不填则默认当前创建者为队长）
     */
    private Long captainUserId;

    private static final long serialVersionUID = 1L;
}
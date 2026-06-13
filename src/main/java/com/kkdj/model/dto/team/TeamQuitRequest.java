package com.kkdj.model.dto.team;

import lombok.Data;

import java.io.Serializable;

/**
 * 退出团队请求
 */
@Data
public class TeamQuitRequest implements Serializable {

    /**
     * 团队id
     */
    private Long teamId;

    private static final long serialVersionUID = 1L;
}
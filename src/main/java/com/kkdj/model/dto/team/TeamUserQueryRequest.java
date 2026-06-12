package com.kkdj.model.dto.team;

import com.kkdj.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 团队成员查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TeamUserQueryRequest extends PageRequest implements Serializable {

    /**
     * 团队id
     */
    private Long teamId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 角色：0-普通成员，1-管理员，2-队长
     */
    private Integer userRole;

    private static final long serialVersionUID = 1L;
}
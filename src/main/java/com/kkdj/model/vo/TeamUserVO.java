package com.kkdj.model.vo;

import com.kkdj.model.entity.TeamUser;
import com.kkdj.model.enums.TeamUserRoleEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 团队成员视图对象
 */
@Data
public class TeamUserVO implements Serializable {

    /**
     * id
     */
    private Long id;

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

    /**
     * 角色名称
     */
    private String userRoleName;

    /**
     * 加入时间
     */
    private Date joinTime;

    /**
     * 用户信息
     */
    private UserVO user;

    private static final long serialVersionUID = 1L;

    /**
     * 对象转包装类
     */
    public static TeamUserVO objToVo(TeamUser teamUser) {
        if (teamUser == null) {
            return null;
        }
        TeamUserVO teamUserVO = new TeamUserVO();
        teamUserVO.setId(teamUser.getId());
        teamUserVO.setTeamId(teamUser.getTeamId());
        teamUserVO.setUserId(teamUser.getUserId());
        teamUserVO.setUserRole(teamUser.getUserRole());
        teamUserVO.setJoinTime(teamUser.getJoinTime());
        // 设置角色名称
        TeamUserRoleEnum roleEnum = TeamUserRoleEnum.getEnumByValue(teamUser.getUserRole());
        if (roleEnum != null) {
            teamUserVO.setUserRoleName(roleEnum.getText());
        }
        return teamUserVO;
    }
}
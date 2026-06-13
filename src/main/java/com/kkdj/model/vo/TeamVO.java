package com.kkdj.model.vo;

import com.kkdj.model.entity.Team;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 团队视图对象
 */
@Data
public class TeamVO implements Serializable {

    /**
     * id
     */
    private Long id;

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
     * 创建者/队长用户id
     */
    private Long userId;

    /**
     * 成员数量
     */
    private Integer memberCount;

    /**
     * 最大成员数量
     */
    private Integer maxMemberCount;

    /**
     * 状态：0-正常，1-禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建者用户信息
     */
    private UserVO createUser;

    /**
     * 当前用户是否已加入
     */
    private Boolean hasJoin;

    /**
     * 当前用户在团队中的角色（未加入则为null）
     */
    private Integer userRole;

    private static final long serialVersionUID = 1L;

    /**
     * 对象转包装类
     */
    public static TeamVO objToVo(Team team) {
        if (team == null) {
            return null;
        }
        TeamVO teamVO = new TeamVO();
        teamVO.setId(team.getId());
        teamVO.setTeamName(team.getTeamName());
        teamVO.setTeamDesc(team.getTeamDesc());
        teamVO.setTeamAvatar(team.getTeamAvatar());
        teamVO.setUserId(team.getUserId());
        teamVO.setMemberCount(team.getMemberCount());
        teamVO.setMaxMemberCount(team.getMaxMemberCount());
        teamVO.setStatus(team.getStatus());
        teamVO.setCreateTime(team.getCreateTime());
        return teamVO;
    }
}
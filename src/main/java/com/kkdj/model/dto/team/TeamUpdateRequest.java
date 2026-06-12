package com.kkdj.model.dto.team;

import com.kkdj.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 更新团队请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TeamUpdateRequest extends PageRequest implements Serializable {

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
     * 最大成员数量
     */
    private Integer maxMemberCount;

    /**
     * 状态：0-正常，1-禁用
     */
    private Integer status;

    private static final long serialVersionUID = 1L;
}
package com.kkdj.model.dto.team;

import com.kkdj.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 团队查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TeamQueryRequest extends PageRequest implements Serializable {

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
     * 创建者用户id
     */
    private Long userId;

    /**
     * 状态：0-正常，1-禁用
     */
    private Integer status;

    /**
     * 用户id（查询用户所在的团队）
     */
    private Long memberId;

    private static final long serialVersionUID = 1L;
}
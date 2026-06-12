package com.kkdj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 团队实体
 */
@TableName(value = "team")
@Data
public class Team implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
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
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
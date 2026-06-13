package com.kkdj.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

@TableName(value ="question")
@Data
public class Question implements Serializable {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 题号（从1001开始递增）
     */
    private Integer questionNumber;

    private String title;
    private String content;
    private String tags;
    private String answer;
    private String sourceCode;
    private Integer submitNum;
    private Integer acceptedNum;
    private Integer difficulty;
    private String judgeCase;
    private String judgeConfig;
    private Integer thumbNum;
    private Integer favourNum;
    private Long userId;
    private Date createTime;
    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    /**
     * 是否公开：1-公开，0-仅比赛可见
     */
    private Integer isPublic;

    /**
     * 关联比赛id（仅比赛题目）
     */
    private Long contestId;

    /**
     * SPJ判题程序代码
     */
    @TableField("spj_code")
    private String spjCode;

    /**
     * SPJ程序语言，默认 java
     */
    @TableField("spj_language")
    private String spjLanguage;

    /**
     * SPJ是否已预编译：0-未编译，1-已编译
     */
    @TableField("spj_compiled")
    private Integer spjCompiled;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
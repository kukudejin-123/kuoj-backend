package com.kkdj.model.vo;

import com.kkdj.model.entity.ContestSubmit;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 比赛提交视图对象
 */
@Data
public class ContestSubmitVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 比赛id
     */
    private Long contestId;

    /**
     * 题目id
     */
    private Long questionId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 编程语言
     */
    private String language;

    /**
     * 提交代码（比赛期间非本人不可见）
     */
    private String code;

    /**
     * 判题信息(JSON)
     */
    private String judgeInfo;

    /**
     * 判题状态
     */
    private String status;

    /**
     * 提交时间
     */
    private Date submitTime;

    /**
     * 是否首次通过
     */
    private Integer isFirstAccept;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 用户信息
     */
    private UserVO user;

    /**
     * 题目顺序字母
     */
    private String questionLabel;

    private static final long serialVersionUID = 1L;

    /**
     * 对象转包装类
     */
    public static ContestSubmitVO objToVo(ContestSubmit contestSubmit) {
        if (contestSubmit == null) {
            return null;
        }
        ContestSubmitVO vo = new ContestSubmitVO();
        vo.setId(contestSubmit.getId());
        vo.setContestId(contestSubmit.getContestId());
        vo.setQuestionId(contestSubmit.getQuestionId());
        vo.setUserId(contestSubmit.getUserId());
        vo.setLanguage(contestSubmit.getLanguage());
        vo.setCode(contestSubmit.getCode());
        vo.setJudgeInfo(contestSubmit.getJudgeInfo());
        vo.setStatus(contestSubmit.getStatus());
        vo.setSubmitTime(contestSubmit.getSubmitTime());
        vo.setIsFirstAccept(contestSubmit.getIsFirstAccept());
        vo.setCreateTime(contestSubmit.getCreateTime());
        return vo;
    }
}
package com.kkdj.model.vo;

import com.kkdj.model.entity.Contest;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 比赛视图对象
 */
@Data
public class ContestVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 比赛名称
     */
    private String contestName;

    /**
     * 比赛描述
     */
    private String contestDesc;

    /**
     * 创建者用户id
     */
    private Long userId;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 比赛类型：0-ACM，1-IOI
     */
    private Integer contestType;

    /**
     * 状态：0-未开始，1-进行中，2-已结束
     */
    private Integer status;

    /**
     * 参赛人数
     */
    private Integer participantCount;

    /**
     * 题目数量
     */
    private Integer questionCount;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建者用户信息
     */
    private UserVO createUser;

    /**
     * 当前用户是否已报名
     */
    private Boolean hasJoin;

    /**
     * 题目列表（比赛详情时返回）
     */
    private List<ContestQuestionVO> questionList;

    private static final long serialVersionUID = 1L;

    /**
     * 对象转包装类
     */
    public static ContestVO objToVo(Contest contest) {
        if (contest == null) {
            return null;
        }
        ContestVO contestVO = new ContestVO();
        contestVO.setId(contest.getId());
        contestVO.setContestName(contest.getContestName());
        contestVO.setContestDesc(contest.getContestDesc());
        contestVO.setUserId(contest.getUserId());
        contestVO.setStartTime(contest.getStartTime());
        contestVO.setEndTime(contest.getEndTime());
        contestVO.setContestType(contest.getContestType());
        contestVO.setStatus(contest.getStatus());
        contestVO.setParticipantCount(contest.getParticipantCount());
        contestVO.setQuestionCount(contest.getQuestionCount());
        contestVO.setCreateTime(contest.getCreateTime());
        return contestVO;
    }
}
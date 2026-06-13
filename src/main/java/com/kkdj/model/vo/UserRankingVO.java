package com.kkdj.model.vo;

import com.kkdj.model.entity.UserRanking;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户排行榜视图对象
 */
@Data
public class UserRankingVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 用户信息
     */
    private UserVO user;

    /**
     * 参赛次数
     */
    private Integer totalContests;

    /**
     * 通过题目数
     */
    private Integer totalAccepted;

    /**
     * 总提交数
     */
    private Integer totalSubmissions;

    /**
     * 积分
     */
    private Integer rating;

    /**
     * 排名
     */
    private Integer rank;

    /**
     * 创建时间
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;

    /**
     * 对象转包装类
     */
    public static UserRankingVO objToVo(UserRanking userRanking) {
        if (userRanking == null) {
            return null;
        }
        UserRankingVO vo = new UserRankingVO();
        vo.setId(userRanking.getId());
        vo.setUserId(userRanking.getUserId());
        vo.setTotalContests(userRanking.getTotalContests());
        vo.setTotalAccepted(userRanking.getTotalAccepted());
        vo.setTotalSubmissions(userRanking.getTotalSubmissions());
        vo.setRating(userRanking.getRating());
        vo.setCreateTime(userRanking.getCreateTime());
        return vo;
    }
}
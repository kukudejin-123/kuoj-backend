package com.kkdj.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kkdj.model.dto.ranking.UserRankingQueryRequest;
import com.kkdj.model.entity.UserRanking;
import com.kkdj.model.vo.UserRankingVO;

/**
 * 用户排行榜服务
 */
public interface UserRankingService extends IService<UserRanking> {

    /**
     * 获取查询条件
     */
    QueryWrapper<UserRanking> getQueryWrapper(UserRankingQueryRequest queryRequest);

    /**
     * 获取用户排行榜VO
     */
    UserRankingVO getUserRankingVO(UserRanking userRanking);

    /**
     * 分页获取用户排行榜VO
     */
    Page<UserRankingVO> getUserRankingVOPage(Page<UserRanking> page, UserRankingQueryRequest queryRequest);

    /**
     * 获取用户排名信息
     */
    UserRankingVO getUserRanking(Long userId);

    /**
     * 更新用户排行榜数据（比赛结束后调用）
     */
    void updateUserRanking(Long userId);

    /**
     * 批量更新比赛参与者的排行榜数据
     */
    void batchUpdateUserRanking(Long contestId);
}
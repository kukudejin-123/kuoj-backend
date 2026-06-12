package com.kkdj.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kkdj.model.entity.ContestParticipant;

/**
 * 比赛参赛者服务
 */
public interface ContestParticipantService extends IService<ContestParticipant> {

    /**
     * 检查用户是否已报名比赛
     */
    boolean isUserJoined(Long contestId, Long userId);

    /**
     * 用户报名比赛
     */
    boolean joinContest(Long contestId, Long userId);

    /**
     * 用户取消报名
     */
    boolean quitContest(Long contestId, Long userId);
}
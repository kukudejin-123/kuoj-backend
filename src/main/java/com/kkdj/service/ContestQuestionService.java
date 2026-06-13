package com.kkdj.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kkdj.model.entity.ContestQuestion;

/**
 * 比赛题目关联服务
 */
public interface ContestQuestionService extends IService<ContestQuestion> {

    /**
     * 根据比赛ID和题目ID获取关联
     */
    ContestQuestion getByContestAndQuestion(Long contestId, Long questionId);

    /**
     * 获取比赛的所有题目
     */
    java.util.List<ContestQuestion> getQuestionsByContestId(Long contestId);
}
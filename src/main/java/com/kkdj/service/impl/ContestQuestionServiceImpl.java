package com.kkdj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kkdj.mapper.ContestQuestionMapper;
import com.kkdj.model.entity.ContestQuestion;
import com.kkdj.service.ContestQuestionService;
import org.springframework.stereotype.Service;

/**
 * 比赛题目关联服务实现
 */
@Service
public class ContestQuestionServiceImpl extends ServiceImpl<ContestQuestionMapper, ContestQuestion> implements ContestQuestionService {

    @Override
    public ContestQuestion getByContestAndQuestion(Long contestId, Long questionId) {
        QueryWrapper<ContestQuestion> wrapper = new QueryWrapper<>();
        wrapper.eq("contestId", contestId);
        wrapper.eq("questionId", questionId);
        return this.getOne(wrapper);
    }
}
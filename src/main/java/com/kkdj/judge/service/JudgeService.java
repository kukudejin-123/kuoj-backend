package com.kkdj.judge.service;

import com.kkdj.model.entity.QuestionSubmit;

/**
 * 判题服务
 * @author kkdj*/
public interface JudgeService {

    /**
     * 判题
     * @param questionSubmitId
     * @return
     */
    QuestionSubmit doJudge(long questionSubmitId);
}

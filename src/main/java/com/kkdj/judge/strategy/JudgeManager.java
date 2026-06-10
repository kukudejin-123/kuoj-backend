package com.kkdj.judge.strategy;

import com.kkdj.judge.strategy.impl.DefaultJudgeStrategyImpl;
import com.kkdj.judge.codeSandbox.model.JudgeInfo;
import org.springframework.stereotype.Service;

/**
 * 判题管理器: 简化判题的调用*/
@Service
public class JudgeManager {

    /**
     * 执行判题
     * @param judgeContext
     * @return
     */
    public JudgeInfo doJudge(JudgeContext judgeContext){
        // 所有语言统一使用默认判题策略
        JudgeStrategy judgeStrategy = new DefaultJudgeStrategyImpl();
        return judgeStrategy.doJudge(judgeContext);
    }
}

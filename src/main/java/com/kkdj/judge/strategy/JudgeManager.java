package com.kkdj.judge.strategy;

import com.kkdj.judge.codeSandbox.model.JudgeInfo;
import com.kkdj.judge.strategy.impl.DefaultJudgeStrategyImpl;
import com.kkdj.judge.strategy.impl.FloatJudgeStrategyImpl;
import com.kkdj.judge.strategy.impl.IgnoreCaseJudgeStrategyImpl;
import com.kkdj.judge.strategy.impl.IgnoreSpaceJudgeStrategyImpl;
import com.kkdj.judge.strategy.impl.MultiAnswerJudgeStrategyImpl;
import com.kkdj.judge.strategy.impl.TestlibJudgeStrategyImpl;
import com.kkdj.model.dto.question.JudgeConfig;
import com.kkdj.model.enums.JudgeModeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 判题管理器: 根据判题模式选择对应的判题策略
 */
@Service
public class JudgeManager {

    @Autowired
    private TestlibJudgeStrategyImpl testlibJudgeStrategy;

    /**
     * 执行判题
     * @param judgeContext 判题上下文
     * @return 判题结果
     */
    public JudgeInfo doJudge(JudgeContext judgeContext) {
        // 获取判题配置
        JudgeConfig judgeConfig = judgeContext.getJudgeConfig();
        String judgeMode = "DEFAULT";

        if (judgeConfig != null && judgeConfig.getJudgeMode() != null) {
            judgeMode = judgeConfig.getJudgeMode();
        }

        // 根据判题模式选择策略
        JudgeStrategy judgeStrategy = getStrategy(judgeMode);

        return judgeStrategy.doJudge(judgeContext);
    }

    /**
     * 根据判题模式获取对应的策略
     */
    private JudgeStrategy getStrategy(String judgeMode) {
        JudgeModeEnum modeEnum = JudgeModeEnum.getEnumByValue(judgeMode);

        switch (modeEnum) {
            case IGNORE_SPACE:
                return new IgnoreSpaceJudgeStrategyImpl();
            case IGNORE_CASE:
                return new IgnoreCaseJudgeStrategyImpl();
            case FLOAT:
                return new FloatJudgeStrategyImpl();
            case MULTI_ANSWER:
                return new MultiAnswerJudgeStrategyImpl();
            case TESTLIB:
                return testlibJudgeStrategy;
            case DEFAULT:
            default:
                return new DefaultJudgeStrategyImpl();
        }
    }
}

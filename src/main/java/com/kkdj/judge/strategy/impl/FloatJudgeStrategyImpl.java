package com.kkdj.judge.strategy.impl;

import cn.hutool.json.JSONUtil;
import com.kkdj.judge.codeSandbox.model.JudgeInfo;
import com.kkdj.judge.strategy.JudgeContext;
import com.kkdj.judge.strategy.JudgeStrategy;
import com.kkdj.model.dto.question.JudgeCase;
import com.kkdj.model.dto.question.JudgeConfig;
import com.kkdj.model.entity.Question;
import com.kkdj.model.enums.JudgeInfoMessageEnum;

import java.util.List;

/**
 * 浮点数精度判题策略
 * 适用于计算结果为小数的题目
 */
public class FloatJudgeStrategyImpl implements JudgeStrategy {

    private static final double DEFAULT_PRECISION = 1e-6;

    @Override
    public JudgeInfo doJudge(JudgeContext judgeContext) {
        JudgeInfo judgeInfo = judgeContext.getJudgeInfo();
        List<String> outputList = judgeContext.getOutputList();
        List<JudgeCase> judgeCaseList = judgeContext.getJudgeCaseList();
        Question question = judgeContext.getQuestion();
        JudgeConfig judgeConfig = judgeContext.getJudgeConfig();

        long time = judgeInfo.getTime();
        long memory = judgeInfo.getMemory();

        JudgeInfo judgeInfoResponse = new JudgeInfo();
        judgeInfoResponse.setMemory(memory);
        judgeInfoResponse.setTime(time);

        // 获取题目配置
        String judgeConfigStr = question.getJudgeConfig();
        JudgeConfig config = JSONUtil.toBean(judgeConfigStr, JudgeConfig.class);
        long memoryLimit = config.getMemoryLimit();
        long timeLimit = config.getTimeLimit();

        // 如果沙箱已经返回了错误状态，检查是否应该改为内存超限
        String sandboxMessage = judgeInfo.getMessage();
        if (sandboxMessage != null && !sandboxMessage.isEmpty()) {
            JudgeInfoMessageEnum sandboxStatus = JudgeInfoMessageEnum.getEnumByValue(sandboxMessage);
            if (sandboxStatus != null && sandboxStatus != JudgeInfoMessageEnum.ACCEPTED) {
                // 即使沙箱没返回内存超限，如果实际内存超过限制，应该返回内存超限
                if (memory > memoryLimit && sandboxStatus != JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED) {
                    judgeInfoResponse.setMessage(JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED.getValue());
                    return judgeInfoResponse;
                }
                judgeInfoResponse.setMessage(sandboxMessage);
                return judgeInfoResponse;
            }
        }

        // 检查时间限制
        if (time > timeLimit) {
            judgeInfoResponse.setMessage(JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED.getValue());
            return judgeInfoResponse;
        }

        // 检查内存限制
        if (memory > memoryLimit) {
            judgeInfoResponse.setMessage(JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED.getValue());
            return judgeInfoResponse;
        }

        // 获取精度阈值
        double precision = DEFAULT_PRECISION;
        if (judgeConfig != null && judgeConfig.getFloatPrecision() != null) {
            precision = judgeConfig.getFloatPrecision();
        }

        // 判断输出数量是否一致
        if (outputList.size() != judgeCaseList.size()) {
            judgeInfoResponse.setMessage(JudgeInfoMessageEnum.WRONG_ANSWER.getValue());
            return judgeInfoResponse;
        }

        // 逐个比较
        for (int i = 0; i < judgeCaseList.size(); i++) {
            JudgeCase judgeCase = judgeCaseList.get(i);
            String expectedOutput = judgeCase.getOutput();
            String actualOutput = outputList.get(i);

            // 使用浮点数精度比较
            if (!compareWithPrecision(expectedOutput, actualOutput, precision)) {
                judgeInfoResponse.setMessage(JudgeInfoMessageEnum.WRONG_ANSWER.getValue());
                return judgeInfoResponse;
            }
        }

        judgeInfoResponse.setMessage(JudgeInfoMessageEnum.ACCEPTED.getValue());
        return judgeInfoResponse;
    }

    private boolean compareWithPrecision(String expected, String actual, double precision) {
        if (expected == null) expected = "";
        if (actual == null) actual = "";

        expected = expected.trim();
        actual = actual.trim();

        // 尝试解析为浮点数
        if (isNumeric(expected) && isNumeric(actual)) {
            try {
                double exp = Double.parseDouble(expected);
                double act = Double.parseDouble(actual);
                return Math.abs(exp - act) < precision;
            } catch (NumberFormatException e) {
                // 解析失败，回退到字符串比较
            }
        }

        return expected.equals(actual);
    }

    private boolean isNumeric(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
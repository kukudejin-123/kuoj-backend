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
 * 多解判题策略
 * 适用于答案有多种可能的情况，如判断题 YES/Yes/yes/Y 都算正确
 */
public class MultiAnswerJudgeStrategyImpl implements JudgeStrategy {

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

        // 获取可接受答案列表
        List<String> acceptableOutputs = null;
        if (judgeConfig != null) {
            acceptableOutputs = judgeConfig.getAcceptableOutputs();
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

            if (actualOutput == null) {
                actualOutput = "";
            }

            String actualTrimmed = actualOutput.trim();

            // 情况1：用户输出在可接受答案列表中
            if (acceptableOutputs != null && !acceptableOutputs.isEmpty()) {
                boolean found = false;
                for (String acceptable : acceptableOutputs) {
                    if (acceptable != null && acceptable.trim().equals(actualTrimmed)) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    continue;
                }
            }

            // 情况2：用户输出与期望输出精确匹配
            if (expectedOutput != null && expectedOutput.trim().equals(actualTrimmed)) {
                continue;
            }

            // 都不满足，答案错误
            judgeInfoResponse.setMessage(JudgeInfoMessageEnum.WRONG_ANSWER.getValue());
            return judgeInfoResponse;
        }

        judgeInfoResponse.setMessage(JudgeInfoMessageEnum.ACCEPTED.getValue());
        return judgeInfoResponse;
    }
}
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

public class DefaultJudgeStrategyImpl implements JudgeStrategy {

    /**
     * 判题逻辑
     * @param judgeContext
     * @return
     */
    @Override
    public JudgeInfo doJudge(JudgeContext judgeContext) {
        // 获取判题信息
        JudgeInfo judgeInfo = judgeContext.getJudgeInfo();
        List<String> inputList = judgeContext.getInputList();
        List<String> outputList = judgeContext.getOutputList();

        // 获取判题用例
        List<JudgeCase> judgeCaseList = judgeContext.getJudgeCaseList();
        Question question = judgeContext.getQuestion();

        // 判题实际执行时间
        long time = judgeInfo.getTime();
        // 判题实际占用内存
        long memory = judgeInfo.getMemory();

        JudgeInfo judgeInfoResponse = new JudgeInfo();
        judgeInfoResponse.setMemory(memory);
        judgeInfoResponse.setTime(time);

        // 先检查时间和内存限制（优先级高于沙箱返回的状态）
        String judgeConfigStr = question.getJudgeConfig();
        JudgeConfig judgeConfig = JSONUtil.toBean(judgeConfigStr, JudgeConfig.class);
        long memoryLimit = judgeConfig.getMemoryLimit();
        long timeLimit = judgeConfig.getTimeLimit();

        // 如果沙箱已经返回了错误状态（编译错误、运行错误、超时等），直接返回
        String sandboxMessage = judgeInfo.getMessage();
        System.out.println("DefaultJudgeStrategy - 沙箱返回消息: " + sandboxMessage);
        System.out.println("DefaultJudgeStrategy - 实际时间: " + time + "ms, 限制: " + timeLimit + "ms");
        System.out.println("DefaultJudgeStrategy - 实际内存: " + memory + "KB, 限制: " + memoryLimit + "KB");

        if (sandboxMessage != null && !sandboxMessage.isEmpty()) {
            JudgeInfoMessageEnum sandboxStatus = JudgeInfoMessageEnum.getEnumByValue(sandboxMessage);
            System.out.println("DefaultJudgeStrategy - 解析后的状态: " + (sandboxStatus != null ? sandboxStatus.getValue() : "null"));
            if (sandboxStatus != null && sandboxStatus != JudgeInfoMessageEnum.ACCEPTED) {
                // 沙箱已经判断出错误（编译错误、运行错误、超时等）
                // 但我们仍然需要检查内存是否超限（沙箱可能没有正确检测）
                if (memory > memoryLimit && sandboxStatus != JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED) {
                    System.out.println("DefaultJudgeStrategy - 内存超限，覆盖沙箱状态");
                    judgeInfoResponse.setMessage(JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED.getValue());
                    return judgeInfoResponse;
                }
                System.out.println("DefaultJudgeStrategy - 直接返回沙箱状态: " + sandboxMessage);
                judgeInfoResponse.setMessage(sandboxMessage);
                return judgeInfoResponse;
            }
        }

        // 检查时间限制
        if (time > timeLimit) {
            System.out.println("DefaultJudgeStrategy - 时间超限");
            judgeInfoResponse.setMessage(JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED.getValue());
            return judgeInfoResponse;
        }

        // 检查内存限制
        if (memory > memoryLimit) {
            System.out.println("DefaultJudgeStrategy - 内存超限");
            judgeInfoResponse.setMessage(JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED.getValue());
            return judgeInfoResponse;
        }

        // 沙箱执行成功，需要判断答案是否正确
        JudgeInfoMessageEnum judgeInfoMessageEnum = JudgeInfoMessageEnum.ACCEPTED;

        // 判断输出数量是否与期望数量一致
        int expectedCount = judgeCaseList.size();
        int actualCount = outputList.size();
        System.out.println("DefaultJudgeStrategy - 输出数量比对: 期望=" + expectedCount + ", 实际=" + actualCount);
        if (actualCount != expectedCount) {
            System.out.println("DefaultJudgeStrategy - 输出行数不一致");
            judgeInfoMessageEnum = JudgeInfoMessageEnum.WRONG_ANSWER;
            judgeInfoResponse.setMessage(judgeInfoMessageEnum.getValue());
            return judgeInfoResponse;
        }

        // 依次判断每个用例是否一致
        for (int i = 0; i < judgeCaseList.size(); i++) {
            JudgeCase judgeCase = judgeCaseList.get(i);
            String expectedOutput = judgeCase.getOutput();
            String actualOutput = outputList.get(i);

            System.out.println("DefaultJudgeStrategy - 第" + (i+1) + "个用例:");
            System.out.println("  期望输出: [" + expectedOutput + "]");
            System.out.println("  实际输出: [" + actualOutput + "]");
            System.out.println("  期望trim: [" + (expectedOutput != null ? expectedOutput.trim() : "null") + "]");
            System.out.println("  实际trim: [" + (actualOutput != null ? actualOutput.trim() : "null") + "]");
            System.out.println("  是否相等: " + (expectedOutput != null && actualOutput != null && expectedOutput.trim().equals(actualOutput.trim())));

            // 去除首尾空白字符后比较，避免换行符/空格差异导致误判
            if (expectedOutput == null || actualOutput == null ||
                !expectedOutput.trim().equals(actualOutput.trim())) {
                System.out.println("DefaultJudgeStrategy - 第" + (i+1) + "个用例不匹配");
                judgeInfoMessageEnum = JudgeInfoMessageEnum.WRONG_ANSWER;
                judgeInfoResponse.setMessage(judgeInfoMessageEnum.getValue());
                return judgeInfoResponse;
            }
        }
        System.out.println("DefaultJudgeStrategy - 所有用例匹配成功");

        judgeInfoResponse.setMessage(judgeInfoMessageEnum.getValue());
        return judgeInfoResponse;
    }
}

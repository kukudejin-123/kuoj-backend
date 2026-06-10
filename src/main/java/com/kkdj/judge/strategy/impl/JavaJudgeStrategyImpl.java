package com.kkdj.judge.strategy.impl;

import cn.hutool.json.JSONUtil;
import com.kkdj.judge.strategy.JudgeContext;
import com.kkdj.judge.strategy.JudgeStrategy;
import com.kkdj.model.dto.question.JudgeCase;
import com.kkdj.model.dto.question.JudgeConfig;
import com.kkdj.judge.codeSandbox.model.JudgeInfo;
import com.kkdj.model.entity.Question;
import com.kkdj.model.enums.JudgeInfoMessageEnum;

import java.util.List;

public class JavaJudgeStrategyImpl implements JudgeStrategy {

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

        // 如果沙箱已经返回了错误状态（编译错误、运行错误、超时等），直接返回
        String sandboxMessage = judgeInfo.getMessage();
        System.out.println("判题策略 - 沙箱返回消息: " + sandboxMessage);
        if (sandboxMessage != null && !sandboxMessage.isEmpty()) {
            JudgeInfoMessageEnum sandboxStatus = JudgeInfoMessageEnum.getEnumByValue(sandboxMessage);
            System.out.println("判题策略 - 解析后的状态: " + (sandboxStatus != null ? sandboxStatus.getValue() : "null"));
            if (sandboxStatus != null && sandboxStatus != JudgeInfoMessageEnum.ACCEPTED) {
                // 沙箱已经判断出错误（编译错误、运行错误、超时等），直接返回
                System.out.println("判题策略 - 直接返回沙箱状态: " + sandboxMessage);
                judgeInfoResponse.setMessage(sandboxMessage);
                return judgeInfoResponse;
            }
        }

        // 沙箱执行成功，需要判断答案是否正确
        JudgeInfoMessageEnum judgeInfoMessageEnum = JudgeInfoMessageEnum.ACCEPTED;

        // 判断执行结果和用例结果的数量是否一致
        if (outputList.size() != inputList.size()) {
            judgeInfoMessageEnum = JudgeInfoMessageEnum.WRONG_ANSWER;
            judgeInfoResponse.setMessage(judgeInfoMessageEnum.getValue());
            return judgeInfoResponse;
        }

        // 依次判断每个用例是否一致
        for (int i = 0; i < judgeCaseList.size(); i++) {
            JudgeCase judgeCase = judgeCaseList.get(i);
            String expectedOutput = judgeCase.getOutput();
            String actualOutput = outputList.get(i);
            // 去除首尾空白字符后比较，避免换行符/空格差异导致误判
            if (expectedOutput == null || actualOutput == null ||
                !expectedOutput.trim().equals(actualOutput.trim())) {
                judgeInfoMessageEnum = JudgeInfoMessageEnum.WRONG_ANSWER;
                judgeInfoResponse.setMessage(judgeInfoMessageEnum.getValue());
                return judgeInfoResponse;
            }
        }

        // 判断其他条件是否符合要求，比如时间、内存限制
        String judgeConfigStr = question.getJudgeConfig();
        JudgeConfig judgeConfig = JSONUtil.toBean(judgeConfigStr, JudgeConfig.class);
        long memoryLimit = judgeConfig.getMemoryLimit();
        long timeLimit = judgeConfig.getTimeLimit();

        if (time > timeLimit) {
            judgeInfoMessageEnum = JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED;
            judgeInfoResponse.setMessage(judgeInfoMessageEnum.getValue());
            return judgeInfoResponse;
        }
        if (memory > memoryLimit) {
            judgeInfoMessageEnum = JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED;
            judgeInfoResponse.setMessage(judgeInfoMessageEnum.getValue());
            return judgeInfoResponse;
        }

        judgeInfoResponse.setMessage(judgeInfoMessageEnum.getValue());
        return judgeInfoResponse;
    }
}

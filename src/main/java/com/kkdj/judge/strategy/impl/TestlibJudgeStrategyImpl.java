package com.kkdj.judge.strategy.impl;

import cn.hutool.json.JSONUtil;
import com.kkdj.judge.codeSandbox.model.JudgeInfo;
import com.kkdj.judge.spj.JavaSpjExecutor;
import com.kkdj.judge.spj.SpjCacheManager;
import com.kkdj.judge.spj.SpjResult;
import com.kkdj.judge.strategy.JudgeContext;
import com.kkdj.judge.strategy.JudgeStrategy;
import com.kkdj.model.dto.question.JudgeCase;
import com.kkdj.model.dto.question.JudgeConfig;
import com.kkdj.model.entity.Question;
import com.kkdj.model.enums.JudgeInfoMessageEnum;
import com.kkdj.testlib.Verdict;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * TESTLIB 判题策略实现
 * 使用 SPJ（Special Judge）进行自定义判题
 */
@Slf4j
@Component
public class TestlibJudgeStrategyImpl implements JudgeStrategy {

    @Resource
    private SpjCacheManager spjCacheManager;

    @Resource
    private JavaSpjExecutor spjExecutor;

    @Override
    public JudgeInfo doJudge(JudgeContext judgeContext) {
        JudgeInfo judgeInfo = judgeContext.getJudgeInfo();
        List<String> inputList = judgeContext.getInputList();
        List<String> outputList = judgeContext.getOutputList();
        List<JudgeCase> judgeCaseList = judgeContext.getJudgeCaseList();
        Question question = judgeContext.getQuestion();
        JudgeConfig judgeConfig = judgeContext.getJudgeConfig();

        long time = judgeInfo.getTime();
        long memory = judgeInfo.getMemory();

        JudgeInfo judgeInfoResponse = new JudgeInfo();
        judgeInfoResponse.setMemory(memory);
        judgeInfoResponse.setTime(time);

        // 1. 检查时间和内存限制
        String judgeConfigStr = question.getJudgeConfig();
        JudgeConfig config = JSONUtil.toBean(judgeConfigStr, JudgeConfig.class);
        long memoryLimit = config.getMemoryLimit();
        long timeLimit = config.getTimeLimit();

        String sandboxMessage = judgeInfo.getMessage();
        log.debug("沙箱返回消息: {}, 时间: {}ms/{}, 内存: {}KB/{}", sandboxMessage, time, timeLimit, memory, memoryLimit);

        // 检查沙箱错误
        if (sandboxMessage != null && !sandboxMessage.isEmpty()) {
            JudgeInfoMessageEnum sandboxStatus = JudgeInfoMessageEnum.getEnumByValue(sandboxMessage);
            if (sandboxStatus != null && sandboxStatus != JudgeInfoMessageEnum.ACCEPTED) {
                if (memory > memoryLimit && sandboxStatus != JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED) {
                    judgeInfoResponse.setMessage(JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED.getValue());
                    return judgeInfoResponse;
                }
                judgeInfoResponse.setMessage(sandboxMessage);
                return judgeInfoResponse;
            }
        }

        // 检查资源限制
        if (time > timeLimit) {
            judgeInfoResponse.setMessage(JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED.getValue());
            return judgeInfoResponse;
        }

        if (memory > memoryLimit) {
            judgeInfoResponse.setMessage(JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED.getValue());
            return judgeInfoResponse;
        }

        // 2. 检查输出数量
        int expectedCount = judgeCaseList.size();
        int actualCount = outputList.size();

        if (actualCount != expectedCount) {
            log.debug("输出行数不一致: 期望={}, 实际={}", expectedCount, actualCount);
            judgeInfoResponse.setMessage(JudgeInfoMessageEnum.WRONG_ANSWER.getValue());
            return judgeInfoResponse;
        }

        // 3. 获取 SPJ 代码
        String spjCode = judgeConfig.getSpjCode();
        if (spjCode == null || spjCode.isEmpty()) {
            log.error("TESTLIB 模式但未提供 SPJ 代码");
            judgeInfoResponse.setMessage(JudgeInfoMessageEnum.SYSTEM_ERROR.getValue());
            judgeInfoResponse.setDetail("题目配置错误：未提供 SPJ 代码");
            return judgeInfoResponse;
        }

        Long questionId = question.getId();

        // 4. 确保 SPJ 已编译
        if (!spjCacheManager.ensureCompiled(spjCode, questionId)) {
            log.error("SPJ 编译失败，题目ID: {}", questionId);
            judgeInfoResponse.setMessage(JudgeInfoMessageEnum.SYSTEM_ERROR.getValue());
            judgeInfoResponse.setDetail("SPJ 编译失败");
            return judgeInfoResponse;
        }

        // 5. 逐个测试用例执行 SPJ 判题
        for (int i = 0; i < judgeCaseList.size(); i++) {
            JudgeCase judgeCase = judgeCaseList.get(i);
            String input = judgeCase.getInput();
            String userOutput = outputList.get(i);
            String answer = judgeCase.getOutput();

            log.debug("执行测试用例 {}: 输入={}, 用户输出={}, 答案={}", i + 1, input, userOutput, answer);

            // 执行 SPJ
            SpjResult result = executeSpj(questionId, input, userOutput, answer, config);

            if (result.isTimeout()) {
                judgeInfoResponse.setMessage(JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED.getValue());
                judgeInfoResponse.setDetail("SPJ 执行超时");
                return judgeInfoResponse;
            }

            if (result.isCompileError()) {
                judgeInfoResponse.setMessage(JudgeInfoMessageEnum.SYSTEM_ERROR.getValue());
                judgeInfoResponse.setDetail("SPJ 编译错误: " + result.getCompileErrorInfo());
                return judgeInfoResponse;
            }

            // 根据 SPJ 退出码判断结果
            Verdict verdict = Verdict.fromExitCode(result.getExitCode());
            log.debug("测试用例 {} 结果: {} - {}", i + 1, verdict, result.getMessage());

            switch (verdict) {
                case OK:
                    // 继续下一个测试用例
                    break;
                case WA:
                    judgeInfoResponse.setMessage(JudgeInfoMessageEnum.WRONG_ANSWER.getValue());
                    judgeInfoResponse.setDetail(result.getMessage());
                    return judgeInfoResponse;
                case PE:
                    judgeInfoResponse.setMessage(JudgeInfoMessageEnum.PRESENTATION_ERROR.getValue());
                    judgeInfoResponse.setDetail(result.getMessage());
                    return judgeInfoResponse;
                case FAIL:
                    judgeInfoResponse.setMessage(JudgeInfoMessageEnum.SYSTEM_ERROR.getValue());
                    judgeInfoResponse.setDetail("SPJ 错误: " + result.getMessage());
                    return judgeInfoResponse;
            }
        }

        // 6. 所有测试用例通过
        log.info("所有测试用例通过，题目ID: {}", questionId);
        judgeInfoResponse.setMessage(JudgeInfoMessageEnum.ACCEPTED.getValue());
        return judgeInfoResponse;
    }

    /**
     * 执行 SPJ 判题
     */
    private SpjResult executeSpj(Long questionId, String input, String userOutput, String answer, JudgeConfig config) {
        // 创建临时文件目录
        String tempDir = System.getProperty("java.io.tmpdir");
        String sessionId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Path sessionDir = Paths.get(tempDir, "oj_spj_session", sessionId);

        try {
            Files.createDirectories(sessionDir);

            // 写入临时文件
            Path inputFile = sessionDir.resolve("input.txt");
            Path userOutputFile = sessionDir.resolve("userOutput.txt");
            Path answerFile = sessionDir.resolve("answer.txt");

            writeStringToFile(inputFile, input != null ? input : "");
            writeStringToFile(userOutputFile, userOutput != null ? userOutput : "");
            writeStringToFile(answerFile, answer != null ? answer : "");

            // 执行 SPJ
            int timeout = (int) (config.getTimeLimit() / 1000) + 5; // SPJ 超时时间比题目时间限制多 5 秒
            SpjResult result = spjExecutor.execute(
                    questionId,
                    inputFile.toString(),
                    userOutputFile.toString(),
                    answerFile.toString(),
                    timeout
            );

            return result;

        } catch (IOException e) {
            log.error("创建临时文件失败", e);
            return SpjResult.systemError("创建临时文件失败: " + e.getMessage());
        } finally {
            // 清理临时文件
            try {
                deleteDirectory(sessionDir);
            } catch (IOException e) {
                log.warn("清理临时文件失败: {}", sessionDir, e);
            }
        }
    }

    /**
     * 删除目录及其内容
     */
    private void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walk(dir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // ignore
                        }
                    });
        }
    }

    /**
     * 写入字符串到文件（Java 8 兼容）
     */
    private void writeStringToFile(Path path, String content) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(path.toFile()), StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }
}
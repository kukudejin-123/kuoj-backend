package com.kkdj.judge.codeSandbox.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.kkdj.judge.codeSandbox.CodeSandbox;
import com.kkdj.judge.codeSandbox.model.ExecuteCodeRequest;
import com.kkdj.judge.codeSandbox.model.ExecuteCodeResponse;
import com.kkdj.judge.codeSandbox.model.JudgeInfo;
import com.kkdj.model.enums.JudgeInfoMessageEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Judge0 代码沙箱实现
 * 官方文档: https://judge0.com/extra-api
 * 公共API: https://ce.judge0.com/submissions?wait=true
 */
@Slf4j
public class Judge0CodeSandbox implements CodeSandbox {

    private static final String DEFAULT_API_URL = "https://ce.judge0.com/submissions";
    private String apiUrl;

    private static final Map<String, Integer> LANGUAGE_ID_MAP = new HashMap<>();
    private static final Map<Integer, JudgeInfoMessageEnum> STATUS_ID_MAP = new HashMap<>();

    static {
        LANGUAGE_ID_MAP.put("java", 62);
        LANGUAGE_ID_MAP.put("python", 71);
        LANGUAGE_ID_MAP.put("python3", 71);
        LANGUAGE_ID_MAP.put("cpp", 54);
        LANGUAGE_ID_MAP.put("c++", 54);
        LANGUAGE_ID_MAP.put("c", 50);
        LANGUAGE_ID_MAP.put("javascript", 63);
        LANGUAGE_ID_MAP.put("js", 63);
        LANGUAGE_ID_MAP.put("go", 60);
        LANGUAGE_ID_MAP.put("golang", 60);
        LANGUAGE_ID_MAP.put("csharp", 51);
        LANGUAGE_ID_MAP.put("c#", 51);
        LANGUAGE_ID_MAP.put("php", 68);
        LANGUAGE_ID_MAP.put("ruby", 72);
        LANGUAGE_ID_MAP.put("rust", 73);
        LANGUAGE_ID_MAP.put("typescript", 74);
        LANGUAGE_ID_MAP.put("ts", 74);
        LANGUAGE_ID_MAP.put("kotlin", 78);
        LANGUAGE_ID_MAP.put("scala", 81);
        LANGUAGE_ID_MAP.put("swift", 83);

        STATUS_ID_MAP.put(1, JudgeInfoMessageEnum.WAITING);
        STATUS_ID_MAP.put(2, JudgeInfoMessageEnum.WAITING);
        STATUS_ID_MAP.put(3, JudgeInfoMessageEnum.ACCEPTED);
        STATUS_ID_MAP.put(4, JudgeInfoMessageEnum.WRONG_ANSWER);
        STATUS_ID_MAP.put(5, JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED);
        STATUS_ID_MAP.put(6, JudgeInfoMessageEnum.COMPILE_ERROR);
        STATUS_ID_MAP.put(7, JudgeInfoMessageEnum.RUNTIME_ERROR);
        STATUS_ID_MAP.put(8, JudgeInfoMessageEnum.RUNTIME_ERROR);
        STATUS_ID_MAP.put(9, JudgeInfoMessageEnum.RUNTIME_ERROR);
        STATUS_ID_MAP.put(10, JudgeInfoMessageEnum.RUNTIME_ERROR);
        STATUS_ID_MAP.put(11, JudgeInfoMessageEnum.COMPILE_ERROR);
        STATUS_ID_MAP.put(12, JudgeInfoMessageEnum.RUNTIME_ERROR);
        STATUS_ID_MAP.put(13, JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED);
        STATUS_ID_MAP.put(14, JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED);
        STATUS_ID_MAP.put(15, JudgeInfoMessageEnum.SYSTEM_ERROR);
        STATUS_ID_MAP.put(16, JudgeInfoMessageEnum.RUNTIME_ERROR);
        STATUS_ID_MAP.put(17, JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED);
        STATUS_ID_MAP.put(18, JudgeInfoMessageEnum.RUNTIME_ERROR);
        STATUS_ID_MAP.put(19, JudgeInfoMessageEnum.RUNTIME_ERROR);
        STATUS_ID_MAP.put(20, JudgeInfoMessageEnum.RUNTIME_ERROR);
    }

    public Judge0CodeSandbox() {
        this.apiUrl = DEFAULT_API_URL;
    }

    public Judge0CodeSandbox(String apiUrl) {
        this.apiUrl = StringUtils.isNotBlank(apiUrl) ? apiUrl : DEFAULT_API_URL;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        log.debug("Judge0 代码沙箱执行中...");

        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        Integer languageId = LANGUAGE_ID_MAP.get(language.toLowerCase());
        if (languageId == null) {
            return getErrorResponse(JudgeInfoMessageEnum.SYSTEM_ERROR, "不支持的语言: " + language);
        }

        List<String> outputList = new ArrayList<>();
        long totalTime = 0;
        long maxMemory = 0;
        JudgeInfoMessageEnum finalStatus = JudgeInfoMessageEnum.ACCEPTED;
        String errorMessage = null;

        for (String input : inputList) {
            try {
                JSONObject requestBody = new JSONObject();
                requestBody.set("source_code", Base64.encode(code.getBytes(StandardCharsets.UTF_8)));
                requestBody.set("language_id", languageId);
                requestBody.set("stdin", Base64.encode((input != null ? input : "").getBytes(StandardCharsets.UTF_8)));
                requestBody.set("cpu_time_limit", 5.0);
                requestBody.set("cpu_extra_time", 1.0);
                requestBody.set("wall_time_limit", 10.0);
                requestBody.set("memory_limit", 131072);

                log.debug("Judge0 请求: language_id={}", languageId);

                String responseStr = HttpUtil.createPost(apiUrl + "?wait=true&base64_encoded=true")
                        .header("Content-Type", "application/json")
                        .body(requestBody.toString())
                        .timeout(30000)
                        .execute()
                        .body();

                log.debug("Judge0 响应: {}", responseStr);

                if (StringUtils.isBlank(responseStr)) {
                    return getErrorResponse(JudgeInfoMessageEnum.SYSTEM_ERROR, "Judge0 API 响应为空");
                }

                JSONObject response;
                try {
                    response = JSONUtil.parseObj(responseStr);
                } catch (Exception e) {
                    log.error("Judge0 响应解析失败: {}", e.getMessage());
                    return getErrorResponse(JudgeInfoMessageEnum.SYSTEM_ERROR, "Judge0 响应解析失败: " + e.getMessage());
                }

                JSONObject status = response.getJSONObject("status");
                Integer statusId = status != null ? status.getInt("id") : null;
                String statusDesc = status != null ? status.getStr("description") : "未知状态";
                log.debug("Judge0 statusId: {}", statusId);

                String stdout = response.getStr("stdout", "");
                String stderr = response.getStr("stderr", "");
                String compileOutput = response.getStr("compile_output", "");

                if (StringUtils.isNotBlank(stdout)) {
                    try {
                        stdout = new String(Base64.decode(stdout), StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        log.debug("stdout 解码失败: {}", e.getMessage());
                    }
                }
                if (StringUtils.isNotBlank(stderr)) {
                    try {
                        stderr = new String(Base64.decode(stderr), StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        log.debug("stderr 解码失败: {}", e.getMessage());
                    }
                }
                if (StringUtils.isNotBlank(compileOutput)) {
                    try {
                        compileOutput = new String(Base64.decode(compileOutput), StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        log.debug("compileOutput 解码失败: {}", e.getMessage());
                    }
                }

                String timeStr = response.getStr("time", "0");
                String memoryStr = response.getStr("memory", "0");
                double timeSeconds = StringUtils.isNotBlank(timeStr) ? Double.parseDouble(timeStr) : 0;
                long memoryKB = StringUtils.isNotBlank(memoryStr) ? Long.parseLong(memoryStr) : 0;
                long timeMs = (long) (timeSeconds * 1000);
                totalTime += timeMs;
                maxMemory = Math.max(maxMemory, memoryKB);

                if (statusId != null) {
                    JudgeInfoMessageEnum currentStatus = STATUS_ID_MAP.getOrDefault(statusId, JudgeInfoMessageEnum.SYSTEM_ERROR);
                    log.debug("Judge0 状态 ID: {}, 映射到: {}", statusId, currentStatus.getValue());

                    if (statusId == 6) {
                        return getErrorResponse(JudgeInfoMessageEnum.COMPILE_ERROR,
                                "编译错误:\n" + (StringUtils.isNotBlank(compileOutput) ? compileOutput : stderr));
                    }

                    if (statusId > 3) {
                        if (finalStatus == JudgeInfoMessageEnum.ACCEPTED || isErrorPriorityHigher(finalStatus, currentStatus)) {
                            finalStatus = currentStatus;
                            errorMessage = StringUtils.isNotBlank(stderr) ? "运行错误:\n" + stderr : currentStatus.getValue() + ": " + statusDesc;
                        }
                    }
                    outputList.add(stdout != null ? stdout.trim() : "");
                } else {
                    finalStatus = JudgeInfoMessageEnum.SYSTEM_ERROR;
                    errorMessage = "无法获取执行状态";
                    outputList.add("");
                }

            } catch (Exception e) {
                log.error("Judge0 执行异常", e);
                return getErrorResponse(JudgeInfoMessageEnum.SYSTEM_ERROR, "Judge0 执行异常: " + e.getMessage());
            }
        }

        ExecuteCodeResponse response = new ExecuteCodeResponse();
        response.setOutputList(outputList);

        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(totalTime);
        judgeInfo.setMemory(maxMemory);
        judgeInfo.setMessage(finalStatus.getValue());

        response.setJudgeInfo(judgeInfo);
        response.setMessage(errorMessage);
        response.setStatus(finalStatus == JudgeInfoMessageEnum.ACCEPTED ? 1 : 3);

        log.debug("最终判题结果: status={}, message={}", finalStatus.getValue(), judgeInfo.getMessage());

        return response;
    }

    private boolean isErrorPriorityHigher(JudgeInfoMessageEnum current, JudgeInfoMessageEnum newStatus) {
        return getErrorPriority(newStatus) > getErrorPriority(current);
    }

    private int getErrorPriority(JudgeInfoMessageEnum status) {
        switch (status) {
            case RUNTIME_ERROR: return 4;
            case TIME_LIMIT_EXCEEDED: return 3;
            case MEMORY_LIMIT_EXCEEDED: return 2;
            case WRONG_ANSWER: return 1;
            default: return 0;
        }
    }

    private ExecuteCodeResponse getErrorResponse(JudgeInfoMessageEnum status, String message) {
        ExecuteCodeResponse response = new ExecuteCodeResponse();
        response.setOutputList(new ArrayList<>());
        response.setMessage(message);
        response.setStatus(3);

        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMessage(status.getValue());
        judgeInfo.setTime(0L);
        judgeInfo.setMemory(0L);
        response.setJudgeInfo(judgeInfo);

        return response;
    }
}
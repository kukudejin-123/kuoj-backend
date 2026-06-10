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
public class Judge0CodeSandbox implements CodeSandbox {

    // Judge0 公共 API 地址
    private static final String DEFAULT_API_URL = "https://ce.judge0.com/submissions";

    private String apiUrl;

    // Judge0 语言 ID 映射
    // 完整列表: https://ce.judge0.com/languages
    private static final Map<String, Integer> LANGUAGE_ID_MAP = new HashMap<>();

    // Judge0 状态 ID 到判题消息的映射
    private static final Map<Integer, JudgeInfoMessageEnum> STATUS_ID_MAP = new HashMap<>();

    static {
        LANGUAGE_ID_MAP.put("java", 62);           // Java (OpenJDK 17.0.6)
        LANGUAGE_ID_MAP.put("python", 71);         // Python (3.11.4)
        LANGUAGE_ID_MAP.put("python3", 71);
        LANGUAGE_ID_MAP.put("cpp", 54);            // C++ (GCC 10.2.0)
        LANGUAGE_ID_MAP.put("c++", 54);
        LANGUAGE_ID_MAP.put("c", 50);              // C (GCC 10.2.0)
        LANGUAGE_ID_MAP.put("javascript", 63);    // JavaScript (Node.js 18.15.0)
        LANGUAGE_ID_MAP.put("js", 63);
        LANGUAGE_ID_MAP.put("go", 60);             // Go (1.19.5)
        LANGUAGE_ID_MAP.put("golang", 60);
        LANGUAGE_ID_MAP.put("csharp", 51);         // C# (Mono 6.12.0)
        LANGUAGE_ID_MAP.put("c#", 51);
        LANGUAGE_ID_MAP.put("php", 68);            // PHP (8.2.3)
        LANGUAGE_ID_MAP.put("ruby", 72);           // Ruby (3.2.0)
        LANGUAGE_ID_MAP.put("rust", 73);           // Rust (1.68.2)
        LANGUAGE_ID_MAP.put("typescript", 74);     // TypeScript (5.0.3)
        LANGUAGE_ID_MAP.put("ts", 74);
        LANGUAGE_ID_MAP.put("kotlin", 78);         // Kotlin (1.8.20)
        LANGUAGE_ID_MAP.put("scala", 81);          // Scala (3.2.2)
        LANGUAGE_ID_MAP.put("swift", 83);          // Swift (5.8.0)

        // Judge0 状态映射
        // 参考: https://github.com/judge0/judge0/blob/master/judge0-v1.13.0/errors.yml
        STATUS_ID_MAP.put(1, JudgeInfoMessageEnum.WAITING);           // In Queue
        STATUS_ID_MAP.put(2, JudgeInfoMessageEnum.WAITING);           // Processing
        STATUS_ID_MAP.put(3, JudgeInfoMessageEnum.ACCEPTED);          // Accepted
        STATUS_ID_MAP.put(4, JudgeInfoMessageEnum.WRONG_ANSWER);      // Wrong Answer
        STATUS_ID_MAP.put(5, JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED); // Time Limit Exceeded
        STATUS_ID_MAP.put(6, JudgeInfoMessageEnum.COMPILE_ERROR);     // Compilation Error
        STATUS_ID_MAP.put(7, JudgeInfoMessageEnum.RUNTIME_ERROR);     // Runtime Error (SIGSEGV)
        STATUS_ID_MAP.put(8, JudgeInfoMessageEnum.RUNTIME_ERROR);     // Runtime Error (other)
        STATUS_ID_MAP.put(9, JudgeInfoMessageEnum.RUNTIME_ERROR);     // Runtime Error (other)
        STATUS_ID_MAP.put(10, JudgeInfoMessageEnum.RUNTIME_ERROR);    // Internal Error
        STATUS_ID_MAP.put(11, JudgeInfoMessageEnum.COMPILE_ERROR);    // Exec Format Error
        STATUS_ID_MAP.put(12, JudgeInfoMessageEnum.RUNTIME_ERROR);    // Runtime Error
        STATUS_ID_MAP.put(13, JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED); // Time Limit Exceeded
        STATUS_ID_MAP.put(14, JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED); // Memory Limit Exceeded
        STATUS_ID_MAP.put(15, JudgeInfoMessageEnum.SYSTEM_ERROR);     // Output Limit Exceeded (当成系统错误处理)
        STATUS_ID_MAP.put(16, JudgeInfoMessageEnum.RUNTIME_ERROR);    // Runtime Error
        STATUS_ID_MAP.put(17, JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED); // Memory Limit Exceeded
        STATUS_ID_MAP.put(18, JudgeInfoMessageEnum.RUNTIME_ERROR);    // Runtime Error
        STATUS_ID_MAP.put(19, JudgeInfoMessageEnum.RUNTIME_ERROR);    // Runtime Error
        STATUS_ID_MAP.put(20, JudgeInfoMessageEnum.RUNTIME_ERROR);    // Runtime Error
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
        System.out.println("Judge0 代码沙箱执行中...");

        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        // 获取语言 ID
        Integer languageId = LANGUAGE_ID_MAP.get(language.toLowerCase());
        if (languageId == null) {
            return getErrorResponse(JudgeInfoMessageEnum.SYSTEM_ERROR, "不支持的语言: " + language);
        }

        // 执行所有测试用例
        List<String> outputList = new ArrayList<>();
        long totalTime = 0;
        long maxMemory = 0;
        JudgeInfoMessageEnum finalStatus = JudgeInfoMessageEnum.ACCEPTED;
        String errorMessage = null;

        for (String input : inputList) {
            try {
                // 构建 Judge0 请求（使用 base64 编码）
                JSONObject requestBody = new JSONObject();
                requestBody.set("source_code", Base64.encode(code.getBytes(StandardCharsets.UTF_8)));
                requestBody.set("language_id", languageId);
                requestBody.set("stdin", Base64.encode((input != null ? input : "").getBytes(StandardCharsets.UTF_8)));

                // 超时设置（秒）
                requestBody.set("cpu_time_limit", 5.0);
                requestBody.set("cpu_extra_time", 1.0);
                requestBody.set("wall_time_limit", 10.0);
                requestBody.set("memory_limit", 131072); // 128MB

                System.out.println("Judge0 请求: language_id=" + languageId);

                // 调用 Judge0 API（wait=true 表示同步等待结果，base64_encoded=true 表示使用 base64 编码）
                String responseStr = HttpUtil.createPost(apiUrl + "?wait=true&base64_encoded=true")
                        .header("Content-Type", "application/json")
                        .body(requestBody.toString())
                        .timeout(30000) // 30秒超时
                        .execute()
                        .body();

                System.out.println("Judge0 响应: " + responseStr);

                if (StringUtils.isBlank(responseStr)) {
                    return getErrorResponse(JudgeInfoMessageEnum.SYSTEM_ERROR, "Judge0 API 响应为空");
                }

                // 解析响应
                JSONObject response;
                try {
                    response = JSONUtil.parseObj(responseStr);
                } catch (Exception e) {
                    System.out.println("Judge0 响应解析失败: " + e.getMessage());
                    System.out.println("原始响应内容: " + responseStr);
                    return getErrorResponse(JudgeInfoMessageEnum.SYSTEM_ERROR, "Judge0 响应解析失败: " + e.getMessage());
                }

                // 获取状态
                JSONObject status = response.getJSONObject("status");
                Integer statusId = status != null ? status.getInt("id") : null;
                String statusDesc = status != null ? status.getStr("description") : "未知状态";
                System.out.println("Judge0 status 对象: " + status);
                System.out.println("Judge0 statusId: " + statusId);

                // 获取输出（需要 base64 解码）
                String stdout = response.getStr("stdout", "");
                String stderr = response.getStr("stderr", "");
                String compileOutput = response.getStr("compile_output", "");

                // base64 解码
                if (StringUtils.isNotBlank(stdout)) {
                    try {
                        stdout = new String(Base64.decode(stdout), StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        System.out.println("stdout 解码失败: " + e.getMessage());
                    }
                }
                if (StringUtils.isNotBlank(stderr)) {
                    try {
                        stderr = new String(Base64.decode(stderr), StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        System.out.println("stderr 解码失败: " + e.getMessage());
                    }
                }
                if (StringUtils.isNotBlank(compileOutput)) {
                    try {
                        compileOutput = new String(Base64.decode(compileOutput), StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        System.out.println("compileOutput 解码失败: " + e.getMessage());
                    }
                }

                // 获取执行时间和内存
                String timeStr = response.getStr("time", "0");
                String memoryStr = response.getStr("memory", "0");

                double timeSeconds = StringUtils.isNotBlank(timeStr) ? Double.parseDouble(timeStr) : 0;
                long memoryKB = StringUtils.isNotBlank(memoryStr) ? Long.parseLong(memoryStr) : 0;

                // 转换为毫秒
                long timeMs = (long) (timeSeconds * 1000);
                totalTime += timeMs;
                maxMemory = Math.max(maxMemory, memoryKB);

                // 根据 Judge0 状态 ID 判断结果
                if (statusId != null) {
                    JudgeInfoMessageEnum currentStatus = STATUS_ID_MAP.getOrDefault(statusId, JudgeInfoMessageEnum.SYSTEM_ERROR);
                    System.out.println("Judge0 状态 ID: " + statusId + ", 映射到: " + currentStatus.getValue());

                    // 如果是编译错误，直接返回
                    if (statusId == 6) { // Compilation Error
                        return getErrorResponse(JudgeInfoMessageEnum.COMPILE_ERROR,
                                "编译错误:\n" + (StringUtils.isNotBlank(compileOutput) ? compileOutput : stderr));
                    }

                    // 如果是错误状态（非 Accepted），记录但继续执行其他测试用例
                    if (statusId > 3) {
                        // 优先级：运行错误 > 时间超限 > 内存超限 > 答案错误
                        if (finalStatus == JudgeInfoMessageEnum.ACCEPTED ||
                            isErrorPriorityHigher(finalStatus, currentStatus)) {
                            finalStatus = currentStatus;
                            if (StringUtils.isNotBlank(stderr)) {
                                errorMessage = "运行错误:\n" + stderr;
                            } else {
                                errorMessage = currentStatus.getValue() + ": " + statusDesc;
                            }
                        }
                    }

                    outputList.add(stdout != null ? stdout.trim() : "");
                } else {
                    finalStatus = JudgeInfoMessageEnum.SYSTEM_ERROR;
                    errorMessage = "无法获取执行状态";
                    outputList.add("");
                }

            } catch (Exception e) {
                e.printStackTrace();
                return getErrorResponse(JudgeInfoMessageEnum.SYSTEM_ERROR, "Judge0 执行异常: " + e.getMessage());
            }
        }

        // 构建响应
        ExecuteCodeResponse response = new ExecuteCodeResponse();
        response.setOutputList(outputList);

        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(totalTime);
        judgeInfo.setMemory(maxMemory);
        judgeInfo.setMessage(finalStatus.getValue());

        response.setJudgeInfo(judgeInfo);
        response.setMessage(errorMessage);

        // 设置状态码：1=成功，3=失败
        response.setStatus(finalStatus == JudgeInfoMessageEnum.ACCEPTED ? 1 : 3);

        System.out.println("========== 最终判题结果 ==========");
        System.out.println("finalStatus: " + finalStatus.getValue());
        System.out.println("judgeInfo.message: " + judgeInfo.getMessage());
        System.out.println("response.status: " + response.getStatus());
        System.out.println("===================================");

        return response;
    }

    /**
     * 判断错误优先级
     * 运行错误 > 时间超限 > 内存超限 > 答案错误
     */
    private boolean isErrorPriorityHigher(JudgeInfoMessageEnum current, JudgeInfoMessageEnum newStatus) {
        int currentPriority = getErrorPriority(current);
        int newPriority = getErrorPriority(newStatus);
        return newPriority > currentPriority;
    }

    private int getErrorPriority(JudgeInfoMessageEnum status) {
        switch (status) {
            case RUNTIME_ERROR:
                return 4;
            case TIME_LIMIT_EXCEEDED:
                return 3;
            case MEMORY_LIMIT_EXCEEDED:
                return 2;
            case WRONG_ANSWER:
                return 1;
            default:
                return 0;
        }
    }

    /**
     * 获取错误响应
     */
    private ExecuteCodeResponse getErrorResponse(JudgeInfoMessageEnum status, String message) {
        ExecuteCodeResponse response = new ExecuteCodeResponse();
        response.setOutputList(new ArrayList<>());
        response.setMessage(message);
        response.setStatus(3); // 失败

        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMessage(status.getValue());
        judgeInfo.setTime(0L);
        judgeInfo.setMemory(0L);
        response.setJudgeInfo(judgeInfo);

        return response;
    }
}

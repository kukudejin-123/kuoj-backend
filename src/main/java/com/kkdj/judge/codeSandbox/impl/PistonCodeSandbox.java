package com.kkdj.judge.codeSandbox.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.kkdj.common.ErrorCode;
import com.kkdj.exception.BusinessException;
import com.kkdj.judge.codeSandbox.CodeSandbox;
import com.kkdj.judge.codeSandbox.model.ExecuteCodeRequest;
import com.kkdj.judge.codeSandbox.model.ExecuteCodeResponse;
import com.kkdj.judge.codeSandbox.model.JudgeInfo;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Piston 代码沙箱实现
 * 官方文档: https://github.com/engineer-man/piston
 * 公共API: https://emkc.org/api/v2/piston/execute
 */
public class PistonCodeSandbox implements CodeSandbox {

    // Piston API 地址
    private static final String DEFAULT_API_URL = "https://emkc.org/api/v2/piston/execute";

    private String apiUrl;

    // 语言版本映射（Piston 要求指定版本）
    private static final Map<String, String> LANGUAGE_VERSION_MAP = new HashMap<>();

    static {
        LANGUAGE_VERSION_MAP.put("java", "17.0.0");
        LANGUAGE_VERSION_MAP.put("python", "3.10.0");
        LANGUAGE_VERSION_MAP.put("cpp", "10.2.0");
        LANGUAGE_VERSION_MAP.put("c", "10.2.0");
        LANGUAGE_VERSION_MAP.put("javascript", "18.15.0");
        LANGUAGE_VERSION_MAP.put("js", "18.15.0");
        LANGUAGE_VERSION_MAP.put("go", "1.16.0");
        LANGUAGE_VERSION_MAP.put("csharp", "6.12.0");
        LANGUAGE_VERSION_MAP.put("php", "8.2.3");
        LANGUAGE_VERSION_MAP.put("ruby", "3.2.0");
        LANGUAGE_VERSION_MAP.put("rust", "1.68.2");
        LANGUAGE_VERSION_MAP.put("typescript", "5.0.0");
        LANGUAGE_VERSION_MAP.put("ts", "5.0.0");
    }

    public PistonCodeSandbox() {
        this.apiUrl = DEFAULT_API_URL;
    }

    public PistonCodeSandbox(String apiUrl) {
        this.apiUrl = StringUtils.isNotBlank(apiUrl) ? apiUrl : DEFAULT_API_URL;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        System.out.println("Piston 代码沙箱执行中...");

        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        // 获取语言版本
        String version = LANGUAGE_VERSION_MAP.get(language.toLowerCase());
        if (version == null) {
            return getErrorResponse("不支持的语言: " + language);
        }

        // 执行所有测试用例
        List<String> outputList = new ArrayList<>();
        long totalTime = 0;
        String errorMessage = null;

        for (String input : inputList) {
            try {
                // 构建 Piston 请求
                JSONObject requestBody = buildRequestBody(code, language.toLowerCase(), version, input);

                System.out.println("Piston 请求: " + requestBody.toString());

                // 调用 Piston API
                String responseStr = HttpUtil.createPost(apiUrl)
                        .header("Content-Type", "application/json")
                        .body(requestBody.toString())
                        .execute()
                        .body();

                System.out.println("Piston 响应: " + responseStr);

                if (StringUtils.isBlank(responseStr)) {
                    return getErrorResponse("Piston API 响应为空");
                }

                // 解析响应
                JSONObject response = JSONUtil.parseObj(responseStr);
                JSONObject runResult = response.getJSONObject("run");

                if (runResult == null) {
                    return getErrorResponse("Piston 响应格式错误: " + responseStr);
                }

                // 获取输出
                String stdout = runResult.getStr("stdout", "");
                String stderr = runResult.getStr("stderr", "");
                Integer exitCode = runResult.getInt("code", -1);

                // 记录错误信息
                if (exitCode != 0 && StringUtils.isNotBlank(stderr)) {
                    if (errorMessage == null) {
                        errorMessage = stderr;
                    }
                }

                outputList.add(stdout.trim());

                // 累计执行时间（Piston 返回的是毫秒）
                // 注意：Piston 不直接返回执行时间，这里使用估算值
                totalTime += estimateExecutionTime(stdout, stderr);

            } catch (Exception e) {
                e.printStackTrace();
                return getErrorResponse("Piston 执行异常: " + e.getMessage());
            }
        }

        // 构建响应
        ExecuteCodeResponse response = new ExecuteCodeResponse();
        response.setOutputList(outputList);

        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(totalTime);
        judgeInfo.setMemory(0L); // Piston 不返回内存信息

        if (errorMessage != null) {
            judgeInfo.setMessage("执行错误");
            response.setStatus(3); // 失败
            response.setMessage(errorMessage);
        } else {
            judgeInfo.setMessage("执行成功");
            response.setStatus(1); // 成功
        }

        response.setJudgeInfo(judgeInfo);
        return response;
    }

    /**
     * 构建 Piston 请求体
     */
    private JSONObject buildRequestBody(String code, String language, String version, String stdin) {
        JSONObject request = new JSONObject();
        request.set("language", normalizeLanguage(language));
        request.set("version", version);

        // 文件数组
        JSONArray files = new JSONArray();
        JSONObject file = new JSONObject();
        file.set("name", getFileName(language));
        file.set("content", code);
        files.add(file);
        request.set("files", files);

        // 标准输入
        if (StringUtils.isNotBlank(stdin)) {
            request.set("stdin", stdin);
        }

        // 编译超时和运行超时（毫秒）
        request.set("compile_timeout", 10000);
        request.set("run_timeout", 5000);

        return request;
    }

    /**
     * 规范化语言名称（Piston 对某些语言有特殊要求）
     */
    private String normalizeLanguage(String language) {
        switch (language.toLowerCase()) {
            case "c":
                return "c";
            case "cpp":
                return "c++";
            case "csharp":
                return "c#";
            case "js":
                return "javascript";
            case "ts":
                return "typescript";
            default:
                return language;
        }
    }

    /**
     * 获取文件名
     */
    private String getFileName(String language) {
        switch (language.toLowerCase()) {
            case "java":
                return "Main.java";
            case "python":
                return "main.py";
            case "c":
                return "main.c";
            case "cpp":
            case "c++":
                return "main.cpp";
            case "javascript":
            case "js":
                return "main.js";
            case "typescript":
            case "ts":
                return "main.ts";
            case "go":
                return "main.go";
            case "csharp":
            case "c#":
                return "main.cs";
            case "php":
                return "main.php";
            case "ruby":
                return "main.rb";
            case "rust":
                return "main.rs";
            default:
                return "main.txt";
        }
    }

    /**
     * 估算执行时间（Piston 不返回执行时间）
     */
    private long estimateExecutionTime(String stdout, String stderr) {
        // 简单估算：根据输出长度估算，实际项目中可以用更精确的方法
        long baseTime = 50; // 基础时间 50ms
        long outputTime = (stdout.length() + stderr.length()) / 100L; // 每百字符 1ms
        return baseTime + outputTime;
    }

    /**
     * 获取错误响应
     */
    private ExecuteCodeResponse getErrorResponse(String message) {
        ExecuteCodeResponse response = new ExecuteCodeResponse();
        response.setOutputList(new ArrayList<>());
        response.setMessage(message);
        response.setStatus(3); // 失败

        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMessage(message);
        judgeInfo.setTime(0L);
        judgeInfo.setMemory(0L);
        response.setJudgeInfo(judgeInfo);

        return response;
    }
}

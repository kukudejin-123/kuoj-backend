package com.kkdj.judge.codeSandbox.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.kkdj.judge.codeSandbox.CodeSandbox;
import com.kkdj.judge.codeSandbox.model.ExecuteCodeRequest;
import com.kkdj.judge.codeSandbox.model.ExecuteCodeResponse;
import com.kkdj.judge.codeSandbox.model.JudgeInfo;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class PistonCodeSandbox implements CodeSandbox {

    private static final String DEFAULT_API_URL = "https://emkc.org/api/v2/piston/execute";
    private String apiUrl;

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
        log.debug("Piston 代码沙箱执行中...");

        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        String version = LANGUAGE_VERSION_MAP.get(language.toLowerCase());
        if (version == null) {
            return getErrorResponse("不支持的语言: " + language);
        }

        List<String> outputList = new ArrayList<>();
        long totalTime = 0;
        String errorMessage = null;

        for (String input : inputList) {
            try {
                JSONObject requestBody = buildRequestBody(code, language.toLowerCase(), version, input);
                log.debug("Piston 请求: {}", requestBody.toString());

                String responseStr = HttpUtil.createPost(apiUrl)
                        .header("Content-Type", "application/json")
                        .body(requestBody.toString())
                        .execute()
                        .body();

                log.debug("Piston 响应: {}", responseStr);

                if (StringUtils.isBlank(responseStr)) {
                    return getErrorResponse("Piston API 响应为空");
                }

                JSONObject response = JSONUtil.parseObj(responseStr);
                JSONObject runResult = response.getJSONObject("run");

                if (runResult == null) {
                    return getErrorResponse("Piston 响应格式错误");
                }

                String stdout = runResult.getStr("stdout", "");
                String stderr = runResult.getStr("stderr", "");
                Integer exitCode = runResult.getInt("code", -1);

                if (exitCode != 0 && StringUtils.isNotBlank(stderr)) {
                    if (errorMessage == null) {
                        errorMessage = stderr;
                    }
                }

                outputList.add(stdout.trim());
                totalTime += estimateExecutionTime(stdout, stderr);

            } catch (Exception e) {
                log.error("Piston 执行异常", e);
                return getErrorResponse("Piston 执行异常: " + e.getMessage());
            }
        }

        ExecuteCodeResponse response = new ExecuteCodeResponse();
        response.setOutputList(outputList);

        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(totalTime);
        judgeInfo.setMemory(0L);

        if (errorMessage != null) {
            judgeInfo.setMessage("执行错误");
            response.setStatus(3);
            response.setMessage(errorMessage);
        } else {
            judgeInfo.setMessage("执行成功");
            response.setStatus(1);
        }

        response.setJudgeInfo(judgeInfo);
        return response;
    }

    private JSONObject buildRequestBody(String code, String language, String version, String stdin) {
        JSONObject request = new JSONObject();
        request.set("language", normalizeLanguage(language));
        request.set("version", version);

        JSONArray files = new JSONArray();
        JSONObject file = new JSONObject();
        file.set("name", getFileName(language));
        file.set("content", code);
        files.add(file);
        request.set("files", files);

        if (StringUtils.isNotBlank(stdin)) {
            request.set("stdin", stdin);
        }

        request.set("compile_timeout", 10000);
        request.set("run_timeout", 5000);

        return request;
    }

    private String normalizeLanguage(String language) {
        switch (language.toLowerCase()) {
            case "cpp": return "c++";
            case "csharp": return "c#";
            case "js": return "javascript";
            case "ts": return "typescript";
            default: return language;
        }
    }

    private String getFileName(String language) {
        switch (language.toLowerCase()) {
            case "java": return "Main.java";
            case "python": return "main.py";
            case "c": return "main.c";
            case "cpp": case "c++": return "main.cpp";
            case "javascript": case "js": return "main.js";
            case "typescript": case "ts": return "main.ts";
            case "go": return "main.go";
            case "csharp": case "c#": return "main.cs";
            case "php": return "main.php";
            case "ruby": return "main.rb";
            case "rust": return "main.rs";
            default: return "main.txt";
        }
    }

    private long estimateExecutionTime(String stdout, String stderr) {
        long baseTime = 50;
        long outputTime = (stdout.length() + stderr.length()) / 100L;
        return baseTime + outputTime;
    }

    private ExecuteCodeResponse getErrorResponse(String message) {
        ExecuteCodeResponse response = new ExecuteCodeResponse();
        response.setOutputList(new ArrayList<>());
        response.setMessage(message);
        response.setStatus(3);

        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMessage(message);
        judgeInfo.setTime(0L);
        judgeInfo.setMemory(0L);
        response.setJudgeInfo(judgeInfo);

        return response;
    }
}
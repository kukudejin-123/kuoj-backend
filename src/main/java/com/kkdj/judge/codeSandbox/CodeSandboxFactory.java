package com.kkdj.judge.codeSandbox;

import com.kkdj.judge.codeSandbox.impl.ExampleCodeSandbox;
import com.kkdj.judge.codeSandbox.impl.Judge0CodeSandbox;
import com.kkdj.judge.codeSandbox.impl.PistonCodeSandbox;
import com.kkdj.judge.codeSandbox.impl.RemoteCodeSandbox;
import com.kkdj.judge.codeSandbox.impl.ThirdPartyCodeSandbox;

/**
 * 代码沙箱工厂（根据字符串参数创建指定的代码沙箱实例）
 * 使用方法：静态工厂(可以根据字符串动态生成实例，提高通用性)*/
public class CodeSandboxFactory {

    // 默认远程沙箱URL
    private static final String DEFAULT_REMOTE_URL = "http://localhost:8090/executeCode";

    // Piston API 地址
    private static final String DEFAULT_PISTON_URL = "http://localhost:2000/api/v2/piston/execute";

    // Judge0 公共 API 地址
    private static final String DEFAULT_JUDGE0_URL = "https://ce.judge0.com/submissions";

    public static CodeSandbox newInstance(String type) {
        return newInstance(type, DEFAULT_REMOTE_URL, DEFAULT_PISTON_URL, DEFAULT_JUDGE0_URL);
    }

    public static CodeSandbox newInstance(String type, String remoteUrl) {
        return newInstance(type, remoteUrl, DEFAULT_PISTON_URL, DEFAULT_JUDGE0_URL);
    }

    public static CodeSandbox newInstance(String type, String remoteUrl, String pistonUrl) {
        return newInstance(type, remoteUrl, pistonUrl, DEFAULT_JUDGE0_URL);
    }

    public static CodeSandbox newInstance(String type, String remoteUrl, String pistonUrl, String judge0Url) {
        switch (type){
            case "example":
                return new ExampleCodeSandbox();
            case "remote":
                RemoteCodeSandbox remoteCodeSandbox = new RemoteCodeSandbox();
                remoteCodeSandbox.setUrl(remoteUrl);
                return remoteCodeSandbox;
            case "piston":
                PistonCodeSandbox pistonCodeSandbox = new PistonCodeSandbox();
                pistonCodeSandbox.setApiUrl(pistonUrl != null ? pistonUrl : DEFAULT_PISTON_URL);
                return pistonCodeSandbox;
            case "judge0":
                Judge0CodeSandbox judge0CodeSandbox = new Judge0CodeSandbox();
                judge0CodeSandbox.setApiUrl(judge0Url != null ? judge0Url : DEFAULT_JUDGE0_URL);
                return judge0CodeSandbox;
            case "thirdParty":
                return new ThirdPartyCodeSandbox();
            default:
                return new ExampleCodeSandbox();
        }
    }
}

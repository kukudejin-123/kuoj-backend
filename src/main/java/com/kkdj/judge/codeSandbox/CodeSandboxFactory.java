package com.kkdj.judge.codeSandbox;

import com.kkdj.judge.codeSandbox.impl.ExampleCodeSandbox;
import com.kkdj.judge.codeSandbox.impl.RemoteCodeSandbox;
import com.kkdj.judge.codeSandbox.impl.ThirdPartyCodeSandbox;

/**
 * 代码沙箱工厂（根据字符串参数创建指定的代码沙箱实例）
 * 使用方法：静态工厂(可以根据字符串动态生成实例，提高通用性)*/
public class CodeSandboxFactory {

    // 默认远程沙箱URL
    private static final String DEFAULT_REMOTE_URL = "http://localhost:8090/executeCode";

    public static CodeSandbox newInstance(String type) {
        return newInstance(type, DEFAULT_REMOTE_URL);
    }

    public static CodeSandbox newInstance(String type, String remoteUrl) {
        switch (type){
            case "example":
                return new ExampleCodeSandbox();
            case "remote":
                RemoteCodeSandbox remoteCodeSandbox = new RemoteCodeSandbox();
                remoteCodeSandbox.setUrl(remoteUrl);
                return remoteCodeSandbox;
            case "thirdParty":
                return new ThirdPartyCodeSandbox();
            default:
                return new ExampleCodeSandbox();
        }
    }
}

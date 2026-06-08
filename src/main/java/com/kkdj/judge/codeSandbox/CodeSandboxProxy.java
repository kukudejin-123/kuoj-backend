package com.kkdj.judge.codeSandbox;

import com.kkdj.judge.codeSandbox.model.ExecuteCodeRequest;
import com.kkdj.judge.codeSandbox.model.ExecuteCodeResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 代码沙箱代理：用于增强代码沙箱功能，把每次请求和响应信息打印*/
@Slf4j
public class CodeSandboxProxy implements CodeSandbox{

    private final CodeSandbox codeSandbox;

    public CodeSandboxProxy(CodeSandbox codeSandbox) {
        this.codeSandbox = codeSandbox;
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest excuteCodeRequest) {
        log.info("代码沙箱请求信息" + excuteCodeRequest.toString());
        ExecuteCodeResponse excuteCodeResponse = codeSandbox.executeCode(excuteCodeRequest);
        if (excuteCodeResponse != null) {
            log.info("代码沙箱响应信息" + excuteCodeResponse.toString());
        } else {
            log.warn("代码沙箱返回 null");
        }
        return excuteCodeResponse;
    }
}

package com.kkdj.judge.codeSandbox.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.kkdj.common.ErrorCode;
import com.kkdj.exception.BusinessException;
import com.kkdj.judge.codeSandbox.CodeSandbox;
import com.kkdj.judge.codeSandbox.model.ExecuteCodeRequest;
import com.kkdj.judge.codeSandbox.model.ExecuteCodeResponse;
import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 远程代码沙箱(业务使用)*/
@Component
public class RemoteCodeSandbox implements CodeSandbox {
    // 一般在公司内API服务使用
    private static final String AUTH_REQUEST_HEADER = "auth";
    private static final String AUTH_REQUEST_SECRET = "secretKey";

    // 默认值
    private static final String DEFAULT_URL = "http://localhost:8090/executeCode";

    @Value("${codesandbox.url:http://localhost:8090/executeCode}")
    private String url;

    // 用于工厂类创建实例时设置URL
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest excuteCodeRequest) {
        System.out.println("远程代码沙箱");
        String json = JSONUtil.toJsonStr(excuteCodeRequest);

        // 使用实际配置的URL，如果为空则使用默认值
        String actualUrl = StringUtils.isNotBlank(url) ? url : DEFAULT_URL;
        System.out.println("请求URL: " + actualUrl);

        String responseStr = HttpUtil.createPost(actualUrl)
                .header(AUTH_REQUEST_HEADER, AUTH_REQUEST_SECRET)
                .body(json)
                .execute()
                .body();
        if (StringUtils.isBlank(responseStr)){
            throw new BusinessException(ErrorCode.API_REQUEST_ERROR,"executeCode remoteSandbox error,message = " + responseStr);
        }
        return JSONUtil.toBean(responseStr, ExecuteCodeResponse.class);
    }
}

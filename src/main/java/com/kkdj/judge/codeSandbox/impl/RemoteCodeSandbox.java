package com.kkdj.judge.codeSandbox.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.kkdj.common.ErrorCode;
import com.kkdj.exception.BusinessException;
import com.kkdj.judge.codeSandbox.CodeSandbox;
import com.kkdj.judge.codeSandbox.model.ExecuteCodeRequest;
import com.kkdj.judge.codeSandbox.model.ExecuteCodeResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 远程代码沙箱(业务使用)
 */
@Slf4j
@Component
public class RemoteCodeSandbox implements CodeSandbox {
    private static final String AUTH_REQUEST_HEADER = "auth";
    private static final String AUTH_REQUEST_SECRET = "secretKey";
    private static final String DEFAULT_URL = "http://localhost:8090/executeCode";

    @Value("${codesandbox.url:http://localhost:8090/executeCode}")
    private String url;

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest excuteCodeRequest) {
        log.debug("远程代码沙箱执行");
        String json = JSONUtil.toJsonStr(excuteCodeRequest);
        String actualUrl = StringUtils.isNotBlank(url) ? url : DEFAULT_URL;
        log.debug("请求URL: {}", actualUrl);

        String responseStr = HttpUtil.createPost(actualUrl)
                .header(AUTH_REQUEST_HEADER, AUTH_REQUEST_SECRET)
                .body(json)
                .execute()
                .body();
        if (StringUtils.isBlank(responseStr)){
            throw new BusinessException(ErrorCode.API_REQUEST_ERROR,"executeCode remoteSandbox error, message = " + responseStr);
        }
        return JSONUtil.toBean(responseStr, ExecuteCodeResponse.class);
    }
}
package com.kkdj.judge.codeSandbox.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.kkdj.common.ErrorCode;
import com.kkdj.exception.BusinessException;
import com.kkdj.judge.codeSandbox.CodeSandbox;
import com.kkdj.judge.codeSandbox.model.ExecuteCodeRequest;
import com.kkdj.judge.codeSandbox.model.ExecuteCodeResponse;
import org.apache.commons.lang3.StringUtils;

/**
 * 远程代码沙箱(业务使用)*/
public class RemoteCodeSandbox implements CodeSandbox {
    // 一般在公司内API服务使用
    private static final String AUTH_REQUEST_HEADER = "auth";
    private static final String AUTH_REQUEST_SECRET = "secretKey";

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest excuteCodeRequest) {
        System.out.println("远程代码沙箱");
        // todo 记得修改沙箱地址，确定连接的是本地沙箱还是服务器上的沙箱
        String url = "http://xx.xx.xx.xx:xx/executeCode";
        String json = JSONUtil.toJsonStr(excuteCodeRequest);

        String responseStr = HttpUtil.createPost(url)
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

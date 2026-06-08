package com.kkdj.judge.codeSandbox;

import com.kkdj.judge.codeSandbox.model.ExecuteCodeRequest;
import com.kkdj.judge.codeSandbox.model.ExecuteCodeResponse;

/**
 * 代码沙箱接口*/

public interface CodeSandbox {

    /**
     * 执行代码
     * @param excuteCodeRequest
     * @return
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest excuteCodeRequest);
}

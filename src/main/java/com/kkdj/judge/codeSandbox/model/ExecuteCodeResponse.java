package com.kkdj.judge.codeSandbox.model;

import lombok.Data;

import java.util.List;
/**
 * 代码沙箱响应接口*/
@Data
public class ExecuteCodeResponse {
    /**
     * 输出结果
     */
    private List<String> outputList;
    /**
     * 接口信息
     */
    private String message;
    /**
     * 状态码（Integer类型，与代码沙箱服务保持一致）
     * 1 - 成功
     * 2 - 沙箱错误
     * 3 - 用户代码错误
     */
    private Integer status;
    /**
     * 判题信息
     */
    private JudgeInfo judgeInfo;
}

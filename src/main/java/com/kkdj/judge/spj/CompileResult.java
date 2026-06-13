package com.kkdj.judge.spj;

import lombok.Data;

/**
 * SPJ 编译结果
 */
@Data
public class CompileResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 退出码
     */
    private int exitCode;

    /**
     * 输出信息（错误信息）
     */
    private String output;

    /**
     * 编译时间（毫秒）
     */
    private long time;

    public CompileResult() {
    }

    public CompileResult(boolean success, int exitCode, String output) {
        this.success = success;
        this.exitCode = exitCode;
        this.output = output;
    }

    public CompileResult(boolean success, int exitCode, String output, long time) {
        this.success = success;
        this.exitCode = exitCode;
        this.output = output;
        this.time = time;
    }
}
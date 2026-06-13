package com.kkdj.judge.spj;

import lombok.Data;

/**
 * SPJ 执行结果
 */
@Data
public class SpjResult {

    /**
     * 退出码
     * 0 = Accepted
     * 1 = Wrong Answer
     * 2 = Presentation Error
     * 3 = System Error (SPJ 程序错误)
     */
    private int exitCode;

    /**
     * 输出信息
     */
    private String message;

    /**
     * 执行时间（毫秒）
     */
    private long time;

    /**
     * 是否超时
     */
    private boolean timeout;

    /**
     * 是否编译错误
     */
    private boolean compileError;

    /**
     * 编译错误信息
     */
    private String compileErrorInfo;

    public SpjResult() {
    }

    public SpjResult(int exitCode, String message, long time, boolean timeout) {
        this.exitCode = exitCode;
        this.message = message;
        this.time = time;
        this.timeout = timeout;
    }

    /**
     * 创建超时结果
     */
    public static SpjResult timeout() {
        return new SpjResult(1, "Time Limit Exceeded", 0, true);
    }

    /**
     * 创建编译错误结果
     */
    public static SpjResult compileError(String errorInfo) {
        SpjResult result = new SpjResult(3, "Compile Error", 0, false);
        result.setCompileError(true);
        result.setCompileErrorInfo(errorInfo);
        return result;
    }

    /**
     * 创建系统错误结果
     */
    public static SpjResult systemError(String message) {
        return new SpjResult(3, message, 0, false);
    }

    /**
     * 是否通过
     */
    public boolean isAccepted() {
        return exitCode == 0 && !timeout && !compileError;
    }

    /**
     * 是否答案错误
     */
    public boolean isWrongAnswer() {
        return exitCode == 1 && !timeout && !compileError;
    }

    /**
     * 是否格式错误
     */
    public boolean isPresentationError() {
        return exitCode == 2 && !timeout && !compileError;
    }
}
package com.kkdj.testlib;

import lombok.Getter;

/**
 * SPJ 判题结果枚举
 * 退出码对应关系：
 * 0 = OK (Accepted)
 * 1 = WA (Wrong Answer)
 * 2 = PE (Presentation Error)
 * 3 = FAIL (SPJ 程序错误)
 */
@Getter
public enum Verdict {

    OK(0, "Accepted"),
    WA(1, "Wrong Answer"),
    PE(2, "Presentation Error"),
    FAIL(3, "Fail");

    private final int exitCode;
    private final String message;

    Verdict(int exitCode, String message) {
        this.exitCode = exitCode;
        this.message = message;
    }

    /**
     * 根据退出码获取对应的 Verdict
     */
    public static Verdict fromExitCode(int exitCode) {
        for (Verdict verdict : values()) {
            if (verdict.getExitCode() == exitCode) {
                return verdict;
            }
        }
        return FAIL;
    }
}

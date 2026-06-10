package com.kkdj.model.dto.question;

import lombok.Data;

/**
 * 题目配置*/
@Data
public class JudgeConfig {

    /**
     * 时间限制(ms)
     */
    private long timeLimit;

    /**
     * 内存限制(kb)
     */
    private long memoryLimit;

    /**
     * 堆栈限制(kb)
     */
    private long stackLimit;

    /**
     * 输入模式
     * single: 单次输入（每个测试用例独立执行，默认）
     * loop: 循环输入（合并所有输入一次性执行，程序内部循环读取）
     */
    private String inputMode;
}

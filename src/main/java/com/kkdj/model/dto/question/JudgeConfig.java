package com.kkdj.model.dto.question;

import lombok.Data;

import java.util.List;

/**
 * 题目配置
 */
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
    private String inputMode = "single";

    /**
     * 判题模式
     * DEFAULT: 默认精确匹配
     * IGNORE_SPACE: 忽略多余空格
     * IGNORE_CASE: 忽略大小写
     * FLOAT: 浮点数精度比较
     * MULTI_ANSWER: 多解判断
     */
    private String judgeMode = "DEFAULT";

    /**
     * 浮点数精度容忍度（仅 FLOAT 模式有效）
     * 例如 1e-6 表示两个浮点数差的绝对值小于 0.000001 即认为相等
     */
    private Double floatPrecision = 1e-6;

    /**
     * 是否忽略行尾空格（仅 IGNORE_SPACE 模式有效）
     */
    private Boolean ignoreTrailingSpace = false;

    /**
     * 是否忽略大小写（仅 IGNORE_CASE 模式有效）
     */
    private Boolean ignoreCase = false;

    /**
     * 可接受的答案列表（仅 MULTI_ANSWER 模式有效）
     * 例如判断题：["YES", "Yes", "yes", "Y", "y"]
     */
    private List<String> acceptableOutputs;

    /**
     * SPJ 程序代码（仅 TESTLIB 模式有效）
     */
    private String spjCode;

    /**
     * SPJ 程序语言（仅 TESTLIB 模式有效），默认 java
     */
    private String spjLanguage;
}
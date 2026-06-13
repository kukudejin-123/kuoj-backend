package com.kkdj.judge.spj;

/**
 * SPJ 执行器接口
 */
public interface SpjExecutor {

    /**
     * 编译 SPJ 程序
     *
     * @param spjCode    SPJ 源代码
     * @param questionId 题目ID（用于缓存）
     * @return 编译结果
     */
    CompileResult compile(String spjCode, Long questionId);

    /**
     * 检查 SPJ 是否已编译（缓存是否存在）
     *
     * @param questionId 题目ID
     * @return 是否已编译
     */
    boolean isCompiled(Long questionId);

    /**
     * 执行 SPJ 程序
     *
     * @param questionId      题目ID
     * @param inputFile       输入文件路径
     * @param userOutputFile  用户输出文件路径
     * @param answerFile      标准答案文件路径
     * @param timeout         超时时间（秒）
     * @return SPJ 执行结果
     */
    SpjResult execute(Long questionId, String inputFile, String userOutputFile, String answerFile, int timeout);

    /**
     * 清理 SPJ 缓存
     *
     * @param questionId 题目ID
     */
    void cleanup(Long questionId);
}
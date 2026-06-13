package com.kkdj.testlib;

import java.io.Closeable;
import java.io.IOException;

/**
 * Testlib 主类
 * 用于 SPJ 程序初始化和结果输出
 *
 * 使用示例：
 * <pre>
 * import com.kkdj.testlib.Testlib;
 * import static com.kkdj.testlib.Testlib.*;
 *
 * public class SpjChecker extends Testlib {
 *     public static void main(String[] args) {
 *         init(args);
 *         int n = ouf.readInt();
 *         if (isPrime(n)) {
 *             ok("Correct answer");
 *         } else {
 *             wrongAnswer(n + " is not a prime number");
 *         }
 *     }
 * }
 * </pre>
 */
public class Testlib implements Closeable {

    /**
     * 输入流（测试输入）
     */
    protected static InStream inf;

    /**
     * 用户输出流
     */
    protected static InStream ouf;

    /**
     * 标准答案流
     */
    protected static InStream ans;

    /**
     * 程序名称
     */
    protected static String programName = "SpjChecker";

    /**
     * 结果输出模式
     */
    protected static boolean outputMode = false;

    /**
     * 初始化 Testlib
     * 参数顺序：args[0] = 输入文件, args[1] = 用户输出文件, args[2] = 标准答案文件
     *
     * @param args 命令行参数
     */
    public static void init(String[] args) {
        init(args, "SpjChecker");
    }

    /**
     * 初始化 Testlib（带程序名）
     *
     * @param args       命令行参数
     * @param name       程序名称
     */
    public static void init(String[] args, String name) {
        programName = name;

        if (args == null || args.length < 3) {
            quit(Verdict.FAIL, "参数不足，需要3个参数: input userOutput answer");
        }

        try {
            inf = new InStream(args[0]);
            ouf = new InStream(args[1]);
            ans = new InStream(args[2]);
        } catch (TestlibException e) {
            quit(Verdict.FAIL, "无法打开文件: " + e.getMessage());
        }
    }

    /**
     * 输出 Accepted 结果并退出
     *
     * @param message 消息
     */
    public static void ok(String message) {
        quit(Verdict.OK, message);
    }

    /**
     * 输出 Accepted 结果并退出（无消息）
     */
    public static void ok() {
        ok("OK");
    }

    /**
     * 输出 Wrong Answer 结果并退出
     *
     * @param message 错误消息
     */
    public static void wrongAnswer(String message) {
        quit(Verdict.WA, message);
    }

    /**
     * 输出 Presentation Error 结果并退出
     *
     * @param message 错误消息
     */
    public static void presentationError(String message) {
        quit(Verdict.PE, message);
    }

    /**
     * 输出 Fail 结果并退出
     *
     * @param message 错误消息
     */
    public static void fail(String message) {
        quit(Verdict.FAIL, message);
    }

    /**
     * 输出结果并退出
     *
     * @param verdict 判题结果
     * @param message 消息
     */
    public static void quit(Verdict verdict, String message) {
        // 输出格式: [程序名]: [结果]
        System.out.println(verdict.getMessage() + ": " + message);
        System.exit(verdict.getExitCode());
    }

    /**
     * 启用输出模式
     */
    public static void enableOutputMode() {
        outputMode = true;
    }

    /**
     * 禁用输出模式
     */
    public static void disableOutputMode() {
        outputMode = false;
    }

    /**
     * 获取输入流
     */
    public static InStream inf() {
        checkInitialized();
        return inf;
    }

    /**
     * 获取用户输出流
     */
    public static InStream ouf() {
        checkInitialized();
        return ouf;
    }

    /**
     * 获取标准答案流
     */
    public static InStream ans() {
        checkInitialized();
        return ans;
    }

    /**
     * 检查是否已初始化
     */
    private static void checkInitialized() {
        if (inf == null || ouf == null || ans == null) {
            throw new TestlibException("Testlib 未初始化，请先调用 init(args)");
        }
    }

    @Override
    public void close() throws IOException {
        if (inf != null) {
            inf.close();
        }
        if (ouf != null) {
            ouf.close();
        }
        if (ans != null) {
            ans.close();
        }
    }
}

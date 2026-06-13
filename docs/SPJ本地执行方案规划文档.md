# OJ 系统 SPJ 本地执行方案规划文档

## 一、背景与目标

### 1.1 背景
当前 OJ 系统的多答案判题策略（`MultiAnswerJudgeStrategyImpl`）需要出题人预先上传所有可能的正确答案，存在以下问题：
- 无法穷举所有正确答案（如输出任意一个质数）
- 灵活性不足，无法实现复杂判题逻辑

### 1.2 目标
实现 SPJ（Special Judge）本地编译执行方案，支持出题人上传自定义判题程序，实现灵活的判题逻辑。

### 1.3 约束条件
- 服务器配置：2 核 CPU / 4GB 内存 / 6Mbps 带宽
- 使用 Judge0 公共 API 执行用户代码
- SPJ 程序在本地服务器执行

---

## 二、技术方案

### 2.1 本地开发测试方案

#### 2.1.1 本地开发前提条件

在本地开发环境实现 SPJ 执行，需要以下条件：

| 条件 | 说明 | 检查方式 |
|-----|------|---------|
| JDK 环境 | 后端本身是 Java，本地已有 JDK | `javac -version` 和 `java -version` |
| 工作目录 | 创建临时目录存放 SPJ 文件 | Windows: `C:\temp\oj_spj\`，Linux/Mac: `/tmp/oj_spj/` |

#### 2.1.2 本地开发不需要的东西

| 不需要 | 原因 |
|-------|------|
| Docker | 本地开发直接用 ProcessBuilder 即可，后续上线可升级 |
| Judge0 | 先测试 SPJ 执行逻辑，后续再对接 Judge0 |
| 服务器 | 本地开发环境就能跑 |

#### 2.1.3 本地测试完整示例

创建一个测试类，模拟完整的 SPJ 执行流程：

```java
package com.kkdj.judge.spj.test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * SPJ 本地执行测试类
 * 用于在本地开发环境测试 SPJ 编译和执行逻辑
 */
public class LocalSpjTest {

    // 工作目录（根据操作系统调整）
    private static final String WORK_DIR = "C:\\temp\\oj_spj\\test";

    public static void main(String[] args) throws Exception {
        // 测试场景：用户输出一个质数
        testSpjExecution();
    }

    /**
     * 测试 SPJ 执行流程
     */
    public static void testSpjExecution() throws Exception {
        System.out.println("========== SPJ 本地执行测试开始 ==========");

        // 1. 准备测试数据
        String input = "10";           // 测试输入：找出小于10的质数
        String userOutput1 = "7";      // 用户输出1：7是质数，应该Accepted
        String userOutput2 = "8";      // 用户输出2：8不是质数，应该Wrong Answer
        String answer = "";            // 标准答案（SPJ场景可能不需要）

        // 2. 创建工作目录
        Files.createDirectories(Paths.get(WORK_DIR));
        System.out.println("工作目录: " + WORK_DIR);

        // 3. 写入测试输入和标准答案文件
        Files.writeString(Paths.get(WORK_DIR, "input.txt"), input);
        Files.writeString(Paths.get(WORK_DIR, "answer.txt"), answer);
        System.out.println("测试文件已准备");

        // 4. 写入 SPJ 源代码（判断输出是否是质数）
        String spjCode = """
            import java.io.*;

            public class SpjChecker {
                public static void main(String[] args) throws Exception {
                    // args[0] = 输入文件, args[1] = 用户输出文件, args[2] = 标准答案文件
                    String userOutputFile = args[1];

                    // 读取用户输出
                    BufferedReader reader = new BufferedReader(new FileReader(userOutputFile));
                    String line = reader.readLine();
                    if (line == null || line.trim().isEmpty()) {
                        System.out.println("WA: 用户输出为空");
                        System.exit(1);
                    }

                    int n = Integer.parseInt(line.trim());

                    // 判断是否是质数
                    if (isPrime(n)) {
                        System.out.println("OK: " + n + " 是质数");
                        System.exit(0);  // 正确
                    } else {
                        System.out.println("WA: " + n + " 不是质数");
                        System.exit(1);  // 错误
                    }
                }

                static boolean isPrime(int n) {
                    if (n < 2) return false;
                    if (n == 2) return true;
                    for (int i = 2; i * i <= n; i++) {
                        if (n % i == 0) return false;
                    }
                    return true;
                }
            }
            """;

        Files.writeString(Paths.get(WORK_DIR, "SpjChecker.java"), spjCode);
        System.out.println("SPJ 源代码已写入");

        // 5. 编译 SPJ 程序
        System.out.println("\n---------- 编译 SPJ ----------");
        ProcessBuilder compilePb = new ProcessBuilder(
            "javac",
            "SpjChecker.java"
        );
        compilePb.directory(new File(WORK_DIR));
        compilePb.redirectErrorStream(true);

        Process compileProcess = compilePb.start();
        boolean compileFinished = compileProcess.waitFor(30, TimeUnit.SECONDS);

        if (!compileFinished) {
            compileProcess.destroyForcibly();
            System.out.println("编译超时！");
            return;
        }

        String compileOutput = readOutput(compileProcess);
        if (compileProcess.exitValue() != 0) {
            System.out.println("编译失败: " + compileOutput);
            return;
        }
        System.out.println("编译成功！生成 SpjChecker.class");

        // 6. 测试用例1：用户输出 7（正确）
        System.out.println("\n---------- 测试用例1: 输出 7 ----------");
        Files.writeString(Paths.get(WORK_DIR, "userOutput.txt"), userOutput1);
        SpjResult result1 = executeSpj();
        printResult(result1);

        // 7. 测试用例2：用户输出 8（错误）
        System.out.println("\n---------- 测试用例2: 输出 8 ----------");
        Files.writeString(Paths.get(WORK_DIR, "userOutput.txt"), userOutput2);
        SpjResult result2 = executeSpj();
        printResult(result2);

        // 8. 清理临时文件
        System.out.println("\n---------- 清理临时文件 ----------");
        cleanup();
        System.out.println("临时文件已清理");

        System.out.println("\n========== SPJ 本地执行测试结束 ==========");
    }

    /**
     * 执行 SPJ 程序
     */
    public static SpjResult executeSpj() throws Exception {
        ProcessBuilder runPb = new ProcessBuilder(
            "java",
            "-Xmx256m",              // 限制内存
            "SpjChecker",
            "input.txt",             // args[0]: 输入文件
            "userOutput.txt",        // args[1]: 用户输出文件
            "answer.txt"             // args[2]: 标准答案文件
        );
        runPb.directory(new File(WORK_DIR));
        runPb.redirectErrorStream(true);

        long startTime = System.currentTimeMillis();
        Process runProcess = runPb.start();

        // 等待执行，最多10秒
        boolean finished = runProcess.waitFor(10, TimeUnit.SECONDS);
        long executeTime = System.currentTimeMillis() - startTime;

        if (!finished) {
            runProcess.destroyForcibly();
            return new SpjResult(1, "执行超时", executeTime, true);
        }

        int exitCode = runProcess.exitValue();
        String output = readOutput(runProcess);

        return new SpjResult(exitCode, output, executeTime, false);
    }

    /**
     * 读取进程输出
     */
    public static String readOutput(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream())
        );
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * 打印执行结果
     */
    public static void printResult(SpjResult result) {
        System.out.println("Exit Code: " + result.exitCode);
        System.out.println("执行时间: " + result.executeTime + "ms");
        System.out.println("输出信息: " + result.message);

        String verdict;
        switch (result.exitCode) {
            case 0: verdict = "Accepted"; break;
            case 1: verdict = "Wrong Answer"; break;
            case 2: verdict = "Presentation Error"; break;
            default: verdict = "System Error"; break;
        }
        System.out.println("判题结果: " + verdict);
    }

    /**
     * 清理临时文件
     */
    public static void cleanup() throws IOException {
        Files.deleteIfExists(Paths.get(WORK_DIR, "input.txt"));
        Files.deleteIfExists(Paths.get(WORK_DIR, "userOutput.txt"));
        Files.deleteIfExists(Paths.get(WORK_DIR, "answer.txt"));
        Files.deleteIfExists(Paths.get(WORK_DIR, "SpjChecker.java"));
        Files.deleteIfExists(Paths.get(WORK_DIR, "SpjChecker.class"));
    }

    /**
     * SPJ 执行结果类
     */
    static class SpjResult {
        int exitCode;       // 退出码：0=正确，1=错误，2=格式错误
        String message;     // 输出信息
        long executeTime;   // 执行时间(ms)
        boolean timeout;    // 是否超时

        public SpjResult(int exitCode, String message, long executeTime, boolean timeout) {
            this.exitCode = exitCode;
            this.message = message;
            this.executeTime = executeTime;
            this.timeout = timeout;
        }
    }
}
```

#### 2.1.4 本地测试执行流程图

```
┌─────────────────────────────────────────────────────────────┐
│                    本地开发环境                               │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. 创建工作目录                                              │
│     C:\temp\oj_spj\test                                      │
│                                                              │
│  2. 准备测试数据                                              │
│     - input.txt      (测试输入)                              │
│     - userOutput.txt (用户输出)                              │
│     - answer.txt     (标准答案)                              │
│                                                              │
│  3. 写入 SPJ 源代码                                           │
│     - SpjChecker.java                                        │
│                                                              │
│  4. javac 编译 SPJ                                           │
│     javac SpjChecker.java                                    │
│     → SpjChecker.class                                       │
│                                                              │
│  5. java 执行 SPJ                                            │
│     java -Xmx256m SpjChecker input.txt userOutput.txt answer.txt│
│                                                              │
│  6. 获取执行结果                                              │
│     - exitCode = 0 → Accepted                                │
│     - exitCode = 1 → Wrong Answer                            │
│                                                              │
│  7. 清理临时文件                                              │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

#### 2.1.5 测试结果预期

| 测试用例 | 用户输出 | 预期 exitCode | 预期结果 |
|---------|---------|--------------|---------|
| 测试1 | 7 | 0 | Accepted（7是质数） |
| 测试2 | 8 | 1 | Wrong Answer（8不是质数） |
| 测试3 | 2 | 0 | Accepted（2是质数） |
| 测试4 | 1 | 1 | Wrong Answer（1不是质数） |

---

### 2.3 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                         前端                                      │
│  - 题目管理页面增加 SPJ 代码编辑器                                  │
│  - 判题模式增加 TESTLIB 选项                                       │
└─────────────────────────────────────────────────────────────────┘
                                ↓
┌─────────────────────────────────────────────────────────────────┐
│                         后端                                      │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐          │
│  │ 题目管理     │    │ 判题服务     │    │ SPJ 执行器   │          │
│  │ - 保存SPJ   │ →  │ - 调用Judge0│ →  │ - 编译SPJ   │          │
│  │ - 预编译SPJ │    │ - 调用SPJ   │    │ - 执行SPJ   │          │
│  └─────────────┘    └─────────────┘    └─────────────┘          │
└─────────────────────────────────────────────────────────────────┘
                                ↓
┌─────────────────────────────────────────────────────────────────┐
│                       存储层                                      │
│  - 数据库：Question 表增加 spj_code、spj_language 字段            │
│  - 文件系统：SPJ 编译缓存目录                                      │
└─────────────────────────────────────────────────────────────────┘
```

### 2.4 SPJ 执行原理详解

#### 2.4.1 核心概念

SPJ（Special Judge）是一种自定义判题方式，允许出题人上传判题程序来验证用户输出是否正确。

**执行方式：使用 Java ProcessBuilder 在服务器本地启动新进程执行 SPJ 程序**

#### 2.4.2 为什么用 ProcessBuilder

| 方案 | 说明 |
|-----|------|
| ProcessBuilder | Java 内置 API，可以在后端代码中启动新进程执行外部命令 |
| Docker | 更安全但需要额外安装 Docker，服务器配置紧张 |
| JNI | 复杂度高，不适合 |

**结论：先用 ProcessBuilder，后续可升级为 Docker**

#### 2.4.3 ProcessBuilder 执行原理

```
┌─────────────────────────────────────────┐
│  你的 Java 后端代码                       │
│                                          │
│  ProcessBuilder pb = new ProcessBuilder( │
│      "java",                             │
│      "-cp", "testlib.jar:.",            │
│      "SpjChecker",                       │
│      "input.txt",                        │
│      "userOutput.txt",                   │
│      "answer.txt"                        │
│  );                                      │
│                                          │
│  Process process = pb.start();           │
│  process.waitFor(10, TimeUnit.SECONDS);  │
│                                          │
│  int exitCode = process.exitValue();     │
└─────────────────────────────────────────┘
                  ↓
        启动一个独立的 Java 进程
                  ↓
┌─────────────────────────────────────────┐
│  SPJ 进程（独立运行）                     │
│                                          │
│  java SpjChecker input.txt output.txt... │
│                                          │
│  - 读取文件                              │
│  - 执行判题逻辑                          │
│  - 返回 exitCode                        │
│    0 = 正确                              │
│    1 = 错误                              │
│    2 = 格式错误                          │
└─────────────────────────────────────────┘
                  ↓
        后端获取 exitCode，判断结果
```

#### 2.4.4 执行流程详解

**Step 1: 准备临时文件**

每次判题前，将数据写入临时文件：

```java
// 写入测试输入（从测试用例获取）
File inputFile = writeTempFile(judgeCase.getInput());

// 写入用户输出（从 Judge0 执行结果获取）
File userOutputFile = writeTempFile(userOutput);

// 写入标准答案（从测试用例获取）
File answerFile = writeTempFile(judgeCase.getOutput());

// 临时文件路径示例：
// /tmp/oj_spj/123/input_1.txt
// /tmp/oj_spj/123/userOutput_1.txt
// /tmp/oj_spj/123/answer_1.txt
```

**Step 2: 编译 SPJ 程序（首次或缓存过期时）**

```java
// 出题时或第一次判题时编译
public void compileSpj(String spjCode, Long questionId) {
    // 1. 创建工作目录
    Path workDir = Paths.get("/tmp/oj_spj/", questionId.toString());
    Files.createDirectories(workDir);

    // 2. 写入 SPJ 源代码
    Path sourceFile = workDir.resolve("SpjChecker.java");
    Files.writeString(sourceFile, spjCode);

    // 3. 执行 javac 编译
    ProcessBuilder pb = new ProcessBuilder(
        "javac",
        "-cp", "/opt/oj/testlib.jar",  // testlib.jar 路径
        sourceFile.toString()
    );
    pb.directory(workDir.toFile());
    pb.redirectErrorStream(true);

    Process process = pb.start();
    boolean finished = process.waitFor(30, TimeUnit.SECONDS);

    if (!finished) {
        process.destroyForcibly();
        throw new CompileException("编译超时");
    }

    if (process.exitValue() != 0) {
        String error = readOutput(process);
        throw new CompileException("编译失败: " + error);
    }

    // 4. 编译成功，生成 SpjChecker.class 文件
    // 路径：/tmp/oj_spj/123/SpjChecker.class
}
```

**Step 3: 执行 SPJ 程序**

```java
public SpjResult executeSpj(Long questionId,
                             String inputFile,
                             String userOutputFile,
                             String answerFile) {
    Path workDir = Paths.get("/tmp/oj_spj/", questionId.toString());
    String testlibJar = "/opt/oj/testlib.jar";

    // 构建 Java 执行命令
    ProcessBuilder pb = new ProcessBuilder(
        "java",
        "-Xmx256m",                    // 限制最大内存 256MB
        "-cp", workDir + ":" + testlibJar,  // 设置 classpath
        "SpjChecker",                  // SPJ 主类名
        inputFile,                     // args[0]: 输入文件
        userOutputFile,                // args[1]: 用户输出文件
        answerFile                     // args[2]: 标准答案文件
    );

    pb.directory(workDir.toFile());    // 设置工作目录
    pb.redirectErrorStream(true);      // 合并 stdout 和 stderr

    Process process = pb.start();

    // 等待执行完成，最多 10 秒
    boolean finished = process.waitFor(10, TimeUnit.SECONDS);

    if (!finished) {
        process.destroyForcibly();     // 超时强制终止
        return SpjResult.timeout();
    }

    // 获取执行结果
    int exitCode = process.exitValue();
    String output = readOutput(process);

    return new SpjResult(exitCode, output);
}
```

**Step 4: 解析执行结果**

```java
public JudgeInfo parseResult(SpjResult result) {
    JudgeInfo judgeInfo = new JudgeInfo();

    switch (result.getExitCode()) {
        case 0:  // OK
            judgeInfo.setMessage("Accepted");
            break;
        case 1:  // Wrong Answer
            judgeInfo.setMessage("Wrong Answer");
            judgeInfo.setDetail(result.getMessage());
            break;
        case 2:  // Presentation Error
            judgeInfo.setMessage("Presentation Error");
            judgeInfo.setDetail(result.getMessage());
            break;
        case 3:  // Fail (SPJ 程序错误)
            judgeInfo.setMessage("System Error");
            judgeInfo.setDetail("SPJ 程序错误: " + result.getMessage());
            break;
        default:
            judgeInfo.setMessage("System Error");
            break;
    }

    return judgeInfo;
}
```

#### 2.4.5 SPJ 程序收到的参数

```bash
java SpjChecker input.txt userOutput.txt answer.txt
```

| 参数位置 | 参数内容 | 说明 |
|---------|---------|------|
| args[0] | input.txt | 测试输入数据 |
| args[1] | userOutput.txt | 用户程序输出（Judge0 返回） |
| args[2] | answer.txt | 标准答案 |

#### 2.4.6 服务器环境要求

| 环境 | 要求 | 说明 |
|-----|------|------|
| JDK | 已安装 | 后端本身就是 Java，服务器已有 JDK |
| testlib.jar | 需部署 | 编译打包 testlib 模块，放到 `/opt/oj/testlib.jar` |
| 工作目录 | `/tmp/oj_spj/` | SPJ 编译和执行的临时目录 |
| javac 命令 | 可执行 | 用于编译 SPJ 源代码 |
| java 命令 | 可执行 | 用于运行 SPJ 程序 |

#### 2.4.7 临时文件管理

```java
// 判题完成后清理临时文件
public void cleanupTempFiles(Long questionId, int testCaseIndex) {
    Path tempDir = Paths.get("/tmp/oj_spj/", questionId.toString());

    // 删除本次判题的临时文件
    Files.deleteIfExists(tempDir.resolve("input_" + testCaseIndex + ".txt"));
    Files.deleteIfExists(tempDir.resolve("userOutput_" + testCaseIndex + ".txt"));
    Files.deleteIfExists(tempDir.resolve("answer_" + testCaseIndex + ".txt"));
}

// 定期清理过期缓存（每天凌晨执行）
@Scheduled(cron = "0 0 3 * * ?")
public void cleanupExpiredCache() {
    Path spjDir = Paths.get("/tmp/oj_spj/");
    long expireTime = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000;  // 7天

    Files.list(spjDir)
        .filter(dir -> Files.getLastModifiedTime(dir).toMillis() < expireTime)
        .forEach(this::deleteDirectory);
}
```

---

### 2.5 完整判题流程

#### 出题阶段
```
出题人 → 填写题目信息 → 选择 TESTLIB 模式 → 编写 SPJ 代码 → 保存
                                                              ↓
                                                    后端保存 SPJ 代码到数据库
                                                              ↓
                                                    可选：预编译 SPJ 并缓存
```

#### 判题阶段
```
用户提交代码
    ↓
┌─────────────────────────────────────┐
│ Step 1: Judge0 执行用户代码          │
│ - 发送用户代码 + 测试输入到 Judge0    │
│ - 获取用户程序输出                    │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ Step 2: 准备临时文件                 │
│ - 写入 input.txt（测试输入）         │
│ - 写入 userOutput.txt（用户输出）    │
│ - 写入 answer.txt（标准答案）        │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ Step 3: 编译 SPJ（如需要）           │
│ - 检查缓存是否存在 .class 文件       │
│ - 不存在则用 javac 编译              │
│ - 缓存编译结果                       │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ Step 4: ProcessBuilder 执行 SPJ     │
│ - 启动新进程                         │
│ - 执行: java SpjChecker <args>      │
│ - 等待结果（最多10秒）               │
│ - 获取 exitCode 和输出信息           │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ Step 5: 解析结果并返回               │
│ - exitCode=0 → Accepted             │
│ - exitCode=1 → Wrong Answer         │
│ - exitCode=2 → Presentation Error   │
│ - 超时 → Time Limit Exceeded        │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ Step 6: 清理临时文件                 │
│ - 删除本次判题的临时文件              │
└─────────────────────────────────────┘
```

---

## 三、详细设计

### 3.1 Java testlib 核心库设计

#### 3.1.1 核心类结构

```
com.kkdj.testlib/
├── Testlib.java              # 主类，初始化和结果输出
├── InStream.java             # 输入流解析器
├── Verdict.java              # 结果枚举
├── CheckerUtils.java         # 判题工具类
└── TestlibException.java     # 异常类
```

#### 3.1.2 核心 API

```java
// Verdict.java - 结果枚举
public enum Verdict {
    OK(0, "Accepted"),           // 正确
    WA(1, "Wrong Answer"),       // 答案错误
    PE(2, "Presentation Error"), // 格式错误
    FAIL(3, "Fail");             // SPJ 程序错误

    private final int exitCode;
    private final String message;
}

// InStream.java - 输入流解析器
public class InStream {
    public int readInt();           // 读取整数
    public long readLong();         // 读取长整数
    public double readDouble();     // 读取浮点数
    public String readLine();       // 读取一行
    public String readWord();       // 读取一个单词
    public boolean isEof();         // 是否到达文件末尾
}

// Testlib.java - 主类
public class Testlib {
    protected static InStream inf;   // 测试输入
    protected static InStream ouf;   // 用户输出
    protected static InStream ans;   // 标准答案

    // 初始化
    public static void init(String[] args);

    // 结果输出
    public static void ok(String message);
    public static void wrongAnswer(String message);
    public static void presentationError(String message);
    public static void quit(Verdict verdict, String message);
}

// CheckerUtils.java - 工具类
public class CheckerUtils {
    public static boolean doubleEquals(double a, double b, double eps);
    public static boolean stringEquals(String a, String b, boolean ignoreCase);
    public static boolean isPrime(long n);
}
```

#### 3.1.3 SPJ 程序示例

```java
import com.kkdj.testlib.Testlib;
import com.kkdj.testlib.CheckerUtils;

public class SpjChecker extends Testlib {
    public static void main(String[] args) {
        init(args);  // 初始化，自动读取三个文件

        // 读取用户输出
        int userAns = ouf.readInt();

        // 验证是否是质数
        if (CheckerUtils.isPrime(userAns)) {
            ok("Answer is a prime number");
        } else {
            wrongAnswer(userAns + " is not a prime number");
        }
    }
}
```

### 3.2 数据库设计

#### 3.2.1 表结构变更

```sql
-- 在 question 表增加 SPJ 相关字段
ALTER TABLE question
ADD COLUMN spj_code TEXT COMMENT 'SPJ判题程序代码',
ADD COLUMN spj_language VARCHAR(20) DEFAULT 'java' COMMENT 'SPJ程序语言',
ADD COLUMN spj_compiled TINYINT(1) DEFAULT 0 COMMENT 'SPJ是否已预编译';
```

#### 3.2.2 JudgeConfig 扩展

```java
// JudgeConfig.java 新增字段
public class JudgeConfig {
    // ... 现有字段 ...

    // SPJ 相关配置（仅 TESTLIB 模式有效）
    private String spjCode;        // SPJ 代码
    private String spjLanguage;    // SPJ 语言，默认 java
}
```

### 3.3 后端服务设计

#### 3.3.1 新增类

```
com.kkdj.judge.spj/
├── SpjExecutor.java          # SPJ 执行器接口
├── JavaSpjExecutor.java      # Java SPJ 执行器实现
├── SpjCompiler.java          # SPJ 编译器
├── SpjCacheManager.java      # SPJ 缓存管理
└── SpjResult.java            # SPJ 执行结果
```

#### 3.3.2 SpjExecutor 接口设计

```java
public interface SpjExecutor {
    /**
     * 编译 SPJ 程序
     * @param spjCode SPJ 源代码
     * @param questionId 题目ID（用于缓存）
     * @return 编译结果
     */
    CompileResult compile(String spjCode, Long questionId);

    /**
     * 执行 SPJ 程序
     * @param questionId 题目ID
     * @param inputFile 输入文件路径
     * @param userOutputFile 用户输出文件路径
     * @param answerFile 标准答案文件路径
     * @param timeout 超时时间（秒）
     * @return SPJ 执行结果
     */
    SpjResult execute(Long questionId, String inputFile,
                      String userOutputFile, String answerFile, int timeout);
}

public class SpjResult {
    private int exitCode;        // 退出码：0=正确，1=错误，2=格式错误
    private String message;      // 输出信息
    private long time;           // 执行时间（ms）
    private boolean timeout;     // 是否超时
}
```

#### 3.3.3 JavaSpjExecutor 核心实现

```java
public class JavaSpjExecutor implements SpjExecutor {

    private static final String SPJ_WORK_DIR = "/tmp/oj_spj/";
    private static final int DEFAULT_TIMEOUT = 10; // 10秒超时

    @Override
    public CompileResult compile(String spjCode, Long questionId) {
        // 1. 创建工作目录
        Path workDir = Paths.get(SPJ_WORK_DIR, questionId.toString());
        Files.createDirectories(workDir);

        // 2. 写入源代码
        Path sourceFile = workDir.resolve("SpjChecker.java");
        Files.writeString(sourceFile, spjCode);

        // 3. 编译（添加 testlib.jar 到 classpath）
        ProcessBuilder pb = new ProcessBuilder(
            "javac", "-cp", getTestlibJarPath(),
            sourceFile.toString()
        );
        pb.directory(workDir.toFile());
        Process process = pb.start();
        process.waitFor(30, TimeUnit.SECONDS);

        // 4. 检查编译结果
        Path classFile = workDir.resolve("SpjChecker.class");
        return new CompileResult(
            classFile.exists(),
            process.exitValue(),
            readOutput(process)
        );
    }

    @Override
    public SpjResult execute(Long questionId, String inputFile,
                             String userOutputFile, String answerFile, int timeout) {
        Path workDir = Paths.get(SPJ_WORK_DIR, questionId.toString());

        // 执行 SPJ 程序
        ProcessBuilder pb = new ProcessBuilder(
            "java", "-cp", workDir + ":" + getTestlibJarPath(),
            "SpjChecker",
            inputFile, userOutputFile, answerFile
        );
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // 等待执行，带超时
        boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            return new SpjResult(1, "Time Limit Exceeded", 0, true);
        }

        return new SpjResult(
            process.exitValue(),
            readOutput(process),
            System.currentTimeMillis() - startTime,
            false
        );
    }
}
```

### 3.4 判题策略扩展

#### 3.4.1 JudgeModeEnum 新增枚举

```java
public enum JudgeModeEnum {
    DEFAULT("DEFAULT", "默认精确匹配"),
    IGNORE_SPACE("IGNORE_SPACE", "忽略多余空格"),
    IGNORE_CASE("IGNORE_CASE", "忽略大小写"),
    FLOAT("FLOAT", "浮点数精度比较"),
    MULTI_ANSWER("MULTI_ANSWER", "多解判断"),
    TESTLIB("TESTLIB", "自定义SPJ判题");  // 新增

    private final String value;
    private final String description;
}
```

#### 3.4.2 TestlibJudgeStrategyImpl 实现

```java
public class TestlibJudgeStrategyImpl implements JudgeStrategy {

    @Resource
    private SpjExecutor spjExecutor;

    @Override
    public JudgeInfo doJudge(JudgeContext context) {
        // 1. 检查时间和内存限制（复用现有逻辑）
        // ...

        // 2. 获取 SPJ 代码和配置
        String spjCode = context.getJudgeConfig().getSpjCode();
        Long questionId = context.getQuestion().getId();

        // 3. 遍历每个测试用例，执行 SPJ 判题
        for (int i = 0; i < judgeCaseList.size(); i++) {
            // 写入临时文件
            String inputFile = writeTempFile(judgeCase.getInput());
            String userOutputFile = writeTempFile(outputList.get(i));
            String answerFile = writeTempFile(judgeCase.getOutput());

            // 执行 SPJ
            SpjResult result = spjExecutor.execute(
                questionId, inputFile, userOutputFile, answerFile, 10
            );

            // 判断结果
            if (result.getExitCode() != 0) {
                return buildJudgeInfo(result);
            }
        }

        // 4. 所有测试用例通过
        return buildAcceptedJudgeInfo();
    }
}
```

### 3.5 前端设计

#### 3.5.1 题目管理页面改造

```
┌─────────────────────────────────────────────────────────────────┐
│  编辑题目                                                        │
├─────────────────────────────────────────────────────────────────┤
│  题目标题：[________________]                                    │
│  题目描述：[文本编辑器]                                           │
│                                                                  │
│  判题配置：                                                       │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ 时间限制：[1000] ms                                          ││
│  │ 内存限制：[256] MB                                           ││
│  │ 判题模式：[TESTLIB ▼]  <-- 下拉选择                           ││
│  │   - DEFAULT                                                  ││
│  │   - IGNORE_SPACE                                             ││
│  │   - IGNORE_CASE                                              ││
│  │   - FLOAT                                                    ││
│  │   - MULTI_ANSWER                                             ││
│  │   - TESTLIB (自定义SPJ)                    <-- 新增选项       ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                  │
│  SPJ 程序代码：(当选择 TESTLIB 模式时显示)                        │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ [Monaco Editor 代码编辑器]                                   ││
│  │ import com.kkdj.testlib.Testlib;                             ││
│  │                                                              ││
│  │ public class SpjChecker extends Testlib {                    ││
│  │     public static void main(String[] args) {                 ││
│  │         init(args);                                          ││
│  │         int n = ouf.readInt();                               ││
│  │         if (isPrime(n)) {                                    ││
│  │             ok("Correct");                                   ││
│  │         } else {                                             ││
│  │             wrongAnswer("Not a prime");                      ││
│  │         }                                                    ││
│  │     }                                                        ││
│  │ }                                                            ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                  │
│  测试用例：                                                       │
│  [添加测试用例]                                                   │
│                                                                  │
│  [保存] [取消]                                                    │
└─────────────────────────────────────────────────────────────────┘
```

#### 3.5.2 前端组件设计

```typescript
// 题目编辑表单增加 SPJ 相关字段
interface QuestionForm {
  // ... 现有字段 ...
  judgeMode: string;
  spjCode?: string;      // SPJ 代码
  spjLanguage?: string;  // SPJ 语言
}

// 判题模式选择组件
<template>
  <a-select v-model="form.judgeMode">
    <a-option value="DEFAULT">默认精确匹配</a-option>
    <a-option value="IGNORE_SPACE">忽略多余空格</a-option>
    <a-option value="IGNORE_CASE">忽略大小写</a-option>
    <a-option value="FLOAT">浮点数精度比较</a-option>
    <a-option value="MULTI_ANSWER">多解判断</a-option>
    <a-option value="TESTLIB">自定义SPJ判题</a-option>
  </a-select>

  <!-- 当选择 TESTLIB 时显示 SPJ 编辑器 -->
  <div v-if="form.judgeMode === 'TESTLIB'" class="spj-editor">
    <a-select v-model="form.spjLanguage" style="width: 100px">
      <a-option value="java">Java</a-option>
    </a-select>
    <MonacoEditor
      v-model="form.spjCode"
      language="java"
      height="300px"
    />
  </div>
</template>
```

---

## 四、改造步骤

### 4.1 第一阶段：核心库开发（2 天）

| 步骤 | 任务 | 产出 |
|-----|------|-----|
| 1.1 | 创建 testlib 模块 | `com.kkdj.testlib` 包结构 |
| 1.2 | 实现 Verdict 枚举 | `Verdict.java` |
| 1.3 | 实现 InStream 输入流解析器 | `InStream.java` |
| 1.4 | 实现 Testlib 主类 | `Testlib.java` |
| 1.5 | 实现 CheckerUtils 工具类 | `CheckerUtils.java` |
| 1.6 | 编写单元测试 | 测试用例 |

### 4.2 第二阶段：数据库改造（0.5 天）

| 步骤 | 任务 | 产出 |
|-----|------|-----|
| 2.1 | 编写数据库迁移脚本 | `V{x}_add_spj_fields.sql` |
| 2.2 | 执行数据库迁移 | Question 表新增字段 |
| 2.3 | 更新 Question 实体类 | 增加 spjCode、spjLanguage 字段 |
| 2.4 | 更新 JudgeConfig DTO | 增加 SPJ 相关字段 |

### 4.3 第三阶段：后端服务开发（2 天）

| 步骤 | 任务 | 产出 |
|-----|------|-----|
| 3.1 | 创建 SPJ 执行模块 | `com.kkdj.judge.spj` 包 |
| 3.2 | 实现 SpjExecutor 接口 | `SpjExecutor.java` |
| 3.3 | 实现 JavaSpjExecutor | `JavaSpjExecutor.java` |
| 3.4 | 实现 SpjCompiler | `SpjCompiler.java` |
| 3.5 | 实现 SpjCacheManager | `SpjCacheManager.java` |
| 3.6 | 更新 JudgeModeEnum | 新增 TESTLIB 枚举 |
| 3.7 | 实现 TestlibJudgeStrategyImpl | 新判题策略 |
| 3.8 | 更新 JudgeManager | 增加策略分支 |

### 4.4 第四阶段：前端改造（1.5 天）

| 步骤 | 任务 | 产出 |
|-----|------|-----|
| 4.1 | 更新判题模式枚举 | 前端枚举定义 |
| 4.2 | 题目编辑页面增加 SPJ 编辑器 | Vue 组件 |
| 4.3 | 表单验证和提交逻辑 | 接口调用 |
| 4.4 | SPJ 代码示例和提示 | 帮助文档 |

### 4.5 第五阶段：测试与优化（1 天）

| 步骤 | 任务 | 产出 |
|-----|------|-----|
| 5.1 | 编写测试题目 | 包含各种 SPJ 场景 |
| 5.2 | 端到端测试 | 完整判题流程 |
| 5.3 | 性能测试 | 并发压力测试 |
| 5.4 | 问题修复 | Bug 修复 |
| 5.5 | 文档编写 | 使用说明 |

---

## 五、安全措施

### 5.1 SPJ 执行安全

| 风险 | 措施 |
|-----|------|
| 恶意代码 | 进程隔离，不使用 Docker（可选升级为 Docker） |
| 资源耗尽 | 限制 CPU 时间（10秒）、内存（256MB） |
| 文件访问 | 限制工作目录，禁止访问其他文件 |
| 网络访问 | 禁止网络连接 |

### 5.2 代码实现

```java
// 安全执行配置
public class SecurityConfig {
    public static final int MAX_CPU_TIME = 10;      // 最大 CPU 时间（秒）
    public static final int MAX_MEMORY = 256;       // 最大内存（MB）
    public static final String ALLOWED_DIR = "/tmp/oj_spj/";  // 允许访问的目录
}

// ProcessBuilder 安全配置
ProcessBuilder pb = new ProcessBuilder(
    "java",
    "-Xmx" + SecurityConfig.MAX_MEMORY + "m",  // 限制内存
    "-cp", workDir + ":" + testlibJar,
    "SpjChecker",
    inputFile, userOutputFile, answerFile
);
pb.directory(workDir.toFile());
pb.redirectErrorStream(true);
```

---

## 六、性能优化

### 6.1 SPJ 编译缓存

```
第一次判题：编译 SPJ → 缓存 .class 文件 → 执行
后续判题：  直接使用缓存的 .class 文件 → 执行
```

### 6.2 并发控制

```java
// 限制同时执行的 SPJ 数量
private static final int MAX_CONCURRENT_SPJ = 2;
private static final Semaphore spjSemaphore = new Semaphore(MAX_CONCURRENT_SPJ);

public SpjResult execute(...) {
    spjSemaphore.acquire();  // 获取信号量
    try {
        // 执行 SPJ
    } finally {
        spjSemaphore.release();  // 释放信号量
    }
}
```

### 6.3 资源清理

```java
// 定期清理过期的 SPJ 缓存（超过 7 天未使用）
@Scheduled(cron = "0 0 3 * * ?")  // 每天凌晨 3 点执行
public void cleanExpiredCache() {
    // 清理逻辑
}
```

---

## 七、文件结构

### 7.1 后端新增文件

```
oj_backend-master/src/main/java/com/kkdj/
├── testlib/                          # testlib 核心库
│   ├── Testlib.java
│   ├── InStream.java
│   ├── Verdict.java
│   ├── CheckerUtils.java
│   └── TestlibException.java
├── judge/
│   ├── spj/                          # SPJ 执行模块
│   │   ├── SpjExecutor.java
│   │   ├── JavaSpjExecutor.java
│   │   ├── SpjCompiler.java
│   │   ├── SpjCacheManager.java
│   │   └── SpjResult.java
│   └── strategy/impl/
│       └── TestlibJudgeStrategyImpl.java  # 新判题策略
└── model/
    ├── entity/Question.java          # 修改：增加 SPJ 字段
    └── enums/JudgeModeEnum.java      # 修改：增加 TESTLIB 枚举
```

### 7.2 前端新增/修改文件

```
oj-frontend-master/src/
├── views/
│   └── admin/
│       └── QuestionManage.vue        # 修改：增加 SPJ 编辑器
├── components/
│   └── SpjEditor.vue                 # 新增：SPJ 代码编辑器组件
└── constants/
    └── judgeMode.ts                  # 修改：增加 TESTLIB 枚举
```

---

## 八、时间规划

| 阶段 | 任务 | 预估时间 | 累计 |
|-----|------|---------|-----|
| 第一阶段 | testlib 核心库开发 | 2 天 | 2 天 |
| 第二阶段 | 数据库改造 | 0.5 天 | 2.5 天 |
| 第三阶段 | 后端服务开发 | 2 天 | 4.5 天 |
| 第四阶段 | 前端改造 | 1.5 天 | 6 天 |
| 第五阶段 | 测试与优化 | 1 天 | 7 天 |

**总计：约 7 天**

---

## 九、风险与应对

| 风险 | 可能性 | 影响 | 应对措施 |
|-----|-------|-----|---------|
| SPJ 编译失败 | 中 | 判题失败 | 增加编译错误提示，出题时验证 |
| SPJ 执行超时 | 中 | 判题卡住 | 强制终止进程，返回超时错误 |
| 内存不足 | 低 | 系统崩溃 | 限制并发数，监控内存使用 |
| 安全漏洞 | 低 | 服务器被攻击 | 进程隔离，资源限制 |

---

## 十、后续扩展

| 扩展方向 | 说明 |
|---------|------|
| 支持 C++ SPJ | 增加 C++ 语言支持 |
| Docker 隔离 | 提升安全性 |
| 交互式判题 | 支持程序与 SPJ 交互 |
| SPJ 模板 | 提供常用 SPJ 模板供出题人参考 |

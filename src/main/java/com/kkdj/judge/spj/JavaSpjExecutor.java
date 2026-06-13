package com.kkdj.judge.spj;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * Java SPJ 执行器实现
 * 使用 ProcessBuilder 在本地执行 Java SPJ 程序
 */
@Slf4j
@Component
public class JavaSpjExecutor implements SpjExecutor {

    /**
     * SPJ 工作目录
     * Windows: C:\temp\oj_spj\
     * Linux/Mac: /tmp/oj_spj/
     */
    private static final String SPJ_WORK_DIR;

    static {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            SPJ_WORK_DIR = "C:\\temp\\oj_spj\\";
        } else {
            SPJ_WORK_DIR = "/tmp/oj_spj/";
        }
    }

    /**
     * 编译超时时间（秒）
     */
    private static final int COMPILE_TIMEOUT = 30;

    /**
     * 默认执行超时时间（秒）
     */
    private static final int DEFAULT_EXECUTE_TIMEOUT = 10;

    /**
     * SPJ 内存限制（MB）
     */
    private static final int SPJ_MEMORY_LIMIT = 256;

    /**
     * SPJ 主类名
     */
    private static final String SPJ_MAIN_CLASS = "SpjChecker";

    @Override
    public CompileResult compile(String spjCode, Long questionId) {
        log.info("开始编译 SPJ，题目ID: {}", questionId);

        try {
            // 1. 创建工作目录
            Path workDir = Paths.get(SPJ_WORK_DIR, questionId.toString());
            Files.createDirectories(workDir);

            // 2. 创建 testlib 包目录
            Path testlibDir = workDir.resolve("com").resolve("kkdj").resolve("testlib");
            Files.createDirectories(testlibDir);

            // 3. 写入 testlib 源代码
            writeStringToFile(testlibDir.resolve("Verdict.java"), TestlibSourceProvider.getVerdictSource());
            writeStringToFile(testlibDir.resolve("TestlibException.java"), TestlibSourceProvider.getTestlibExceptionSource());
            writeStringToFile(testlibDir.resolve("InStream.java"), TestlibSourceProvider.getInStreamSource());
            writeStringToFile(testlibDir.resolve("CheckerUtils.java"), TestlibSourceProvider.getCheckerUtilsSource());
            writeStringToFile(testlibDir.resolve("Testlib.java"), TestlibSourceProvider.getTestlibSource());

            // 4. 写入 SPJ 源代码
            Path sourceFile = workDir.resolve(SPJ_MAIN_CLASS + ".java");
            writeStringToFile(sourceFile, spjCode);

            // 5. 执行 javac 编译（编译所有 .java 文件）
            ProcessBuilder pb = new ProcessBuilder(
                    "javac",
                    "-encoding", "UTF-8",
                    "SpjChecker.java",
                    "com/kkdj/testlib/Verdict.java",
                    "com/kkdj/testlib/TestlibException.java",
                    "com/kkdj/testlib/InStream.java",
                    "com/kkdj/testlib/CheckerUtils.java",
                    "com/kkdj/testlib/Testlib.java"
            );
            pb.directory(workDir.toFile());
            pb.redirectErrorStream(true);

            long startTime = System.currentTimeMillis();
            Process process = pb.start();

            boolean finished = process.waitFor(COMPILE_TIMEOUT, TimeUnit.SECONDS);
            long compileTime = System.currentTimeMillis() - startTime;

            if (!finished) {
                process.destroyForcibly();
                log.error("SPJ 编译超时，题目ID: {}", questionId);
                return new CompileResult(false, -1, "编译超时", compileTime);
            }

            // 6. 读取编译输出
            String output = readProcessOutput(process);
            int exitCode = process.exitValue();

            // 7. 检查编译结果
            Path classFile = workDir.resolve(SPJ_MAIN_CLASS + ".class");
            boolean success = Files.exists(classFile) && exitCode == 0;

            if (success) {
                log.info("SPJ 编译成功，题目ID: {}", questionId);
            } else {
                log.error("SPJ 编译失败，题目ID: {}，错误: {}", questionId, output);
            }

            return new CompileResult(success, exitCode, output, compileTime);

        } catch (Exception e) {
            log.error("SPJ 编译异常，题目ID: {}", questionId, e);
            return new CompileResult(false, -1, "编译异常: " + e.getMessage(), 0);
        }
    }

    @Override
    public boolean isCompiled(Long questionId) {
        Path workDir = Paths.get(SPJ_WORK_DIR, questionId.toString());
        Path classFile = workDir.resolve(SPJ_MAIN_CLASS + ".class");
        return Files.exists(classFile);
    }

    @Override
    public SpjResult execute(Long questionId, String inputFile, String userOutputFile, String answerFile, int timeout) {
        log.debug("开始执行 SPJ，题目ID: {}，超时: {}s", questionId, timeout);

        // 使用默认超时时间
        if (timeout <= 0) {
            timeout = DEFAULT_EXECUTE_TIMEOUT;
        }

        try {
            Path workDir = Paths.get(SPJ_WORK_DIR, questionId.toString());

            // 检查编译后的 class 文件是否存在
            Path classFile = workDir.resolve(SPJ_MAIN_CLASS + ".class");
            if (!Files.exists(classFile)) {
                log.error("SPJ 未编译，题目ID: {}", questionId);
                return SpjResult.systemError("SPJ 未编译");
            }

            // 构建 Java 执行命令
            ProcessBuilder pb = new ProcessBuilder(
                    "java",
                    "-Xmx" + SPJ_MEMORY_LIMIT + "m",  // 限制内存
                    "-Dfile.encoding=UTF-8",
                    SPJ_MAIN_CLASS,
                    inputFile,
                    userOutputFile,
                    answerFile
            );
            pb.directory(workDir.toFile());
            pb.redirectErrorStream(true);

            long startTime = System.currentTimeMillis();
            Process process = pb.start();

            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
            long executeTime = System.currentTimeMillis() - startTime;

            if (!finished) {
                process.destroyForcibly();
                log.warn("SPJ 执行超时，题目ID: {}，超时时间: {}s", questionId, timeout);
                return SpjResult.timeout();
            }

            int exitCode = process.exitValue();
            String output = readProcessOutput(process);

            log.debug("SPJ 执行完成，题目ID: {}，退出码: {}，耗时: {}ms，输出: {}", questionId, exitCode, executeTime, output);

            return new SpjResult(exitCode, output, executeTime, false);

        } catch (Exception e) {
            log.error("SPJ 执行异常，题目ID: {}", questionId, e);
            return SpjResult.systemError("执行异常: " + e.getMessage());
        }
    }

    @Override
    public void cleanup(Long questionId) {
        log.info("清理 SPJ 缓存，题目ID: {}", questionId);

        try {
            Path workDir = Paths.get(SPJ_WORK_DIR, questionId.toString());
            if (Files.exists(workDir)) {
                deleteDirectory(workDir);
                log.debug("已删除工作目录: {}", workDir);
            }
        } catch (Exception e) {
            log.error("清理 SPJ 缓存失败，题目ID: {}", questionId, e);
        }
    }

    /**
     * 读取进程输出
     */
    private String readProcessOutput(Process process) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString().trim();
    }

    /**
     * 写入字符串到文件（Java 8 兼容）
     */
    private void writeStringToFile(Path path, String content) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(path.toFile()), StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }

    /**
     * 删除目录及其内容
     */
    private void deleteDirectory(Path dir) throws IOException {
        if (Files.isDirectory(dir)) {
            Files.list(dir).forEach(file -> {
                try {
                    if (Files.isDirectory(file)) {
                        deleteDirectory(file);
                    } else {
                        Files.deleteIfExists(file);
                    }
                } catch (IOException e) {
                    log.warn("删除文件失败: {}", file, e);
                }
            });
        }
        Files.deleteIfExists(dir);
    }

    /**
     * 获取工作目录路径
     */
    public static String getWorkDir(Long questionId) {
        return Paths.get(SPJ_WORK_DIR, questionId.toString()).toString();
    }
}
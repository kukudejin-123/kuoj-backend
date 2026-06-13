package com.kkdj.judge.spj;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * SPJ 缓存管理器
 * 负责 SPJ 编译结果的缓存和清理
 */
@Slf4j
@Component
public class SpjCacheManager {

    @Resource
    private JavaSpjExecutor spjExecutor;

    /**
     * 缓存过期时间（天）
     */
    private static final int CACHE_EXPIRE_DAYS = 7;

    /**
     * 编译锁，防止同一题目并发编译
     */
    private final ReentrantLock compileLock = new ReentrantLock();

    /**
     * 确保 SPJ 已编译
     * 如果未编译则立即编译
     *
     * @param spjCode    SPJ 源代码
     * @param questionId 题目ID
     * @return 是否编译成功
     */
    public boolean ensureCompiled(String spjCode, Long questionId) {
        // 检查是否已编译
        if (spjExecutor.isCompiled(questionId)) {
            log.debug("SPJ 已缓存，题目ID: {}", questionId);
            return true;
        }

        // 加锁防止并发编译
        compileLock.lock();
        try {
            // 再次检查（可能在等待锁时已被其他线程编译）
            if (spjExecutor.isCompiled(questionId)) {
                return true;
            }

            // 执行编译
            CompileResult result = spjExecutor.compile(spjCode, questionId);
            return result.isSuccess();
        } finally {
            compileLock.unlock();
        }
    }

    /**
     * 强制重新编译 SPJ
     *
     * @param spjCode    SPJ 源代码
     * @param questionId 题目ID
     * @return 编译结果
     */
    public CompileResult forceCompile(String spjCode, Long questionId) {
        // 先清理旧缓存
        spjExecutor.cleanup(questionId);

        // 重新编译
        return spjExecutor.compile(spjCode, questionId);
    }

    /**
     * 定期清理过期缓存
     * 每天凌晨 3 点执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredCache() {
        log.info("开始清理过期 SPJ 缓存");

        String workDir = System.getProperty("os.name").toLowerCase().contains("win")
                ? "C:\\temp\\oj_spj\\"
                : "/tmp/oj_spj/";

        Path spjDir = Paths.get(workDir);
        if (!Files.exists(spjDir)) {
            log.debug("SPJ 工作目录不存在: {}", spjDir);
            return;
        }

        Instant expireTime = Instant.now().minus(CACHE_EXPIRE_DAYS, ChronoUnit.DAYS);
        int cleanedCount = 0;

        try {
            Files.list(spjDir)
                    .filter(Files::isDirectory)
                    .forEach(dir -> {
                        try {
                            BasicFileAttributes attrs = Files.readAttributes(dir, BasicFileAttributes.class);
                            if (attrs.lastModifiedTime().toInstant().isBefore(expireTime)) {
                                deleteDirectory(dir);
                                log.debug("已清理过期缓存: {}", dir);
                            }
                        } catch (IOException e) {
                            log.warn("清理目录失败: {}", dir, e);
                        }
                    });
        } catch (IOException e) {
            log.error("遍历 SPJ 目录失败", e);
        }

        log.info("SPJ 缓存清理完成，清理数量: {}", cleanedCount);
    }

    /**
     * 清理单个题目的缓存
     *
     * @param questionId 题目ID
     */
    public void cleanup(Long questionId) {
        spjExecutor.cleanup(questionId);
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
     * 获取缓存统计信息
     */
    public CacheStats getCacheStats() {
        String workDir = System.getProperty("os.name").toLowerCase().contains("win")
                ? "C:\\temp\\oj_spj\\"
                : "/tmp/oj_spj/";

        Path spjDir = Paths.get(workDir);

        int totalCacheCount = 0;
        long totalSize = 0;

        if (Files.exists(spjDir)) {
            try {
                totalCacheCount = (int) Files.list(spjDir)
                        .filter(Files::isDirectory)
                        .count();

                totalSize = Files.walk(spjDir)
                        .filter(Files::isRegularFile)
                        .mapToLong(file -> {
                            try {
                                return Files.size(file);
                            } catch (IOException e) {
                                return 0;
                            }
                        })
                        .sum();
            } catch (IOException e) {
                log.warn("获取缓存统计失败", e);
            }
        }

        return new CacheStats(totalCacheCount, totalSize);
    }

    /**
     * 缓存统计信息
     */
    public static class CacheStats {
        private final int count;
        private final long size;

        public CacheStats(int count, long size) {
            this.count = count;
            this.size = size;
        }

        public int getCount() {
            return count;
        }

        public long getSize() {
            return size;
        }

        public String getSizeFormatted() {
            if (size < 1024) {
                return size + " B";
            } else if (size < 1024 * 1024) {
                return (size / 1024) + " KB";
            } else {
                return (size / (1024 * 1024)) + " MB";
            }
        }
    }
}
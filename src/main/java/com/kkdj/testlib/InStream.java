package com.kkdj.testlib;

import java.io.*;
import java.util.StringTokenizer;

/**
 * 输入流解析器
 * 用于读取输入文件、用户输出文件和标准答案文件
 */
public class InStream implements Closeable {

    private BufferedReader reader;
    private StringTokenizer tokenizer;
    private String currentLine;
    private int lineNumber;
    private String fileName;

    /**
     * 从文件路径创建输入流
     */
    public InStream(String fileName) throws TestlibException {
        this.fileName = fileName;
        this.lineNumber = 0;
        try {
            reader = new BufferedReader(new FileReader(fileName));
        } catch (FileNotFoundException e) {
            throw new TestlibException("无法打开文件: " + fileName, e);
        }
    }

    /**
     * 读取下一行
     */
    public String readLine() throws TestlibException {
        try {
            currentLine = reader.readLine();
            lineNumber++;
            return currentLine;
        } catch (IOException e) {
            throw new TestlibException("读取文件失败: " + fileName, e);
        }
    }

    /**
     * 读取一个单词（以空白字符分隔）
     */
    public String readWord() throws TestlibException {
        while (tokenizer == null || !tokenizer.hasMoreTokens()) {
            String line = readLine();
            if (line == null) {
                return null;
            }
            tokenizer = new StringTokenizer(line);
        }
        return tokenizer.nextToken();
    }

    /**
     * 读取整数
     */
    public int readInt() throws TestlibException {
        String word = readWord();
        if (word == null) {
            throw new TestlibException("意外到达文件末尾，期望读取整数");
        }
        try {
            return Integer.parseInt(word);
        } catch (NumberFormatException e) {
            throw new TestlibException("无法解析整数: " + word, e);
        }
    }

    /**
     * 读取长整数
     */
    public long readLong() throws TestlibException {
        String word = readWord();
        if (word == null) {
            throw new TestlibException("意外到达文件末尾，期望读取长整数");
        }
        try {
            return Long.parseLong(word);
        } catch (NumberFormatException e) {
            throw new TestlibException("无法解析长整数: " + word, e);
        }
    }

    /**
     * 读取双精度浮点数
     */
    public double readDouble() throws TestlibException {
        String word = readWord();
        if (word == null) {
            throw new TestlibException("意外到达文件末尾，期望读取浮点数");
        }
        try {
            return Double.parseDouble(word);
        } catch (NumberFormatException e) {
            throw new TestlibException("无法解析浮点数: " + word, e);
        }
    }

    /**
     * 读取字符串（一个单词）
     */
    public String readString() throws TestlibException {
        return readWord();
    }

    /**
     * 读取字符
     */
    public char readChar() throws TestlibException {
        String word = readWord();
        if (word == null || word.isEmpty()) {
            throw new TestlibException("意外到达文件末尾，期望读取字符");
        }
        return word.charAt(0);
    }

    /**
     * 是否到达文件末尾
     */
    public boolean isEof() throws TestlibException {
        if (tokenizer != null && tokenizer.hasMoreTokens()) {
            return false;
        }
        try {
            return !reader.ready() && reader.readLine() == null;
        } catch (IOException e) {
            throw new TestlibException("检查文件末尾失败: " + fileName, e);
        }
    }

    /**
     * 是否还有更多内容
     */
    public boolean hasMore() throws TestlibException {
        return !isEof();
    }

    /**
     * 获取当前行号
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * 获取当前行内容
     */
    public String getCurrentLine() {
        return currentLine;
    }

    /**
     * 获取文件名
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * 跳过空白行
     */
    public void skipBlankLines() throws TestlibException {
        while (true) {
            String line = readLine();
            if (line == null) {
                break;
            }
            if (!line.trim().isEmpty()) {
                // 回退一行（保存当前行供下次使用）
                currentLine = line;
                lineNumber--;
                break;
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }
}

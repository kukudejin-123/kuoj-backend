package com.kkdj.testlib;

/**
 * Testlib 异常类
 */
public class TestlibException extends RuntimeException {

    public TestlibException(String message) {
        super(message);
    }

    public TestlibException(String message, Throwable cause) {
        super(message, cause);
    }
}

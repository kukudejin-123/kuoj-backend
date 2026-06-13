package com.kkdj.testlib;

/**
 * 判题工具类
 * 提供常用的判题辅助方法
 */
public class CheckerUtils {

    private CheckerUtils() {
        // 工具类不允许实例化
    }

    /**
     * 比较两个浮点数是否相等（考虑精度）
     *
     * @param a   第一个数
     * @param b   第二个数
     * @param eps 精度容忍度
     * @return 是否相等
     */
    public static boolean doubleEquals(double a, double b, double eps) {
        if (Double.isNaN(a) && Double.isNaN(b)) {
            return true;
        }
        if (Double.isInfinite(a) && Double.isInfinite(b)) {
            return a > 0 == b > 0;
        }
        return Math.abs(a - b) < eps;
    }

    /**
     * 比较两个浮点数是否相等（默认精度 1e-6）
     */
    public static boolean doubleEquals(double a, double b) {
        return doubleEquals(a, b, 1e-6);
    }

    /**
     * 比较两个字符串是否相等
     *
     * @param a          第一个字符串
     * @param b          第二个字符串
     * @param ignoreCase 是否忽略大小写
     * @return 是否相等
     */
    public static boolean stringEquals(String a, String b, boolean ignoreCase) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return ignoreCase ? a.equalsIgnoreCase(b) : a.equals(b);
    }

    /**
     * 比较两个字符串是否相等（区分大小写）
     */
    public static boolean stringEquals(String a, String b) {
        return stringEquals(a, b, false);
    }

    /**
     * 判断是否是质数
     *
     * @param n 待判断的数
     * @return 是否是质数
     */
    public static boolean isPrime(long n) {
        if (n < 2) {
            return false;
        }
        if (n == 2) {
            return true;
        }
        if (n % 2 == 0) {
            return false;
        }
        for (long i = 3; i * i <= n; i += 2) {
            if (n % i == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 计算最大公约数
     */
    public static long gcd(long a, long b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b != 0) {
            long temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    /**
     * 计算最小公倍数
     */
    public static long lcm(long a, long b) {
        return Math.abs(a / gcd(a, b) * b);
    }

    /**
     * 判断字符串是否是数字
     */
    public static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 判断字符串是否是整数
     */
    public static boolean isInteger(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Long.parseLong(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 比较两个整数数组是否相等
     */
    public static boolean arrayEquals(int[] a, int[] b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 比较两个长整数数组是否相等
     */
    public static boolean arrayEquals(long[] a, long[] b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 比较两个浮点数数组是否相等
     */
    public static boolean arrayEquals(double[] a, double[] b, double eps) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (!doubleEquals(a[i], b[i], eps)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 比较两个字符串数组是否相等
     */
    public static boolean arrayEquals(String[] a, String[] b, boolean ignoreCase) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (!stringEquals(a[i], b[i], ignoreCase)) {
                return false;
            }
        }
        return true;
    }
}

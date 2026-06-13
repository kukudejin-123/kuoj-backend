package com.kkdj.judge.spj;

/**
 * Testlib 源代码提供者
 * 用于在编译 SPJ 时提供 testlib 库的源代码（无第三方依赖版本）
 */
public class TestlibSourceProvider {

    /**
     * 获取 Verdict.java 源代码
     */
    public static String getVerdictSource() {
        return "package com.kkdj.testlib;\n" +
                "\n" +
                "/**\n" +
                " * SPJ 判题结果枚举\n" +
                " */\n" +
                "public enum Verdict {\n" +
                "\n" +
                "    OK(0, \"Accepted\"),\n" +
                "    WA(1, \"Wrong Answer\"),\n" +
                "    PE(2, \"Presentation Error\"),\n" +
                "    FAIL(3, \"Fail\");\n" +
                "\n" +
                "    private final int exitCode;\n" +
                "    private final String message;\n" +
                "\n" +
                "    Verdict(int exitCode, String message) {\n" +
                "        this.exitCode = exitCode;\n" +
                "        this.message = message;\n" +
                "    }\n" +
                "\n" +
                "    public int getExitCode() {\n" +
                "        return exitCode;\n" +
                "    }\n" +
                "\n" +
                "    public String getMessage() {\n" +
                "        return message;\n" +
                "    }\n" +
                "\n" +
                "    public static Verdict fromExitCode(int exitCode) {\n" +
                "        for (Verdict verdict : values()) {\n" +
                "            if (verdict.getExitCode() == exitCode) {\n" +
                "                return verdict;\n" +
                "            }\n" +
                "        }\n" +
                "        return FAIL;\n" +
                "    }\n" +
                "}\n";
    }

    /**
     * 获取 TestlibException.java 源代码
     */
    public static String getTestlibExceptionSource() {
        return "package com.kkdj.testlib;\n" +
                "\n" +
                "/**\n" +
                " * Testlib 异常类\n" +
                " */\n" +
                "public class TestlibException extends RuntimeException {\n" +
                "\n" +
                "    public TestlibException(String message) {\n" +
                "        super(message);\n" +
                "    }\n" +
                "\n" +
                "    public TestlibException(String message, Throwable cause) {\n" +
                "        super(message, cause);\n" +
                "    }\n" +
                "}\n";
    }

    /**
     * 获取 InStream.java 源代码
     */
    public static String getInStreamSource() {
        return "package com.kkdj.testlib;\n" +
                "\n" +
                "import java.io.*;\n" +
                "import java.util.StringTokenizer;\n" +
                "\n" +
                "/**\n" +
                " * 输入流解析器\n" +
                " */\n" +
                "public class InStream implements Closeable {\n" +
                "\n" +
                "    private BufferedReader reader;\n" +
                "    private StringTokenizer tokenizer;\n" +
                "    private String currentLine;\n" +
                "    private int lineNumber;\n" +
                "    private String fileName;\n" +
                "\n" +
                "    public InStream(String fileName) throws TestlibException {\n" +
                "        this.fileName = fileName;\n" +
                "        this.lineNumber = 0;\n" +
                "        try {\n" +
                "            reader = new BufferedReader(new FileReader(fileName));\n" +
                "        } catch (FileNotFoundException e) {\n" +
                "            throw new TestlibException(\"File not found: \" + fileName, e);\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    public String readLine() throws TestlibException {\n" +
                "        try {\n" +
                "            currentLine = reader.readLine();\n" +
                "            lineNumber++;\n" +
                "            return currentLine;\n" +
                "        } catch (IOException e) {\n" +
                "            throw new TestlibException(\"Read error: \" + fileName, e);\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    public String readWord() throws TestlibException {\n" +
                "        while (tokenizer == null || !tokenizer.hasMoreTokens()) {\n" +
                "            String line = readLine();\n" +
                "            if (line == null) {\n" +
                "                return null;\n" +
                "            }\n" +
                "            tokenizer = new StringTokenizer(line);\n" +
                "        }\n" +
                "        return tokenizer.nextToken();\n" +
                "    }\n" +
                "\n" +
                "    public int readInt() throws TestlibException {\n" +
                "        String word = readWord();\n" +
                "        if (word == null) {\n" +
                "            throw new TestlibException(\"Unexpected EOF while reading int\");\n" +
                "        }\n" +
                "        try {\n" +
                "            return Integer.parseInt(word);\n" +
                "        } catch (NumberFormatException e) {\n" +
                "            throw new TestlibException(\"Cannot parse int: \" + word, e);\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    public long readLong() throws TestlibException {\n" +
                "        String word = readWord();\n" +
                "        if (word == null) {\n" +
                "            throw new TestlibException(\"Unexpected EOF while reading long\");\n" +
                "        }\n" +
                "        try {\n" +
                "            return Long.parseLong(word);\n" +
                "        } catch (NumberFormatException e) {\n" +
                "            throw new TestlibException(\"Cannot parse long: \" + word, e);\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    public double readDouble() throws TestlibException {\n" +
                "        String word = readWord();\n" +
                "        if (word == null) {\n" +
                "            throw new TestlibException(\"Unexpected EOF while reading double\");\n" +
                "        }\n" +
                "        try {\n" +
                "            return Double.parseDouble(word);\n" +
                "        } catch (NumberFormatException e) {\n" +
                "            throw new TestlibException(\"Cannot parse double: \" + word, e);\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    public String readString() throws TestlibException {\n" +
                "        return readWord();\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public void close() throws IOException {\n" +
                "        if (reader != null) {\n" +
                "            reader.close();\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
    }

    /**
     * 获取 CheckerUtils.java 源代码
     */
    public static String getCheckerUtilsSource() {
        return "package com.kkdj.testlib;\n" +
                "\n" +
                "/**\n" +
                " * 判题工具类\n" +
                " */\n" +
                "public class CheckerUtils {\n" +
                "\n" +
                "    private CheckerUtils() {}\n" +
                "\n" +
                "    public static boolean doubleEquals(double a, double b, double eps) {\n" +
                "        if (Double.isNaN(a) && Double.isNaN(b)) return true;\n" +
                "        if (Double.isInfinite(a) && Double.isInfinite(b)) return a > 0 == b > 0;\n" +
                "        return Math.abs(a - b) < eps;\n" +
                "    }\n" +
                "\n" +
                "    public static boolean doubleEquals(double a, double b) {\n" +
                "        return doubleEquals(a, b, 1e-6);\n" +
                "    }\n" +
                "\n" +
                "    public static boolean stringEquals(String a, String b, boolean ignoreCase) {\n" +
                "        if (a == null && b == null) return true;\n" +
                "        if (a == null || b == null) return false;\n" +
                "        return ignoreCase ? a.equalsIgnoreCase(b) : a.equals(b);\n" +
                "    }\n" +
                "\n" +
                "    public static boolean isPrime(long n) {\n" +
                "        if (n < 2) return false;\n" +
                "        if (n == 2) return true;\n" +
                "        if (n % 2 == 0) return false;\n" +
                "        for (long i = 3; i * i <= n; i += 2) {\n" +
                "            if (n % i == 0) return false;\n" +
                "        }\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    public static long gcd(long a, long b) {\n" +
                "        a = Math.abs(a);\n" +
                "        b = Math.abs(b);\n" +
                "        while (b != 0) {\n" +
                "            long temp = b;\n" +
                "            b = a % b;\n" +
                "            a = temp;\n" +
                "        }\n" +
                "        return a;\n" +
                "    }\n" +
                "\n" +
                "    public static long lcm(long a, long b) {\n" +
                "        return Math.abs(a / gcd(a, b) * b);\n" +
                "    }\n" +
                "}\n";
    }

    /**
     * 获取 Testlib.java 源代码
     */
    public static String getTestlibSource() {
        return "package com.kkdj.testlib;\n" +
                "\n" +
                "import java.io.Closeable;\n" +
                "import java.io.IOException;\n" +
                "\n" +
                "/**\n" +
                " * Testlib 主类\n" +
                " */\n" +
                "public class Testlib implements Closeable {\n" +
                "\n" +
                "    protected static InStream inf;\n" +
                "    protected static InStream ouf;\n" +
                "    protected static InStream ans;\n" +
                "\n" +
                "    public static void init(String[] args) {\n" +
                "        if (args == null || args.length < 3) {\n" +
                "            System.out.println(\"Fail: Invalid arguments\");\n" +
                "            System.exit(3);\n" +
                "        }\n" +
                "        try {\n" +
                "            inf = new InStream(args[0]);\n" +
                "            ouf = new InStream(args[1]);\n" +
                "            ans = new InStream(args[2]);\n" +
                "        } catch (TestlibException e) {\n" +
                "            System.out.println(\"Fail: \" + e.getMessage());\n" +
                "            System.exit(3);\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    public static void ok(String message) {\n" +
                "        System.out.println(\"Accepted: \" + message);\n" +
                "        System.exit(0);\n" +
                "    }\n" +
                "\n" +
                "    public static void ok() {\n" +
                "        ok(\"OK\");\n" +
                "    }\n" +
                "\n" +
                "    public static void wrongAnswer(String message) {\n" +
                "        System.out.println(\"Wrong Answer: \" + message);\n" +
                "        System.exit(1);\n" +
                "    }\n" +
                "\n" +
                "    public static void presentationError(String message) {\n" +
                "        System.out.println(\"Presentation Error: \" + message);\n" +
                "        System.exit(2);\n" +
                "    }\n" +
                "\n" +
                "    public static void fail(String message) {\n" +
                "        System.out.println(\"Fail: \" + message);\n" +
                "        System.exit(3);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public void close() throws IOException {\n" +
                "        if (inf != null) inf.close();\n" +
                "        if (ouf != null) ouf.close();\n" +
                "        if (ans != null) ans.close();\n" +
                "    }\n" +
                "}\n";
    }
}

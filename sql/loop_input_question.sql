-- 循环输入模式示例题目
-- 需要先有用户，如果没有请修改userId

-- 示例题目: A+B 问题（循环输入版本）
-- 用户需要使用 while(sc.hasNextInt()) 循环读取
INSERT INTO question (title, content, tags, answer, sourceCode, submitNum, acceptedNum, judgeCase, judgeConfig, thumbNum, favourNum, userId, isDelete)
VALUES (
    'A + B 问题（循环输入）',
    '## 题目描述

计算两个整数的和。

## 输入格式

输入包含多组测试数据，每组数据占一行，包含两个整数 A 和 B。

输入可能有多组数据，请处理到文件结束（EOF）。

## 输出格式

对于每组输入，输出一行，包含一个整数，表示 A + B 的结果。

## 样例

**输入：**
```
1 2
3 4
5 6
```

**输出：**
```
3
7
11
```

## 提示

请使用循环读取输入，例如：
- Java: `while (scanner.hasNextInt()) { ... }`
- C++: `while (cin >> a >> b) { ... }`
- Python: `for line in sys.stdin: ...`',
    '["入门","数学","循环输入"]',
    '3
7
11',
    'import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextInt()) {
            int a = scanner.nextInt();
            int b = scanner.nextInt();
            System.out.println(a + b);
        }
    }
}',
    0,
    0,
    '[{"input":"1 2","output":"3"},{"input":"3 4","output":"7"},{"input":"5 6","output":"11"}]',
    '{"timeLimit":1000,"memoryLimit":65536,"stackLimit":8192,"inputMode":"loop"}',
    0,
    0,
    1,
    0
);

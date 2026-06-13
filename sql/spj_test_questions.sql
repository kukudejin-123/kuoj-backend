-- SPJ测试题目数据
-- 执行此SQL可以向数据库插入测试题目

-- 首先查询管理员用户ID（如果不知道的话）
-- SELECT id, userAccount FROM user WHERE userRole = 'admin';

-- ============================================
-- 题目1：浮点数精度测试 - 计算圆的面积
-- ============================================
INSERT INTO question (title, content, tags, answer, sourceCode, submitNum, acceptedNum, judgeCase, judgeConfig, thumbNum, favourNum, userId, isDelete)
VALUES (
    '计算圆的面积',
    '## 题目描述

给定圆的半径 r，计算圆的面积（保留两位小数输出）。

## 输入格式

输入一个实数 r（0 < r < 100），表示圆的半径。

## 输出格式

输出圆的面积，结果允许有浮点数误差（精度要求 0.01）。

## 样例输入

```
5
```

## 样例输出

```
78.54
```

## 提示

圆的面积公式：S = π × r²
π 取 3.141592653589793',
    '["数学", "浮点数"]',
    '使用公式 S = π * r * r 计算，注意浮点数精度问题',
    'import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        double r = sc.nextDouble();
        double area = Math.PI * r * r;
        System.out.printf("%.2f", area);
    }
}',
    0,
    0,
    '[{"input":"5","output":"78.54"},{"input":"1","output":"3.14"},{"input":"10","output":"314.16"}]',
    '{"timeLimit":1000,"memoryLimit":256000,"stackLimit":1000,"inputMode":"single","judgeMode":"FLOAT","floatPrecision":0.01}',
    0,
    0,
    1,
    0
);

-- ============================================
-- 题目2：忽略大小写 - 判断质数
-- ============================================
INSERT INTO question (title, content, tags, answer, sourceCode, submitNum, acceptedNum, judgeCase, judgeConfig, thumbNum, favourNum, userId, isDelete)
VALUES (
    '判断质数',
    '## 题目描述

给定一个正整数 n，判断它是否为质数。

## 输入格式

输入一个正整数 n（1 ≤ n ≤ 10^9）。

## 输出格式

如果是质数，输出 YES；否则输出 NO。

注意：答案不区分大小写，YES、Yes、yes、Y、y 都算正确。

## 样例输入1

```
7
```

## 样例输出1

```
YES
```

## 样例输入2

```
4
```

## 样例输出2

```
NO
```',
    '["数学", "质数"]',
    '使用试除法判断质数',
    'import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        if (n < 2) {
            System.out.println("NO");
            return;
        }
        for (int i = 2; i * i <= n; i++) {
            if (n % i == 0) {
                System.out.println("NO");
                return;
            }
        }
        System.out.println("YES");
    }
}',
    0,
    0,
    '[{"input":"7","output":"YES"},{"input":"4","output":"NO"},{"input":"1","output":"NO"},{"input":"2","output":"YES"},{"input":"997","output":"YES"}]',
    '{"timeLimit":1000,"memoryLimit":256000,"stackLimit":1000,"inputMode":"single","judgeMode":"IGNORE_CASE"}',
    0,
    0,
    1,
    0
);

-- ============================================
-- 题目3：多解判断 - 奇偶判断
-- ============================================
INSERT INTO question (title, content, tags, answer, sourceCode, submitNum, acceptedNum, judgeCase, judgeConfig, thumbNum, favourNum, userId, isDelete)
VALUES (
    '奇偶判断',
    '## 题目描述

给定一个整数 n，判断它是奇数还是偶数。

## 输入格式

输入一个整数 n（-10^9 ≤ n ≤ 10^9）。

## 输出格式

如果是奇数，输出 ODD；
如果是偶数，输出 EVEN。

注意：ODD/EVEN/奇数/偶数 都算正确答案。

## 样例输入1

```
3
```

## 样例输出1

```
ODD
```

## 样例输入2

```
4
```

## 样例输出2

```
EVEN
```',
    '["基础", "条件判断"]',
    '使用取模运算判断奇偶',
    'import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        if (n % 2 == 0) {
            System.out.println("EVEN");
        } else {
            System.out.println("ODD");
        }
    }
}',
    0,
    0,
    '[{"input":"3","output":"ODD"},{"input":"4","output":"EVEN"},{"input":"0","output":"EVEN"},{"input":"-5","output":"ODD"}]',
    '{"timeLimit":1000,"memoryLimit":256000,"stackLimit":1000,"inputMode":"single","judgeMode":"MULTI_ANSWER","acceptableOutputs":["ODD","EVEN","奇数","偶数","odd","even","Odd","Even"]}',
    0,
    0,
    1,
    0
);

-- ============================================
-- 题目4：忽略空格 - 数字求和
-- ============================================
INSERT INTO question (title, content, tags, answer, sourceCode, submitNum, acceptedNum, judgeCase, judgeConfig, thumbNum, favourNum, userId, isDelete)
VALUES (
    '数字求和',
    '## 题目描述

给定 n 个整数，求它们的和。

## 输入格式

第一行输入一个整数 n（1 ≤ n ≤ 100）。
第二行输入 n 个整数，用空格分隔。

## 输出格式

输出这 n 个整数的和。

## 样例输入

```
5
1 2 3 4 5
```

## 样例输出

```
15
```

## 提示

输出时多余的空格不影响判题结果。',
    '["基础", "数组"]',
    '累加求和',
    'import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int sum = 0;
        for (int i = 0; i < n; i++) {
            sum += sc.nextInt();
        }
        System.out.println(sum);
    }
}',
    0,
    0,
    '[{"input":"5\\n1 2 3 4 5","output":"15"},{"input":"3\\n10 20 30","output":"60"},{"input":"1\\n100","output":"100"}]',
    '{"timeLimit":1000,"memoryLimit":256000,"stackLimit":1000,"inputMode":"single","judgeMode":"IGNORE_SPACE"}',
    0,
    0,
    1,
    0
);

-- ============================================
-- 题目5：综合测试 - 简单计算器
-- ============================================
INSERT INTO question (title, content, tags, answer, sourceCode, submitNum, acceptedNum, judgeCase, judgeConfig, thumbNum, favourNum, userId, isDelete)
VALUES (
    '简单计算器',
    '## 题目描述

实现一个简单计算器，支持加、减、乘、除四种运算。

## 输入格式

输入两个整数 a 和 b，以及一个运算符 op（+、-、*、/），用空格分隔。

## 输出格式

输出运算结果。
- 加减乘运算输出整数结果
- 除法运算输出浮点数结果（保留2位小数）

## 样例输入1

```
5 3 +
```

## 样例输出1

```
8
```

## 样例输入2

```
10 3 /
```

## 样例输出2

```
3.33
```

## 提示

除法运算结果为浮点数，允许 0.01 的误差。',
    '["基础", "模拟"]',
    '根据运算符进行相应计算',
    'import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int a = sc.nextInt();
        int b = sc.nextInt();
        String op = sc.next();

        switch (op) {
            case "+":
                System.out.println(a + b);
                break;
            case "-":
                System.out.println(a - b);
                break;
            case "*":
                System.out.println(a * b);
                break;
            case "/":
                System.out.printf("%.2f", (double) a / b);
                break;
        }
    }
}',
    0,
    0,
    '[{"input":"5 3 +","output":"8"},{"input":"10 3 -","output":"7"},{"input":"4 5 *","output":"20"},{"input":"10 3 /","output":"3.33"}]',
    '{"timeLimit":1000,"memoryLimit":256000,"stackLimit":1000,"inputMode":"single","judgeMode":"FLOAT","floatPrecision":0.01}',
    0,
    0,
    1,
    0
);

-- ============================================
-- 查询插入的题目
-- ============================================
SELECT id, title, JSON_EXTRACT(judgeConfig, '$.judgeMode') as judgeMode
FROM question
WHERE title IN ('计算圆的面积', '判断质数', '奇偶判断', '数字求和', '简单计算器')
ORDER BY id DESC;

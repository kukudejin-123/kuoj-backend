-- 插入样例题目数据
-- 需要先有一个用户（userId=1），如果没有请先创建用户

-- 样例题目1: A+B 问题
INSERT INTO question (title, content, tags, answer, sourceCode, submitNum, acceptedNum, judgeCase, judgeConfig, thumbNum, favourNum, userId, isDelete)
VALUES (
    'A + B 问题',
    '## 题目描述\n\n输入两个整数 A 和 B，输出它们的和。\n\n## 输入格式\n\n一行，包含两个整数 A 和 B。\n\n## 输出格式\n\n输出一个整数，表示 A + B 的结果。\n\n## 样例\n\n**输入：**\n```\n1 2\n```\n\n**输出：**\n```\n3\n```',
    '["入门","数学"]',
    '3',
    'import java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner scanner = new Scanner(System.in);\n        int a = scanner.nextInt();\n        int b = scanner.nextInt();\n        System.out.println(a + b);\n    }\n}',
    0,
    0,
    '[{"input":"1 2","output":"3"},{"input":"5 7","output":"12"},{"input":"-1 1","output":"0"}]',
    '{"timeLimit":1000,"memoryLimit":65536,"stackLimit":8192}',
    0,
    0,
    1,
    0
);

-- 样例题目2: 两数之和
INSERT INTO question (title, content, tags, answer, sourceCode, submitNum, acceptedNum, judgeCase, judgeConfig, thumbNum, favourNum, userId, isDelete)
VALUES (
    '两数之和',
    '## 题目描述\n\n给定一个整数数组 nums 和一个整数目标值 target，请你在该数组中找出和为目标值的那两个整数，并返回它们的数组下标。\n\n## 输入格式\n\n第一行：数组长度 n 和目标值 target\n第二行：n 个整数\n\n## 输出格式\n\n两个整数，表示两个数的下标（从0开始）\n\n## 样例\n\n**输入：**\n```\n4 9\n2 7 11 15\n```\n\n**输出：**\n```\n0 1\n```',
    '["数组","哈希表","中等"]',
    '0 1',
    'import java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner scanner = new Scanner(System.in);\n        int n = scanner.nextInt();\n        int target = scanner.nextInt();\n        int[] nums = new int[n];\n        for (int i = 0; i < n; i++) {\n            nums[i] = scanner.nextInt();\n        }\n        // 在这里编写你的代码\n    }\n}',
    0,
    0,
    '[{"input":"4 9\\n2 7 11 15","output":"0 1"},{"input":"3 6\\n3 2 4","output":"1 2"},{"input":"2 6\\n3 3","output":"0 1"}]',
    '{"timeLimit":1000,"memoryLimit":65536,"stackLimit":8192}',
    0,
    0,
    1,
    0
);

-- 样例题目3: 判断素数
INSERT INTO question (title, content, tags, answer, sourceCode, submitNum, acceptedNum, judgeCase, judgeConfig, thumbNum, favourNum, userId, isDelete)
VALUES (
    '判断素数',
    '## 题目描述\n\n给定一个整数 n，判断它是否为素数。\n\n## 输入格式\n\n一个整数 n\n\n## 输出格式\n\n如果是素数，输出 "YES"；否则输出 "NO"\n\n## 样例\n\n**输入：**\n```\n7\n```\n\n**输出：**\n```\nYES\n```',
    '["数学","入门"]',
    'YES',
    'import java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner scanner = new Scanner(System.in);\n        int n = scanner.nextInt();\n        // 在这里编写你的代码\n    }\n}',
    0,
    0,
    '[{"input":"7","output":"YES"},{"input":"4","output":"NO"},{"input":"2","output":"YES"},{"input":"1","output":"NO"}]',
    '{"timeLimit":1000,"memoryLimit":65536,"stackLimit":8192}',
    0,
    0,
    1,
    0
);

-- 样例题目4: 斐波那契数列
INSERT INTO question (title, content, tags, answer, sourceCode, submitNum, acceptedNum, judgeCase, judgeConfig, thumbNum, favourNum, userId, isDelete)
VALUES (
    '斐波那契数列',
    '## 题目描述\n\n求斐波那契数列的第 n 项。\n\n斐波那契数列定义：F(1) = 1, F(2) = 1, F(n) = F(n-1) + F(n-2)\n\n## 输入格式\n\n一个整数 n\n\n## 输出格式\n\n第 n 项斐波那契数\n\n## 样例\n\n**输入：**\n```\n5\n```\n\n**输出：**\n```\n5\n```',
    '["递归","动态规划","入门"]',
    '5',
    'import java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner scanner = new Scanner(System.in);\n        int n = scanner.nextInt();\n        // 在这里编写你的代码\n    }\n}',
    0,
    0,
    '[{"input":"5","output":"5"},{"input":"10","output":"55"},{"input":"1","output":"1"}]',
    '{"timeLimit":1000,"memoryLimit":65536,"stackLimit":8192}',
    0,
    0,
    1,
    0
);

-- 样例题目5: 字符串反转
INSERT INTO question (title, content, tags, answer, sourceCode, submitNum, acceptedNum, judgeCase, judgeConfig, thumbNum, favourNum, userId, isDelete)
VALUES (
    '字符串反转',
    '## 题目描述\n\n输入一个字符串，将其反转后输出。\n\n## 输入格式\n\n一个字符串\n\n## 输出格式\n\n反转后的字符串\n\n## 样例\n\n**输入：**\n```\nhello\n```\n\n**输出：**\n```\nolleh\n```',
    '["字符串","入门"]',
    'olleh',
    'import java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner scanner = new Scanner(System.in);\n        String s = scanner.nextLine();\n        // 在这里编写你的代码\n    }\n}',
    0,
    0,
    '[{"input":"hello","output":"olleh"},{"input":"abcdef","output":"fedcba"},{"input":"a","output":"a"}]',
    '{"timeLimit":1000,"memoryLimit":65536,"stackLimit":8192}',
    0,
    0,
    1,
    0
);

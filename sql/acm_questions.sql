-- ACM 竞赛风格题目数据
-- 需要先有用户（userId=1），如果没有请先创建用户

-- ============================================================
-- 题目 1: 数字三角形（动态规划入门）
-- ============================================================
INSERT INTO question (title, content, tags, answer, sourceCode, submitNum, acceptedNum, judgeCase, judgeConfig, thumbNum, favourNum, userId, isDelete, difficulty)
VALUES (
    '数字三角形',
    '## 题目描述

给定一个数字三角形，从顶部出发，在每一结点可以选择移动至其左下方的结点或右下方的结点，一直走到底层，要求找出一条路径，使路径上的数字之和最大。

## 输入格式

第一行包含一个整数 n，表示三角形的行数。

接下来的 n 行，每行包含若干个整数，表示数字三角形。第 i 行有 i 个整数。

## 输出格式

输出一个整数，表示路径上的最大数字之和。

## 样例

**输入：**
```
5
7
3 8
8 1 0
2 7 4 4
4 5 2 6 5
```

**输出：**
```
30
```

## 数据范围

1 ≤ n ≤ 100
三角形中的数字均为非负整数，且不超过 100',
    '["动态规划","中等"]',
    '30',
    'import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();
        int[][] a = new int[n + 1][n + 1];
        int[][] dp = new int[n + 1][n + 1];

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= i; j++) {
                a[i][j] = scanner.nextInt();
            }
        }

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= i; j++) {
                dp[i][j] = Math.max(dp[i-1][j-1], dp[i-1][j]) + a[i][j];
            }
        }

        int ans = 0;
        for (int j = 1; j <= n; j++) {
            ans = Math.max(ans, dp[n][j]);
        }
        System.out.println(ans);
    }
}',
    0,
    0,
    '[{"input":"5\\n7\\n3 8\\n8 1 0\\n2 7 4 4\\n4 5 2 6 5","output":"30"},{"input":"3\\n1\\n2 3\\n4 5 6","output":"10"},{"input":"1\\n5","output":"5"}]',
    '{"timeLimit":1000,"memoryLimit":131072,"stackLimit":8192,"inputMode":"single","judgeMode":"DEFAULT"}',
    0,
    0,
    1,
    0,
    2
);

-- ============================================================
-- 题目 2: 01背包问题（动态规划经典）
-- ============================================================
INSERT INTO question (title, content, tags, answer, sourceCode, submitNum, acceptedNum, judgeCase, judgeConfig, thumbNum, favourNum, userId, isDelete, difficulty)
VALUES (
    '01背包问题',
    '## 题目描述

有 N 件物品和一个容量为 V 的背包。第 i 件物品的体积是 v[i]，价值是 w[i]。

每件物品只能使用一次，求解将哪些物品装入背包，可使这些物品的总体积不超过背包容量，且总价值最大。输出最大价值。

## 输入格式

第一行两个整数 N 和 V，用空格隔开，分别表示物品数量和背包容积。

接下来有 N 行，每行两个整数 v[i] 和 w[i]，用空格隔开，分别表示第 i 件物品的体积和价值。

## 输出格式

输出一个整数，表示最大价值。

## 样例

**输入：**
```
4 5
1 2
2 4
3 4
4 5
```

**输出：**
```
8
```

## 数据范围

0 < N, V ≤ 1000
0 < v[i], w[i] ≤ 1000',
    '["动态规划","中等"]',
    '8',
    'import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int N = scanner.nextInt();
        int V = scanner.nextInt();
        int[] v = new int[N + 1];
        int[] w = new int[N + 1];
        int[] dp = new int[V + 1];

        for (int i = 1; i <= N; i++) {
            v[i] = scanner.nextInt();
            w[i] = scanner.nextInt();
        }

        for (int i = 1; i <= N; i++) {
            for (int j = V; j >= v[i]; j--) {
                dp[j] = Math.max(dp[j], dp[j - v[i]] + w[i]);
            }
        }

        System.out.println(dp[V]);
    }
}',
    0,
    0,
    '[{"input":"4 5\\n1 2\\n2 4\\n3 4\\n4 5","output":"8"},{"input":"3 10\\n3 4\\n4 5\\n5 6","output":"11"},{"input":"1 1\\n2 3","output":"0"}]',
    '{"timeLimit":1000,"memoryLimit":131072,"stackLimit":8192,"inputMode":"single","judgeMode":"DEFAULT"}',
    0,
    0,
    1,
    0,
    2
);

-- ============================================================
-- 题目 3: 括号匹配（栈）
-- ============================================================
INSERT INTO question (title, content, tags, answer, sourceCode, submitNum, acceptedNum, judgeCase, judgeConfig, thumbNum, favourNum, userId, isDelete, difficulty)
VALUES (
    '括号匹配',
    '## 题目描述

给定一个只包含括号的字符串，判断括号是否匹配有效。

有效字符串需满足：
1. 左括号必须用相同类型的右括号闭合
2. 左括号必须以正确的顺序闭合

## 输入格式

一行，包含一个只包含括号的字符串。

## 输出格式

如果括号匹配，输出 "YES"；否则输出 "NO"。

## 样例

**输入：**
```
()[]{}
```

**输出：**
```
YES
```

**输入：**
```
([)]
```

**输出：**
```
NO
```

## 数据范围

字符串长度不超过 10^5',
    '["栈","简单"]',
    'YES',
    'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String s = scanner.nextLine();
        Stack<Character> stack = new Stack<>();

        for (char c : s.toCharArray()) {
            if (c == \'(\' || c == \'[\' || c == \'{\') {
                stack.push(c);
            } else {
                if (stack.isEmpty()) {
                    System.out.println("NO");
                    return;
                }
                char top = stack.pop();
                if ((c == \')\' && top != \'(\') ||
                    (c == \']\' && top != \'[\') ||
                    (c == \'}\' && top != \'{\')) {
                    System.out.println("NO");
                    return;
                }
            }
        }

        System.out.println(stack.isEmpty() ? "YES" : "NO");
    }
}',
    0,
    0,
    '[{"input":"()[]{}","output":"YES"},{"input":"([)]","output":"NO"},{"input":"((()))","output":"YES"},{"input":"(","output":"NO"},{"input":"","output":"YES"}]',
    '{"timeLimit":1000,"memoryLimit":65536,"stackLimit":8192,"inputMode":"single","judgeMode":"DEFAULT"}',
    0,
    0,
    1,
    0,
    1
);

-- ============================================================
-- 题目 4: 最大子数组和（贪心/动态规划）
-- ============================================================
INSERT INTO question (title, content, tags, answer, sourceCode, submitNum, acceptedNum, judgeCase, judgeConfig, thumbNum, favourNum, userId, isDelete, difficulty)
VALUES (
    '最大子数组和',
    '## 题目描述

给定一个整数数组 nums，找到一个具有最大和的连续子数组（子数组最少包含一个元素），返回其最大和。

## 输入格式

第一行一个整数 n，表示数组长度。
第二行 n 个整数，表示数组元素。

## 输出格式

输出一个整数，表示最大子数组和。

## 样例

**输入：**
```
9
-2 1 -3 4 -1 2 1 -5 4
```

**输出：**
```
6
```

**解释：** 连续子数组 [4,-1,2,1] 的和最大，为 6。

## 数据范围

1 ≤ n ≤ 10^5
-10^4 ≤ nums[i] ≤ 10^4',
    '["动态规划","贪心","中等"]',
    '6',
    'import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();
        int[] nums = new int[n];

        for (int i = 0; i < n; i++) {
            nums[i] = scanner.nextInt();
        }

        int maxSum = nums[0];
        int currentSum = nums[0];

        for (int i = 1; i < n; i++) {
            currentSum = Math.max(nums[i], currentSum + nums[i]);
            maxSum = Math.max(maxSum, currentSum);
        }

        System.out.println(maxSum);
    }
}',
    0,
    0,
    '[{"input":"9\\n-2 1 -3 4 -1 2 1 -5 4","output":"6"},{"input":"1\\n-1","output":"-1"},{"input":"5\\n5 4 -1 7 8","output":"23"}]',
    '{"timeLimit":1000,"memoryLimit":65536,"stackLimit":8192,"inputMode":"single","judgeMode":"DEFAULT"}',
    0,
    0,
    1,
    0,
    2
);

-- ============================================================
-- 题目 5: 二分查找
-- ============================================================
INSERT INTO question (title, content, tags, answer, sourceCode, submitNum, acceptedNum, judgeCase, judgeConfig, thumbNum, favourNum, userId, isDelete, difficulty)
VALUES (
    '二分查找',
    '## 题目描述

给定一个 n 个元素有序的（升序）整型数组 nums 和一个目标值 target，写一个函数搜索 nums 中的 target，如果目标值存在返回下标，否则返回 -1。

## 输入格式

第一行两个整数 n 和 target，分别表示数组长度和目标值。
第二行 n 个升序排列的整数。

## 输出格式

如果找到目标值，输出其在数组中的下标（从0开始）；否则输出 -1。

## 样例

**输入：**
```
6 9
-1 0 3 5 9 12
```

**输出：**
```
4
```

## 数据范围

1 ≤ n ≤ 10^4
-10^4 ≤ nums[i], target ≤ 10^4
nums 按升序排列',
    '["二分查找","简单"]',
    '4',
    'import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();
        int target = scanner.nextInt();
        int[] nums = new int[n];

        for (int i = 0; i < n; i++) {
            nums[i] = scanner.nextInt();
        }

        int left = 0, right = n - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] == target) {
                System.out.println(mid);
                return;
            } else if (nums[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        System.out.println(-1);
    }
}',
    0,
    0,
    '[{"input":"6 9\\n-1 0 3 5 9 12","output":"4"},{"input":"6 2\\n-1 0 3 5 9 12","output":"-1"},{"input":"1 5\\n5","output":"0"}]',
    '{"timeLimit":1000,"memoryLimit":65536,"stackLimit":8192,"inputMode":"single","judgeMode":"DEFAULT"}',
    0,
    0,
    1,
    0,
    1
);

-- ============================================================
-- 题目 6: 爬楼梯（动态规划）
-- ============================================================
INSERT INTO question (title, content, tags, answer, sourceCode, submitNum, acceptedNum, judgeCase, judgeConfig, thumbNum, favourNum, userId, isDelete, difficulty)
VALUES (
    '爬楼梯',
    '## 题目描述

假设你正在爬楼梯。需要 n 阶你才能到达楼顶。

每次你可以爬 1 或 2 个台阶。你有多少种不同的方法可以爬到楼顶？

## 输入格式

一个整数 n，表示楼梯的阶数。

## 输出格式

输出爬到楼顶的方法数。

## 样例

**输入：**
```
3
```

**输出：**
```
3
```

**解释：**
- 方法1：1阶 + 1阶 + 1阶
- 方法2：1阶 + 2阶
- 方法3：2阶 + 1阶

## 数据范围

1 ≤ n ≤ 45',
    '["动态规划","简单"]',
    '3',
    'import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();

        if (n <= 2) {
            System.out.println(n);
            return;
        }

        int a = 1, b = 2;
        for (int i = 3; i <= n; i++) {
            int c = a + b;
            a = b;
            b = c;
        }
        System.out.println(b);
    }
}',
    0,
    0,
    '[{"input":"3","output":"3"},{"input":"1","output":"1"},{"input":"5","output":"8"},{"input":"10","output":"89"}]',
    '{"timeLimit":1000,"memoryLimit":65536,"stackLimit":8192,"inputMode":"single","judgeMode":"DEFAULT"}',
    0,
    0,
    1,
    0,
    1
);

-- ============================================================
-- 题目 7: 合并两个有序链表（链表）
-- ============================================================
INSERT INTO question (title, content, tags, answer, sourceCode, submitNum, acceptedNum, judgeCase, judgeConfig, thumbNum, favourNum, userId, isDelete, difficulty)
VALUES (
    '合并两个有序数组',
    '## 题目描述

给定两个有序整数数组 nums1 和 nums2，将 nums2 合并到 nums1 中，使得 nums1 成为一个有序数组。

输出合并后的有序数组。

## 输入格式

第一行两个整数 m 和 n，分别表示 nums1 和 nums2 的有效元素个数。
第二行 m 个整数，表示 nums1 的元素。
第三行 n 个整数，表示 nums2 的元素。

## 输出格式

输出一行，包含 m+n 个升序排列的整数。

## 样例

**输入：**
```
3 3
1 2 3
2 5 6
```

**输出：**
```
1 2 2 3 5 6
```

## 数据范围

-10^9 ≤ nums1[i], nums2[j] ≤ 10^9
0 ≤ m, n ≤ 10^4',
    '["数组","双指针","简单"]',
    '1 2 2 3 5 6',
    'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int m = scanner.nextInt();
        int n = scanner.nextInt();

        int[] nums1 = new int[m];
        int[] nums2 = new int[n];

        for (int i = 0; i < m; i++) nums1[i] = scanner.nextInt();
        for (int i = 0; i < n; i++) nums2[i] = scanner.nextInt();

        int[] result = new int[m + n];
        int i = 0, j = 0, k = 0;

        while (i < m && j < n) {
            if (nums1[i] <= nums2[j]) {
                result[k++] = nums1[i++];
            } else {
                result[k++] = nums2[j++];
            }
        }

        while (i < m) result[k++] = nums1[i++];
        while (j < n) result[k++] = nums2[j++];

        StringBuilder sb = new StringBuilder();
        for (int x = 0; x < result.length; x++) {
            if (x > 0) sb.append(" ");
            sb.append(result[x]);
        }
        System.out.println(sb.toString());
    }
}',
    0,
    0,
    '[{"input":"3 3\\n1 2 3\\n2 5 6","output":"1 2 2 3 5 6"},{"input":"1 0\\n1\\n","output":"1"},{"input":"0 2\\n\\n1 2","output":"1 2"}]',
    '{"timeLimit":1000,"memoryLimit":65536,"stackLimit":8192,"inputMode":"single","judgeMode":"IGNORE_SPACE"}',
    0,
    0,
    1,
    0,
    1
);

-- ============================================================
-- 题目 8: 最长公共子序列（动态规划）
-- ============================================================
INSERT INTO question (title, content, tags, answer, sourceCode, submitNum, acceptedNum, judgeCase, judgeConfig, thumbNum, favourNum, userId, isDelete, difficulty)
VALUES (
    '最长公共子序列',
    '## 题目描述

给定两个字符串 text1 和 text2，返回这两个字符串的最长公共子序列的长度。

子序列: 一个字符串的子序列是指这样一个新的字符串：它是由原字符串在不改变字符的相对顺序的情况下删除某些字符（也可以不删除任何字符）后组成的新字符串。

## 输入格式

两行，每行一个字符串。

## 输出格式

输出一个整数，表示最长公共子序列的长度。

## 样例

**输入：**
```
abcde
ace
```

**输出：**
```
3
```

**解释：** 最长公共子序列是 "ace"，它的长度为 3。

## 数据范围

1 ≤ text1.length, text2.length ≤ 1000
字符串只包含小写英文字母',
    '["动态规划","中等"]',
    '3',
    'import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String text1 = scanner.nextLine();
        String text2 = scanner.nextLine();

        int m = text1.length();
        int n = text2.length();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        System.out.println(dp[m][n]);
    }
}',
    0,
    0,
    '[{"input":"abcde\\nace","output":"3"},{"input":"abc\\nabc","output":"3"},{"input":"abc\\ndef","output":"0"}]',
    '{"timeLimit":1000,"memoryLimit":131072,"stackLimit":8192,"inputMode":"single","judgeMode":"DEFAULT"}',
    0,
    0,
    1,
    0,
    2
);

-- ============================================================
-- 题目 9: 阶乘末尾零的个数（数学）
-- ============================================================
INSERT INTO question (title, content, tags, answer, sourceCode, submitNum, acceptedNum, judgeCase, judgeConfig, thumbNum, favourNum, userId, isDelete, difficulty)
VALUES (
    '阶乘末尾零的个数',
    '## 题目描述

给定一个整数 n，返回 n! 结果中尾随零的数量。

## 输入格式

一个整数 n。

## 输出格式

输出 n! 尾随零的数量。

## 样例

**输入：**
```
5
```

**输出：**
```
1
```

**解释：** 5! = 120，有一个尾随零。

## 提示

n! 尾随零的数量等于 n! 中因子 5 的个数。

## 数据范围

0 ≤ n ≤ 10^9',
    '["数学","中等"]',
    '1',
    'import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();

        int count = 0;
        while (n >= 5) {
            n /= 5;
            count += n;
        }

        System.out.println(count);
    }
}',
    0,
    0,
    '[{"input":"5","output":"1"},{"input":"10","output":"2"},{"input":"25","output":"6"},{"input":"0","output":"0"}]',
    '{"timeLimit":1000,"memoryLimit":65536,"stackLimit":8192,"inputMode":"single","judgeMode":"DEFAULT"}',
    0,
    0,
    1,
    0,
    2
);

-- ============================================================
-- 题目 10: 快速排序第K大的数
-- ============================================================
INSERT INTO question (title, content, tags, answer, sourceCode, submitNum, acceptedNum, judgeCase, judgeConfig, thumbNum, favourNum, userId, isDelete, difficulty)
VALUES (
    '第K大的数',
    '## 题目描述

给定一个无序数组，找到数组中第 k 大的元素。

## 输入格式

第一行两个整数 n 和 k。
第二行 n 个整数，表示数组元素。

## 输出格式

输出第 k 大的元素。

## 样例

**输入：**
```
6 2
3 2 1 5 6 4
```

**输出：**
```
5
```

## 数据范围

1 ≤ k ≤ n ≤ 10^5
-10^4 ≤ nums[i] ≤ 10^4',
    '["排序","分治","中等"]',
    '5',
    'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();
        int k = scanner.nextInt();
        int[] nums = new int[n];

        for (int i = 0; i < n; i++) {
            nums[i] = scanner.nextInt();
        }

        // 降序排序，找第k大
        Integer[] arr = new Integer[n];
        for (int i = 0; i < n; i++) arr[i] = nums[i];
        Arrays.sort(arr, Collections.reverseOrder());

        System.out.println(arr[k - 1]);
    }
}',
    0,
    0,
    '[{"input":"6 2\\n3 2 1 5 6 4","output":"5"},{"input":"3 1\\n1 2 3","output":"3"},{"input":"5 5\\n5 4 3 2 1","output":"1"}]',
    '{"timeLimit":1000,"memoryLimit":65536,"stackLimit":8192,"inputMode":"single","judgeMode":"DEFAULT"}',
    0,
    0,
    1,
    0,
    2
);

-- ============================================================
-- 题目 11: 无重复字符的最长子串（滑动窗口）
-- ============================================================
INSERT INTO question (title, content, tags, answer, sourceCode, submitNum, acceptedNum, judgeCase, judgeConfig, thumbNum, favourNum, userId, isDelete, difficulty)
VALUES (
    '无重复字符的最长子串',
    '## 题目描述

给定一个字符串 s，请你找出其中不含有重复字符的最长子串的长度。

## 输入格式

一个字符串 s。

## 输出格式

输出一个整数，表示无重复字符的最长子串的长度。

## 样例

**输入：**
```
abcabcbb
```

**输出：**
```
3
```

**解释：** 因为无重复字符的最长子串是 "abc"，所以其长度为 3。

## 数据范围

0 ≤ s.length ≤ 5 * 10^4
s 由英文字母、数字、符号和空格组成',
    '["滑动窗口","哈希表","中等"]',
    '3',
    'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String s = scanner.nextLine();

        Set<Character> set = new HashSet<>();
        int maxLen = 0;
        int left = 0;

        for (int right = 0; right < s.length(); right++) {
            while (set.contains(s.charAt(right))) {
                set.remove(s.charAt(left));
                left++;
            }
            set.add(s.charAt(right));
            maxLen = Math.max(maxLen, right - left + 1);
        }

        System.out.println(maxLen);
    }
}',
    0,
    0,
    '[{"input":"abcabcbb","output":"3"},{"input":"bbbbb","output":"1"},{"input":"pwwkew","output":"3"},{"input":"","output":"0"}]',
    '{"timeLimit":1000,"memoryLimit":65536,"stackLimit":8192,"inputMode":"single","judgeMode":"DEFAULT"}',
    0,
    0,
    1,
    0,
    2
);

-- ============================================================
-- 题目 12: 计算阶乘（高精度）
-- ============================================================
INSERT INTO question (title, content, tags, answer, sourceCode, submitNum, acceptedNum, judgeCase, judgeConfig, thumbNum, favourNum, userId, isDelete, difficulty)
VALUES (
    '计算阶乘',
    '## 题目描述

计算 n 的阶乘，即 n! = 1 × 2 × 3 × ... × n。

## 输入格式

一个整数 n（0 ≤ n ≤ 100）。

## 输出格式

输出 n! 的值。

## 样例

**输入：**
```
10
```

**输出：**
```
3628800
```

## 数据范围

0 ≤ n ≤ 100

注意：当 n > 20 时，阶乘结果会超过 long 的范围，需要使用 BigInteger。',
    '["高精度","简单"]',
    '3628800',
    'import java.math.BigInteger;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();

        BigInteger result = BigInteger.ONE;
        for (int i = 2; i <= n; i++) {
            result = result.multiply(BigInteger.valueOf(i));
        }

        System.out.println(result);
    }
}',
    0,
    0,
    '[{"input":"10","output":"3628800"},{"input":"0","output":"1"},{"input":"20","output":"2432902008176640000"}]',
    '{"timeLimit":1000,"memoryLimit":65536,"stackLimit":8192,"inputMode":"single","judgeMode":"DEFAULT"}',
    0,
    0,
    1,
    0,
    1
);

-- ============================================================
-- 题目 13: 反转链表（链表）
-- ============================================================
INSERT INTO question (title, content, tags, answer, sourceCode, submitNum, acceptedNum, judgeCase, judgeConfig, thumbNum, favourNum, userId, isDelete, difficulty)
VALUES (
    '反转链表',
    '## 题目描述

给定一个链表，反转该链表，并返回反转后的链表。

用数组模拟链表，输入一个链表数组，输出反转后的数组。

## 输入格式

第一行一个整数 n，表示链表节点数。
第二行 n 个整数，表示链表各节点的值。

## 输出格式

输出反转后链表的各节点值，以空格分隔。

## 样例

**输入：**
```
5
1 2 3 4 5
```

**输出：**
```
5 4 3 2 1
```

## 数据范围

1 ≤ n ≤ 5000',
    '["链表","简单"]',
    '5 4 3 2 1',
    'import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();
        int[] arr = new int[n];

        for (int i = 0; i < n; i++) {
            arr[i] = scanner.nextInt();
        }

        StringBuilder sb = new StringBuilder();
        for (int i = n - 1; i >= 0; i--) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(arr[i]);
        }
        System.out.println(sb.toString());
    }
}',
    0,
    0,
    '[{"input":"5\\n1 2 3 4 5","output":"5 4 3 2 1"},{"input":"1\\n1","output":"1"},{"input":"3\\n10 20 30","output":"30 20 10"}]',
    '{"timeLimit":1000,"memoryLimit":65536,"stackLimit":8192,"inputMode":"single","judgeMode":"IGNORE_SPACE"}',
    0,
    0,
    1,
    0,
    1
);

-- ============================================================
-- 题目 14: 二叉树的最大深度（树）
-- ============================================================
INSERT INTO question (title, content, tags, answer, sourceCode, submitNum, acceptedNum, judgeCase, judgeConfig, thumbNum, favourNum, userId, isDelete, difficulty)
VALUES (
    '二叉树的最大深度',
    '## 题目描述

给定一个二叉树，找出其最大深度。

二叉树的深度为根节点到最远叶子节点的最长路径上的节点数。

用数组表示二叉树，空节点用 -1 表示。数组按层序遍历顺序给出。

## 输入格式

第一行一个整数 n，表示数组长度。
第二行 n 个整数，表示二叉树的层序遍历序列（-1 表示空节点）。

## 输出格式

输出二叉树的最大深度。

## 样例

**输入：**
```
7
3 9 20 -1 -1 15 7
```

**输出：**
```
3
```

**解释：** 二叉树结构：
```
    3
   / \\
  9  20
    /  \\
   15   7
```
最大深度为 3。

## 数据范围

1 ≤ n ≤ 10^4',
    '["树","递归","简单"]',
    '3',
    'import java.util.Scanner;

public class Main {
    static int[] tree;
    static int n;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        n = scanner.nextInt();
        tree = new int[n];

        for (int i = 0; i < n; i++) {
            tree[i] = scanner.nextInt();
        }

        System.out.println(maxDepth(0));
    }

    static int maxDepth(int index) {
        if (index >= n || tree[index] == -1) {
            return 0;
        }
        int leftDepth = maxDepth(2 * index + 1);
        int rightDepth = maxDepth(2 * index + 2);
        return Math.max(leftDepth, rightDepth) + 1;
    }
}',
    0,
    0,
    '[{"input":"7\\n3 9 20 -1 -1 15 7","output":"3"},{"input":"1\\n1","output":"1"},{"input":"3\\n1 -1 2","output":"2"}]',
    '{"timeLimit":1000,"memoryLimit":65536,"stackLimit":8192,"inputMode":"single","judgeMode":"DEFAULT"}',
    0,
    0,
    1,
    0,
    1
);

-- ============================================================
-- 题目 15: 浮点数平方根（二分+浮点精度）
-- ============================================================
INSERT INTO question (title, content, tags, answer, sourceCode, submitNum, acceptedNum, judgeCase, judgeConfig, thumbNum, favourNum, userId, isDelete, difficulty)
VALUES (
    '平方根',
    '## 题目描述

给定一个非负整数 x，计算并返回 x 的平方根。

结果只保留整数部分，小数部分将被舍去。

## 输入格式

一个非负整数 x。

## 输出格式

输出 x 的平方根的整数部分。

## 样例

**输入：**
```
8
```

**输出：**
```
2
```

**解释：** 8 的平方根是 2.82842...，返回整数部分 2。

## 数据范围

0 ≤ x ≤ 2^31 - 1',
    '["二分查找","数学","简单"]',
    '2',
    'import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int x = scanner.nextInt();

        if (x == 0) {
            System.out.println(0);
            return;
        }

        int left = 1, right = x;
        int result = 0;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (mid <= x / mid) {
                result = mid;
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        System.out.println(result);
    }
}',
    0,
    0,
    '[{"input":"8","output":"2"},{"input":"4","output":"2"},{"input":"1","output":"1"},{"input":"0","output":"0"}]',
    '{"timeLimit":1000,"memoryLimit":65536,"stackLimit":8192,"inputMode":"single","judgeMode":"DEFAULT"}',
    0,
    0,
    1,
    0,
    1
);

-- ============================================================
-- 题目 16: 循环输入求和（循环输入模式示例）
-- ============================================================
INSERT INTO question (title, content, tags, answer, sourceCode, submitNum, acceptedNum, judgeCase, judgeConfig, thumbNum, favourNum, userId, isDelete, difficulty)
VALUES (
    '多组输入求和',
    '## 题目描述

给定多组测试数据，每组数据包含两个整数 a 和 b，请计算 a + b 的值。

输入包含多组数据，每组数据占一行，每行包含两个整数 a 和 b。

对于每组数据，输出 a + b 的值，每个结果占一行。

## 输入格式

多组输入，每组两个整数 a 和 b，以空格分隔。当 a 和 b 都为 0 时结束输入。

## 输出格式

对于每组输入，输出 a + b 的结果。

## 样例

**输入：**
```
1 2
3 4
0 0
```

**输出：**
```
3
7
```

## 数据范围

0 ≤ a, b ≤ 10^9',
    '["循环输入","入门"]',
    '3\n7',
    'import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            int a = scanner.nextInt();
            int b = scanner.nextInt();
            if (a == 0 && b == 0) break;
            System.out.println(a + b);
        }
    }
}',
    0,
    0,
    '[{"input":"1 2","output":"3"},{"input":"3 4","output":"7"}]',
    '{"timeLimit":1000,"memoryLimit":65536,"stackLimit":8192,"inputMode":"loop","judgeMode":"DEFAULT"}',
    0,
    0,
    1,
    0,
    0
);

-- ============================================================
-- 题目 17: 忽略空格比较（判题模式示例）
-- ============================================================
INSERT INTO question (title, content, tags, answer, sourceCode, submitNum, acceptedNum, judgeCase, judgeConfig, thumbNum, favourNum, userId, isDelete, difficulty)
VALUES (
    '输出格式练习',
    '## 题目描述

给定 n 个整数，按要求输出。

## 输入格式

第一行一个整数 n。
第二行 n 个整数。

## 输出格式

输出这 n 个整数，每个整数后跟一个空格。

## 样例

**输入：**
```
3
1 2 3
```

**输出：**
```
1 2 3
```

注意：输出每个数字后有空格也可以接受。

## 数据范围

1 ≤ n ≤ 100
1 ≤ ai ≤ 1000',
    '["输出格式","入门"]',
    '1 2 3 ',
    'import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();
        for (int i = 0; i < n; i++) {
            int x = scanner.nextInt();
            System.out.print(x + " ");
        }
    }
}',
    0,
    0,
    '[{"input":"3\\n1 2 3","output":"1 2 3 "},{"input":"5\\n10 20 30 40 50","output":"10 20 30 40 50 "}]',
    '{"timeLimit":1000,"memoryLimit":65536,"stackLimit":8192,"inputMode":"single","judgeMode":"IGNORE_SPACE"}',
    0,
    0,
    1,
    0,
    0
);

-- ============================================================
-- 题目 18: 浮点数计算（浮点判题模式示例）
-- ============================================================
INSERT INTO question (title, content, tags, answer, sourceCode, submitNum, acceptedNum, judgeCase, judgeConfig, thumbNum, favourNum, userId, isDelete, difficulty)
VALUES (
    '圆的面积',
    '## 题目描述

给定圆的半径 r，求圆的面积。

π 取 3.14159265358979323846

## 输入格式

一个实数 r，表示圆的半径。

## 输出格式

输出圆的面积，保留小数点后 6 位。

## 样例

**输入：**
```
2.0
```

**输出：**
```
12.566371
```

## 数据范围

0 < r ≤ 10000',
    '["数学","浮点数","简单"]',
    '12.566370614359172',
    'import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        double r = scanner.nextDouble();
        double PI = 3.14159265358979323846;
        double area = PI * r * r;
        System.out.printf("%.6f", area);
    }
}',
    0,
    0,
    '[{"input":"2.0","output":"12.566371"},{"input":"1.0","output":"3.141593"},{"input":"3.5","output":"38.484510"}]',
    '{"timeLimit":1000,"memoryLimit":65536,"stackLimit":8192,"inputMode":"single","judgeMode":"FLOAT","floatPrecision":1e-6}',
    0,
    0,
    1,
    0,
    1
);

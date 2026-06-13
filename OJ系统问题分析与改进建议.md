# OJ在线判题系统 - 问题分析与改进建议

> 文档生成日期：2026-06-08
> 分析范围：核心判题模块、用户模块、社区模块、系统架构、前端界面

---

## 目录

1. [严重Bug](#一严重bug)
2. [代码层面问题](#二代码层面问题)
3. [功能设计缺陷](#三功能设计缺陷)
4. [前端问题与不足](#四前端问题与不足)
5. [与主流OJ对比的不足](#五与主流oj对比的不足)
6. [改进优先级建议](#六改进优先级建议)

---

## 一、严重Bug

### Bug 1: JudgeServiceImpl 查询提交记录用错ID

**状态**: ✅ 已修复

**位置**: `JudgeServiceImpl.java:107`

```java
// 错误代码
QuestionSubmit questionSubmitResult = questionSubmitService.getById(questionId);

// 正确代码
QuestionSubmit questionSubmitResult = questionSubmitService.getById(questionSubmitId);
```

**影响**: 返回的是题目信息而非提交记录，导致前端展示错误数据或返回null。

**修复**: 将 `questionId` 改为 `questionSubmitId`。

---

### Bug 2: 判题异常未捕获，提交记录卡死

**状态**: ✅ 已修复

**位置**: `JudgeServiceImpl.java:72-86`

```java
// 当前代码 - 没有try-catch
codeSandbox = new CodeSandboxProxy(codeSandbox);
ExecuteCodeRequest excuteCodeRequest = ExecuteCodeRequest.builder()
        .code(code)
        .language(language)
        .inputList(inputList)
        .build();
ExecuteCodeResponse excuteCodeResponse = codeSandbox.executeCode(excuteCodeRequest);
```

**影响**:
- 沙箱执行异常（编译错误、沙箱崩溃、网络超时）时，提交记录永远停留在 `RUNNING` 状态
- 用户无法感知判题失败
- 数据库状态不一致

**修复建议**:
```java
try {
    ExecuteCodeResponse excuteCodeResponse = codeSandbox.executeCode(excuteCodeRequest);
    // ... 判题逻辑 ...
} catch (Exception e) {
    log.error("判题失败, submitId={}", questionSubmitId, e);
    questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
    questionSubmitService.updateById(questionSubmitUpdate);
}
```

---

### Bug 3: CodeSandboxProxy 空指针风险

**状态**: ✅ 已修复

**位置**: `CodeSandboxProxy.java:21`

```java
// 当前代码
ExecuteCodeResponse excuteCodeResponse = codeSandbox.executeCode(excuteCodeRequest);
log.info("代码沙箱响应信息" + excuteCodeResponse.toString());  // NPE风险
```

**影响**: 如果沙箱返回null（如 ThirdPartyCodeSandbox 直接返回null），会抛出 NullPointerException。

**修复建议**:
```java
ExecuteCodeResponse excuteCodeResponse = codeSandbox.executeCode(excuteCodeRequest);
if (excuteCodeResponse != null) {
    log.info("代码沙箱响应信息: {}", excuteCodeResponse);
} else {
    log.warn("代码沙箱返回 null");
    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "沙箱执行失败");
}
```

---

### Bug 4: 判题状态与结果混淆

**位置**: `JudgeServiceImpl.java:99-101`

```java
// 当前代码 - 无论判题结果如何，状态都是 SUCCEED
questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
questionSubmitUpdate.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));
```

**影响**:
- 用户代码超时(TLE)、答案错误(WA)、内存超限(MLE)时，系统状态显示"成功"
- 无法区分"判题执行成功"和"答案正确"
- 缺少 FAILED 状态的正确使用场景

**修复建议**: 增加 `result` 字段表示判题结果（AC/WA/TLE/MLE/CE/RE），`status` 只表示判题流程状态。

---

### Bug 5: ExampleCodeSandbox 是假实现

**位置**: `ExampleCodeSandbox.java`

```java
// 当前代码 - 直接返回输入作为输出
excuteCodeResponse.setOutputList(inputList);
excuteCodeResponse.setMessage("测试执行成功");
```

**影响**: 如果误配到生产环境，所有提交都会通过。

**修复建议**: 启动时检查沙箱类型，如果是 example 模式且是生产环境则报错。

---

### Bug 6: 用户注册参数为空返回null

**状态**: ✅ 已修复

**位置**: `UserController.java:75-77`

```java
if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
    return null;  // 返回null而不是报错
}
```

**影响**: 前端收到 200 OK 但 data 为 null，而不是明确的错误信息。

**修复建议**:
```java
if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
    throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
}
```

---

### Bug 7: 用户更新接口可越权修改角色

**状态**: ✅ 已修复

**位置**: `UserController.java:303-316`

```java
@PostMapping("/update/my")
public BaseResponse<Boolean> updateMyUser(@RequestBody UserUpdateMyRequest userUpdateMyRequest, ...) {
    User user = new User();
    BeanUtils.copyProperties(userUpdateMyRequest, user);
    user.setId(loginUser.getId());
    boolean result = userService.updateById(user);  // 可能更新userRole
```

**影响**: 如果 `UserUpdateMyRequest` 包含 `userRole` 字段，用户可以修改自己的角色为 admin。

**修复建议**: 使用白名单方式更新字段，或专门定义只允许修改的字段。

---

## 四、前端问题与不足

### 4.1 严重Bug

#### Bug 8: GlobalHeader 组件存在硬编码的模拟登录逻辑

**位置**: `GlobalHeader.vue:78-83`

```javascript
setTimeout(() => {
  store.dispatch("user/getLoginUser", {
    userName: "管理员",
    userRole: ACCESS_ENUM.ADMIN,
  });
}, 3000);
```

**影响**:
- 页面加载3秒后自动模拟管理员登录
- 任何用户访问页面3秒后都会变成管理员权限
- 这是开发测试代码，应该删除

**修复建议**: 删除这段代码。

---

#### Bug 9: QuestionsView 通过率计算写死为固定值

**状态**: ✅ 已修复

**位置**: `QuestionsView.vue:80`

```javascript
const rate = ref(5/8);  // 写死为62.5%
```

**影响**:
- 所有题目的通过率都显示为62.5%
- 实际通过率数据没有使用
- 用户无法看到真实的通过率

**修复建议**:
```javascript
// 使用实际数据计算
const calculateRate = (record: any) => {
  if (!record.submitNum || record.submitNum === 0) return 0;
  return Math.round((record.acceptedNum / record.submitNum) * 100);
};
```

---

#### Bug 10: QuestionSubmitView 表格列数据索引错误

**位置**: `QuestionSubmitView.vue:100-101`

```javascript
{
  title: "题目标题",
  dataIndex: "questionVO.title",  // 这种嵌套索引方式在 Arco Table 中可能无效
}
```

**影响**:
- 题目列表中题目标题可能显示为空
- 提交者名称也可能无法显示

**修复建议**: 使用自定义 slot 渲染嵌套数据。

---

#### Bug 11: AddQuestionView 表单缺少验证

**位置**: `AddQuestionView.vue`

**问题**:
- 创建题目时表单没有必填验证
- 时间限制、内存限制可以为负数或0
- 测试用例输入输出可以为空
- 提交后没有跳转或重置表单

**影响**:
- 可能创建无效题目
- 用户操作后没有明确反馈

**修复建议**: 增加表单验证规则。

---

#### Bug 12: ViewQuestionView 缺少防重复提交

**状态**: ✅ 已修复

**位置**: `ViewQuestionView.vue:116-131`

```javascript
const doSubmit = async () => {
  // ... 直接提交，没有防重复机制
  const res = await QuestionControllerService.doQuestionSubmitUsingPost({
    ...form.value,
    questionId: question.value.id,
  });
```

**影响**:
- 用户可以快速连续点击提交按钮
- 产生大量重复提交记录
- 后端判题队列被占满

**修复建议**: 增加提交状态锁。
```javascript
const isSubmitting = ref(false);
const doSubmit = async () => {
  if (isSubmitting.value) return;
  isSubmitting.value = true;
  try {
    // ... 提交逻辑
  } finally {
    isSubmitting.value = false;
  }
};
```

---

#### Bug 13: UserRegisterView 表单类型断言错误

**状态**: ✅ 已修复

**位置**: `UserRegisterView.vue:59-63`

```javascript
const form = reactive({
  userAccount: "",
  userPassword: "",
  checkPassword: "",
} as API.UserRegisterRequest);
```

**影响**:
- `API.UserRegisterRequest` 类型可能不存在或已更改
- 代码中注释掉了 `import API from "@/api"`
- 可能导致类型错误

**修复建议**: 使用正确的类型导入。

---

### 4.2 前端功能缺失

#### 4.2.1 缺少判题状态实时更新

**当前状态**:
- 提交代码后跳转到提交记录页面
- 提交记录页面不会自动刷新判题状态
- 用户需要手动刷新页面才能看到最新状态

**主流OJ做法**:
- LeetCode: 实时轮询或 WebSocket 推送判题结果
- 洛谷: 提交后自动轮询状态更新

**建议**: 增加轮询机制或 WebSocket 实时推送。

---

#### 4.2.2 缺少代码编辑器高级功能

**当前状态**:
- 使用了 Monaco Editor，但只配置了基本功能
- 缺少代码格式化
- 缺少自动补全配置
- 缺少代码模板（初始代码）
- 主题固定为 dark，无法切换

**位置**: `CodeEditor.vue:91-101`

```javascript
codeEditor.value = monaco.editor.create(codeEditorRef.value, {
  value: props.value,
  language: props.language,
  automaticLayout: true,
  colorDecorators: true,
  minimap: { enabled: true },
  readOnly: false,
  theme: "vs-dark",  // 固定主题
});
```

**建议**:
- 增加主题切换功能
- 配置代码格式化快捷键
- 增加语言特定的代码模板
- 增加代码高亮和语法检查

---

#### 4.2.3 缺少用户个人中心

**当前状态**:
- 只有简单的登录/注册页面
- 没有用户个人中心页面
- 无法查看自己的提交记录
- 无法查看解题统计

**主流OJ功能**:
- 个人解题统计
- 提交历史
- 通过率分析
- 连续打卡

---

#### 4.2.4 缺少题目详情页的交互功能

**当前状态**:
- 题目详情页有 "评论" 和 "答案" Tab，但都被禁用或占位
- 没有题解分享功能
- 没有收藏题目功能
- 没有类似题目推荐

**位置**: `ViewQuestionView.vue:35-36`

```html
<a-tab-pane key="comment" title="评论" disabled> 评论区</a-tab-pane>
<a-tab-pane key="answer" title="答案"> 暂时无法查看答案</a-tab-pane>
```

---

#### 4.2.5 缺少响应式布局优化

**当前状态**:
- 部分页面在小屏幕设备上显示不佳
- 代码编辑器在移动端几乎无法使用
- 表格列过多时横向滚动体验差

---

#### 4.2.6 缺少加载状态和错误处理

**当前状态**:
- 数据加载时没有 loading 状态
- 网络请求失败时只有简单的 message 提示
- 没有重试机制
- 没有骨架屏或占位内容

---

#### 4.2.7 路由权限控制不完善

**位置**: `router/routes.ts`

**问题**:
- 路由守卫没有全局配置
- 权限检查只在菜单渲染时进行
- 直接访问需要权限的 URL 不会正确跳转
- 没有404页面处理

**建议**: 增加全局路由守卫。
```javascript
router.beforeEach((to, from, next) => {
  const loginUser = store.state.user.loginUser;
  const needAccess = to.meta?.access;
  if (needAccess && !checkAccess(loginUser, needAccess)) {
    next('/noAuth');
    return;
  }
  next();
});
```

---

#### 4.2.8 缺少全局状态管理

**当前状态**:
- Vuex store 只有 user 模块
- 没有全局 loading 状态
- 没有全局错误处理
- 没有缓存机制

---

### 4.3 前端代码质量问题

| 问题 | 位置 | 影响 | 建议 |
|------|------|------|------|
| console.log 未清理 | 多处 | 生产环境泄露信息 | 删除或替换为日志工具 |
| 硬编码 API 地址 | `request.ts:5` | 环境切换困难 | 使用环境变量配置 |
| 缺少 TypeScript 严格类型 | 多处 | 类型不安全 | 启用 strict 模式 |
| 组件缺少 Props 验证 | 多处 | 运行时错误 | 增加类型定义和默认值 |
| 样式内联过多 | 多处 | 维护困难 | 提取到 CSS 文件 |
| 缺少单元测试 | 全局 | 代码质量无法保证 | 增加测试用例 |

---

### 4.4 前端安全问题

| 问题 | 位置 | 影响 | 建议 |
|------|------|------|------|
| XSS 风险 | `MdViewer.vue` | 渲染用户输入的 Markdown | 使用安全的 Markdown 渲染器 |
| CSRF 防护不足 | `request.ts` | 只有 withCredentials | 增加 CSRF Token |
| 敏感信息暴露 | 全局 | 密码明文传输 | 前端初步哈希 |
| 路由权限绕过 | `router/index.ts` | 直接访问 URL | 增加路由守卫 |

---

## 二、代码层面问题

### 2.1 安全问题

| 问题 | 位置 | 影响 | 建议 |
|------|------|------|------|
| 密码使用 MD5 加密 | `UserServiceImpl.java:67` | 不安全，易被彩虹表攻击 | 使用 BCryptPasswordEncoder |
| 配置文件密码明文 | `application.yml:26` | 敏感信息泄露 | 使用环境变量或配置中心 |
| 文件名路径穿越 | `FileController.java:60` | 安全风险 | 过滤特殊字符 |
| 缺少 HTTPS | 全局 | 密码明文传输 | 启用 HTTPS |

### 2.2 性能问题

| 问题 | 位置 | 影响 | 建议 |
|------|------|------|------|
| 缺少自定义线程池 | `QuestionSubmitServiceImpl.java:105` | 使用默认 ForkJoinPool | 注入自定义线程池 |
| 缺少缓存机制 | 全局 | Redis未启用 | 启用 Redis 缓存 |
| 数据库查询未优化 | `QuestionServiceImpl.java` | N+1查询 | 使用批量查询 |
| ES 同步滞后 | `PostServiceImpl.java` | 数据不一致 | 增加同步补偿机制 |

### 2.3 代码质量问题

| 问题 | 位置 | 影响 | 建议 |
|------|------|------|------|
| 重复导入 | `QuestionController.java:14-15` | 代码冗余 | 删除重复行 |
| 魔法数字 | `JavaJudgeStrategyImpl.java:64` | 10秒补偿值硬编码 | 配置化 |
| 硬编码地址 | `RemoteCodeSandbox.java:23` | 维护困难 | 配置文件读取 |
| 重复代码 | `DefaultJudgeStrategyImpl` vs `JavaJudgeStrategyImpl` | 90%重复 | 使用模板方法模式 |
| @Deprecated 类仍在用 | `QuestionSubmitController.java:31` | 代码混乱 | 确认后移除或取消标记 |

---

## 三、功能设计缺陷

### 3.1 判题核心功能

#### 3.1.1 缺少多语言真正支持

**当前状态**:
- 枚举定义了 JAVA、CPP、GOLANG
- 但只有 Java 有特殊判题策略
- 沙箱未真正支持多语言执行

**与主流OJ对比**:

| OJ | 支持语言 |
|----|---------|
| LeetCode | C++, Java, Python, JavaScript, Go, Rust |
| 洛谷 | C, C++, Pascal, Python, Java |
| Codeforces | C++, Java, Python, Kotlin |
| **当前系统** | **仅 Java（其他未实现）** |

#### 3.1.2 缺少详细的判题结果反馈

**当前状态**:
- 只有简单的状态码和消息
- 没有编译错误详细信息
- 没有运行时错误堆栈
- 没有测试用例级别的反馈

**与主流OJ对比**:

| 判题结果 | 主流 OJ | 当前系统 |
|---------|---------|---------|
| 编译错误 (CE) | 详细错误信息 | 无 |
| 运行时错误 (RE) | 错误类型和行号 | 无 |
| 答案错误 (WA) | 显示期望输出 vs 实际输出 | 无 |
| 超时 (TLE) | 显示时间限制和实际用时 | 仅返回超时 |
| 内存超限 (MLE) | 显示内存限制和实际用量 | 仅返回超限 |

#### 3.1.3 缺少特殊判题 (SPJ) 支持

**当前状态**: 只有简单的字符串比对

**主流OJ支持**:
- 输出顺序不敏感的题目
- 浮点数精度容忍
- 多解题目
- 交互式判题

#### 3.1.4 沙箱安全性不足

| 安全措施 | 主流 OJ | 当前系统 |
|---------|---------|---------|
| 代码隔离（容器/虚拟机） | 有 | 无 |
| 资源限制（CPU/内存/时间） | 严格限制 | 依赖沙箱实现 |
| 网络访问限制 | 禁止 | 未处理 |
| 文件系统隔离 | 有 | 无 |
| 危险代码检测 | 有 | 无 |

### 3.2 题目管理功能

#### 3.2.1 缺少题目难度分级

**当前状态**: 无难度字段

**主流OJ**:
- LeetCode: 简单 / 中等 / 困难
- 洛谷: 入门 / 普及 / 提高 / 省选 / NOI / 国家集训队
- Codeforces: 难度分 (800-3500)

#### 3.2.2 缺少题目统计信息

**当前状态**: 字段有但未更新

| 统计信息 | 主流 OJ | 当前系统 |
|---------|---------|---------|
| 通过率 | 有 | 字段有但未更新 |
| 提交人数 | 有 | 无 |
| 通过人数 | 有 | 无 |
| 难度分布图 | 有 | 无 |

### 3.3 用户功能

#### 3.3.1 缺少用户解题统计

| 功能 | 主流 OJ | 当前系统 |
|------|---------|---------|
| 解题数量统计 | 有 | 无 |
| 通过率统计 | 有 | 无 |
| 连续打卡天数 | 有 | 无 |
| 解题热力图 | 有 | 无 |
| 技能雷达图 | 有 | 无 |

#### 3.3.2 缺少排行榜系统

| 排行榜类型 | 主流 OJ | 当前系统 |
|-----------|---------|---------|
| 总解题数排行 | 有 | 无 |
| 通过率排行 | 有 | 无 |
| 周/月活跃排行 | 有 | 无 |
| 题目难度排行 | 有 | 无 |

#### 3.3.3 缺少社交功能

| 功能 | 主流 OJ | 当前系统 |
|------|---------|---------|
| 题解分享 | 有 | 无 |
| 评论区 | 有 | 无 |
| 收藏题目 | 有 | 无 |
| 关注用户 | 有 | 无 |

### 3.4 社区功能

#### 3.4.1 点赞/收藏并发安全问题

**位置**: `PostThumbServiceImpl.java:46-49`

```java
synchronized (String.valueOf(userId).intern()) {
    return postThumbService.doPostThumbInner(userId, postId);
}
```

**问题**:
- `String.intern()` 锁在 JVM 层面是全局的
- 高并发时可能成为瓶颈
- 分布式部署时完全失效

#### 3.4.2 点赞/收藏计数不一致

**问题**: 并发操作时可能出现计数偏差

### 3.5 代码提交体验

#### 3.5.1 缺少在线代码编辑器

| 功能 | 主流 OJ | 当前系统 |
|------|---------|---------|
| 在线代码编辑器 | 有 | 无 |
| 代码模板 | 有 | 无 |
| 自动补全 | 有 | 无 |
| 代码格式化 | 有 | 无 |

#### 3.5.2 缺少提交历史管理

| 功能 | 主流 OJ | 当前系统 |
|------|---------|---------|
| 查看历史提交 | 有 | 功能简单 |
| 代码对比 | 有 | 无 |
| 提交时间线 | 有 | 无 |
| 提交统计图表 | 有 | 无 |

### 3.6 系统功能

#### 3.6.1 缺少比赛/竞赛功能

**主流OJ支持**:
- LeetCode: 周赛、双周赛
- 洛谷: 题单、比赛、模拟赛
- Codeforces: Contest、Gym

**当前系统**: 只有单个题目 CRUD

#### 3.6.2 缺少消息通知系统

| 通知类型 | 主流 OJ | 当前系统 |
|---------|---------|---------|
| 判题完成通知 | 有 | 无 |
| 比赛提醒 | 有 | 无 |
| 系统公告 | 有 | 无 |

#### 3.6.3 缺少数据可视化

| 功能 | 主流 OJ | 当前系统 |
|------|---------|---------|
| 用户数据看板 | 有 | 无 |
| 系统监控面板 | 有 | 无 |
| 判题队列监控 | 有 | 无 |

---

## 五、前端问题与不足

### 5.1 严重Bug

#### Bug 8: GlobalHeader 组件存在硬编码的模拟登录逻辑

**状态**: ✅ 已修复

**位置**: `GlobalHeader.vue:78-83`

```javascript
setTimeout(() => {
  store.dispatch("user/getLoginUser", {
    userName: "管理员",
    userRole: ACCESS_ENUM.ADMIN,
  });
}, 3000);
```

**影响**:
- 页面加载3秒后自动模拟管理员登录
- 任何用户访问页面3秒后都会变成管理员权限
- 这是开发测试代码，应该删除

**修复建议**: 删除这段代码。

---

#### Bug 9: QuestionsView 通过率计算写死为固定值

**状态**: ✅ 已修复

**位置**: `QuestionsView.vue:80`

```javascript
const rate = ref(5/8);  // 写死为62.5%
```

**影响**:
- 所有题目的通过率都显示为62.5%
- 实际通过率数据没有使用
- 用户无法看到真实的通过率

**修复建议**:
```javascript
// 使用实际数据计算
const calculateRate = (record: any) => {
  if (!record.submitNum || record.submitNum === 0) return 0;
  return Math.round((record.acceptedNum / record.submitNum) * 100);
};
```

---

#### Bug 10: QuestionSubmitView 表格列数据索引错误

**位置**: `QuestionSubmitView.vue:100-101`

```javascript
{
  title: "题目标题",
  dataIndex: "questionVO.title",  // 这种嵌套索引方式在 Arco Table 中可能无效
}
```

**影响**:
- 题目列表中题目标题可能显示为空
- 提交者名称也可能无法显示

**修复建议**: 使用自定义 slot 渲染嵌套数据。

---

#### Bug 11: AddQuestionView 表单缺少验证

**位置**: `AddQuestionView.vue`

**问题**:
- 创建题目时表单没有必填验证
- 时间限制、内存限制可以为负数或0
- 测试用例输入输出可以为空
- 提交后没有跳转或重置表单

**影响**:
- 可能创建无效题目
- 用户操作后没有明确反馈

**修复建议**: 增加表单验证规则。

---

#### Bug 12: ViewQuestionView 缺少防重复提交

**状态**: ✅ 已修复

**位置**: `ViewQuestionView.vue:116-131`

```javascript
const doSubmit = async () => {
  // ... 直接提交，没有防重复机制
  const res = await QuestionControllerService.doQuestionSubmitUsingPost({
    ...form.value,
    questionId: question.value.id,
  });
```

**影响**:
- 用户可以快速连续点击提交按钮
- 产生大量重复提交记录
- 后端判题队列被占满

**修复建议**: 增加提交状态锁。
```javascript
const isSubmitting = ref(false);
const doSubmit = async () => {
  if (isSubmitting.value) return;
  isSubmitting.value = true;
  try {
    // ... 提交逻辑
  } finally {
    isSubmitting.value = false;
  }
};
```

---

#### Bug 13: UserRegisterView 表单类型断言错误

**状态**: ✅ 已修复

**位置**: `UserRegisterView.vue:59-63`

```javascript
const form = reactive({
  userAccount: "",
  userPassword: "",
  checkPassword: "",
} as API.UserRegisterRequest);
```

**影响**:
- `API.UserRegisterRequest` 类型可能不存在或已更改
- 代码中注释掉了 `import API from "@/api"`
- 可能导致类型错误

**修复建议**: 使用正确的类型导入。

---

### 5.2 前端功能缺失

#### 5.2.1 缺少判题状态实时更新

**当前状态**:
- 提交代码后跳转到提交记录页面
- 提交记录页面不会自动刷新判题状态
- 用户需要手动刷新页面才能看到最新状态

**主流OJ做法**:
- LeetCode: 实时轮询或 WebSocket 推送判题结果
- 洛谷: 提交后自动轮询状态更新

**建议**: 增加轮询机制或 WebSocket 实时推送。

---

#### 5.2.2 缺少代码编辑器高级功能

**当前状态**:
- 使用了 Monaco Editor，但只配置了基本功能
- 缺少代码格式化
- 缺少自动补全配置
- 缺少代码模板（初始代码）
- 主题固定为 dark，无法切换

**位置**: `CodeEditor.vue:91-101`

```javascript
codeEditor.value = monaco.editor.create(codeEditorRef.value, {
  value: props.value,
  language: props.language,
  automaticLayout: true,
  colorDecorators: true,
  minimap: { enabled: true },
  readOnly: false,
  theme: "vs-dark",  // 固定主题
});
```

**建议**:
- 增加主题切换功能
- 配置代码格式化快捷键
- 增加语言特定的代码模板
- 增加代码高亮和语法检查

---

#### 5.2.3 缺少用户个人中心

**当前状态**:
- 只有简单的登录/注册页面
- 没有用户个人中心页面
- 无法查看自己的提交记录
- 无法查看解题统计

**主流OJ功能**:
- 个人解题统计
- 提交历史
- 通过率分析
- 连续打卡

---

#### 5.2.4 缺少题目详情页的交互功能

**当前状态**:
- 题目详情页有 "评论" 和 "答案" Tab，但都被禁用或占位
- 没有题解分享功能
- 没有收藏题目功能
- 没有类似题目推荐

**位置**: `ViewQuestionView.vue:35-36`

```html
<a-tab-pane key="comment" title="评论" disabled> 评论区</a-tab-pane>
<a-tab-pane key="answer" title="答案"> 暂时无法查看答案</a-tab-pane>
```

---

#### 5.2.5 缺少响应式布局优化

**当前状态**:
- 部分页面在小屏幕设备上显示不佳
- 代码编辑器在移动端几乎无法使用
- 表格列过多时横向滚动体验差

---

#### 5.2.6 缺少加载状态和错误处理

**当前状态**:
- 数据加载时没有 loading 状态
- 网络请求失败时只有简单的 message 提示
- 没有重试机制
- 没有骨架屏或占位内容

---

#### 5.2.7 路由权限控制不完善

**位置**: `router/routes.ts`

**问题**:
- 路由守卫没有全局配置
- 权限检查只在菜单渲染时进行
- 直接访问需要权限的 URL 不会正确跳转
- 没有404页面处理

**建议**: 增加全局路由守卫。
```javascript
router.beforeEach((to, from, next) => {
  const loginUser = store.state.user.loginUser;
  const needAccess = to.meta?.access;
  if (needAccess && !checkAccess(loginUser, needAccess)) {
    next('/noAuth');
    return;
  }
  next();
});
```

---

#### 5.2.8 缺少全局状态管理

**当前状态**:
- Vuex store 只有 user 模块
- 没有全局 loading 状态
- 没有全局错误处理
- 没有缓存机制

---

### 5.3 前端代码质量问题

| 问题 | 位置 | 影响 | 建议 |
|------|------|------|------|
| console.log 未清理 | 多处 | 生产环境泄露信息 | 删除或替换为日志工具 |
| 硬编码 API 地址 | `request.ts:5` | 环境切换困难 | 使用环境变量配置 |
| 缺少 TypeScript 严格类型 | 多处 | 类型不安全 | 启用 strict 模式 |
| 组件缺少 Props 验证 | 多处 | 运行时错误 | 增加类型定义和默认值 |
| 样式内联过多 | 多处 | 维护困难 | 提取到 CSS 文件 |
| 缺少单元测试 | 全局 | 代码质量无法保证 | 增加测试用例 |

---

### 5.4 前端安全问题

| 问题 | 位置 | 影响 | 建议 |
|------|------|------|------|
| XSS 风险 | `MdViewer.vue` | 渲染用户输入的 Markdown | 使用安全的 Markdown 渲染器 |
| CSRF 防护不足 | `request.ts` | 只有 withCredentials | 增加 CSRF Token |
| 敏感信息暴露 | 全局 | 密码明文传输 | 前端初步哈希 |
| 路由权限绕过 | `router/index.ts` | 直接访问 URL | 增加路由守卫 |

---

## 五、与主流OJ对比的不足

### 5.1 功能对比总表

| 功能模块 | LeetCode | 洛谷 | Codeforces | 牛客 | 当前系统 |
|---------|---------|------|-----------|------|---------|
| 多语言支持 | ✅ | ✅ | ✅ | ✅ | ⚠️ |
| 详细判题反馈 | ✅ | ✅ | ✅ | ✅ | ❌ |
| SPJ 支持 | ✅ | ✅ | ✅ | ✅ | ❌ |
| 沙箱安全隔离 | ✅ | ✅ | ✅ | ✅ | ❌ |
| 难度分级 | ✅ | ✅ | ✅ | ✅ | ❌ |
| 题单/比赛 | ✅ | ✅ | ✅ | ✅ | ❌ |
| 排行榜 | ✅ | ✅ | ✅ | ✅ | ❌ |
| 用户统计 | ✅ | ✅ | ✅ | ✅ | ❌ |
| 题解分享 | ✅ | ✅ | ✅ | ✅ | ❌ |
| 在线编辑器 | ✅ | ✅ | ✅ | ✅ | ❌ |
| 消息通知 | ✅ | ✅ | ✅ | ✅ | ❌ |
| 数据可视化 | ✅ | ✅ | ✅ | ✅ | ❌ |

### 5.2 架构对比

| 架构特性 | 主流 OJ | 当前系统 |
|---------|---------|---------|
| 微服务架构 | 部分采用 | 单体应用 |
| 容器化部署 | 有 | 无 |
| 负载均衡 | 有 | 无 |
| 消息队列 | 有 | 无 |
| 分布式缓存 | 有 | 未启用 |
| 分布式锁 | 有 | 无 |

---

## 六、改进优先级建议

### 6.1 高优先级（核心功能 - P0）

1. **修复严重Bug（后端）**
   - [ ] JudgeServiceImpl 查询用错ID
   - [ ] 判题异常未捕获
   - [ ] CodeSandboxProxy NPE风险

2. **修复严重Bug（前端）**
   - [ ] GlobalHeader 硬编码模拟登录
   - [ ] QuestionsView 通过率写死
   - [ ] ViewQuestionView 缺少防重复提交

3. **完善判题核心**
   - [ ] 增加详细判题反馈（CE/RE/WA/TLE/MLE）
   - [ ] 增加判题结果与状态分离
   - [ ] 完善异常处理机制

4. **增强沙箱安全性**
   - [ ] 代码隔离（Docker容器）
   - [ ] 资源限制（CPU/内存/时间）
   - [ ] 危险代码检测

### 6.2 中优先级（用户体验 - P1）

5. **增加题目管理**
   - [ ] 题目难度分级
   - [ ] 题目统计信息自动更新
   - [ ] 题目分类浏览

6. **增加用户功能**
   - [ ] 用户解题统计
   - [ ] 排行榜系统
   - [ ] 防重复提交机制

7. **完善社区功能**
   - [ ] 题解分享
   - [ ] 评论区
   - [ ] 收藏题目

8. **优化性能**
   - [ ] 启用 Redis 缓存
   - [ ] 自定义线程池
   - [ ] 数据库查询优化

9. **前端功能增强**
   - [ ] 判题状态实时更新（轮询/WebSocket）
   - [ ] 代码编辑器高级功能（格式化、主题切换）
   - [ ] 用户个人中心
   - [ ] 路由权限守卫
   - [ ] 全局状态管理完善

### 6.3 低优先级（锦上添花 - P2）

10. **增加比赛功能**
    - [ ] 题单管理
    - [ ] 比赛模式
    - [ ] 模拟考试

11. **增强系统功能**
    - [ ] 消息通知系统
    - [ ] 数据可视化面板
    - [ ] 在线代码编辑器增强

12. **架构升级**
    - [ ] 微服务拆分
    - [ ] 容器化部署
    - [ ] 消息队列集成

13. **前端优化**
    - [ ] 响应式布局
    - [ ] 加载状态优化
    - [ ] 单元测试覆盖
    - [ ] 代码质量提升

---

## 附录：问题清单汇总

### Bug 清单

| 编号 | Bug描述 | 严重程度 | 位置 |
|------|---------|---------|------|
| 1 | JudgeServiceImpl查询用错ID | 严重 | JudgeServiceImpl.java:107 |
| 2 | 判题异常未捕获 | 严重 | JudgeServiceImpl.java:72-86 |
| 3 | CodeSandboxProxy NPE风险 | 严重 | CodeSandboxProxy.java:21 |
| 4 | 判题状态与结果混淆 | 严重 | JudgeServiceImpl.java:99-101 |
| 5 | ExampleCodeSandbox是假实现 | 严重 | ExampleCodeSandbox.java |
| 6 | 用户注册参数为空返回null | 中等 | UserController.java:75-77 |
| 7 | 用户更新接口可越权修改角色 | 严重 | UserController.java:303-316 |
| 8 | GlobalHeader 硬编码模拟登录 | 严重 | GlobalHeader.vue:78-83 |
| 9 | QuestionsView 通过率写死 | 严重 | QuestionsView.vue:80 |
| 10 | QuestionSubmitView 表格列索引错误 | 中等 | QuestionSubmitView.vue:100-101 |
| 11 | AddQuestionView 表单缺少验证 | 中等 | AddQuestionView.vue |
| 12 | ViewQuestionView 缺少防重复提交 | 中等 | ViewQuestionView.vue:116-131 |
| 13 | UserRegisterView 表单类型断言错误 | 低 | UserRegisterView.vue:59-63 |

### 功能缺失清单

| 编号 | 功能 | 优先级 |
|------|------|--------|
| 1 | 多语言判题支持 | 高 |
| 2 | 详细判题反馈 | 高 |
| 3 | SPJ特殊判题 | 高 |
| 4 | 沙箱安全隔离 | 高 |
| 5 | 题目难度分级 | 中 |
| 6 | 用户解题统计 | 中 |
| 7 | 排行榜系统 | 中 |
| 8 | 题解分享 | 中 |
| 9 | 比赛/竞赛功能 | 低 |
| 10 | 在线代码编辑器 | 低 |
| 11 | 消息通知系统 | 低 |
| 12 | 数据可视化 | 低 |

### 前端问题清单

| 编号 | 问题 | 优先级 |
|------|------|--------|
| 1 | 判题状态实时更新 | 高 |
| 2 | 代码编辑器高级功能 | 中 |
| 3 | 用户个人中心 | 中 |
| 4 | 路由权限守卫 | 高 |
| 5 | 全局状态管理完善 | 中 |
| 6 | 响应式布局优化 | 低 |
| 7 | 加载状态和错误处理 | 中 |
| 8 | 题目详情页交互功能 | 低 |

---

> 本文档将持续更新，建议定期回顾和跟踪修复进度。

---

## 七、沙箱安全与恶意提交防御方案

### 7.1 当前系统的安全风险

#### 7.1.1 ExampleCodeSandbox 是假实现

**位置**: `ExampleCodeSandbox.java`

```java
// 当前代码 - 直接返回输入作为输出，没有真正执行代码
excuteCodeResponse.setOutputList(inputList);
excuteCodeResponse.setMessage("测试执行成功");
```

**影响**:
- 用户代码根本没有被执行
- 系统直接把测试用例的输入当作输出来比对
- 如果测试用例的输入和期望输出不同，就会显示"答案错误"

**示例（A+B 题目）**:
- 输入：`1 2`
- 期望输出：`3`
- 沙箱返回：`1 2`（直接把输入返回）
- 比对：`1 2` != `3` → **答案错误**

---

### 7.2 恶意提交攻击方式

如果实现真正的代码执行，没有安全防护的情况下，用户可能进行以下恶意攻击：

| 攻击类型 | 代码示例 | 危害 |
|---------|---------|------|
| **无限循环** | `while(true){}` | CPU 占满，判题机卡死 |
| **内存炸弹** | `new byte[1024*1024*1024]` | OOM，系统崩溃 |
| **Fork 炸弹** | `Runtime.getRuntime().exec("fork炸弹")` | 进程数耗尽，系统崩溃 |
| **文件删除** | `Runtime.getRuntime().exec("rm -rf /")` | 数据丢失，系统损坏 |
| **网络攻击** | 访问外部 API 或端口扫描 | 成为攻击跳板 |
| **线程耗尽** | 创建大量线程 | 系统资源耗尽 |
| **输出轰炸** | 无限打印输出 | 磁盘占满 |
| **系统命令** | `Runtime.getRuntime().exec("cat /etc/passwd")` | 信息泄露 |

---

### 7.3 多层防御方案

#### 第一层：资源限制（容器级别）

使用 **Docker** 运行代码，限制资源：

```bash
docker run \
  --cpus=1 \           # CPU 限制
  --memory=256m \      # 内存限制
  --pids-limit=50 \    # 进程数限制
  --network=none \     # 禁止网络
  --read-only \        # 只读文件系统
  --tmpfs /tmp:rw,noexec,nosuid,size=10m \  # 临时目录
  -v /tmp/input:/input:ro \  # 只读挂载输入
  image_name
```

**关键配置**:

| 限制项 | 建议值 | 说明 |
|--------|--------|------|
| CPU | 1 核 | 防止 CPU 占满 |
| 内存 | 256MB | 防止 OOM |
| 执行时间 | 1-3 秒 | 根据题目要求 |
| 输出大小 | 1MB | 防止输出轰炸 |
| 进程数 | 50 | 防止 fork 炸弹 |
| 文件写入 | 禁止或限制目录 | 防止文件破坏 |

---

#### 第二层：超时控制

```java
// 使用 Future + 线程池实现超时
ExecutorService executor = Executors.newSingleThreadExecutor();
Future<?> future = executor.submit(() -> {
    // 执行代码
});

try {
    future.get(2, TimeUnit.SECONDS);  // 2秒超时
} catch (TimeoutException e) {
    future.cancel(true);  // 中断执行
    return TIME_LIMIT_EXCEEDED;
}
```

---

#### 第三层：代码静态检查（执行前）

检查危险代码模式：

```java
// 禁止的代码模式
String[] DANGEROUS_PATTERNS = {
    "Runtime.getRuntime().exec",  // 执行系统命令
    "ProcessBuilder",             // 创建进程
    "java.io.File",               // 文件操作（可选）
    "while(true)",                // 明显死循环（简单检测）
    "Thread.sleep",               // 长时间睡眠
    "System.exit",                // 退出 JVM
    "for(;;)",                    // 无限循环
};
```

---

#### 第四层：沙箱安全策略（Java SecurityManager）

```java
// 自定义 SecurityManager
public class SandboxSecurityManager extends SecurityManager {
    @Override
    public void checkExec(String cmd) {
        throw new SecurityException("禁止执行系统命令");
    }
    
    @Override
    public void checkWrite(String file) {
        // 只允许写入指定目录
        if (!file.startsWith("/tmp/sandbox/")) {
            throw new SecurityException("禁止写入此路径");
        }
    }
    
    @Override
    public void checkListen(int port) {
        throw new SecurityException("禁止网络监听");
    }
}
```

---

#### 第五层：监控与熔断

```java
// 系统资源监控
RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

// 如果系统负载过高，拒绝新任务
if (getSystemLoad() > 0.9) {
    return SYSTEM_BUSY;
}

// 队列长度限制
if (judgeQueue.size() > 100) {
    return QUEUE_FULL;
}
```

---

### 7.4 完整的安全沙箱架构

```
┌─────────────────────────────────────┐
│           用户提交代码                │
└─────────────┬───────────────────────┘
              ▼
┌─────────────────────────────────────┐
│      静态检查（危险代码扫描）          │
│  - 黑名单关键字匹配                   │
│  - AST 语法树分析                     │
└─────────────┬───────────────────────┘
              ▼
┌─────────────────────────────────────┐
│      资源预检查（队列/负载）           │
│  - 系统负载 < 80%                    │
│  - 队列长度 < 100                    │
└─────────────┬───────────────────────┘
              ▼
┌─────────────────────────────────────┐
│      Docker 容器（隔离执行）          │
│  - CPU 限制 1核                      │
│  - 内存限制 256MB                    │
│  - 进程限制 50个                     │
│  - 无网络访问                        │
│  - 只读文件系统                      │
│  - 2秒执行超时                       │
└─────────────┬───────────────────────┘
              ▼
┌─────────────────────────────────────┐
│      结果收集与清理                   │
│  - 读取输出文件                      │
│  - 销毁容器                          │
│  - 清理临时文件                      │
└─────────────────────────────────────┘
```

---

### 7.5 简化方案（不使用 Docker）

如果环境限制无法使用 Docker，可以用 **Linux cgroups + seccomp**：

```java
// 使用 Linux 命令限制资源
ProcessBuilder pb = new ProcessBuilder(
    "bash", "-c",
    "ulimit -t 2 && " +           // CPU 时间 2秒
    "ulimit -v 262144 && " +       // 虚拟内存 256MB
    "ulimit -u 50 && " +            // 最大进程数 50
    "java -Xmx128m -XX:+UseContainerSupport " +
    "-Djava.security.manager " +
    "-Djava.security.policy=sandbox.policy " +
    "Main"
);
```

---

### 7.6 关键配置建议总结

| 方面 | 当前状态 | 建议方案 |
|------|---------|---------|
| 代码隔离 | 无 | Docker 容器 |
| 资源限制 | 无 | CPU/内存/时间/进程数限制 |
| 网络访问 | 未处理 | 完全禁止 |
| 文件系统 | 未处理 | 只读或限制目录 |
| 超时控制 | 无 | 2-3秒强制终止 |
| 静态检查 | 无 | 危险代码模式匹配 |
| 监控熔断 | 无 | 系统负载监控 |

---

## 八、判题策略增强方案

### 8.1 浮点数精度判断

#### 当前问题
当前判题是简单的字符串比对：
```java
if (!judgeCase.getOutput().equals(outputList.get(i))) {
    // 答案错误
}
```

浮点数比较时，由于精度问题，直接字符串比对会导致误判。

#### 实现方案
```java
public class FloatJudgeStrategy implements JudgeStrategy {
    
    // 默认精度容忍度
    private static final double EPSILON = 1e-6;
    
    @Override
    public JudgeInfo doJudge(JudgeContext context) {
        // ...
        for (int i = 0; i < judgeCaseList.size(); i++) {
            String expected = judgeCaseList.get(i).getOutput().trim();
            String actual = outputList.get(i).trim();
            
            if (isFloatComparison(expected, actual)) {
                if (!compareFloat(expected, actual, EPSILON)) {
                    return WRONG_ANSWER;
                }
            } else {
                // 原有字符串比对
                if (!expected.equals(actual)) {
                    return WRONG_ANSWER;
                }
            }
        }
    }
    
    // 判断是否是浮点数比较
    private boolean isFloatComparison(String expected, String actual) {
        try {
            Double.parseDouble(expected);
            Double.parseDouble(actual);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    // 浮点数精度比较
    private boolean compareFloat(String expected, String actual, double epsilon) {
        double exp = Double.parseDouble(expected);
        double act = Double.parseDouble(actual);
        return Math.abs(exp - act) < epsilon;
    }
}
```

#### 题目配置
```json
{
  "judgeConfig": {
    "timeLimit": 1000,
    "memoryLimit": 65536,
    "floatPrecision": 1e-6,
    "judgeMode": "float"
  }
}
```

---

### 8.2 空格处理（SPJ - Special Judge）

#### 当前问题
当前判题严格比对字符串，多余的空格会导致答案错误。

#### 实现方案
```java
public class SpjJudgeStrategy implements JudgeStrategy {
    
    @Override
    public JudgeInfo doJudge(JudgeContext context) {
        // ...
        for (int i = 0; i < judgeCaseList.size(); i++) {
            String expected = judgeCaseList.get(i).getOutput();
            String actual = outputList.get(i);
            
            // 使用标准化处理
            if (!normalize(expected).equals(normalize(actual))) {
                return WRONG_ANSWER;
            }
        }
    }
    
    // 标准化处理：去除多余空格、统一换行符
    private String normalize(String str) {
        return str
            .trim()                                    // 去除首尾空格
            .replaceAll("\\s+", " ")                   // 多个空格合并为一个
            .replaceAll("\\r\\n", "\n")               // 统一换行符
            .replaceAll(" +\\n", "\n")                // 行尾空格去除
            .replaceAll("\\n+", "\n");                // 多个换行合并
    }
}
```

#### 配置选项
```json
{
  "judgeConfig": {
    "ignoreTrailingSpaces": true,
    "ignoreBlankLines": true,
    "caseSensitive": true,
    "judgeMode": "spj"
  }
}
```

---

### 8.3 交互题（Interactive Judge）

#### 概念
交互题需要程序与判题系统进行实时交互，而不是一次性输入输出。

#### 典型场景
- **猜数字**：系统想一个数，用户程序猜测，系统反馈大了/小了
- **迷宫探索**：系统提供迷宫，用户程序发送移动指令，系统返回新位置
- **游戏AI**：用户程序与系统对弈

#### 实现方案
```java
public interface InteractiveJudgeStrategy {
    
    /**
     * 执行交互式判题
     * @param code 用户代码
     * @param language 编程语言
     * @param testCase 测试用例
     * @return 判题结果
     */
    JudgeInfo doInteractiveJudge(String code, String language, InteractiveTestCase testCase);
}

public class InteractiveJudgeImpl implements InteractiveJudgeStrategy {
    
    @Override
    public JudgeInfo doInteractiveJudge(String code, String language, InteractiveTestCase testCase) {
        // 启动用户程序
        Process process = startUserProgram(code, language);
        
        // 获取输入输出流
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        
        try {
            // 交互循环
            for (InteractionStep step : testCase.getSteps()) {
                // 向用户程序发送输入
                writer.write(step.getInput());
                writer.flush();
                
                // 读取用户程序输出
                String userOutput = reader.readLine();
                
                // 验证输出
                if (!step.validate(userOutput)) {
                    return WRONG_ANSWER;
                }
            }
            
            return ACCEPTED;
        } catch (IOException e) {
            return RUNTIME_ERROR;
        }
    }
}
```

#### 交互题配置示例
```json
{
  "type": "interactive",
  "steps": [
    {
      "input": "5\n",
      "expectedOutput": "3\n",
      "maxTime": 1000
    },
    {
      "input": "higher\n",
      "expectedOutput": "4\n"
    }
  ]
}
```

---

### 8.4 判题策略工厂模式

```java
// 判题策略工厂
public class JudgeStrategyFactory {
    
    public static JudgeStrategy getStrategy(JudgeMode mode) {
        switch (mode) {
            case EXACT:           // 精确匹配（默认）
                return new ExactJudgeStrategy();
            case FLOAT:           // 浮点数精度
                return new FloatJudgeStrategy();
            case SPJ:             // 特殊判题（空格处理）
                return new SpjJudgeStrategy();
            case INTERACTIVE:     // 交互题
                return new InteractiveJudgeStrategy();
            case CUSTOM:          // 自定义判题（上传判题程序）
                return new CustomJudgeStrategy();
            default:
                return new ExactJudgeStrategy();
        }
    }
}

// 题目配置
public class JudgeConfig {
    private JudgeMode judgeMode = JudgeMode.EXACT;
    private double floatPrecision = 1e-6;
    private boolean ignoreTrailingSpaces = false;
    private boolean ignoreBlankLines = false;
    private boolean caseSensitive = true;
    // ...
}
```

---

### 8.5 数据库表设计

```sql
-- 题目表增加判题配置
ALTER TABLE question ADD COLUMN judge_mode VARCHAR(50) DEFAULT 'exact';
ALTER TABLE question ADD COLUMN float_precision DOUBLE DEFAULT 1e-6;
ALTER TABLE question ADD COLUMN ignore_trailing_spaces TINYINT DEFAULT 0;
ALTER TABLE question ADD COLUMN ignore_blank_lines TINYINT DEFAULT 0;
ALTER TABLE question ADD COLUMN case_sensitive TINYINT DEFAULT 1;
```

---

### 8.6 实现优先级

| 功能 | 难度 | 优先级 | 说明 |
|------|------|--------|------|
| 浮点数精度判断 | 低 | 高 | 常见需求，实现简单 |
| 空格处理(SPJ) | 低 | 高 | 常见需求，实现简单 |
| 交互题 | 高 | 中 | 架构改动大，需要进程间通信 |
| 自定义判题 | 中 | 低 | 需要上传和执行判题程序 |

---

> 本文档将持续更新，建议定期回顾和跟踪修复进度。

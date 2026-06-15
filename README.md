# 酷OJ 在线判题系统 - 后端

<div align="center">

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.2-6DB33F?style=flat-square&logo=spring-boot)
![Java](https://img.shields.io/badge/Java-8-ED8B00?style=flat-square&logo=java)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql)
![Redis](https://img.shields.io/badge/Redis-6.x-DC382D?style=flat-square&logo=redis)
![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)

**一个功能强大、架构清晰的在线判题系统后端**

[API文档](#api-文档) | [前端仓库](https://github.com/kukudejin-123/kuoj-frontend) | [问题反馈](https://github.com/kukudejin-123/kuoj-backend/issues)

</div>

---

## 📖 目录

- [项目简介](#项目简介)
- [功能特性](#功能特性)
- [技术架构](#技术架构)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [核心模块详解](#核心模块详解)
- [判题系统](#判题系统)
- [API文档](#api文档)
- [项目亮点](#项目亮点)
- [部署指南](#部署指南)

---

## 项目简介

**酷OJ 后端** 是一个基于 Spring Boot 构建的在线判题系统服务端，提供题目管理、代码判题、竞赛管理、团队协作等核心功能。系统采用模块化设计，支持多种判题沙箱，具备完善的权限控制和安全机制。

### 🎯 核心能力

- **多语言支持**: Java, C, C++, Python, Go, JavaScript, C#, PHP, Ruby, Rust, Kotlin, Scala, Swift
- **多判题策略**: 精确匹配、忽略空白、忽略大小写、浮点比较、多答案匹配、SPJ 特判
- **多沙箱实现**: Judge0、Piston、自建沙箱等多种代码执行环境
- **高并发处理**: 异步判题、Redis缓存、分布式Session

---

## 功能特性

### 👤 用户管理

| 功能 | 描述 |
|------|------|
| 用户注册/登录 | 安全的用户认证，BCrypt密码加密 |
| 微信登录 | 支持微信公众号OAuth登录 |
| 个人信息管理 | 修改头像、昵称、个人简介 |
| 权限管理 | 用户、管理员、封禁三种角色 |

### 📝 题目管理

| 功能 | 描述 |
|------|------|
| 题目CRUD | 创建、编辑、删除题目（管理员） |
| 标签分类 | 支持多标签分类筛选 |
| 测试用例 | 支持多组测试用例配置 |
| 判题配置 | 时间限制、内存限制、判题模式配置 |
| 题目统计 | 提交数、通过数、通过率统计 |

### ⚖️ 判题系统

| 功能 | 描述 |
|------|------|
| 代码提交 | 支持多语言代码在线提交 |
| 异步判题 | CompletableFuture异步处理 |
| 实时状态 | 等待、运行中、完成等状态 |
| 详细结果 | 通过、错误、超时、内存超限等 |
| 代码保护 | 非管理员无法查看他人代码 |

### 🏆 竞赛系统

| 功能 | 描述 |
|------|------|
| 竞赛创建 | 创建竞赛、设置时间、添加题目 |
| 竞赛参与 | 用户报名参加竞赛 |
| 实时排名 | 竞赛中实时排行榜 |
| 状态自动更新 | 定时任务更新竞赛状态 |

### 👥 团队系统

| 功能 | 描述 |
|------|------|
| 团队创建 | 创建团队、设置团队信息 |
| 成员管理 | 邀请、移除成员 |
| 队长转让 | 将队长权限转让给其他成员 |
| 团队竞赛 | 团队为单位参加竞赛 |

### 📊 排名系统

| 功能 | 描述 |
|------|------|
| 用户排名 | 根据解题数和时间排名 |
| 提交统计 | 统计用户提交和通过数据 |

### 📄 帖子系统

| 功能 | 描述 |
|------|------|
| 帖子发布 | 发布讨论帖子 |
| 点赞收藏 | 支持点赞和收藏功能 |
| 帖子搜索 | 搜索帖子内容 |

---

## 技术架构

### 核心框架

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 2.7.2 | 核心框架 |
| MyBatis-Plus | 3.5.2 | ORM框架，简化数据库操作 |
| MySQL | 8.0 | 主数据库 |
| Redis | 6.x | 缓存、Session存储 |
| Spring Session | - | 分布式Session管理 |

### 其他组件

| 技术 | 版本 | 说明 |
|------|------|------|
| Knife4j | 4.4.0 | API文档（Swagger增强） |
| 微信SDK | 4.4.0 | 微信公众号登录 |
| 腾讯云COS | 5.6.89 | 对象存储（文件上传） |
| Hutool | 5.8.8 | Java工具库 |
| EasyExcel | 3.1.1 | Excel处理 |

### 项目架构图

```
┌─────────────────────────────────────────────────────────────┐
│                         客户端层                              │
│                    (Vue3 + Arco Design)                      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                        网关层                                │
│                     (Nginx 反向代理)                         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      应用服务层                              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐        │
│  │Controller│ │  AOP     │ │ Exception│ │  Job     │        │
│  │   层     │ │  拦截器  │ │  处理器  │ │  定时   │        │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘        │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐        │
│  │ Service  │ │  Judge   │ │  Sandbox │ │  File   │        │
│  │   层     │ │  判题    │ │  沙箱    │ │  存储   │        │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       数据层                                 │
│  ┌──────────────┐              ┌──────────────┐             │
│  │    MySQL     │              │    Redis     │             │
│  │  (持久化存储) │              │  (缓存/Session)│            │
│  └──────────────┘              └──────────────┘             │
└─────────────────────────────────────────────────────────────┘
```

---

## 项目结构

```
oj_backend-master/
├── src/main/java/com/kkdj/
│   ├── annotation/              # 自定义注解
│   │   └── AuthCheck.java       # 权限校验注解
│   ├── aop/                     # AOP切面
│   │   ├── AuthInterceptor.java # 权限拦截器
│   │   └── LogInterceptor.java  # 日志拦截器
│   ├── common/                  # 公共类
│   │   ├── BaseResponse.java    # 统一响应
│   │   ├── ErrorCode.java       # 错误码
│   │   └── ResultUtils.java     # 响应工具
│   ├── config/                  # 配置类
│   │   ├── CorsConfig.java      # 跨域配置
│   │   ├── RedisConfig.java     # Redis配置
│   │   └── SessionConfig.java   # Session配置
│   ├── constant/                # 常量定义
│   ├── controller/              # 控制器层
│   │   ├── UserController.java
│   │   ├── QuestionController.java
│   │   ├── ContestController.java
│   │   ├── TeamController.java
│   │   └── ...
│   ├── exception/               # 异常处理
│   │   ├── BusinessException.java
│   │   └── GlobalExceptionHandler.java
│   ├── job/                     # 定时任务
│   │   └── ContestStatusJob.java # 竞赛状态更新
│   ├── judge/                   # 判题核心模块
│   │   ├── codeSandbox/         # 代码沙箱实现
│   │   │   ├── CodeSandbox.java      # 沙箱接口
│   │   │   ├── CodeSandboxFactory.java
│   │   │   ├── CodeSandboxProxy.java
│   │   │   ├── ExampleCodeSandbox.java
│   │   │   ├── RemoteCodeSandbox.java
│   │   │   ├── Judge0CodeSandbox.java
│   │   │   └── PistonCodeSandbox.java
│   │   ├── service/             # 判题服务
│   │   │   └── JudgeServiceImpl.java
│   │   ├── strategy/            # 判题策略
│   │   │   ├── JudgeManager.java
│   │   │   ├── JudgeStrategy.java
│   │   │   ├── DefaultJudgeStrategyImpl.java
│   │   │   ├── IgnoreSpaceJudgeStrategyImpl.java
│   │   │   ├── IgnoreCaseJudgeStrategyImpl.java
│   │   │   ├── FloatJudgeStrategyImpl.java
│   │   │   ├── MultiAnswerJudgeStrategyImpl.java
│   │   │   └── TestlibJudgeStrategyImpl.java
│   │   └── spj/                 # SPJ特判系统
│   │       └── testlib/         # Testlib库
│   ├── manager/                 # 管理器
│   │   └── CosManager.java      # 对象存储管理
│   ├── mapper/                  # MyBatis Mapper
│   ├── model/                   # 数据模型
│   │   ├── dto/                 # 数据传输对象
│   │   ├── entity/              # 实体类
│   │   ├── enums/               # 枚举类
│   │   └── vo/                  # 视图对象
│   ├── service/                 # 服务层
│   │   ├── UserService.java
│   │   ├── QuestionService.java
│   │   ├── ContestService.java
│   │   ├── TeamService.java
│   │   └── ...
│   ├── testlib/                 # Testlib测试库
│   ├── utils/                   # 工具类
│   ├── wxmp/                    # 微信公众号
│   └── MainApplication.java     # 启动类
├── src/main/resources/
│   ├── application.yml          # 主配置文件
│   ├── application-prod.yml     # 生产环境配置
│   └── mapper/                  # MyBatis XML映射
├── sql/                         # SQL脚本
│   ├── create_table.sql         # 核心表
│   ├── contest_tables.sql       # 竞赛表
│   ├── team_tables.sql          # 团队表
│   └── ...
├── pom.xml                      # Maven配置
├── Dockerfile                   # Docker构建文件
└── README.md                    # 项目说明
```

---

## 快速开始

### 环境要求

- JDK 8+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.6+

### 安装步骤

```bash
# 1. 克隆项目
git clone https://github.com/kukudejin-123/kuoj-backend.git
cd kuoj-backend

# 2. 创建数据库
mysql -u root -p
CREATE DATABASE oj;

# 3. 导入SQL脚本
mysql -u root -p oj < sql/create_table.sql
mysql -u root -p oj < sql/contest_tables.sql
mysql -u root -p oj < sql/team_tables.sql

# 4. 修改配置
# 编辑 src/main/resources/application-prod.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/oj
    username: your_username
    password: your_password
  redis:
    host: localhost
    port: 6379

# 5. 编译运行
mvn clean package -DskipTests
java -jar target/oj_backend-0.0.1-SNAPSHOT.jar

# 或使用 Maven 直接运行
mvn spring-boot:run
```

### 配置说明

核心配置项说明：

```yaml
# application.yml
server:
  port: 8802
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/oj
    username: root
    password: your_password
  
  redis:
    host: localhost
    port: 6379
    database: 1
  
  session:
    timeout: 2592000  # 30天

# 代码沙箱配置
codeSandbox:
  type: judge0  # 可选: example, remote, judge0, piston, thirdParty
  judge0:
    url: https://ce.judge0.com/submissions
```

---

## 核心模块详解

### 用户认证模块

```java
// 用户注册
@PostMapping("/register")
public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest request) {
    // 1. 参数校验
    // 2. 密码加密（BCrypt）
    // 3. 创建用户
    // 4. 返回用户ID
}

// 用户登录
@PostMapping("/login")
public BaseResponse<User> userLogin(@RequestBody UserLoginRequest request, HttpServletRequest req) {
    // 1. 参数校验
    // 2. 验证密码
    // 3. 创建Session
    // 4. 返回用户信息
}
```

### 权限控制模块

基于注解的权限控制：

```java
// 自定义注解
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {
    String anyRole() default "";
    String mustRole() default "";
}

// 使用示例
@PostMapping("/add")
@AuthCheck(mustRole = "admin")
public BaseResponse<Long> addQuestion(@RequestBody QuestionAddRequest request) {
    // 只有管理员可以访问
}
```

### 题目管理模块

```java
// 题目实体
public class Question {
    private Long id;
    private String title;
    private String content;        // Markdown内容
    private List<String> tags;     // 标签列表
    private String answer;         // 参考答案
    private List<JudgeCase> judgeCases;     // 测试用例
    private JudgeConfig judgeConfig;         // 判题配置
    private Integer submitNum;     // 提交数
    private Integer acceptedNum;  // 通过数
}
```

---

## 判题系统

### 架构设计

```
                    ┌─────────────────┐
                    │  用户提交代码   │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │ QuestionSubmit   │
                    │   (WAITING)      │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │  JudgeService    │
                    │   异步判题       │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              ▼              ▼
        ┌──────────┐  ┌──────────┐  ┌──────────┐
        │ Judge0   │  │  Piston  │  │  Remote  │
        │ Sandbox  │  │ Sandbox  │  │ Sandbox  │
        └──────────┘  └──────────┘  └──────────┘
              │              │              │
              └──────────────┼──────────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │  JudgeManager    │
                    │  策略选择        │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              ▼              ▼
        ┌──────────┐  ┌──────────┐  ┌──────────┐
        │ Default │  │ Ignore   │  │  Testlib │
        │ Strategy│  │ Strategy │  │   SPJ    │
        └──────────┘  └──────────┘  └──────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │   判题结果      │
                    │ ACCEPTED / WA   │
                    └─────────────────┘
```

### 代码沙箱

系统支持多种代码沙箱实现：

| 沙箱类型 | 说明 | 适用场景 |
|---------|------|---------|
| example | 本地Mock沙箱 | 测试环境 |
| remote | 自建Java沙箱 | 私有部署 |
| judge0 | Judge0公共API | 开发测试 |
| piston | Piston自建服务 | 生产环境推荐 |
| thirdParty | 第三方沙箱 | 自定义扩展 |

### 判题策略

| 策略 | 说明 | 示例 |
|------|------|------|
| DEFAULT | 精确匹配 | 输出必须完全一致 |
| IGNORE_SPACE | 忽略空白 | 忽略多余空格、换行 |
| IGNORE_CASE | 忽略大小写 | "ABC" 等于 "abc" |
| FLOAT | 浮点比较 | 允许误差范围 `1e-6` |
| MULTI_ANSWER | 多答案匹配 | 多个正确答案任一匹配 |
| TESTLIB | SPJ特判 | 自定义判定程序 |

### SPJ 特判系统

支持 Testlib 风格的特判程序：

```java
// TestlibJudgeStrategyImpl.java
public JudgeInfo doJudge(JudgeContext context) {
    // 1. 编译SPJ程序
    // 2. 执行SPJ，传入：输入、用户输出、标准答案
    // 3. 解析SPJ输出结果
    // 4. 返回判题信息
}
```

### 判题结果状态

| 状态 | 说明 |
|------|------|
| ACCEPTED | 通过 |
| WRONG_ANSWER | 答案错误 |
| COMPILE_ERROR | 编译错误 |
| TIME_LIMIT_EXCEEDED | 时间超限 |
| MEMORY_LIMIT_EXCEEDED | 内存超限 |
| RUNTIME_ERROR | 运行错误 |
| SYSTEM_ERROR | 系统错误 |
| DANGEROUS_OPERATION | 危险操作 |
| OUTPUT_LIMIT_EXCEEDED | 输出超限 |
| PRESENTATION_ERROR | 格式错误 |

---

## API文档

启动项目后访问 Knife4j 接口文档：`http://localhost:8802/api/doc.html`

### 主要接口

#### 用户接口 `/api/user`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | /register | 用户注册 | 公开 |
| POST | /login | 用户登录 | 公开 |
| POST | /logout | 用户登出 | 登录 |
| GET | /get/login | 获取当前用户 | 登录 |
| GET | /get/vo | 获取用户信息 | 登录 |

#### 题目接口 `/api/question`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | /add | 创建题目 | 管理员 |
| POST | /delete | 删除题目 | 管理员 |
| POST | /update | 更新题目 | 管理员 |
| GET | /get/vo | 获取题目详情 | 登录 |
| POST | /list/page/vo | 分页查询题目 | 登录 |
| POST | /question_submit/do | 提交代码 | 登录 |
| POST | /question_submit/list/page | 查询提交记录 | 登录 |

#### 竞赛接口 `/api/contest`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | /add | 创建竞赛 | 管理员 |
| POST | /join | 参加竞赛 | 登录 |
| POST | /quit | 退出竞赛 | 登录 |
| GET | /get | 获取竞赛详情 | 登录 |
| POST | /list/page | 分页查询竞赛 | 登录 |

#### 团队接口 `/api/team`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | /add | 创建团队 | 登录 |
| POST | /invite | 邀请成员 | 队长 |
| POST | /kick | 移除成员 | 队长 |
| POST | /transfer | 转让队长 | 队长 |
| POST | /quit | 退出团队 | 登录 |

---

## 项目亮点

### 🚀 高性能设计

- **异步判题**: 使用 `CompletableFuture` 异步处理判题请求
- **Redis缓存**: 缓存热点数据，减少数据库压力
- **分布式Session**: 支持多实例部署
- **数据库索引优化**: 关键字段建立索引

### 🛡️ 安全性设计

- **密码加密**: BCrypt 单向加密存储
- **权限控制**: 基于注解的 AOP 权限校验
- **代码隔离**: 沙箱执行用户代码
- **敏感数据保护**: VO 模式过滤敏感字段
- **请求防刷**: 分页大小限制

### 📐 代码规范

- **统一响应**: `BaseResponse<T>` 统一返回格式
- **统一异常**: `BusinessException` 业务异常 + 全局处理
- **统一错误码**: `ErrorCode` 枚举定义错误码
- **逻辑删除**: MyBatis-Plus 逻辑删除

### 🔧 可扩展性

- **沙箱工厂模式**: 支持快速扩展新的代码沙箱
- **判题策略模式**: 支持自定义判题策略
- **模块化设计**: 各模块职责清晰

---

## 部署指南

### Docker 部署

```bash
# 构建镜像
docker build -t kuoj-backend:latest .

# 运行容器
docker run -d \
  --name kuoj-backend \
  -p 8802:8802 \
  -e MYSQL_HOST=host.docker.internal \
  -e REDIS_HOST=host.docker.internal \
  kuoj-backend:latest
```

### Docker Compose 部署

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: oj
    volumes:
      - ./sql:/docker-entrypoint-initdb.d
      - mysql_data:/var/lib/mysql

  redis:
    image: redis:6-alpine
    volumes:
      - redis_data:/data

  backend:
    image: kuoj-backend:latest
    depends_on:
      - mysql
      - redis
    ports:
      - "8802:8802"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/oj
      SPRING_REDIS_HOST: redis

volumes:
  mysql_data:
  redis_data:
```

---

## 相关链接

- 🖥️ [前端仓库](https://github.com/kukudejin-123/kuoj-frontend)
- 📚 [API文档](#api文档)
- 🐛 [问题反馈](https://github.com/kukudejin-123/kuoj-backend/issues)

---

## 开源协议

本项目基于 [MIT](LICENSE) 协议开源。

---

<div align="center">

**⭐ 如果这个项目对你有帮助，请给一个 Star ⭐**

Made with ❤️ by [kukudejin-123](https://github.com/kukudejin-123)

</div>
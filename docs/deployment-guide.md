# OJ 系统上线部署指南

> 本文档涵盖上线前的准备工作、部署步骤以及 SPJ（Special Judge）模块的改造方案。

---

## 一、系统架构概览

### 1.1 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 2.7.2 |
| 前端框架 | Vue 3 | 3.2.13 |
| 数据库 | MySQL | 8.x |
| 缓存 | Redis | 6.x+ |
| 代码沙箱 | Judge0 / Piston | - |
| 对象存储 | 腾讯云 COS | - |

### 1.2 服务依赖

```
┌─────────────────────────────────────────────────────────────┐
│                        用户请求                              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Nginx (反向代理)                          │
│                 静态资源 + API 转发                          │
└─────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│   前端静态资源    │ │   后端 API 服务   │ │   代码沙箱服务    │
│   (Vue 3 SPA)    │ │  (Spring Boot)   │ │   (Judge0/Piston) │
│   Port: 80/443   │ │   Port: 8802     │ │   Port: 2000     │
└──────────────────┘ └──────────────────┘ └──────────────────┘
              │               │               │
              └───────────────┼───────────────┘
                              ▼
              ┌───────────────────────────────┐
              │         基础设施服务           │
              │         MySQL │ Redis         │
              └───────────────────────────────┘
```

---

## 二、上线前准备工作

### 2.1 服务器环境准备

#### 2.1.1 服务器配置建议

| 服务 | 配置建议 | 数量 |
|------|----------|------|
| 应用服务器 | 4核8G | 1+ |
| 数据库服务器 | 4核8G + SSD | 1 (主) + 1 (从) |
| Redis 服务器 | 2核4G | 1 |
| 代码沙箱服务器 | 4核8G | 1+ |

#### 2.1.2 软件环境

```bash
# 基础环境
- JDK 1.8+
- Node.js 16+ / 18+
- MySQL 8.0+
- Redis 6.0+
- Nginx 1.20+

# 代码沙箱 (二选一)
- Docker (推荐 Judge0 或 Piston)
- 或独立部署的沙箱服务
```

#### 2.1.3 系统配置优化

```bash
# /etc/sysctl.conf
# 增加文件描述符限制
fs.file-max = 65535

# TCP 连接优化
net.ipv4.tcp_tw_reuse = 1
net.ipv4.tcp_fin_timeout = 30
net.core.somaxconn = 65535

# 应用后生效
sysctl -p
```

```bash
# /etc/security/limits.conf
* soft nofile 65535
* hard nofile 65535
```

### 2.2 安全准备

#### 2.2.1 敏感信息处理

**必须修改的配置项：**

| 文件 | 配置项 | 说明 |
|------|--------|------|
| application-prod.yml | datasource.password | 数据库密码 |
| application-prod.yml | redis.password | Redis 密码 |
| application.yml | cos.client.accessKey | COS 密钥 |
| application.yml | cos.client.secretKey | COS 密钥 |
| application.yml | wx.* | 微信相关配置 |

**建议做法：**
1. 使用环境变量注入敏感配置
2. 或使用配置中心（如 Nacos、Apollo）
3. 生产配置文件不要提交到代码仓库

#### 2.2.2 网络安全

```bash
# 防火墙配置 (仅开放必要端口)
# 对外开放
- 80/443 (Web 服务)
- 22 (SSH，建议限制 IP)

# 内部开放
- 3306 (MySQL，仅应用服务器可访问)
- 6379 (Redis，仅应用服务器可访问)
- 8802 (后端 API，仅 Nginx 可访问)
```

### 2.3 数据库准备

#### 2.3.1 数据库初始化

```sql
-- 1. 创建数据库
CREATE DATABASE oj DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. 创建用户
CREATE USER 'oj_user'@'%' IDENTIFIED BY '强密码';

-- 3. 授权
GRANT ALL PRIVILEGES ON oj.* TO 'oj_user'@'%';
FLUSH PRIVILEGES;
```

#### 2.3.2 执行 SQL 脚本

按顺序执行 `oj_backend-master/sql/` 目录下的脚本：
1. 建表脚本
2. 初始数据脚本
3. spj_test_questions.sql (SPJ 测试题目)

#### 2.3.3 数据库优化配置

```ini
# /etc/my.cnf
[mysqld]
# InnoDB 配置
innodb_buffer_pool_size = 2G  # 物理内存的 50-70%
innodb_log_file_size = 256M
innodb_flush_log_at_trx_commit = 2

# 连接配置
max_connections = 500
wait_timeout = 600

# 慢查询日志
slow_query_log = 1
long_query_time = 2
```

---

## 三、部署步骤

### 3.1 后端部署

#### 3.1.1 配置修改

```yaml
# application-prod.yml 关键配置
server:
  port: 8802

spring:
  datasource:
    url: jdbc:mysql://数据库IP:3306/oj?useSSL=true&serverTimezone=Asia/Shanghai
    username: oj_user
    password: ${DB_PASSWORD}  # 使用环境变量

  redis:
    host: Redis服务器IP
    password: ${REDIS_PASSWORD}

# 代码沙箱配置
codesandbox:
  type: piston  # 生产环境推荐 Piston
  piston-url: http://沙箱服务器IP:2000/api/v2/piston/execute
```

#### 3.1.2 打包构建

```bash
# 进入后端目录
cd oj_backend-master

# Maven 打包 (跳过测试)
mvn clean package -DskipTests

# 输出文件
# target/oj_kkdj_backend-0.0.1-SNAPSHOT.jar
```

#### 3.1.3 启动服务

```bash
# 方式一：直接启动
java -jar oj_kkdj_backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod

# 方式二：Systemd 服务 (推荐)
# /etc/systemd/system/oj-backend.service
[Unit]
Description=OJ Backend Service
After=network.target mysql.service redis.service

[Service]
Type=simple
User=oj
WorkingDirectory=/opt/oj/backend
ExecStart=/usr/bin/java -Xms512m -Xmx2g -jar oj_kkdj_backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target

# 启动服务
systemctl daemon-reload
systemctl enable oj-backend
systemctl start oj-backend
```

#### 3.1.4 健康检查

```bash
# 检查服务状态
curl http://localhost:8802/api/actuator/health

# 检查 API 文档
curl http://localhost:8802/api/doc.html
```

### 3.2 前端部署

#### 3.2.1 配置修改

```javascript
// vue.config.js
module.exports = {
  productionSourceMap: false,  // 关闭 sourcemap

  // API 代理 (开发环境)
  devServer: {
    proxy: {
      '/api': {
        target: 'http://localhost:8802',
        changeOrigin: true
      }
    }
  }
}
```

#### 3.2.2 构建打包

```bash
# 进入前端目录
cd oj-frontend-master

# 安装依赖
npm install

# 生产环境构建
npm run build

# 输出目录：dist/
```

#### 3.2.3 Nginx 配置

```nginx
# /etc/nginx/conf.d/oj.conf
server {
    listen 80;
    server_name your-domain.com;

    # 强制 HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    # SSL 配置
    ssl_certificate /etc/nginx/ssl/your-domain.com.crt;
    ssl_certificate_key /etc/nginx/ssl/your-domain.com.key;

    # 前端静态资源
    root /opt/oj/frontend/dist;
    index index.html;

    # 前端路由 (Vue Router History 模式)
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 代理
    location /api/ {
        proxy_pass http://127.0.0.1:8802/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 超时配置
        proxy_connect_timeout 60s;
        proxy_read_timeout 120s;
        proxy_send_timeout 120s;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # Gzip 压缩
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml;
    gzip_min_length 1024;
}
```

### 3.3 代码沙箱部署

#### 3.3.1 方案一：Judge0 (推荐)

```bash
# 使用 Docker 部署
git clone https://github.com/judge0/judge0.git
cd judge0

# 配置
# 修改 judge0.conf 中的配置

# 启动
docker-compose up -d

# 服务地址
# http://localhost:2358
```

#### 3.3.2 方案二：Piston

```bash
# Docker 部署
docker run -d \
  --name piston \
  -p 2000:2000 \
  --restart always \
  ghcr.io/engineer-man/piston

# 服务地址
# http://localhost:2000/api/v2/piston/execute
```

### 3.4 Redis 部署

```bash
# 方式一：直接安装
yum install redis

# /etc/redis.conf
bind 127.0.0.1
requirepass 强密码
maxmemory 2gb
maxmemory-policy allkeys-lru

# 启动
systemctl enable redis
systemctl start redis

# 方式二：Docker
docker run -d \
  --name redis \
  -p 6379:6379 \
  -v /opt/redis/data:/data \
  redis:7-alpine \
  redis-server --requirepass 强密码 --appendonly yes
```

---

## 四、SPJ 模块服务器部署改造方案

### 4.1 当前问题分析

当前 SPJ 模块 (`JavaSpjExecutor.java`) 存在以下问题：

| 问题 | 影响 | 风险等级 |
|------|------|----------|
| 使用本地临时目录 `/tmp/oj_spj/` | 多实例部署时缓存不共享 | 中 |
| 直接调用 `javac` 和 `java` 命令 | 依赖服务器 JDK 环境 | 低 |
| 无资源隔离 | SPJ 程序可能占用过多资源 | 高 |
| 无安全沙箱 | 恶意 SPJ 代码可能危害系统 | 高 |
| 同步执行 | 高并发时性能瓶颈 | 中 |

### 4.2 改造方案概述

```
┌─────────────────────────────────────────────────────────────────┐
│                     改造前架构                                   │
│  ┌─────────────┐      ┌─────────────────┐      ┌──────────────┐ │
│  │  后端服务   │ ───▶ │ JavaSpjExecutor │ ───▶ │ 本地执行 SPJ │ │
│  └─────────────┘      └─────────────────┘      └──────────────┘ │
│                              │                                  │
│                              ▼                                  │
│                       /tmp/oj_spj/                              │
│                       (本地文件系统)                             │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                     改造后架构                                   │
│  ┌─────────────┐      ┌─────────────────┐      ┌──────────────┐ │
│  │  后端服务   │ ───▶ │ RemoteSpjExec   │ ───▶ │  SPJ 沙箱    │ │
│  └─────────────┘      └─────────────────┘      │   服务       │ │
│                              │                 └──────────────┘ │
│                              ▼                        │         │
│                       Redis 缓存              Docker 容器       │
│                       (分布式缓存)           (资源隔离)         │
└─────────────────────────────────────────────────────────────────┘
```

### 4.3 详细改造方案

#### 4.3.1 方案一：Docker 容器化执行（推荐）

**架构设计：**

```
后端服务 ──HTTP──▶ SPJ 执行服务 ──Docker API──▶ SPJ 容器
                                                    │
                                                    ▼
                                             资源限制 + 超时控制
```

**实现步骤：**

**步骤 1：创建 SPJ 执行服务**

新建独立服务或扩展现有代码沙箱服务：

```java
// 新建 RemoteSpjExecutor.java
package com.kkdj.judge.spj;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 远程 SPJ 执行器
 * 通过 HTTP 调用独立的 SPJ 执行服务
 */
@Slf4j
@Component
public class RemoteSpjExecutor implements SpjExecutor {

    @Value("${spj.service.url:http://localhost:8091}")
    private String spjServiceUrl;

    @Value("${spj.service.timeout:30}")
    private int defaultTimeout;

    @Override
    public CompileResult compile(String spjCode, Long questionId) {
        try {
            JSONObject request = new JSONObject();
            request.set("spjCode", spjCode);
            request.set("questionId", questionId);

            String response = HttpUtil.post(
                spjServiceUrl + "/spj/compile",
                request.toString()
            );

            JSONObject result = JSONUtil.parseObj(response);
            return new CompileResult(
                result.getBool("success", false),
                result.getInt("exitCode", -1),
                result.getStr("message", ""),
                result.getLong("compileTime", 0L)
            );
        } catch (Exception e) {
            log.error("远程 SPJ 编译失败", e);
            return new CompileResult(false, -1, "远程编译失败: " + e.getMessage(), 0L);
        }
    }

    @Override
    public boolean isCompiled(Long questionId) {
        try {
            String response = HttpUtil.get(
                spjServiceUrl + "/spj/compiled?questionId=" + questionId
            );
            JSONObject result = JSONUtil.parseObj(response);
            return result.getBool("compiled", false);
        } catch (Exception e) {
            log.error("检查 SPJ 编译状态失败", e);
            return false;
        }
    }

    @Override
    public SpjResult execute(Long questionId, String inputFile,
                             String userOutputFile, String answerFile, int timeout) {
        try {
            JSONObject request = new JSONObject();
            request.set("questionId", questionId);
            request.set("inputFile", inputFile);
            request.set("userOutputFile", userOutputFile);
            request.set("answerFile", answerFile);
            request.set("timeout", timeout > 0 ? timeout : defaultTimeout);

            String response = HttpUtil.post(
                spjServiceUrl + "/spj/execute",
                request.toString()
            );

            JSONObject result = JSONUtil.parseObj(response);
            return new SpjResult(
                result.getInt("exitCode", -1),
                result.getStr("output", ""),
                result.getLong("executeTime", 0L),
                result.getBool("timeout", false)
            );
        } catch (Exception e) {
            log.error("远程 SPJ 执行失败", e);
            return SpjResult.systemError("远程执行失败: " + e.getMessage());
        }
    }

    @Override
    public void cleanup(Long questionId) {
        try {
            HttpUtil.delete(spjServiceUrl + "/spj/cache?questionId=" + questionId);
        } catch (Exception e) {
            log.error("清理 SPJ 缓存失败", e);
        }
    }
}
```

**步骤 2：创建 SPJ 执行服务端**

独立 Spring Boot 服务，通过 Docker API 执行 SPJ：

```java
// SpjDockerExecutor.java - SPJ Docker 执行器
package com.kkdj.spj.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SpjDockerExecutor {

    private final DockerClient dockerClient;
    private static final String SPJ_IMAGE = "openjdk:8-jdk-alpine";
    private static final long MEMORY_LIMIT = 256 * 1024 * 1024L; // 256MB
    private static final long CPU_QUOTA = 50000L; // 0.5 CPU

    public SpjDockerExecutor() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .build();
        this.dockerClient = DockerClientBuilder.getInstance(config).build();
    }

    /**
     * 在 Docker 容器中执行 SPJ
     */
    public SpjExecutionResult executeInDocker(Long questionId,
                                               String inputPath,
                                               String userOutputPath,
                                               String answerPath,
                                               int timeout) {
        String containerId = null;
        try {
            // 1. 创建容器
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withMemory(MEMORY_LIMIT)
                    .withCpuQuota(CPU_QUOTA)
                    .withBinds(
                        new Bind(getSpjWorkDir(questionId).toString(),
                                 new Volume("/spj")),
                        new Bind(inputPath, new Volume("/input.txt")),
                        new Bind(userOutputPath, new Volume("/userOutput.txt")),
                        new Bind(answerPath, new Volume("/answer.txt"))
                    )
                    .withNetworkMode("none"); // 禁用网络

            CreateContainerResponse container = dockerClient.createContainerCmd(SPJ_IMAGE)
                    .withHostConfig(hostConfig)
                    .withCmd("java", "-Xmx200m", "SpjChecker",
                             "/input.txt", "/userOutput.txt", "/answer.txt")
                    .withWorkingDir("/spj")
                    .exec();

            containerId = container.getId();

            // 2. 启动容器
            dockerClient.startContainerCmd(containerId).exec();

            // 3. 等待执行完成
            long startTime = System.currentTimeMillis();
            boolean finished = dockerClient.waitContainerCmd(containerId)
                    .start().awaitCompletion(timeout, TimeUnit.SECONDS);
            long executeTime = System.currentTimeMillis() - startTime;

            if (!finished) {
                dockerClient.stopContainerCmd(containerId).withTimeout(5).exec();
                return SpjExecutionResult.timeout();
            }

            // 4. 获取退出码
            Long exitCode = dockerClient.inspectContainerCmd(containerId)
                    .exec().getState().getExitCodeLong();

            // 5. 获取输出
            String output = readContainerLogs(containerId);

            return new SpjExecutionResult(exitCode.intValue(), output, executeTime, false);

        } catch (Exception e) {
            log.error("Docker 执行 SPJ 失败", e);
            return SpjExecutionResult.systemError("执行失败: " + e.getMessage());
        } finally {
            // 清理容器
            if (containerId != null) {
                try {
                    dockerClient.removeContainerCmd(containerId).withForce(true).exec();
                } catch (Exception ignored) {}
            }
        }
    }

    private Path getSpjWorkDir(Long questionId) {
        return Path.of("/opt/spj/cache", questionId.toString());
    }

    private String readContainerLogs(String containerId) throws Exception {
        StringBuilder logs = new StringBuilder();
        dockerClient.logContainerCmd(containerId)
                .withStdOut(true)
                .withStdErr(true)
                .exec(new LogContainerResultCallback() {
                    @Override
                    public void onNext(Frame frame) {
                        logs.append(new String(frame.getPayload()));
                    }
                }).awaitCompletion();
        return logs.toString();
    }
}
```

**步骤 3：添加 Maven 依赖**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.github.docker-java</groupId>
    <artifactId>docker-java-core</artifactId>
    <version>3.3.0</version>
</dependency>
<dependency>
    <groupId>com.github.docker-java</groupId>
    <artifactId>docker-java-transport-httpclient5</artifactId>
    <version>3.3.0</version>
</dependency>
```

**步骤 4：配置切换**

```yaml
# application.yml
spj:
  mode: remote  # local 或 remote
  service:
    url: http://spj-service:8091
    timeout: 30
  docker:
    enabled: true
    image: openjdk:8-jdk-alpine
    memory: 256m
```

#### 4.3.2 方案二：进程池 + 资源限制

如果不想引入 Docker，可以使用进程池 + cgroups 限制：

```java
// ProcessPoolSpjExecutor.java
@Slf4j
@Component
public class ProcessPoolSpjExecutor implements SpjExecutor {

    private final ExecutorService processPool;

    public ProcessPoolSpjExecutor() {
        // 限制并发执行的 SPJ 数量
        this.processPool = Executors.newFixedThreadPool(4);
    }

    @Override
    public SpjResult execute(Long questionId, String inputFile,
                             String userOutputFile, String answerFile, int timeout) {
        Future<SpjResult> future = processPool.submit(() -> {
            ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-Xmx256m",           // 堆内存限制
                "-Xss256k",           // 栈大小限制
                "-XX:MaxDirectMemorySize=64m",  // 直接内存限制
                "SpjChecker",
                inputFile, userOutputFile, answerFile
            );

            // 设置工作目录和环境
            pb.directory(new File(getWorkDir(questionId)));
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // 使用 cgroups 限制 (需要在系统层面配置)
            // 或者使用 ulimit

            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return SpjResult.timeout();
            }

            return new SpjResult(process.exitValue(), readOutput(process), 0, false);
        });

        try {
            return future.get(timeout + 5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            return SpjResult.timeout();
        } catch (Exception e) {
            return SpjResult.systemError(e.getMessage());
        }
    }
}
```

#### 4.3.3 方案三：集成到现有代码沙箱

将 SPJ 执行集成到 Judge0 或 Piston：

```java
// 将 SPJ 代码作为普通程序提交到 Judge0 执行
// 需要修改 TestlibJudgeStrategyImpl

public class SandboxIntegratedSpjExecutor implements SpjExecutor {

    @Resource
    private CodeSandbox codeSandbox;  // 复用现有沙箱

    @Override
    public SpjResult execute(Long questionId, String inputFile,
                             String userOutputFile, String answerFile, int timeout) {
        // 构建 SPJ 执行请求
        ExecuteCodeRequest request = new ExecuteCodeRequest();
        request.setLanguage("java");
        request.setCode(buildSpjCode(questionId, inputFile, userOutputFile, answerFile));

        // 通过代码沙箱执行
        ExecuteCodeResponse response = codeSandbox.executeCode(request);

        // 解析结果
        return convertToSpjResult(response);
    }
}
```

### 4.4 推荐改造路径

```
阶段一（快速上线）
    │
    ▼
保持本地执行 + 安全加固
    - 限制 SPJ 代码大小
    - 限制执行用户权限
    - 添加超时强制终止
    │
    ▼
阶段二（稳定性提升）
    │
    ▼
进程池 + 资源限制
    - 使用独立用户运行 SPJ
    - 配置 cgroups 资源限制
    - 实现进程池管理
    │
    ▼
阶段三（生产级方案）
    │
    ▼
Docker 容器化执行
    - 完全资源隔离
    - 网络隔离
    - 文件系统隔离
```

### 4.5 安全加固建议

无论采用哪种方案，都需要：

1. **代码审查**：SPJ 代码上传前进行安全检查
2. **权限控制**：使用低权限用户执行
3. **资源限制**：CPU、内存、时间限制
4. **网络隔离**：禁止 SPJ 访问网络
5. **文件隔离**：限制文件系统访问范围
6. **日志审计**：记录所有 SPJ 执行日志

---

## 五、部署检查清单

### 5.1 部署前检查

- [ ] 服务器资源满足要求
- [ ] 所有敏感配置已修改
- [ ] 数据库已初始化
- [ ] Redis 服务正常
- [ ] 代码沙箱服务正常
- [ ] SSL 证书已配置

### 5.2 部署后验证

- [ ] 后端服务健康检查通过
- [ ] 前端页面正常访问
- [ ] 用户注册/登录功能正常
- [ ] 题目列表加载正常
- [ ] 代码提交判题正常
- [ ] SPJ 题目判题正常
- [ ] 比赛功能正常

### 5.3 性能测试

```bash
# API 压测
ab -n 1000 -c 100 http://your-domain/api/question/list

# 数据库连接池监控
# Redis 内存监控
```

---

## 六、运维建议

### 6.1 监控告警

```yaml
监控指标:
  - 后端服务存活状态
  - JVM 内存使用率
  - 数据库连接数
  - Redis 内存使用率
  - 代码沙箱队列长度
  - API 响应时间

告警规则:
  - 服务宕机 → 立即告警
  - 内存使用 > 80% → 告警
  - API 响应时间 > 3s → 告警
```

### 6.2 日志管理

```bash
# 后端日志
/opt/oj/logs/application.log

# Nginx 日志
/var/log/nginx/access.log
/var/log/nginx/error.log

# 日志轮转配置
# /etc/logrotate.d/oj
/opt/oj/logs/*.log {
    daily
    rotate 30
    compress
    missingok
    notifempty
}
```

### 6.3 备份策略

```bash
# 数据库备份脚本
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
mysqldump -u oj_user -p'password' oj > /backup/oj_$DATE.sql

# 保留最近 7 天备份
find /backup -name "oj_*.sql" -mtime +7 -delete
```

---

## 七、常见问题

### Q1: SPJ 编译失败？
检查服务器 JDK 是否正确安装，`javac` 命令是否可用。

### Q2: 判题超时？
检查代码沙箱服务是否正常，网络连接是否稳定。

### Q3: 内存溢出？
调整 JVM 参数，增加堆内存限制。

### Q4: 前端页面空白？
检查 Nginx 配置，确保静态资源正确部署。

---

## 八、联系与支持

如有问题，请检查：
1. 服务日志
2. 数据库日志
3. Nginx 日志

祝部署顺利！

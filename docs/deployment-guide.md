# OJ 系统上线部署指南

> 本文档涵盖从零开始在 Ubuntu 服务器上部署 OJ 系统的完整步骤。
>
> **服务器配置：2核4G6M Ubuntu**（单机部署，代码沙箱使用 Judge0 公共 API）

---

## 一、系统架构概览

### 1.1 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 2.7.2 |
| 前端框架 | Vue 3 | 3.2.13 |
| 数据库 | MySQL | 8.x |
| 缓存 | Redis | 6.x+ |
| 反向代理 | Nginx | 1.20+ |
| 代码沙箱 | Judge0 公共 API | - |
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
│   前端静态资源    │ │   后端 API 服务   │ │   Judge0 公共API │
│   (Vue 3 SPA)    │ │  (Spring Boot)   │ │  (远程调用判题)   │
│   Port: 80/443   │ │   Port: 8802     │ │   ce.judge0.com  │
└──────────────────┘ └──────────────────┘ └──────────────────┘
              │               │
              └───────────────┼───────────────┘
                              ▼
              ┌───────────────────────────────┐
              │         基础设施服务           │
              │         MySQL │ Redis         │
              │       (本地部署)              │
              └───────────────────────────────┘
```

### 1.3 内存占用估算

| 服务 | 内存占用 |
|------|----------|
| MySQL | ~500MB |
| Redis | ~200MB |
| Spring Boot | ~768MB |
| Nginx | ~50MB |
| 系统 + 其他 | ~300MB |
| **总计** | **~1.8GB** |

> 留有约 2GB 余量，适合 4G 内存服务器。

---

## 二、部署方式选择

### 2.1 方式对比

| 方式 | 优点 | 缺点 | 推荐度 |
|------|------|------|--------|
| **宝塔面板** | 图形化管理，操作简单，一键安装环境 | 占用部分资源，需要学习面板操作 | ⭐⭐⭐⭐⭐ 推荐 |
| 命令行部署 | 完全控制，资源占用最小 | 需要熟悉 Linux 命令，操作复杂 | ⭐⭐⭐ |

### 2.2 推荐方案

**新手推荐使用宝塔面板**，可以大大简化部署流程。本文档以宝塔面板为主进行讲解。

---

## 三、宝塔面板安装与配置

### 3.1 安装宝塔面板

#### 步骤 1：SSH 连接服务器

在本地电脑打开终端（Windows 使用 PowerShell 或 PuTTY）：

```bash
ssh root@你的服务器IP
```

输入密码登录。

#### 步骤 2：安装宝塔面板

**Ubuntu/Debian 系统：**

```bash
wget -O install.sh http://download.bt.cn/install/install-ubuntu_6.0.sh && sudo bash install.sh ed8484bec1
```

安装过程会提示确认，输入 `y` 回车继续。

安装完成后会显示：
```
==================================================================
Congratulations! Installed successfully!
==================================================================
外网面板地址: https://xxx.xxx.xxx.xxx:8888/xxxxxxxx
内网面板地址: https://192.168.x.x:8888/xxxxxxxx
username: xxxxxxxx
password: xxxxxxxx
==================================================================
```

**⚠️ 重要：请保存好上面的面板地址、用户名和密码！**

#### 步骤 3：开放宝塔端口

如果云服务商有安全组，需要开放以下端口：
- **8888**：宝塔面板端口
- **80**：HTTP
- **443**：HTTPS
- **22**：SSH

在云服务器控制台（阿里云/腾讯云/华为云等）的安全组中添加这些端口规则。

#### 步骤 4：登录宝塔面板

1. 浏览器访问面板地址（如 `https://xxx.xxx.xxx.xxx:8888/xxxxxxxx`）
2. 输入用户名和密码登录
3. 首次登录会提示绑定宝塔账号，可以跳过或注册绑定

#### 步骤 5：一键安装环境

登录后，面板会推荐安装环境，选择以下组件：

| 组件 | 版本 | 说明 |
|------|------|------|
| Nginx | 1.22+ | 必装 |
| MySQL | 8.0 | 必装 |
| Redis | 6.0+ | 必装 |
| Tomcat | 不装 | 不需要 |
| PHP | 不装 | 不需要 |
| phpMyAdmin | 可选 | 数据库管理工具 |

点击"一键安装"，等待安装完成（约 10-20 分钟）。

### 3.2 宝塔面板基础设置

#### 3.2.1 修改面板端口（安全建议）

1. 面板设置 → 面板端口 → 改为其他端口（如 18888）
2. 记得在安全组开放新端口

#### 3.2.2 安装 Java 环境

宝塔面板默认不包含 Java，需要手动安装：

**方法一：通过软件商店安装**
1. 软件商店 → 搜索 "Java项目管理器" → 安装
2. 或者搜索 "JDK" 安装

**方法二：命令行安装（推荐）**

在宝塔面板的"终端"中执行：

```bash
# 安装 OpenJDK 8
apt update
apt install openjdk-8-jdk -y

# 验证安装
java -version
javac -version
```

显示版本信息即安装成功。

#### 3.2.3 配置 MySQL

1. 数据库 → 添加数据库
2. 填写信息：
   - 数据库名：`oj`
   - 用户名：`oj_user`
   - 密码：自己设置一个强密码
   - 访问权限：本地服务器

3. 点击提交创建

或者用命令行：
```bash
mysql -u root -p
```

```sql
CREATE DATABASE oj DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'oj_user'@'localhost' IDENTIFIED BY '你的密码';
GRANT ALL PRIVILEGES ON oj.* TO 'oj_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

#### 3.2.4 配置 Redis

1. 软件商店 → 已安装 → Redis → 设置
2. 修改配置：
   - 设置密码：点击"密码"设置一个密码
   - 最大内存：设置为 200MB
   - 最大内存策略：allkeys-lru

或在终端执行：
```bash
# 编辑 Redis 配置
vim /etc/redis/redis.conf

# 找到并修改以下配置
# requirepass 你的密码
# maxmemory 200mb
# maxmemory-policy allkeys-lru

# 重启 Redis
systemctl restart redis
```

#### 3.2.5 安全组设置

在面板"安全"页面，确保以下端口已开放：

| 端口 | 说明 |
|------|------|
| 8888/18888 | 宝塔面板 |
| 80 | HTTP |
| 443 | HTTPS |
| 22 | SSH |

**不需要开放的端口（内部使用）：**
- 3306（MySQL）
- 6379（Redis）
- 8802（后端 API）

---

## 四、项目打包与上传

### 4.1 本地打包项目

#### 4.1.1 后端打包

在你的开发电脑上：

```bash
# 进入后端目录
cd oj_backend-master

# 修改生产配置
# 编辑 src/main/resources/application-prod.yml
```

**application-prod.yml 配置示例：**

```yaml
server:
  port: 8802

spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/oj?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: oj_user
    password: 你的数据库密码

  redis:
    host: localhost
    port: 6379
    password: 你的Redis密码
    database: 1
    timeout: 5000

mybatis-plus:
  configuration:
    log-impl: ''

# 代码沙箱配置
codesandbox:
  type: judge0
  judge0-url: https://ce.judge0.com/submissions

logging:
  level:
    com.kkdj: info
```

```bash
# Maven 打包
mvn clean package -DskipTests

# 打包完成后，jar 包位置：
# target/oj_kkdj_backend-0.0.1-SNAPSHOT.jar
```

#### 4.1.2 前端打包

```bash
# 进入前端目录
cd oj-frontend-master

# 安装依赖
npm install

# 生产环境构建
npm run build

# 打包完成后，静态文件位置：
# dist/
```

#### 4.1.3 准备 SQL 脚本

需要上传的 SQL 文件（在 `sql/` 目录下）：
- `create_table.sql`
- `contest_tables.sql`
- `team_tables.sql`
- `sample_questions.sql`
- `acm_questions.sql`（新增的 ACM 题目）

### 4.2 上传文件到服务器

#### 方法一：宝塔面板上传（推荐新手）

1. 宝塔面板 → 文件
2. 创建目录：`/www/wwwroot/oj/`
3. 在目录下创建：
   - `backend/` - 存放后端 jar 包
   - `frontend/` - 存放前端 dist
   - `sql/` - 存放 SQL 脚本
4. 进入对应目录，点击"上传"按钮上传文件

#### 方法二：SFTP 上传（推荐）

使用 FileZilla 或 WinSCP：

1. 连接信息：
   - 主机：你的服务器 IP
   - 端口：22
   - 用户名：root
   - 密码：服务器密码

2. 上传文件：
   - 后端 jar 包 → `/www/wwwroot/oj/backend/`
   - 前端 dist 目录内容 → `/www/wwwroot/oj/frontend/`
   - SQL 脚本 → `/www/wwwroot/oj/sql/`

#### 方法三：命令行上传

在本地终端执行：

```bash
# 上传后端 jar 包
scp target/oj_kkdj_backend-0.0.1-SNAPSHOT.jar root@服务器IP:/www/wwwroot/oj/backend/

# 上传前端 dist
scp -r dist/* root@服务器IP:/www/wwwroot/oj/frontend/

# 上传 SQL 脚本
scp sql/*.sql root@服务器IP:/www/wwwroot/oj/sql/
```

### 4.3 导入数据库

#### 方法一：宝塔面板导入

1. 宝塔面板 → 数据库 → 找到 oj 数据库 → 管理
2. 点击"导入"按钮
3. 按顺序选择并导入 SQL 文件：
   - `create_table.sql`
   - `contest_tables.sql`
   - `team_tables.sql`
   - `sample_questions.sql`
   - `acm_questions.sql`

#### 方法二：命令行导入

```bash
# SSH 登录服务器后
cd /www/wwwroot/oj/sql

# 导入 SQL 文件（按顺序）
mysql -u oj_user -p oj < create_table.sql
mysql -u oj_user -p oj < contest_tables.sql
mysql -u oj_user -p oj < team_tables.sql
mysql -u oj_user -p oj < sample_questions.sql
mysql -u oj_user -p oj < acm_questions.sql

# 验证导入
mysql -u oj_user -p oj -e "SHOW TABLES;"
```

---

## 五、启动后端服务

### 5.1 创建启动脚本

在宝塔面板的终端或通过 SSH：

```bash
# 创建启动脚本
cat > /www/wwwroot/oj/backend/start.sh << 'EOF'
#!/bin/bash
cd /www/wwwroot/oj/backend
nohup java \
  -Xms256m \
  -Xmx768m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -Dspring.profiles.active=prod \
  -jar oj_kkdj_backend-0.0.1-SNAPSHOT.jar \
  > logs/app.log 2>&1 &
echo $! > app.pid
echo "服务启动中，PID: $(cat app.pid)"
EOF

chmod +x /www/wwwroot/oj/backend/start.sh

# 创建日志目录
mkdir -p /www/wwwroot/oj/backend/logs
```

### 5.2 创建停止脚本

```bash
cat > /www/wwwroot/oj/backend/stop.sh << 'EOF'
#!/bin/bash
cd /www/wwwroot/oj/backend
if [ -f app.pid ]; then
    PID=$(cat app.pid)
    kill $PID 2>/dev/null
    echo "服务已停止，PID: $PID"
    rm app.pid
else
    echo "未找到 PID 文件"
fi
EOF

chmod +x /www/wwwroot/oj/backend/stop.sh
```

### 5.3 创建重启脚本

```bash
cat > /www/wwwroot/oj/backend/restart.sh << 'EOF'
#!/bin/bash
cd /www/wwwroot/oj/backend
./stop.sh
sleep 3
./start.sh
EOF

chmod +x /www/wwwroot/oj/backend/restart.sh
```

### 5.4 启动服务

```bash
# 启动
cd /www/wwwroot/oj/backend
./start.sh

# 查看日志
tail -f logs/app.log

# 测试服务
curl http://localhost:8802/api/doc.html
```

### 5.5 配置开机自启动（可选）

使用 systemd 管理服务：

```bash
# 创建服务文件
cat > /etc/systemd/system/oj-backend.service << 'EOF'
[Unit]
Description=OJ Backend Service
After=network.target mysql.service redis.service

[Service]
Type=simple
User=root
WorkingDirectory=/www/wwwroot/oj/backend
ExecStart=/usr/bin/java \
  -Xms256m \
  -Xmx768m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -Dspring.profiles.active=prod \
  -jar oj_kkdj_backend-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# 启用并启动服务
systemctl daemon-reload
systemctl enable oj-backend
systemctl start oj-backend

# 查看状态
systemctl status oj-backend
```

---

## 六、配置 Nginx

### 6.1 宝塔面板配置站点

1. 宝塔面板 → 网站 → 添加站点
2. 填写信息：
   - 域名：你的域名（没有域名就填服务器 IP）
   - 根目录：`/www/wwwroot/oj/frontend`
   - PHP版本：纯静态
   - 创建数据库：不创建（已创建）

3. 点击提交

### 6.2 配置反向代理

1. 点击刚创建的站点 → 设置
2. 点击"反向代理" → "添加反向代理"
3. 填写：
   - 代理名称：oj-api
   - 目标URL：`http://127.0.0.1:8802`
   - 发送域名：`$host`
   - 代理目录：`/api`

或者手动修改配置：

1. 点击"配置文件"
2. 在 `server` 块内添加：

```nginx
# API 反向代理
location /api/ {
    proxy_pass http://127.0.0.1:8802/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    
    # 超时配置（判题可能较慢）
    proxy_connect_timeout 60s;
    proxy_read_timeout 120s;
    proxy_send_timeout 120s;
}

# Vue Router History 模式支持
location / {
    try_files $uri $uri/ /index.html;
}

# 静态资源缓存
location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
    expires 7d;
    add_header Cache-Control "public";
}

# Gzip 压缩
gzip on;
gzip_types text/plain text/css application/json application/javascript text/xml application/xml;
gzip_min_length 1024;
```

### 6.3 完整 Nginx 配置示例

```nginx
server {
    listen 80;
    server_name your-domain.com;  # 改成你的域名或 IP
    
    root /www/wwwroot/oj/frontend;
    index index.html;

    # Gzip 压缩
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml;
    gzip_min_length 1024;

    # API 反向代理
    location /api/ {
        proxy_pass http://127.0.0.1:8802/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        proxy_connect_timeout 60s;
        proxy_read_timeout 120s;
        proxy_send_timeout 120s;
    }

    # Vue Router History 模式
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 7d;
        add_header Cache-Control "public";
    }
}
```

保存后，重载 Nginx：

```bash
nginx -t          # 测试配置
nginx -s reload   # 重载配置
```

或在宝塔面板中点击"重载配置"。

### 6.4 配置 HTTPS（可选）

如果有 SSL 证书：

1. 宝塔面板 → 网站 → 点击站点 → SSL
2. 选择"其他证书"
3. 粘贴证书内容（.pem/.crt）和私钥内容（.key）
4. 保存并启用

---

## 七、验证部署

### 7.1 检查服务状态

```bash
# 检查后端
systemctl status oj-backend
# 或
ps aux | grep java

# 检查 Nginx
systemctl status nginx

# 检查 MySQL
systemctl status mysql

# 检查 Redis
systemctl status redis
```

### 7.2 检查端口

```bash
# 查看端口占用
netstat -tlnp | grep -E '80|8802|3306|6379'
```

应该看到：
- 80：nginx
- 8802：java
- 3306：mysql
- 6379：redis

### 7.3 访问测试

在浏览器中访问：

1. **前端页面**：`http://你的域名或IP/`
   - 应该能看到 OJ 系统首页

2. **API 文档**：`http://你的域名或IP/api/doc.html`
   - 应该能看到 Swagger 文档

3. **注册登录**：尝试注册一个用户

4. **提交代码**：尝试提交一道题目的代码，测试判题功能

---

## 八、常见问题排查

### Q1: 访问前端页面 502 错误？

**原因**：后端服务未启动或端口不对

**解决**：
```bash
# 检查后端服务
systemctl status oj-backend
# 或查看日志
tail -f /www/wwwroot/oj/backend/logs/app.log
```

### Q2: 访问前端页面空白？

**原因**：前端路由配置问题或静态文件路径错误

**解决**：
1. 检查 Nginx 配置中的 `root` 路径是否正确
2. 检查 `try_files` 配置是否正确
3. 检查前端 `dist` 文件是否完整上传

### Q3: 判题一直等待？

**原因**：Judge0 API 无法访问

**解决**：
```bash
# 测试 Judge0 API
curl https://ce.judge0.com/submissions

# 如果无法访问，检查服务器是否能访问外网
ping google.com
```

### Q4: 数据库连接失败？

**原因**：数据库配置错误或密码不对

**解决**：
```bash
# 测试数据库连接
mysql -u oj_user -p

# 检查配置文件中的数据库密码
cat /www/wwwroot/oj/backend/application-prod.yml
```

### Q5: Redis 连接失败？

**原因**：Redis 密码未配置或配置错误

**解决**：
```bash
# 测试 Redis 连接
redis-cli -a 你的密码 ping

# 检查配置
cat /etc/redis/redis.conf | grep requirepass
```

### Q6: 内存不足？

**原因**：4G 内存可能不够用

**解决**：
```bash
# 查看内存使用
free -h

# 减少 JVM 内存
# 修改启动脚本中的 -Xmx 参数
# 如：-Xmx512m

# 重启服务
systemctl restart oj-backend
```

---

## 九、日常运维

### 9.1 查看日志

```bash
# 后端日志
tail -f /www/wwwroot/oj/backend/logs/app.log

# Nginx 日志
tail -f /www/wwwlogs/your-domain.com.log
tail -f /www/wwwlogs/your-domain.com.error.log
```

### 9.2 重启服务

```bash
# 重启后端
systemctl restart oj-backend

# 重启 Nginx
systemctl restart nginx

# 重启 MySQL
systemctl restart mysql

# 重启 Redis
systemctl restart redis
```

### 9.3 数据库备份

在宝塔面板中：
1. 计划任务 → 添加任务
2. 任务类型：备份网站
3. 执行周期：每天
4. 备份内容：选择 oj 数据库

或使用脚本：
```bash
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
mysqldump -u oj_user -p'密码' oj > /www/wwwroot/oj/backup/oj_$DATE.sql
find /www/wwwroot/oj/backup -name "oj_*.sql" -mtime +7 -delete
```

### 9.4 更新代码

```bash
# 1. 本地重新打包
mvn clean package -DskipTests
npm run build

# 2. 上传新文件覆盖旧文件

# 3. 重启后端
systemctl restart oj-backend

# 4. 清理 Nginx 缓存（如有）
nginx -s reload
```

---

## 十、部署完成检查清单

### 部署前
- [ ] 已购买服务器并获取 IP
- [ ] 已配置安全组开放必要端口（80、443、22、8888）
- [ ] 已准备域名（可选）

### 环境安装
- [ ] 已安装宝塔面板
- [ ] 已安装 Nginx
- [ ] 已安装 MySQL 8.0
- [ ] 已安装 Redis
- [ ] 已安装 JDK 8

### 数据库配置
- [ ] 已创建 oj 数据库
- [ ] 已创建数据库用户并授权
- [ ] 已导入所有 SQL 脚本

### 应用部署
- [ ] 已修改 application-prod.yml 配置
- [ ] 已上传后端 jar 包
- [ ] 已上传前端 dist 文件
- [ ] 已启动后端服务
- [ ] 已配置 Nginx 站点

### 验证测试
- [ ] 能正常访问首页
- [ ] 能正常注册用户
- [ ] 能正常登录
- [ ] 能正常查看题目列表
- [ ] 能正常提交代码判题
- [ ] API 文档能正常访问

---

## 十一、总结

本文档详细介绍了从零开始在 Ubuntu 服务器上部署 OJ 系统的完整流程：

| 阶段 | 主要工作 |
|------|----------|
| 环境准备 | 安装宝塔面板，一键安装 Nginx/MySQL/Redis/JDK |
| 数据库配置 | 创建数据库和用户，导入 SQL 脚本 |
| 项目打包 | 本地 Maven/npm 打包，上传到服务器 |
| 服务部署 | 启动后端服务，配置 Nginx 反向代理 |
| 验证测试 | 访问测试各项功能 |

**推荐使用宝塔面板**，可以大大简化运维工作，适合新手快速部署。

祝部署顺利！
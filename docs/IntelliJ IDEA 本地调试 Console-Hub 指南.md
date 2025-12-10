# IntelliJ IDEA 本地调试 Console-Hub 指南

本指南帮助你在 IntelliJ IDEA 中调试 Console-Hub (console/backend/hub)，同时其他服务继续在 Docker 中运行。

## 架构说明

```
浏览器 → Nginx (Docker) → Console-Hub (本地 IDEA) → Java Workflow (Docker)
```

## 前置准备

1. ✅ 确保已安装 IntelliJ IDEA
2. ✅ 确保已安装 Java 21
3. ✅ 确保已配置 Maven

## 步骤 1: 停止 Docker 中的 Console-Hub

```bash
cd docker/astronAgent
docker compose stop console-hub
docker compose rm -f console-hub
```

## 步骤 2: 配置本地环境变量

创建 `console/backend/hub/.env.local` 文件（从 Docker 环境变量复制）:

```bash
# 数据库配置
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DATABASE=astron_console
MYSQL_USER=root
MYSQL_PASSWORD=root123

# PostgreSQL 配置
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DATABASE=workflow_java
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres123

# Redis 配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis123

# MinIO 配置
MINIO_ENDPOINT=http://localhost:18999
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=astron

# Workflow 服务 URL (指向 Java 版本)
WORKFLOW_CHAT_URL=http://localhost:7880/api/v1/workflow/chat/stream
WORKFLOW_DEBUG_URL=http://localhost:7880/api/v1/workflow/chat/stream
WORKFLOW_RESUME_URL=http://localhost:7880/api/v1/workflow/chat/resume

# Casdoor 配置
CASDOOR_ENDPOINT=http://localhost:7001
CASDOOR_CLIENT_ID=your_client_id
CASDOOR_CLIENT_SECRET=your_client_secret
```

## 步骤 3: 在 IntelliJ IDEA 中打开项目

1. **File → Open** → 选择 `console/backend` 目录
2. **等待 Maven 依赖下载完成**
3. **确保 JDK 设置为 Java 21**:
   - File → Project Structure → Project SDK → 选择 21

## 步骤 4: 配置运行配置 (Run Configuration)

### 方式 1: 使用 Spring Boot 运行配置

1. 打开 `console/backend/hub/src/main/java/com/iflytek/astron/console/hub/HubApplication.java`
2. 右键点击 `main` 方法 → **Run 'HubApplication.main()'**
3. 点击 **Edit Configurations...**
4. 配置环境变量:

```
Environment Variables:
MYSQL_HOST=localhost;
MYSQL_PORT=3306;
MYSQL_DATABASE=astron_console;
MYSQL_USER=root;
MYSQL_PASSWORD=root123;
POSTGRES_HOST=localhost;
POSTGRES_PORT=5432;
POSTGRES_DATABASE=workflow_java;
POSTGRES_USER=postgres;
POSTGRES_PASSWORD=postgres123;
REDIS_HOST=localhost;
REDIS_PORT=6379;
REDIS_PASSWORD=redis123;
WORKFLOW_CHAT_URL=http://localhost:7880/api/v1/workflow/chat/stream;
WORKFLOW_DEBUG_URL=http://localhost:7880/api/v1/workflow/chat/stream;
WORKFLOW_RESUME_URL=http://localhost:7880/api/v1/workflow/chat/resume
```

5. **VM Options** (可选，用于调试):
```
-Xdebug
-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005
```

### 方式 2: 使用 Maven 运行配置

1. **Run → Edit Configurations...**
2. **+ → Maven**
3. 配置:
   - Name: `console-hub (local debug)`
   - Working directory: `$ProjectFileDir$/hub`
   - Command line: `spring-boot:run`
   - Environment variables: (同上)

## 步骤 5: 设置断点

在以下关键位置设置断点:

### 1️⃣ **WorkflowChatController.java** (入口点)
```java
// console/backend/hub/src/main/java/com/iflytek/astron/console/hub/controller/WorkflowChatController.java

@PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter workflowChatStream(@Valid @RequestBody WorkflowChatRequest request) {
    log.info("Starting workflow chat stream, flowId: {}, userId: {}, chatId: {}",
            request.getFlowId(), request.getUserId(), request.getChatId());  // 👈 在这里打断点

    return workflowChatService.workflowChatStream(request);  // 👈 在这里打断点
}
```

### 2️⃣ **WorkflowChatService.java** (服务层)
```java
// console/backend/hub/src/main/java/com/iflytek/astron/console/hub/service/WorkflowChatService.java

public SseEmitter workflowChatStream(WorkflowChatRequest request) {
    // 👈 在方法入口打断点
    // 查看如何调用 Java Workflow
}
```

### 3️⃣ **WorkflowClient.java** (HTTP 客户端)
```java
// console/backend/commons/src/main/java/com/iflytek/astron/console/commons/workflow/WorkflowClient.java

// 👈 找到发送 HTTP 请求到 Java Workflow 的代码，打断点
```

## 步骤 6: 启动调试

1. **点击 Debug 按钮** (或按 `Shift + F9`)
2. **等待服务启动** (看到 "Started HubApplication" 日志)
3. **验证服务运行**:
```bash
curl http://localhost:8080/actuator/health
```

## 步骤 7: 测试调试

### 浏览器测试
1. **打开浏览器**: `http://localhost/work_flow/184742/arrange?botId=57`
2. **打开开发者工具 (F12)** → Network 标签
3. **点击 "调试" 按钮**
4. **IDEA 中应该命中断点**

### curl 测试
```bash
curl -X POST http://localhost:8080/api/v1/workflow/chat/stream \
  -H "Content-Type: application/json" \
  -d '{
    "flowId": "184742",
    "inputs": {"user_input": "test"},
    "userId": "test-user",
    "chatId": "test-chat-123"
  }'
```

## 步骤 8: 调试技巧

### 查看请求参数
在断点处，使用 IDEA 的 **Evaluate Expression** (Alt + F8):
```java
request.getFlowId()
request.getInputs()
request.getUserId()
```

### 查看调用栈
在 **Debug 窗口** 中查看 **Frames** 标签，了解完整的调用链路。

### 查看变量值
在 **Variables 窗口** 中查看所有局部变量和字段。

### 条件断点
右键点击断点 → **Edit Breakpoint** → 设置条件:
```java
request.getFlowId().equals("184742")
```

## 常见问题

### ❌ 无法连接到 MySQL/PostgreSQL/Redis

**原因**: Docker 服务的端口可能没有映射到 localhost

**解决方案**:
```bash
# 检查端口映射
docker ps | grep -E "mysql|postgres|redis"

# 如果没有端口映射，修改 docker-compose.yaml 添加 ports:
# mysql:
#   ports:
#     - "3306:3306"
# postgres:
#   ports:
#     - "5432:5432"
# redis:
#   ports:
#     - "6379:6379"

# 重启服务
docker compose up -d mysql postgres redis
```

### ❌ 断点不命中

**检查清单**:
1. ✅ Console-Hub 是否在 IDEA 中运行 (不是 Docker)
2. ✅ Nginx 是否将请求路由到 localhost:8080 (而不是 Docker 中的 console-hub)
3. ✅ 浏览器请求的 URL 是否正确

**临时解决方案**: 直接用 curl 测试 localhost:8080，跳过 Nginx

### ❌ Nginx 仍然路由到 Docker 中的 console-hub

**解决方案**: 修改 Nginx 配置指向 host.docker.internal:8080

编辑 `docker/astronAgent/nginx/nginx.conf`:
```nginx
location /console-api/ {
    # 原来: proxy_pass http://console-hub:8080/;
    # 修改为:
    proxy_pass http://host.docker.internal:8080/;
    # ...
}
```

重启 Nginx:
```bash
docker compose restart nginx
```

## 快速启动脚本

创建 `scripts/debug-console-hub.sh`:

```bash
#!/bin/bash
# 停止 Docker 中的 console-hub
docker compose -f docker/astronAgent/docker-compose.yaml stop console-hub
docker compose -f docker/astronAgent/docker-compose.yaml rm -f console-hub

echo "✅ Docker console-hub 已停止"
echo "📝 现在可以在 IntelliJ IDEA 中启动 HubApplication"
echo "🔗 调试端点: http://localhost:8080/api/v1/workflow/chat/stream"
```

## 恢复 Docker 环境

调试完成后，恢复 Docker 环境:

```bash
# 停止 IDEA 中的 console-hub
# 重新启动 Docker 中的 console-hub
cd docker/astronAgent
docker compose up -d console-hub
```

## 推荐调试流程

1. **启动所有 Docker 服务 (除了 console-hub)**
2. **在 IDEA 中以 Debug 模式启动 console-hub**
3. **在关键方法打断点**
4. **在浏览器中触发工作流**
5. **单步调试，查看变量值**
6. **完成后恢复 Docker 环境**

## 核心断点位置总结

| 文件 | 方法 | 作用 |
|------|------|------|
| `WorkflowChatController.java` | `workflowChatStream()` | 接收前端请求 |
| `WorkflowChatService.java` | `workflowChatStream()` | 调用 Workflow 服务 |
| `WorkflowClient.java` | HTTP 调用方法 | 发送请求到 Java Workflow |

---

祝调试顺利! 🎉

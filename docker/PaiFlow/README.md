# PaiFlow Docker 部署教程

欢迎来到 PaiFlow 的 Docker 部署指南！

这篇文档主要是为了帮助大家快速上手，把 PaiFlow 这个项目在本地跑起来。我们专门设计了一套“一键启动”的方案，利用 Docker 的能力，帮你屏蔽掉繁琐的环境配置问题。不管你用的是 Windows、Mac 还是 Linux，只要你的电脑上装了 Docker，就能轻松搞定。

## 准备工作

在开始之前，请确保你的电脑上已经安装了下面这两个工具：

1.  **Docker Engine** (推荐 20.10 及以上版本)
2.  **Docker Compose** (推荐 2.0 及以上版本)

你**不需要**在本地安装 Java、Maven 或者 Node.js 环境。所有的编译和运行工作，都会在 Docker 容器里自动完成，保持你本地环境的整洁。

---

## 它是怎么工作的？

为了让你对整个过程有个清晰的认识，我们画了一张图来解释 Docker 是怎么把代码变成可运行的服务的。

我们采用了一种叫做**“多阶段构建”**的技术。你可以把它想象成“厨房”和“餐厅”的关系：我们在一个装满工具的“厨房容器”里把代码做成“菜”（编译成 Jar 包或静态文件），然后把“菜”端到一个干净清爽的“餐厅容器”里运行。这样做出来的镜像非常小，运行效率也高。

```mermaid
graph TD
    subgraph "第一步：基础设施准备"
        MySQL[MySQL 数据库]
        Redis[Redis 缓存]
        MinIO[MinIO 对象存储]
    end

    subgraph "第二步：前端构建 (Dockerfile.frontend)"
        SrcFront[前端源码 console/frontend] -->|复制| NodeEnv[Node.js 构建环境]
        NodeEnv -->|npm run build| StaticFiles[生成静态资源 HTML/CSS/JS]
        StaticFiles -->|复制| Nginx[Nginx 服务器]
    end

    subgraph "第三步：后端构建 (Dockerfile.backend)"
        SrcBack[后端源码 console/backend] -->|复制| MavenEnv[Maven 构建环境]
        MavenEnv -->|mvn package| JarFile[生成 Jar 包]
        JarFile -->|复制| JRE[Java 运行环境 JDK 21]
    end

    MySQL -.->|等待启动| JRE
    Redis -.->|等待启动| JRE
    MinIO -.->|等待启动| JRE
    
    style Nginx fill:#e1f5fe,stroke:#01579b
    style JRE fill:#e8f5e9,stroke:#2e7d32
    style NodeEnv fill:#fff3e0,stroke:#ef6c00
    style MavenEnv fill:#fff3e0,stroke:#ef6c00
```

### 深入理解 Dockerfile

也许你对 Dockerfile 里的内容感到好奇，我们把三个核心服务的构建过程拆解开来看看。

#### 1. 后端构建 (`Dockerfile.backend`)
这是 Java 后端服务的构建过程。

```dockerfile
# 1. 定义“厨房”：使用 Maven 和 JDK 21 的镜像作为构建环境
# 这个镜像标签 (Tag) 包含了丰富的信息：
# - 3.9.9：代表 Maven 的版本是 3.9.9
# - eclipse-temurin-21：代表 JDK 的版本是 21 (来自 Eclipse Temurin 发行版)
# - noble：代表底层的 Linux 操作系统是 Ubuntu 24.04 LTS (代号 Noble Numbat)
# 注意：这些工具是 Docker 从互联网上拉取的，完全独立于你本机的环境。
FROM maven:3.9.9-eclipse-temurin-21-noble AS build
WORKDIR /app

# 2. 把源代码“搬进厨房”
COPY console/backend /app/console/backend

# 3. 开始“做菜”：进入目录并运行 Maven 打包命令
WORKDIR /app/console/backend
# -DskipTests 表示跳过单元测试，加快构建速度
RUN mvn clean package -DskipTests

# 4. 定义“餐厅”：使用轻量级的 JRE 21 镜像作为运行环境
FROM eclipse-temurin:21-jre-noble
WORKDIR /app

# 5. 设置时区为上海时间，避免日志时间错乱
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 6. 把“做好的菜”（Jar 包）从“厨房”端到“餐厅”
COPY --from=build /app/console/backend/hub/target/hub-server.jar /app/hub-server.jar

# 7. 准备日志目录和暴露端口
RUN mkdir -p /app/logs
EXPOSE 8080

# 8. 启动应用：配置 Java 内存参数并运行 Jar 包
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/hub-server.jar"]
```

#### 2. 前端构建 (`Dockerfile.frontend`)
这是 React 前端页面的构建过程。

```dockerfile
# 1. 定义“厨房”：使用 Node.js 环境
FROM node:18-alpine AS builder
WORKDIR /app

# 2. 复制代码并安装依赖
COPY console/frontend /app/console/frontend
WORKDIR /app/console/frontend
# npm ci 是一种更干净、更快速的安装依赖方式
RUN npm config set registry https://registry.npmjs.org/ && \
    npm ci --legacy-peer-deps && \
    npm run build-prod

# 3. 定义“餐厅”：使用 Nginx 服务器
FROM nginx:1.15-alpine

# 4. 配置 Nginx（省略了部分细节配置）
RUN echo "..." > /etc/nginx/nginx.conf

# 5. 把编译好的静态文件（HTML/CSS/JS）从“厨房”搬到 Nginx 的目录
COPY --from=builder /app/console/frontend/dist /var/www

# 6. 启动 Nginx
ENTRYPOINT ["/docker-entrypoint.sh"]
```

#### 3. 工作流构建 (`Dockerfile.workflow`)
工作流引擎也是 Java 项目，所以它的构建过程和后端几乎一模一样，也是 Maven 编译 -> 复制 Jar 包 -> JRE 运行。

```dockerfile
# 1. Maven 构建环境
FROM maven:3.9.9-eclipse-temurin-21-noble AS build
# ... (复制源码并 mvn package)

# 2. JRE 运行环境
FROM eclipse-temurin:21-jre-noble
# ... (复制 workflow-java.jar)

# 3. 启动应用
ENTRYPOINT ["java", ..., "-jar", "/app/workflow-java.jar"]
```

---

## 动手试试：一键启动

打开你的终端（Terminal 或 CMD），进入到当前这个 `docker/PaiFlow` 目录，然后执行下面这行命令：

```bash
docker-compose up -d --build
```

### 这个命令到底干了什么？

当你敲下回车后，Docker 会忙活好一阵子，具体流程是这样的：

1.  **下载基础镜像**：如果你本地没有 `maven`、`node`、`mysql` 等镜像，Docker 会先去互联网下载它们。
2.  **构建前端**：它会读取 `Dockerfile.frontend`，启动一个临时的 Node.js 容器，把 React 代码编译成 HTML 和 JS 文件。
3.  **构建后端**：同时，它会读取 `Dockerfile.backend`，启动一个临时的 Maven 容器，下载 Java 依赖包（这一步取决于网速，可能需要几分钟），然后把代码编译成 `.jar` 文件。
4.  **启动数据库**：构建完成后，Docker 会先启动 MySQL、Redis 和 MinIO，并等待它们初始化完成。
5.  **启动应用**：最后，它会启动前端 Nginx 和后端 Java 应用，让它们连接到已经准备好的数据库上。

> **温馨提示**：第一次运行时，因为要下载大量的 Maven 依赖（Jar 包）和 NPM 依赖，可能需要 5-10 分钟，请耐心喝杯咖啡等待一下。之后的运行就会非常快了。

### 验证是否成功

当命令执行完毕，且没有报错时，你可以打开浏览器看看效果：

*   **想看界面？** 访问前端：[http://localhost:3000](http://localhost:3000)
*   **想看接口？** 访问后端：[http://localhost:8080](http://localhost:8080)
*   **想看工作流？** 访问引擎：[http://localhost:7880](http://localhost:7880)
*   **想看文件存储？** 访问 MinIO：[http://localhost:19001](http://localhost:19001)
    *   账号：`minioadmin`
    *   密码：`minioadmin123`
    *   API 端口：`19000` (代码连接使用)

如果能看到画面，恭喜你，部署成功了！

### 停止服务

玩够了想关闭？在终端里运行：

```bash
docker-compose down
```

如果你想把数据库里的数据也清空，重新来过，可以加一个 `-v` 参数：`docker-compose down -v`。

---

## 常见问题解答 (FAQ)

**Q: 启动时提示端口被占用（Port already in use）怎么办？**

A: 这通常是因为你本地已经运行了 MySQL (3306) 或者 Redis (6379)。
*   **方法一（推荐）**：关掉你本地冲突的服务。
*   **方法二**：打开 `docker-compose.yaml`，找到冲突的服务，修改 `ports` 部分。
*   **注意**：为了防止冲突，我们将 MinIO 的默认端口改为了 `19000` (API) 和 `19001` (控制台)。

**Q: 数据库名为什么是 `paiflow_console`？**

A: 我们在 `docker-compose.yaml` 里把 MySQL 的默认数据库设置成了 `paiflow_console`。这是为了配合后端代码里的配置。如果随意修改名字，后端程序启动时找不到对应的数据库就会报错。

**Q: 第一次运行数据库是空的吗？**

A: **不是的**。我们已经为您准备好了数据库初始化脚本，它们位于 `docker/PaiFlow/mysql/` 目录下。
当您第一次运行 `docker-compose up` 时，MySQL 容器会自动执行这些 SQL 脚本，创建所有必要的表结构和初始数据。
所以您不需要手动做任何事情。

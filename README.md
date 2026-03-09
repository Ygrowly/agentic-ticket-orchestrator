# PaiFlow - 企业级 AI Agent 工作流编排平台

<div align="center">

[![License](https://img.shields.io/badge/license-apache2.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Python](https://img.shields.io/badge/Python-3.11+-blue.svg)](https://www.python.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen.svg)](https://spring.io/)

</div>

## 项目简介

**PaiFlow（派派工作流）** 是一个企业级的 AI Agent 工作流编排平台，支持用户通过可视化方式编排大模型节点、工具节点和流程逻辑等。类似于 Dify、Coze、n8n 的可视化流程编排平台。

![](https://cdn.tobebetterjavaer.com/stutymore/README-20251028201755.png)

### 核心特性

- **可视化工作流编排**：拖拽式节点编排，支持 LLM 节点、工具节点、条件分支等
- **双引擎支持**：Python 版（FastAPI）和 Java 版（Spring Boot 3.5 + LangGraph4J）
- **多模型集成**：支持 DeepSeek、OpenAI、通义千问等多种大模型
- **实时流式输出**：基于 SSE 的实时推送，边跑边看结果
- **插件生态**：支持 MCP 协议、工具注册系统
- **一键部署**：Docker Compose 编排，开箱即用

## 技术栈

### 后端技术

| 技术 | 说明 |
|------|------|
| JDK 21 | 虚拟线程、Record 类型 |
| Spring Boot 3.5 | 微服务框架 |
| Spring AI 1.1 | 大模型统一抽象层 |
| LangGraph4J | Agent 编排框架 |
| FastAPI | Python 异步框架 |
| Pydantic | 数据验证 |
| MyBatis-Plus 3.5 | 持久层框架 |
| PostgreSQL | 工作流数据存储 |
| MySQL | 业务数据存储 |
| Redis 7 | 分布式缓存 |
| MinIO | 对象存储 |

### 前端技术

| 技术 | 说明 |
|------|------|
| React 18 | UI 框架 |
| TypeScript | 类型安全 |
| Ant Design 5 | UI 组件库 |
| ReactFlow | 工作流可视化 |
| Monaco Editor | 代码编辑器 |
| Vite | 构建工具 |

## 项目结构

```
PaiFlow/
├── console/                    # 控制台服务
│   ├── backend/                # Spring Boot 后端 (8080)
│   └── frontend/               # React 前端 (1881)
├── core/                       # Python 工作流引擎
│   ├── agent/                  # Agent 核心实现
│   ├── plugin/                 # 插件服务
│   │   ├── aitools/            # AI 工具服务 (18668)
│   │   └── link/               # 链路服务
│   ├── workflow/               # 工作流引擎 (7880)
│   └── common/                 # 公共模块
├── core-workflow-java/         # Java 工作流引擎 (7880)
├── docker/                     # Docker 部署配置
├── docs/                       # 项目文档
└── scripts/                    # 工具脚本
```

## 架构设计

整个项目架构分为四层：

```
┌─────────────────────────────────────────────────────────┐
│                    前端表达层 (React)                      │
│     工作流编排 │ 节点配置 │ 执行监控 │ 日志查看               │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                  控制台中枢层 (Hub)                        │
│   用户鉴权 │ 模型管理 │ 流程元数据 │ 执行调度                │
└─────────────────────────────────────────────────────────┘
                           │
           ┌───────────────┴───────────────┐
           ▼                               ▼
┌─────────────────────┐         ┌─────────────────────┐
│  Python 工作流引擎   │         │   Java 工作流引擎    │
│  (FastAPI + LangChain)│        │ (Spring Boot + LangGraph4J) │
└─────────────────────┘         └─────────────────────┘
           │                               │
           └───────────────┬───────────────┘
                           ▼
┌─────────────────────────────────────────────────────────┐
│                   底层基础设施层                           │
│   PostgreSQL │ MySQL │ Redis │ MinIO │ Nginx             │
└─────────────────────────────────────────────────────────┘
```

### 工作流引擎核心机制

- **DSL 驱动**：解析 DSL 定义，构建内存中的工作流对象
- **节点执行器解耦**：策略模式 + 工厂模式 + 责任链模式
- **变量池隔离**：VariablePool 管理节点间数据传递
- **拓扑排序**：DAG 链路解析，支持并行执行

## 快速开始

### 环境要求

- Docker 20.10+
- Docker Compose 2.0+
- 8GB+ 可用内存
- 20GB+ 可用磁盘空间

### 一键部署

```bash
# 1. 克隆项目
git clone https://github.com/itwanger/PaiFlow.git
cd PaiFlow/docker

# 2. 复制环境变量配置
cp .env.example .env

# 3. 启动所有服务
docker compose up -d

# 4. 查看服务状态
docker compose ps
```

### 访问应用

启动完成后，访问以下地址：

- **应用前端**：http://localhost
- **默认账户**：admin / 123

### 切换工作流引擎

Python 版和 Java 版共用端口 7880，修改 Hub 配置即可切换：

```yaml
# 使用 Python 引擎
workflow:
  engine: python
  endpoint: http://core-workflow:7880

# 使用 Java 引擎
workflow:
  engine: java
  endpoint: http://core-workflow-java:7880
```

## 核心功能

### 1. AI 播客生成工作流

用户输入主题 → LLM 改写为播客风格 → TTS 语音合成 → 输出音频

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│  输入节点  │ ──▶ │  LLM节点  │ ──▶ │  TTS节点  │ ──▶ │  输出节点  │
└──────────┘     └──────────┘     └──────────┘     └──────────┘
```

### 2. 可视化工作流编排

- 拖拽式节点编排
- 实时预览和调试
- 支持条件分支、并行执行
- 节点参数可视化配置

### 3. SSE 实时推送

```java
// LLM 节点执行时的回调
LlmResVo llmOutput = modelServiceClient.chatCompletion(req, chatResponse -> {
    // 每收到一个 token 就往前端推一次
    nodeState.callback().onNodeProcess(
        0,
        node.getId(),
        chatResponse.getResult().getOutput().getText(),
        chatResponse.getResult().getOutput().getMetadata().get("reasoningContent")
    );
});
```

## 服务端口

| 服务 | 说明 | 端口 |
|------|------|------|
| nginx | 反向代理 | 80 |
| console-hub | 控制台后端 | 8080 |
| console-frontend | 前端界面 | 1881 |
| core-workflow | Python 工作流引擎 | 7880 |
| core-workflow-java | Java 工作流引擎 | 7880 |
| core-aitools | AI 工具服务 | 18668 |
| postgres | PostgreSQL | 5432 |
| mysql | MySQL | 3306 |
| redis | Redis | 6379 |
| minio | 对象存储 | 18998/18999 |

## 技术亮点

### JDK 21 虚拟线程

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> executeNode(node, variablePool, callback));
}
```

### Spring AI 多模型统一

```java
ChatClient chatClient = ChatClient.create(chatModel);
Flux<ChatResponse> stream = chatClient.prompt()
    .user(prompt)
    .stream()
    .chatResponse();
```

### 分布式追踪

通过 OpenTelemetry 对整个工作流引擎进行埋点，精确回溯每个节点的耗时。

## 常见问题

### 工作流执行失败？

检查以下配置：

1. **工具版本号**：确保 `tools_schema` 表中工具版本为 `V1.0`
2. **服务地址**：超拟人合成服务地址应为 `http://core-aitools:18668`
3. **app_id**：确保工具的 `app_id` 与工作流一致

### 容器重启后出现 502 错误？

```bash
docker compose restart nginx
```

### 查看服务日志

```bash
# 查看所有服务日志
docker compose logs -f

# 查看特定服务日志
docker compose logs -f console-hub
docker compose logs -f core-workflow
```

## 学习资源

- **项目教程**：https://paicoding.com/column/13/1
- **项目介绍**：https://javabetter.cn/zhishixingqiu/paiflow.html

## 贡献指南

如果您有任何建议或发现问题，欢迎提交 Issue 或 Pull Request。

## 开源协议

本项目基于 Apache 2.0 协议开源。

## 致谢

- [讯飞 AstronAgent](https://github.com/iflytek/astron-agent) - 提供强大的智能体开发平台
- [Spring AI](https://spring.io/projects/spring-ai) - 大模型集成框架
- [LangGraph4J](https://github.com/langgraph4j/langgraph4j) - Agent 编排框架

---

**PaiFlow** - 让 AI Agent 开发更简单

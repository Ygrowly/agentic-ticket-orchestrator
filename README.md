# 企业运维告警与工单处置 Agent 工作流编排系统

> **项目状态**：方向验证型作品，2026-04 后停止迭代，不再跟进。
>
> **命名说明**：仓库名 `agentic-ticket-orchestrator`，代码与文档中的项目名为 **PaiFlow**——两个名字指同一个东西，是历史遗留，未统一。
>
> **它验证什么**：把「告警接入 → 异常识别 → 自动建单 → SOP / 历史案例检索 → 处置建议 → 工具检查 → 结果反馈」串成一条可编排的工作流，并把每一步的执行过程实时推给使用者看，而不是只给一个最终结论。
>
> **它不是什么**：没接真实告警源，不是生产系统；没有多租户与权限模型；流程节点的 LLM 调用只在样例流程上手工验证过，未做规模化评测。

<div align="center">

[![License](https://img.shields.io/badge/license-apache2.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Python](https://img.shields.io/badge/Python-3.11+-blue.svg)](https://www.python.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen.svg)](https://spring.io/)

</div>

## 项目简介

 围绕企业运维告警与工单处置场景构建。传统运维流程里，告警出现后需要人工判断异常类型、创建工单、查 SOP、执行工具检查、记录处理结果，链路长且依赖人工经验。PaiFlow 做的是 Agent 工作流编排系统，把告警接入、异常识别、工单生成、SOP / 历史案例检索、处置建议、工具检查和结果反馈串成自动化流程，提升响应效率和可追踪性。


### 核心特性

- **告警驱动的工作流编排**：围绕告警接入、异常识别、工单生成、处置建议、工具检查和结果反馈组织流程
- **可视化流程配置**：拖拽式节点编排，支持 LLM 节点、工具节点、条件分支、并行检查和异常兜底
- **双引擎支持**：Python 版（FastAPI）和 Java 版（Spring Boot 3.5 + LangGraph4J）
- **多模型集成**：支持 DeepSeek、OpenAI、通义千问等多种大模型
- **实时处置过程反馈**：基于 SSE 推送节点执行进度、模型分析结果、工具检查日志和最终处置结论
- **知识与工具协同**：支持 SOP、历史案例、知识库检索，并通过插件机制调用巡检、诊断、查询等运维工具
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
| MinIO | 工单附件、诊断报告和对象存储 |

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
│   │   └── link/               # 工具链路服务
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
│     告警处置编排 │ 节点配置 │ 执行监控 │ 工单日志               │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                  控制台中枢层 (Hub)                        │
│   用户鉴权 │ 模型管理 │ 工单元数据 │ 执行调度                │
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
cd PaiFlow/docker/PaiFlow

# 2. 复制环境变量配置
cp .env.example .env

# 3. 启动所有服务
docker compose up -d

# 4. 查看服务状态
docker compose ps
```

### 访问应用

启动完成后，访问以下地址：

- **应用前端**：http://localhost:3000
- **控制台后端**：http://localhost:8081
- **MinIO Console**：http://localhost:9001
- **默认账户**：admin / 123

如果你之前已经启动过旧版本 Docker 环境，`mysql_data` 持久卷里的旧表结构不会随着新 SQL 自动升级。这种情况下如果出现中文/emoji 乱码，执行：

```bash
./fix-docker-mysql-charset.sh
```

如果是全新安装且不需要保留旧数据，也可以直接删除旧卷后重新初始化：

```bash
docker compose down -v
docker compose up -d
```

### 切换工作流引擎

当前 `docker/PaiFlow/docker-compose.yaml` 默认只启动 Java 工作流引擎 `core-workflow-java`。下面的配置说明是项目层面的引擎切换思路，如果要在 Docker 中切到 Python 版，还需要额外补充对应服务定义。

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

### 1. 告警到工单的自动化处置工作流

告警接入 → 异常识别 → 工单生成 → SOP / 历史案例检索 → 处置建议 → 工具检查 → 结果反馈

```
┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐
│ 告警接入 │ ─▶ │ 异常识别 │ ─▶ │ 工单生成 │ ─▶ │ 工具检查 │ ─▶ │ 结果反馈 │
└──────────┘   └──────────┘   └──────────┘   └──────────┘   └──────────┘
```

### 2. SOP 与历史案例检索

- 按告警类型、服务、错误码、组件标签检索 SOP
- 结合历史工单和处理记录生成处置建议
- 支持人工确认、自动执行和异常兜底分支
- 将推荐依据、工具结果和最终结论回写到工单

### 3. 可视化工作流编排

- 拖拽式节点编排
- 实时预览和调试
- 支持条件分支、并行执行
- 节点参数可视化配置

### 4. SSE 实时推送

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
| console-hub | 控制台后端 | 8081 |
| console-frontend | 前端界面 | 3000 |
| core-workflow-java | Java 工作流引擎 | 7880 |
| mysql | MySQL | 3307 |
| redis | Redis | 6379 |
| minio | 对象存储 | 9000/9001 |

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

通过 OpenTelemetry 对整个工作流引擎进行埋点，精确回溯告警处置链路中每个节点的耗时和执行结果。

## 常见问题

### 工作流执行失败？

检查以下配置：

1. **工具版本号**：确保 `tools_schema` 表中工具版本为 `V1.0`
2. **服务地址**：工具服务地址应为 `http://core-aitools:18668`
3. **app_id**：确保工具的 `app_id` 与工作流一致

### Docker 部署后中文或 emoji 乱码？

```bash
./fix-docker-mysql-charset.sh
```

如果不需要保留已有数据，也可以直接删除持久卷后重新初始化：

```bash
docker compose down -v
docker compose up -d
```

### 查看服务日志

```bash
# 查看所有服务日志
docker compose logs -f

# 查看特定服务日志
docker compose logs -f console-hub
docker compose logs -f core-workflow-java
```


## 贡献指南

如果您有任何建议或发现问题，欢迎提交 Issue 或 Pull Request。

## 开源协议

本项目基于 Apache 2.0 协议开源。



---

 - 让企业运维告警处置更自动化、更可追踪

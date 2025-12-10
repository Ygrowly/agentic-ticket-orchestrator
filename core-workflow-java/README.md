# Java Workflow 开发指南

## 项目结构

```
core-workflow-java/
├── src/
│   ├── main/
│   │   ├── java/com/iflytek/astron/workflow/
│   │   │   ├── WorkflowApplication.java     # 启动类
│   │   │   ├── controller/                  # REST API控制器
│   │   │   ├── engine/                      # 工作流引擎核心
│   │   │   │   ├── WorkflowEngine.java      # 工作流执行引擎
│   │   │   │   ├── VariablePool.java        # 变量池管理
│   │   │   │   ├── constants/               # 枚举常量定义
│   │   │   │   ├── context/                 # 上下文管理
│   │   │   │   ├── domain/                  # 领域模型
│   │   │   │   ├── integration/             # 集成服务
│   │   │   │   ├── node/                    # 节点执行器
│   │   │   │   │   ├── AbstractNodeExecutor.java  # 抽象节点执行器
│   │   │   │   │   ├── NodeExecutor.java    # 节点执行器接口
│   │   │   │   │   ├── StreamCallback.java  # 流式回调接口
│   │   │   │   │   ├── callback/            # 回调实现
│   │   │   │   │   └── impl/                # 具体节点实现
│   │   │   │   └── util/                    # 工具类
│   │   │   ├── exception/                   # 异常处理
│   │   │   ├── flow/                        # 工作流服务
│   │   │   └── config/                      # 配置类
│   │   └── resources/
│   │       ├── application.yml              # 配置文件
│   │       └── mapper/                      # MyBatis Mapper XML
│   └── test/                                # 测试代码
├── pom.xml                                  # Maven 配置
├── Dockerfile                               # Docker 镜像
└── README.md                                # 本文件
```

## 快速开始

### 1. 环境准备

确保已安装：
- JDK 21（通过 jenv 管理）
- Maven 3.8+
- Docker & Docker Compose

### 2. 本地开发

```bash
# 设置 Java 版本
jenv local 21

# 编译项目
mvn clean package -DskipTests

# 本地运行（不使用 Docker）
java -jar target/workflow-java.jar

# 访问健康检查
curl http://localhost:7881/actuator/health
```

### 3. Docker 部署

```bash
# 方式 1：使用快速重启脚本（推荐）
./scripts/restart-java-workflow.sh

# 方式 2：手动构建和启动
cd docker/astronAgent
docker compose -f docker-compose.workflow-dual.yml --profile java-workflow up -d core-workflow-java

# 查看日志
docker logs -f astron-agent-core-workflow-java
```


window操作系统，docker本地部署步骤

- Step1: idea，选中 core-workflow-java/pom.xml 文件 -> 鼠标右键 -> Run Maven -> Package
- Step2: 打开Window PowerShell，进入工程所在目录，进入 core-workflow-java 目录下
- Step3: 执行构建镜像命令 `docker build -t core-workflow-java:latest .`
- Step4: 运行镜像

```bash
# 为了避免docker内应用，无法直接访问本机的mysql、redis、minio，我们通过传入环境参数来替换默认的配置
# 前台执行
docker run -p 7880:7880 -e OSS_ENDPOINT=http://host.docker.internal:9000 -e OSS_REMOTE_ENDPOINT=http://host.docker.internal:9000 -e MYSQL_HOST=host.docker.internal -e REDIS_HOST=host.docker.internal -e AITOOLS_URL=http://host.docker.internal:18888 core-workflow-java

# 后台执行，通过添加 -d 来表示后台运行(detached mode)
docker run -d -p 7880:7880 -e OSS_ENDPOINT=http://host.docker.internal:9000 -e OSS_REMOTE_ENDPOINT=http://host.docker.internal:9000 -e MYSQL_HOST=host.docker.internal -e REDIS_HOST=host.docker.internal -e AITOOLS_URL=http://host.docker.internal:18888 core-workflow-java
```

## 核心概念

### 工作流DSL (Domain Specific Language)
工作流由节点（Node）和边（Edge）组成，定义了执行流程和数据传递方式：
- **Node（节点）**：执行单元，包含不同类型如StartNode、LLMNode、PluginNode、EndNode等
- **Edge（边）**：连接节点，定义执行顺序和数据流向
- **VariablePool（变量池）**：存储节点间传递的数据

### 节点类型
- **StartNode**：流程起点，接收初始输入参数
- **EndNode**：流程终点，生成最终输出结果
- **LLMNode**：调用大语言模型（如DeepSeek）进行文本处理
- **PluginNode**：调用插件工具（如讯飞语音合成）
- **其他节点**：条件判断、循环控制等流程控制节点

## 工作流引擎执行机制

### 流程编排执行

工作流引擎的核心是`WorkflowEngine`类，负责解析DSL并执行工作流：

1. **DSL解析与验证**：
   - 验证工作流定义的完整性（必须包含StartNode和EndNode）
   - 构建节点间的执行链路（根据Edge建立节点的前后关系）
   - 注册各类型节点的执行器

2. **执行链路构建**：
   - 识别StartNode作为入口点
   - 根据Edge建立节点间的NextNodes和FailNodes关系
   - 支持正常执行路径和异常处理路径的分支

3. **节点执行流程**：
   ```java
   // 执行当前节点
   NodeExecStatusEnum execStatus = executor.execute(node, variablePool, callback);
   
   // 根据执行结果决定后续流程
   if (execStatus == NodeExecStatusEnum.SUCCESS) {
       // 执行成功，继续正常流程
       executeNormalCondition(node, variablePool, callback);
   } else if (execStatus == NodeExecStatusEnum.ERR_FAIL_CONDITION) {
       // 执行失败，进入异常处理流程
       executeFailedCondition(node, variablePool, callback);
   }
   ```

4. **节点执行器机制**：
   - 每种节点类型都有对应的执行器实现`NodeExecutor`接口
   - 执行器继承`AbstractNodeExecutor`抽象类，实现通用逻辑
   - 具体执行逻辑在`executeNode`方法中实现
   - 支持重试机制和超时控制

### 节点定义与执行

#### 节点数据结构
每个节点包含以下核心属性：
- `id`：节点唯一标识符（格式："node-type::sequenceId"）
- `data`：节点配置数据，包括输入输出定义、参数配置等
- `status`：节点执行状态（INIT, RUNNING, SUCCESS, ERROR, SKIP等）
- `nextNodes`：后续执行节点列表
- `failNodes`：异常处理节点列表

#### 节点执行流程
1. **输入解析**：从VariablePool中解析节点所需输入参数
2. **执行逻辑**：调用具体节点的执行逻辑（如调用LLM或插件）
3. **输出存储**：将执行结果存入VariablePool供后续节点使用
4. **回调通知**：通过StreamCallback通知客户端执行进度

#### 示例：StartNode执行器
```java
@Component
public class StartNodeExecutor extends AbstractNodeExecutor {
    @Override
    protected NodeRunResult executeNode(NodeState nodeState, Map<String, Object> inputs) {
        Map<String, Object> outputs = new HashMap<>(inputs);

        NodeRunResult result = new NodeRunResult();
        result.setInputs(inputs);
        result.setOutputs(outputs);
        result.setStatus(NodeExecStatusEnum.SUCCESS);
        return result;
    }
}
```

## 消息回传机制 (Callback策略)

### SSE (Server-Sent Events) 实现
系统通过SSE实现实时消息推送，客户端可以实时接收工作流执行进度：

1. **回调接口设计**：
   - `StreamCallback`接口定义了各类回调方法
   - `SseStreamCallback`实现了SSE消息推送
   - `WorkflowStreamCallback`封装了工作流级别的回调处理

2. **消息类型**：
   - `onSparkflowStart()`：工作流开始
   - `onNodeStart()`：节点开始执行
   - `onNodeProcess()`：节点处理过程
   - `onNodeEnd()`：节点执行结束
   - `onSparkflowEnd()`：工作流结束
   - `onNodeInterrupt()`：节点中断

3. **消息推送机制**：
   ```java
   // 在WorkflowStreamCallback中异步处理消息队列
   AsyncUtil.execute(() -> {
       while (tag) {
           // 从队列中获取消息并推送给客户端
           LLMGenerate resp = streamQueue.poll();
           if (resp != null) {
               underlyingCallback.send("stream", resp);
           }
       }
   });
   ```

4. **API接口**：
   ```java
   @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
   public SseEmitter executeWorkflow(@RequestBody WorkflowRequest request) {
       SseEmitter emitter = new SseEmitter(600_000L);
       // 异步执行工作流并将结果通过SSE推送给客户端
       AsyncUtil.execute(() -> {
           StreamCallback callback = new SseStreamCallback(emitter);
           workflowEngine.execute(workflowDSL, new VariablePool(), request.getInputs(), callback);
       });
       return emitter;
   }
   ```

## 开发计划

### 修改代码后快速重启

```bash
# 一键重启（自动编译、构建镜像、重启容器）
./scripts/restart-java-workflow.sh
```

### 功能清单

- [x] 工作流执行引擎（实现前端编排的流程节点执行）
- [ ] 节点支持
  - [x] LLM节点
    - [x] 支持OpenAI风格的LLM模型调用
    - [x] 支持多轮对话历史（基于内存，保存单个Node的聊天历史）
    - [ ] 结构化返回：目前LLM调用，只返回文本；未支持结构化输出
  - [x] Plugin节点
  - [ ] 其他
- [x] 消息回传机制
  - [x] 工作流开始消息
  - [x] 节点执行消息：节点开始、处理中、结束、中断、LLM流式返回消息
  - [x] 工作流结束消息
- [x] Node三种异常策略支持
  - [x] Node执行超时限制
  - [x] 多次重试策略支持
  - [x] 中断策略
  - [x] 根据预设的错误输入、错误码继续走后面的流程
  - [x] 错误分支流程
- [x] 前端流程调试，直接通过 DebugController 进行完整的流程验证
- [ ] Node的循环执行
- [ ] Node的并发执行

## 调试技巧

### 1. 查看 Python 版本实现

```bash
# 查看 Python 版本的日志
docker logs -f astron-agent-core-workflow-python

# 对比执行结果
./scripts/compare-workflows.sh 184736
```

### 2. 使用 Python 版本作为参考

当 Java 版本出现问题时：
1. 切换到 Python 版本：`./scripts/switch-to-python.sh`
2. 观察 Python 版本的行为
3. 对比日志和输出
4. 修复 Java 代码
5. 重启 Java 版本：`./scripts/restart-java-workflow.sh`

### 3. 常用命令

```bash
# 查看 Java 日志
docker logs -f astron-agent-core-workflow-java

# 查看 Python 日志
docker logs -f astron-agent-core-workflow-python

# 健康检查
curl http://localhost:7881/actuator/health  # Java
curl http://localhost:7880/health           # Python

# 进入容器调试
docker exec -it astron-agent-core-workflow-java sh
```
## 相关文档

- [AGENTS.md](../AGENTS.md) - 项目整体开发指南
- [Makefile-readme.md](../docs/Makefile-readme.md) - 构建工具说明
- [Python Workflow 源码](../core/workflow/) - 参考实现
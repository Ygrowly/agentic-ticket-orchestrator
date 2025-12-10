#!/bin/bash

# Console-Hub 本地调试准备脚本
# 用途: 停止 Docker 中的 console-hub，准备在 IDEA 中调试

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DOCKER_DIR="$PROJECT_ROOT/docker/astronAgent"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Console-Hub 本地调试准备${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# 1. 停止 Docker 中的 console-hub
echo -e "${YELLOW}[1/4] 停止 Docker 中的 console-hub...${NC}"
cd "$DOCKER_DIR"
docker compose stop console-hub
docker compose rm -f console-hub
echo -e "${GREEN}✓ Console-Hub 容器已停止${NC}"
echo ""

# 2. 确保数据库端口已暴露
echo -e "${YELLOW}[2/4] 检查数据库端口映射...${NC}"

# 检查 MySQL 端口
if docker ps | grep astron-agent-mysql | grep -q "3306->3306"; then
    echo -e "${GREEN}✓ MySQL 端口 3306 已映射${NC}"
else
    echo -e "${RED}✗ MySQL 端口 3306 未映射${NC}"
    echo -e "${YELLOW}  需要在 docker-compose.yaml 中为 mysql 服务添加端口映射:${NC}"
    echo -e "${CYAN}  ports:${NC}"
    echo -e "${CYAN}    - \"3306:3306\"${NC}"
fi

# 检查 PostgreSQL 端口
if docker ps | grep astron-agent-postgres | grep -q "5432->5432"; then
    echo -e "${GREEN}✓ PostgreSQL 端口 5432 已映射${NC}"
else
    echo -e "${RED}✗ PostgreSQL 端口 5432 未映射${NC}"
fi

# 检查 Redis 端口
if docker ps | grep astron-agent-redis | grep -q "6379->6379"; then
    echo -e "${GREEN}✓ Redis 端口 6379 已映射${NC}"
else
    echo -e "${RED}✗ Redis 端口 6379 未映射${NC}"
fi
echo ""

# 3. 提取环境变量
echo -e "${YELLOW}[3/4] 生成 IDEA 环境变量配置...${NC}"

ENV_FILE="$PROJECT_ROOT/console/backend/hub/.env.local"

cat > "$ENV_FILE" << 'EOF'
# Console-Hub 本地调试环境变量
# 复制到 IntelliJ IDEA Run Configuration 的 Environment Variables

# MySQL 配置
MYSQL_URL=jdbc:mysql://localhost:3306/astron_console?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
MYSQL_USER=root
MYSQL_PASSWORD=root123

# Redis 配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_DATABASE_CONSOLE=1

# Workflow 服务 URL (Java 版本)
WORKFLOW_CHAT_URL=http://localhost:7880/api/v1/workflow/chat/stream
WORKFLOW_DEBUG_URL=http://localhost:7880/api/v1/workflow/chat/stream
WORKFLOW_RESUME_URL=http://localhost:7880/api/v1/workflow/chat/resume
WORKFLOW_URL=http://localhost:7880

# Workflow 配置服务
MAAS_WORKFLOW_VERSION=http://127.0.0.1:8080/workflow/version
MAAS_WORKFLOW_CONFIG=http://127.0.0.1:8080/workflow/get-flow-advanced-config

# Spring Boot 配置
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=local

# 日志级别
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_IFLYTEK=DEBUG
EOF

echo -e "${GREEN}✓ 环境变量配置已保存到: ${CYAN}$ENV_FILE${NC}"
echo ""

# 4. 显示 IDEA 配置指南
echo -e "${YELLOW}[4/4] IntelliJ IDEA 配置指南${NC}"
echo ""
echo -e "${CYAN}📝 IDEA 配置步骤:${NC}"
echo ""
echo -e "${BLUE}1. 打开 IntelliJ IDEA${NC}"
echo "   File → Open → $PROJECT_ROOT/console/backend"
echo ""
echo -e "${BLUE}2. 配置运行配置${NC}"
echo "   Run → Edit Configurations... → + → Spring Boot"
echo "   - Name: console-hub (local debug)"
echo "   - Main class: com.iflytek.astron.console.hub.HubApplication"
echo "   - Working directory: $PROJECT_ROOT/console/backend/hub"
echo "   - Use classpath of module: hub"
echo ""
echo -e "${BLUE}3. 初始化本地数据库 (如果还没有初始化)${NC}"
echo "   ./scripts/init-local-mysql.sh"
echo "   ./scripts/init-local-postgres.sh    # 可选，如果 Java Workflow 需要"
echo ""
echo -e "${BLUE}4. 添加环境变量 (复制以下内容)${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
cat "$ENV_FILE" | grep -v "^#" | grep -v "^$" | tr '\n' ';' | sed 's/;$/\n/'
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${YELLOW}提示: 环境变量格式为 KEY=VALUE;KEY2=VALUE2${NC}"
echo ""
echo -e "${BLUE}4. 设置断点${NC}"
echo "   文件: console/backend/hub/src/main/java/com/iflytek/astron/console/hub/controller/WorkflowChatController.java"
echo "   方法: workflowChatStream() - 第 41 行"
echo ""
echo -e "${BLUE}5. 启动调试${NC}"
echo "   点击 Debug 按钮 (Shift + F9)"
echo ""
echo -e "${BLUE}6. 测试${NC}"
echo "   浏览器访问: http://localhost/work_flow/184742/arrange?botId=57"
echo "   点击 \"调试\" 按钮，断点应该会命中"
echo ""

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  准备完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${CYAN}📖 详细文档: ${NC}docs/DEBUG_CONSOLE_HUB.md"
echo -e "${CYAN}🔧 环境变量文件: ${NC}console/backend/hub/.env.local"
echo ""
echo -e "${YELLOW}⚠️  注意事项:${NC}"
echo "1. 确保 MySQL、PostgreSQL、Redis 的端口已映射到 localhost"
echo "2. console-hub 必须在 IDEA 中运行，不能在 Docker 中运行"
echo "3. 调试完成后，运行 ${CYAN}./scripts/restore-docker-env.sh${NC} 恢复环境"
echo ""

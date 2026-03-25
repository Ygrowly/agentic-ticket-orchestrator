#!/bin/bash

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

MYSQL_HOST="localhost"
MYSQL_PORT="3306"
MYSQL_USER="root"

if [ -z "$MYSQL_PASSWORD" ]; then
    echo -e "${YELLOW}请输入 MySQL root 用户密码 (直接回车使用默认值: 123456):${NC}"
    read -s MYSQL_PASSWORD
    echo ""

    if [ -z "$MYSQL_PASSWORD" ]; then
        MYSQL_PASSWORD="123456"
    fi
fi

MYSQL_CNF=$(mktemp)
echo "[client]" > "$MYSQL_CNF"
echo "user=$MYSQL_USER" >> "$MYSQL_CNF"
echo "password=$MYSQL_PASSWORD" >> "$MYSQL_CNF"
echo "host=$MYSQL_HOST" >> "$MYSQL_CNF"
echo "port=$MYSQL_PORT" >> "$MYSQL_CNF"

trap 'rm -f "$MYSQL_CNF"' EXIT

echo -e "${YELLOW}开始修复本地 MySQL 字符集...${NC}"

mysql --defaults-extra-file="$MYSQL_CNF" <<'EOF'
ALTER DATABASE `paiflow-workflow` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `paiflow-workflow`.`app` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `paiflow-workflow`.`app_source` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `paiflow-workflow`.`flow` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE `paiflow-workflow`.`license` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

ALTER DATABASE `paiflow-tenant` CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
ALTER TABLE `paiflow-tenant`.`tb_app` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
ALTER TABLE `paiflow-tenant`.`tb_auth` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
EOF

echo -e "${GREEN}✓ 本地 MySQL 字符集修复完成${NC}"

#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

echo "======================================"
echo " GymMind Docker 基础设施一键启动"
echo "======================================"

if ! command -v docker >/dev/null 2>&1; then
  echo "[ERR] 未找到 docker 命令"
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "[ERR] Docker 守护进程未运行，请先启动 Docker"
  exit 1
fi

if [[ ! -f .env ]]; then
  if [[ ! -f .env.example ]]; then
    echo "[ERR] 缺少 .env.example"
    exit 1
  fi
  cp .env.example .env
  echo "[!!] 已从 .env.example 生成 .env，请编辑 MYSQL_PASSWORD"
fi

COMPOSE="docker compose"
if ! docker compose version >/dev/null 2>&1; then
  COMPOSE="docker-compose"
fi

PROFILE=""
if grep -q '^USE_DOCKER_MYSQL=1' .env 2>/dev/null; then
  PROFILE="--profile mysql"
fi

echo ">> 构建并启动容器..."
# shellcheck disable=SC2086
$COMPOSE --env-file .env up -d --build $PROFILE

echo ">> 等待服务就绪..."
sleep 15
$COMPOSE ps

echo ""
echo "======================================"
echo " 基础设施已启动"
echo "======================================"
echo "  Redis:         localhost:${REDIS_HOST_PORT:-6379}"
echo "  Elasticsearch: http://localhost:${ES_HOST_PORT:-9202}"
echo "  Milvus:        localhost:${MILVUS_PORT:-19530}"
echo "  MinIO 控制台:  http://localhost:${MINIO_CONSOLE_PORT:-9001}"
echo ""
echo "后端: SPRING_PROFILES_ACTIVE=dev,docker mvn spring-boot:run"
echo ""

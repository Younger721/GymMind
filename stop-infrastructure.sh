#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

COMPOSE="docker compose"
if ! docker compose version >/dev/null 2>&1; then
  COMPOSE="docker-compose"
fi

if [[ "${1:-}" == "-v" || "${1:-}" == "--volumes" ]]; then
  echo "停止并删除数据卷..."
  $COMPOSE --env-file .env --profile mysql down -v
else
  echo "停止容器（保留数据卷）..."
  $COMPOSE --env-file .env --profile mysql down
fi

echo "基础设施已停止。"

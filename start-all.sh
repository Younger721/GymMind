#!/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"

echo "======================================"
echo "GymMind 项目启动脚本"
echo "======================================"
echo ""

if [[ -f "$ROOT/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT/.env"
  set +a
fi

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"

echo "步骤 1/2: 启动后端 (Spring Boot) profile=$SPRING_PROFILES_ACTIVE ..."
cd "$ROOT/backend"
nohup mvn spring-boot:run > backend.log 2>&1 &
echo "  后端 PID: $!"
echo "  日志: $ROOT/backend/backend.log"
echo ""

echo "步骤 2/2: 启动前端 (Vite)..."
cd "$ROOT/frontend"
nohup npm run dev > frontend.log 2>&1 &
echo "  前端 PID: $!"
echo "  日志: $ROOT/frontend/frontend.log"
echo ""
echo "访问: http://localhost:5173/  后端: http://localhost:8080/"
echo "配置说明: docs/CONFIGURATION.md"

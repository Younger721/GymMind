#!/bin/bash

echo "======================================"
echo "GymMind 项目启动脚本"
echo "======================================"
echo ""

# 1. 启动后端
echo "步骤 1/2: 启动后端 (Spring Boot)..."
cd D:/java/project/full-project/GymMind/backend
nohup mvn spring-boot:run > backend.log 2>&1 &
BACKEND_PID=$!
echo "  后端启动中... PID: $BACKEND_PID"
echo "  日志文件: D:/java/project/full-project/GymMind/backend/backend.log"
echo ""

# 2. 启动前端
echo "步骤 2/2: 启动前端 (Vite)..."
cd D:/java/project/full-project/GymMind/frontend
nohup npm run dev > frontend.log 2>&1 &
FRONTEND_PID=$!
echo "  前端启动中... PID: $FRONTEND_PID"
echo "  日志文件: D:/java/project/full-project/GymMind/frontend/frontend.log"
echo ""

echo "======================================"
echo "启动完成！"
echo "======================================"
echo ""
echo "请等待 30-60 秒让服务完全启动"
echo ""
echo "访问地址:"
echo "  前端: http://localhost:5173/"
echo "  后端: http://localhost:8080/"
echo ""
echo "检查状态:"
echo "  后端: tail -f D:/java/project/full-project/GymMind/backend/backend.log"
echo "  前端: tail -f D:/java/project/full-project/GymMind/frontend/frontend.log"
echo ""

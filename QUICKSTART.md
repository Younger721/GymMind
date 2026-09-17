# GymMind 快速启动指南

## 前置检查

在启动项目前，请确保以下环境已准备好：

- ✅ Java 17+ 已安装
- ✅ Maven 3.8+ 已安装
- ✅ Node.js 18+ 已安装
- ✅ Docker Desktop 已安装并运行
- ✅ MySQL 8.0 已安装并运行

## 启动步骤

### 1️⃣ 配置环境变量

编辑项目根目录的 `.env` 文件，配置以下必填项：

```bash
# 修改为你的 MySQL 密码
MYSQL_PASSWORD=你的MySQL密码

# 修改为强密钥（生产环境必须更换）
JWT_SECRET=gymmind_jwt_secret_key_change_this_in_production_12345678

# 配置 AI API 密钥（DeepSeek 或 OpenAI）
AI_API_KEY=your_ai_api_key_here
```

### 2️⃣ 创建数据库

在 MySQL 中执行：

```sql
CREATE DATABASE gymmind CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3️⃣ 启动基础设施

**Windows 用户：**
```bash
start-infrastructure.bat
```

**Linux/Mac 用户：**
```bash
chmod +x start-infrastructure.sh
./start-infrastructure.sh
```

等待约 10 秒，确保以下服务启动成功：
- ✅ Redis: localhost:6379
- ✅ Elasticsearch: http://localhost:9200
- ✅ Milvus: localhost:19530
- ✅ MinIO: http://localhost:9001

验证 Elasticsearch：
```bash
curl http://localhost:9200
```

### 4️⃣ 启动后端

打开新终端：

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

看到以下日志表示启动成功：
```
Started GymMindApplication in X.XXX seconds
```

后端将在 http://localhost:8080 启动

### 5️⃣ 启动前端

打开新终端：

```bash
cd frontend
npm install
npm run dev
```

看到以下日志表示启动成功：
```
VITE ready in XXX ms
Local: http://localhost:5173/
```

### 6️⃣ 访问应用

在浏览器打开：http://localhost:5173

你将看到登录页面。

## 功能测试

### 测试注册和登录

1. 点击 "Register now" 进入注册页面
2. 填写用户信息：
   - Username: testuser
   - Email: test@example.com
   - Password: 123456
3. 点击 "Register" 注册
4. 注册成功后自动跳转到 Dashboard

### 测试用户档案

1. 点击左侧菜单 "Profile"
2. 填写你的身体数据和健身目标
3. 点击 "Save Profile"
4. 刷新页面，确认数据已保存

### 测试 JWT 认证

1. 打开浏览器开发者工具 (F12)
2. 查看 Network 标签
3. 点击任意页面，查看请求头
4. 确认请求中包含 `Authorization: Bearer <token>`

### 测试登出

1. 点击顶部 "Logout" 按钮
2. 确认返回登录页面
3. 尝试直接访问 http://localhost:5173/dashboard
4. 确认自动跳转到登录页面（路由守卫生效）

## 常见问题

### Q1: 后端启动失败，报错 "Access denied for user"

**解决方案**：检查 `.env` 文件中的 `MYSQL_PASSWORD` 是否正确

### Q2: 前端无法连接后端，报错 "Network Error"

**解决方案**：
1. 确认后端已启动（访问 http://localhost:8080/api/auth/login 应该返回 405）
2. 检查 Vite 代理配置：`frontend/vite.config.ts`

### Q3: Docker 服务启动失败

**解决方案**：
1. 确认 Docker Desktop 正在运行
2. 检查端口是否被占用：`netstat -ano | findstr "9200"`
3. 查看日志：`docker-compose logs`

### Q4: Elasticsearch 连接失败

**解决方案**：
1. 等待 30 秒让 ES 完全启动
2. 验证 ES 状态：`curl http://localhost:9200`
3. 查看 ES 日志：`docker logs gymmind-es`

### Q5: Milvus 连接失败

**解决方案**：
1. 确认 etcd 和 MinIO 已启动
2. 查看 Milvus 日志：`docker logs gymmind-milvus`

## 停止服务

### 停止前端和后端
在各自终端按 `Ctrl + C`

### 停止基础设施

**Windows:**
```bash
stop-infrastructure.bat
```

**Linux/Mac:**
```bash
./stop-infrastructure.sh
```

## 下一步

阶段 1 基础功能已完成，接下来可以：

1. **阶段 2**：开发个人知识库与文档处理
2. **阶段 3**：实现 RAG 智能问答
3. **阶段 4**：添加联网搜索功能

详见 `docs/AI智能健身RAG系统_开发计划.md`

## 技术支持

如遇到其他问题，请查看：
- 完整文档：`docs/项目搭建完成报告.md`
- Docker 指南：`docs/DOCKER复用指南.md`
- 开发计划：`docs/AI智能健身RAG系统_开发计划.md`

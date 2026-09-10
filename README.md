# GymMind - AI 智能健身 RAG 系统

个性化智能健身 Web 系统，结合用户画像、知识库、训练记录和饮食数据，提供有来源、可解释、可追踪的健身辅助建议。

## 技术栈

### 后端
- Java 17+
- Spring Boot 3
- Spring Security (JWT 认证)
- Spring Data JPA
- Spring AI
- Maven

### AI & RAG
- Elasticsearch 9.4.2 (全文搜索 + 中文分词)
- Milvus v2.4.17 (向量数据库)
- OpenAI Compatible API (支持 DeepSeek、Qwen、OpenAI)

### 数据存储
- MySQL 8.0 (关系型数据库)
- Redis 7 (缓存和会话)
- MinIO (对象存储)
- etcd (Milvus 依赖)

### 前端
- Vue 3
- TypeScript
- Vite
- Element Plus
- Pinia
- ECharts

## 快速开始

### 1. 环境要求
- JDK 17+
- Maven 3.8+
- Docker & Docker Compose
- Node.js 18+ (前端开发)
- 本地 MySQL 8.0

### 2. 配置环境变量
```bash
cp .env.example .env
# 编辑 .env 文件，填入你的配置
```

### 3. 启动 Docker 服务
```bash
# 启动所有服务（Redis、Elasticsearch、etcd、MinIO、Milvus）
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

### 4. 初始化本地 MySQL 数据库
```sql
CREATE DATABASE IF NOT EXISTS gymmind CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 5. 启动后端服务
```bash
cd backend
mvn spring-boot:run
```

### 6. 启动前端服务
```bash
cd frontend
npm install
npm run dev
```

## 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| 后端 API | 8080 | Spring Boot 应用 |
| 前端 | 5173 | Vite 开发服务器 |
| MySQL | 3306 | 本地 MySQL |
| Redis | 6379 | 缓存 |
| Elasticsearch | 9200 | 全文搜索 |
| MinIO API | 9000 | 对象存储 API |
| MinIO Console | 9001 | MinIO 管理控制台 |
| Milvus | 19530 | 向量数据库 gRPC |
| Milvus Metrics | 9091 | Milvus 监控 |

## Docker 服务说明

本项目复用了图书馆管理系统的 Docker 配置：

- **Redis**: 缓存和会话存储
- **Elasticsearch**: 全文搜索引擎，预装 analysis-smartcn 中文分词插件
- **etcd**: Milvus 的依赖服务
- **MinIO**: S3 兼容的对象存储，用于文档和图片存储，同时作为 Milvus 的依赖
- **Milvus**: 向量数据库，用于 RAG 向量检索

详细的 Docker 服务配置说明请参考：`docs/DOCKER复用指南.md`

## 停止服务

```bash
# 停止所有 Docker 服务
docker-compose down

# 停止并删除数据卷（慎用，会清空所有数据）
docker-compose down -v
```

## 项目文档

- [开发计划](docs/AI智能健身RAG系统_开发计划.md)
- [Docker 复用指南](docs/DOCKER复用指南.md)

## 开发进度

当前阶段：**阶段 0 - 需求冻结与设计**

详细进度请查看：[开发计划](docs/AI智能健身RAG系统_开发计划.md)

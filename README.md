# GymMind - AI智能健身 RAG 系统

AI-Powered Personal Fitness RAG System with Elasticsearch + Milvus dual indexing architecture.

## 项目结构

```
GymMind/
├── backend/           # Spring Boot 后端
├── frontend/          # Vue 3 前端
├── docker/            # Docker 配置文件
├── docs/              # 项目文档
├── sql/               # 数据库脚本
├── docker-compose.yml # Docker Compose 配置
└── README.md
```

## 技术栈

### 后端
- Java 17
- Spring Boot 3.2.5
- Spring Security + JWT
- Spring Data JPA
- MySQL 8.0
- Redis
- Elasticsearch 9.4.2 (全文检索)
- Milvus v2.4.17 (向量检索)
- MinIO (对象存储)

### 前端
- Vue 3
- TypeScript
- Vite
- Element Plus
- Pinia
- Vue Router
- Axios

## 快速开始

### 1. 环境要求

- Java 17+
- Node.js 18+
- Maven 3.8+
- Docker & Docker Compose
- MySQL 8.0 (本地已安装)

### 2. 配置环境变量

复制 `.env.example` 到 `.env` 并修改配置：

```bash
cp .env.example .env
```

修改 `.env` 文件中的配置：
- `MYSQL_PASSWORD`: 你的本地 MySQL 密码
- `JWT_SECRET`: JWT 密钥（生产环境必须修改）
- `AI_API_KEY`: AI API 密钥（DeepSeek/OpenAI）
- `MINIO_ROOT_USER` 和 `MINIO_ROOT_PASSWORD`: MinIO 访问凭证

### 3. 启动基础设施

使用 Docker Compose 启动 Redis、Elasticsearch、Milvus、etcd 和 MinIO：

```bash
docker-compose up -d
```

验证服务状态：
```bash
docker-compose ps
```

访问服务：
- Elasticsearch: http://localhost:9200
- MinIO Console: http://localhost:9001
- Milvus: localhost:19530

### 4. 初始化数据库

在本地 MySQL 中创建数据库：

```sql
CREATE DATABASE gymmind CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 5. 启动后端

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

后端将在 http://localhost:8080 启动

### 6. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端将在 http://localhost:5173 启动

## API 文档

### 认证接口

- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/logout` - 用户登出

### 用户档案接口

- `GET /api/user/profile` - 获取用户档案
- `PUT /api/user/profile` - 更新用户档案

## 开发进度

详见 [开发计划](./docs/AI智能健身RAG系统_开发计划.md)

当前阶段：**阶段 1 - 基础工程、认证与用户档案**

已完成：
- ✅ Spring Boot 后端项目结构
- ✅ Vue 3 前端项目结构
- ✅ Docker Compose 配置 (ES + Milvus)
- ✅ 统一响应、异常处理、日志配置
- ✅ 用户注册、登录、JWT 鉴权
- ✅ 用户档案管理

进行中：
- 🔄 测试与验证

## 项目特色

### 双重索引架构

本项目采用 **Elasticsearch + Milvus 双重索引**架构实现混合检索：

- **Milvus**: 向量检索，语义相似度匹配
- **Elasticsearch**: 全文检索，关键词匹配和元数据过滤
- **混合召回**: RRF (Reciprocal Rank Fusion) 融合排序
- **用户隔离**: 所有检索都严格按 userId 过滤

## 许可证

本项目为毕业设计项目。

## 联系方式

如有问题，请联系项目负责人。

# Docker 服务复用指南

本文档列出了图书馆管理系统项目中的 Docker 服务配置，这些服务可以复用到其他项目中。

## 📋 服务清单

### 1. MySQL 8.0
**用途**: 关系型数据库

**配置特点**:
- 使用 MySQL 8.0 官方镜像
- UTF8MB4 字符集，支持完整的 Unicode 字符（包括 emoji）
- 支持通过环境变量配置密码和端口
- 自动初始化 SQL 脚本

**复用配置**:
```yaml
mysql:
  image: mysql:8.0
  container_name: your-project-mysql
  environment:
    MYSQL_ROOT_PASSWORD: ${MYSQL_PASSWORD:?MYSQL_PASSWORD is required}
    MYSQL_DATABASE: your_database_name
  ports:
    - "${MYSQL_HOST_PORT:-3306}:3306"
  command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
  volumes:
    - mysql_data:/var/lib/mysql
    - ./sql/schema.sql:/docker-entrypoint-initdb.d/schema.sql:ro
```

**环境变量**:
- `MYSQL_PASSWORD`: 必需，MySQL root 密码
- `MYSQL_HOST_PORT`: 可选，默认 3306，宿主机端口

---

### 2. Redis 7
**用途**: 缓存、会话存储、消息队列

**配置特点**:
- 使用轻量级的 Alpine 版本
- 默认配置，开箱即用

**复用配置**:
```yaml
redis:
  image: redis:7-alpine
  container_name: your-project-redis
  ports:
    - "${REDIS_HOST_PORT:-6379}:6379"
```

**建议**: 生产环境建议添加持久化和密码配置

---

### 3. Elasticsearch 9.4.2 (带中文分词)
**用途**: 全文搜索引擎

**配置特点**:
- 基于官方镜像构建
- **预装 analysis-smartcn 中文分词插件**
- 单节点模式
- 禁用了 X-Pack 安全功能（开发环境）
- JVM 内存限制 512MB

**复用配置**:
```yaml
elasticsearch:
  build:
    context: ./docker/elasticsearch
    dockerfile: Dockerfile
  image: your-project-elasticsearch:9.4.2-smartcn
  container_name: your-project-es
  environment:
    - discovery.type=single-node
    - xpack.security.enabled=false
    - ES_JAVA_OPTS=-Xms512m -Xmx512m
  ports:
    - "${ES_HOST_PORT:-9200}:9200"
  volumes:
    - es_data:/usr/share/elasticsearch/data
```

**Dockerfile** (需要创建 `docker/elasticsearch/Dockerfile`):
```dockerfile
FROM docker.elastic.co/elasticsearch/elasticsearch:9.4.2

RUN elasticsearch-plugin install --batch analysis-smartcn
```

**环境变量**:
- `ES_HOST_PORT`: 可选，默认 9200，宿主机端口

**建议**: 生产环境建议调整 JVM 内存并启用安全功能

---

### 4. etcd v3.5.16
**用途**: 分布式配置中心、服务发现

**配置特点**:
- 轻量级分布式键值存储
- 自动压缩配置
- 4GB 存储配额

**复用配置**:
```yaml
etcd:
  image: quay.io/coreos/etcd:v3.5.16
  container_name: your-project-etcd
  environment:
    ETCD_AUTO_COMPACTION_MODE: revision
    ETCD_AUTO_COMPACTION_RETENTION: "1000"
    ETCD_QUOTA_BACKEND_BYTES: "4294967296"
  command: >
    etcd
    -advertise-client-urls=http://etcd:2379
    -listen-client-urls=http://0.0.0.0:2379
    --data-dir=/etcd
  volumes:
    - etcd_data:/etcd
```

**适用场景**: 微服务配置管理、Milvus 依赖

---

### 5. MinIO
**用途**: 对象存储（S3 兼容）

**配置特点**:
- 兼容 Amazon S3 API
- 带 Web 管理控制台
- 需要配置访问密钥

**复用配置**:
```yaml
minio:
  image: minio/minio:RELEASE.2023-03-20T20-16-18Z
  container_name: your-project-minio
  environment:
    MINIO_ROOT_USER: ${MINIO_ROOT_USER:?MINIO_ROOT_USER is required}
    MINIO_ROOT_PASSWORD: ${MINIO_ROOT_PASSWORD:?MINIO_ROOT_PASSWORD is required}
  command: minio server /minio_data --console-address ":9001"
  ports:
    - "9000:9000"  # API 端口
    - "9001:9001"  # 控制台端口
  volumes:
    - minio_data:/minio_data
```

**环境变量**:
- `MINIO_ROOT_USER`: 必需，管理员用户名
- `MINIO_ROOT_PASSWORD`: 必需，管理员密码（至少 8 位）

**访问地址**:
- API: http://localhost:9000
- 控制台: http://localhost:9001

---

### 6. Milvus v2.4.17
**用途**: 向量数据库（AI/机器学习应用）

**配置特点**:
- 专为向量搜索优化
- 依赖 etcd 和 MinIO
- 单机模式 (standalone)

**复用配置**:
```yaml
milvus:
  image: milvusdb/milvus:v2.4.17
  container_name: your-project-milvus
  command: ["milvus", "run", "standalone"]
  environment:
    ETCD_ENDPOINTS: etcd:2379
    MINIO_ADDRESS: minio:9000
    MINIO_ACCESS_KEY_ID: ${MINIO_ROOT_USER:?MINIO_ROOT_USER is required}
    MINIO_SECRET_ACCESS_KEY: ${MINIO_ROOT_PASSWORD:?MINIO_ROOT_PASSWORD is required}
  ports:
    - "19530:19530"  # gRPC 端口
    - "9091:9091"    # Metrics 端口
  depends_on:
    - etcd
    - minio
  volumes:
    - milvus_data:/var/lib/milvus
```

**依赖服务**: 需要同时运行 etcd 和 MinIO

**适用场景**: 
- 语义搜索
- 推荐系统
- 图像/视频检索
- RAG (检索增强生成) 应用

---

## 🚀 快速开始

### 1. 创建 `.env` 文件
```env
# MySQL
MYSQL_PASSWORD=your_mysql_password
MYSQL_HOST_PORT=3306

# Elasticsearch
ES_HOST_PORT=9200

# MinIO (Milvus 依赖)
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin123
```

### 2. 启动服务
```bash
# 启动所有服务
docker-compose up -d

# 启动特定服务
docker-compose up -d mysql redis

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down

# 停止并删除数据卷（慎用）
docker-compose down -v
```

---

## 📦 常用组合方案

### 方案 1: 基础 Web 应用
```yaml
services:
  mysql:    # 数据库
  redis:    # 缓存
```

### 方案 2: 带搜索功能的应用
```yaml
services:
  mysql:          # 数据库
  redis:          # 缓存
  elasticsearch:  # 搜索引擎
```

### 方案 3: AI/向量搜索应用
```yaml
services:
  mysql:          # 数据库
  redis:          # 缓存
  elasticsearch:  # 传统搜索
  etcd:           # Milvus 依赖
  minio:          # Milvus 依赖
  milvus:         # 向量数据库
```

### 方案 4: 微服务架构
```yaml
services:
  mysql:          # 数据库
  redis:          # 缓存
  etcd:           # 配置中心/服务发现
  minio:          # 对象存储
```

---

## ⚠️ 注意事项

1. **端口冲突**: 如果宿主机已有服务占用端口，通过环境变量调整：
   ```env
   MYSQL_HOST_PORT=3307
   ES_HOST_PORT=9201
   ```

2. **数据持久化**: 所有数据都存储在 Docker volumes 中，删除容器不会丢失数据。但 `docker-compose down -v` 会删除数据卷。

3. **生产环境**: 以上配置主要用于开发环境，生产环境需要额外配置：
   - 启用认证和加密
   - 调整资源限制
   - 配置备份策略
   - 使用外部网络

4. **依赖关系**: Milvus 必须与 etcd 和 MinIO 一起使用，不能单独运行。

5. **内存要求**: Elasticsearch 和 Milvus 需要较多内存，确保 Docker 分配足够资源。

---

## 📚 参考文档

- [MySQL Docker Hub](https://hub.docker.com/_/mysql)
- [Redis Docker Hub](https://hub.docker.com/_/redis)
- [Elasticsearch 官方文档](https://www.elastic.co/guide/en/elasticsearch/reference/current/docker.html)
- [etcd 官方文档](https://etcd.io/docs/)
- [MinIO 官方文档](https://min.io/docs/minio/linux/operations/installation.html)
- [Milvus 官方文档](https://milvus.io/docs)

---

**创建日期**: 2026-09-10  
**项目**: 图书馆管理系统  
**版本**: 1.0

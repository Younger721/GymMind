# GymMind 后端运行指南

## 环境要求

- Java 17（设置 `JAVA_HOME`）
- MySQL 8，库名 `gymmind`
- Redis 7（JWT 撤销与缓存）
- 可选：Elasticsearch、Milvus、MinIO（生产 RAG 与对象存储）

## 本地开发（无容器）

开发 profile 默认使用内存检索后端与本地对象存储，无需 ES/Milvus/MinIO 即可启动：

```powershell
cd backend
$env:JAVA_HOME='D:\java\jdk-17.0.14'
$env:SPRING_PROFILES_ACTIVE='dev'
.\mvnw.cmd spring-boot:run
```

健康检查：`GET http://localhost:8080/actuator/health`

OpenAPI UI：`http://localhost:8080/swagger-ui/index.html`

## 一键验证

```powershell
cd backend
$env:JAVA_HOME='D:\java\jdk-17.0.14'
.\scripts\verify-backend.ps1
```

脚本会执行 `mvn clean verify -DskipITs`、启动 jar 并轮询 `/actuator/health`。

## 生产中间件

| 组件 | 环境变量 | 说明 |
|------|----------|------|
| MySQL | `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_USER`, `MYSQL_PASSWORD` | 业务库 `gymmind` |
| Redis | `SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT` | 会话撤销 |
| ES | `GYMMIND_ES_URL` | 全文索引 |
| Milvus | `GYMMIND_MILVUS_URL` | 向量索引 |
| MinIO | `gymmind.object-store=minio` | 知识文档对象存储 |
| 百炼 | `DASHSCOPE_API_KEY` | AI 能力（未配置时使用本地降级） |

## 集成测试

需要 Docker 的 `*IT` 测试通过 Maven profile 运行：

```powershell
.\mvnw.cmd verify -Pintegration
```

容器连接失败时可先跳过，单元测试 `mvn test` 不依赖 Docker。

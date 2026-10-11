# GymMind 配置说明

本地只维护 **一套全栈开发配置**：本机 MySQL + Docker（Redis / Elasticsearch / Milvus / MinIO）+ 百炼 DashScope。

## 优先级（高 → 低）

1. IDEA / 命令行环境变量（不覆盖时以 `.env` 补充）
2. 仓库根 **`.env`**
3. `application.yml` + **`application-dev.yml`**（`spring.profiles.active=dev`）

## 启动

```text
1. 复制 .env.example → .env，填写 MYSQL_PASSWORD、DASHSCOPE_API_KEY
2. start-infrastructure.bat
3. start-all.bat   （或 IDEA 运行 GymMindApplication，profile=dev，工作目录 backend）
```

| 服务 | 地址 |
|------|------|
| 后端 | http://localhost:8080 |
| 前端 | http://localhost:5173 |
| ES | http://localhost:9202 |
| Milvus | http://localhost:19530 |
| MinIO API | http://localhost:9000 |
| Redis | localhost:6379 |

## 固定技术选型（dev）

| 能力 | 配置 |
|------|------|
| AI | `DASHSCOPE_API_KEY` → `gymmind.ai.dashscope.*` |
| 检索 | Elasticsearch + Milvus（见 `application-dev.yml`） |
| 知识库文件 / 切片 artifact | MinIO |
| 异步入库队列 | Redis Stream（`gymmind.job-queue=redis`） |

可选：`GYMMIND_KNOWLEDGE_INGEST_TWO_PHASE=true` 开启切片落 MinIO 再向量化，见 [design/2026-10-knowledge-chunk-artifact-minio.md](./design/2026-10-knowledge-chunk-artifact-minio.md)。

## 知识库检索（`gymmind.search.*`）

| 键 | 默认 | 说明 |
|----|------|------|
| `recall-top-k` / `fusion-top-k` | 30 / 20 | 双路召回与 RRF 融合上限 |
| `max-segments-per-document` | 3 | 单文档进入上下文的最大片段数 |
| `context-budget-chars` | 12000 | 引用整理字符预算 |
| `query-embedding-cache.*` | 开 / 300s / 512 | 检索 query 向量缓存 |
| `rerank.max-attempts` | 2 | 百炼 rerank 失败重试（仍失败则本地词面排序） |
| `lexical.*` | 见 `application.yml` | 词面证据过滤阈值 |

## RAG 评测

[rag-eval/README.md](./rag-eval/README.md)

CI：`.github/workflows/rag-knowledge-search-ci.yml`（单测 + 语料校验；配置仓库变量 `GYMMIND_EVAL_BASE_URL` 后可选跑 `-SearchOnly`）。

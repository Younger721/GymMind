# GymMind 配置说明

## 优先级（高 → 低）

1. **IDEA Run / 命令行环境变量**（`SPRING_PROFILES_ACTIVE`、`GYMMIND_*`、`DASHSCOPE_API_KEY`）
2. **仓库根** `GymMind/.env`（Spring `DotEnvEnvironmentPostProcessor` 自动合并，不覆盖已有环境变量）
3. **`application.yml` + `application-{profile}.yml`**

AI 仅使用 **阿里百炼**：`DASHSCOPE_API_KEY` → `gymmind.ai.dashscope.*`。

## IDEA 启动（推荐）

| 项 | 建议 |
|----|------|
| `SPRING_PROFILES_ACTIVE` | `dev,docker` |
| `GYMMIND_*` | ES/Milvus/Redis/MinIO 等（与全栈一致） |
| `DASHSCOPE_API_KEY` | Run 环境变量，或写在根 `.env` |

## 启动方式

| 方式 | 说明 |
|------|------|
| IDEA ▶ `GymMindApplication` | 工作目录 `backend`，读根 `.env` |
| `start-all.bat` / `start-all.sh` | 先 `source`/读取根 `.env`，再 `mvn spring-boot:run` |
| `cd backend && mvnw spring-boot:run` | 需自行 export profile；根 `.env` 仍会自动合并 |

`.env` 只放在 **仓库根**，不要建 `backend/.env`。

## 百炼变量

| 变量 | 说明 |
|------|------|
| `DASHSCOPE_API_KEY` | 必填 |
| `DASHSCOPE_BASE_URL` | 可选，默认 DashScope 兼容地址 |
| `DASHSCOPE_CHAT_MODEL` 等 | 可选，默认见 `application.yml` |

## RAG 权限评测

本地 ES/Milvus 与后端就绪后，可使用固定语料与脚本做 Search + Harness 召回/权限评测，见 [rag-eval/README.md](./rag-eval/README.md)。

## 知识库两阶段入库（可选）

| 变量 | 说明 |
|------|------|
| `GYMMIND_KNOWLEDGE_INGEST_TWO_PHASE` | `true` 时：PARSE job 将切片 JSON 写入 MinIO（`chunks/*.json`）后再 INDEX job 向量化；默认 `false` 保持单 job 行为 |

设计说明见 [design/2026-10-knowledge-chunk-artifact-minio.md](./design/2026-10-knowledge-chunk-artifact-minio.md)。

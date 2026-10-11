# RAG 权限与召回评测（local_manual + full_pipeline）

本目录提供**固定语料、问句集与 PowerShell 脚本**，在本地 ES + Milvus + 后端已启动的前提下，评测：

- **Search 通道**：`GET /api/v1/knowledge/search` 的 Recall@K、Precision@K、PermissionLeak
- **Harness 通道**（可选）：`POST /api/v1/ai/harness/chat`（仅 `knowledge_search` 插件）引用 **citations**，不评价 LLM 文案；与 Search 共用 `KnowledgeSearchService`，报告会对比两通道 Top 结果的 **chunkId 顺序**

## 前置条件

1. 中间件与后端：`dev` profile，见 [CONFIGURATION.md](../CONFIGURATION.md)
2. 根目录 `.env` 含有效 `DASHSCOPE_API_KEY`（Harness 与入库 Embedding 需要）
3. Windows **PowerShell 5.1+**（仓库脚本已兼容；若已安装 PowerShell 7 也可用 `pwsh`）
4. 平台管理员默认账号：`admin` / `123456`（与 `application-dev.yml` 一致）

## 目录说明

| 路径 | 说明 |
|------|------|
| `corpus/*.md` | 5 篇评测语料，含 `EVAL-*` 唯一锚点 |
| `manifest.yaml` / `manifest.json` | 租户、受众角色、上传范围（脚本读 JSON） |
| `queries.json` | 17 条问句（含正例与 forbidden 负例） |
| `state/ingested.json` | `seed-and-ingest.ps1` 写入的文档 id（可 gitignore） |
| `reports/` | `run-eval.ps1` 生成的 JSON / MD 报告 |

## 一键流程

```powershell
# 仓库根目录
cd D:\java\project\full-project\GymMind

# 1. 注册评测租户、上传并发布语料
powershell -File ./scripts/rag-eval/seed-and-ingest.ps1 -BaseUrl http://localhost:8080

# 2. 全量评测（Search + Harness）
powershell -File ./scripts/rag-eval/run-eval.ps1 -BaseUrl http://localhost:8080

# 仅 Search（不调 LLM，适合 CI 或 Key 不可用）
powershell -File ./scripts/rag-eval/run-eval.ps1 -SearchOnly

# CI 语料结构校验（无需后端）
powershell -File ./scripts/rag-eval/validate-eval-fixtures.ps1
```

GitHub Actions 见 `.github/workflows/rag-knowledge-search-ci.yml`：默认跑检索相关单测 + `validate-eval-fixtures`；在仓库 Variables 配置 `GYMMIND_EVAL_BASE_URL` 后额外跑 `-SearchOnly`。

重复导入可加 `-SkipIfStateExists`；若需强制重传，删除 `docs/rag-eval/state/ingested.json` 后重跑 seed（可能产生重复文档，建议专用库或重置 DB）。

索引升级（含 `audienceRoles` / `visibility` 字段）后需 **重新入库** 或开启 `GYMMIND_SEARCH_RECOVER_PUBLISHED=true` 做一次全量重灌；Milvus schema 不兼容时会自动重建 collection。

## 指标定义

- **AnchorRecall@K**：`expectedAnchors` 中至少出现在 Top-K 文本里的比例（pass 判定仍用它）
- **ChunkPrecision@K**：Top-K **每条**片段/citation 若含期望锚点或期望文档名则计为相关，相关条数 / K（经典精确率）
- **ChunkRecall@K**（报告明细字段）：Top-K 中相关条数与期望锚点数的比值（上限 1；多锚点题可 &lt; 1）
- 报告中的 `recallAtK` / `precisionAtK` 为兼容别名，分别等于 AnchorRecall、ChunkPrecision
- **PermissionLeak**：Top 结果中出现 `forbiddenAnchors` 或 `forbiddenDocuments` 即为泄漏，**必须为 0**

## 与生产样例隔离

评测租户代码为 `rag-eval-a` / `rag-eval-b`，语料文件名均以 `eval-` 开头，与 doc8 `rag-safety-test.md` 等手工样例分离。

`queries.json` 中租户用户的 **username** 为注册/邀请邮箱的 `@` 前本地段（例如 `rag-eval-a-admin@eval.local` → `rag-eval-a-admin`），与 `UserAccount.defaultUsernameFromEmail` 一致。

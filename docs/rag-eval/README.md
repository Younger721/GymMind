# RAG 权限与召回评测（local_manual + full_pipeline）

本目录提供**固定语料、问句集与 PowerShell 脚本**，在本地 ES + Milvus + 后端已启动的前提下，评测：

- **Search 通道**：`GET /api/v1/knowledge/search` 的 Recall@K、Precision@K、PermissionLeak
- **Harness 通道**（可选）：`POST /api/v1/ai/harness/chat`（仅 `knowledge_search` 插件）引用 **citations**，不评价 LLM 文案

## 前置条件

1. 中间件与后端：`dev,docker` 配置，见 [CONFIGURATION.md](../CONFIGURATION.md)
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
```

重复导入可加 `-SkipIfStateExists`；若需强制重传，删除 `docs/rag-eval/state/ingested.json` 后重跑 seed（可能产生重复文档，建议专用库或重置 DB）。

## 指标定义

- **Recall@K**：`expectedAnchors` 中出现在 Top-K 片段/citation 里的比例
- **Precision@K**：Top-K 中命中期望锚点的条数 / K（仅对有期望锚点的问句计算）
- **PermissionLeak**：Top 结果中出现 `forbiddenAnchors` 或 `forbiddenDocuments` 即为泄漏，**必须为 0**

## 与生产样例隔离

评测租户代码为 `rag-eval-a` / `rag-eval-b`，语料文件名均以 `eval-` 开头，与 doc8 `rag-safety-test.md` 等手工样例分离。

`queries.json` 中租户用户的 **username** 为注册/邀请邮箱的 `@` 前本地段（例如 `rag-eval-a-admin@eval.local` → `rag-eval-a-admin`），与 `UserAccount.defaultUsernameFromEmail` 一致。

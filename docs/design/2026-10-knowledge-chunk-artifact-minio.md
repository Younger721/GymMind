# 知识库入库：切片产物落 MinIO + 分阶段异步索引

> 状态：已实现（默认 `gymmind.knowledge.ingest.two-phase=false`，可配置开启）  
> 目标：切片完成后持久化到 MinIO，向量化/写 ES·Milvus 失败时可从 checkpoint 续跑，避免重复解析与切片。

---

## 1. 背景与问题

### 1.1 现状

- 上传 API 写 MySQL + MinIO 原文件，Redis 队列唤醒 `KnowledgeIngestWorker`。
- Worker 单次调用 `KnowledgeIndexingService.publish`：**解析 → 切片 → Embedding → 双写 ES/Milvus**，切片结果仅存在于 **JVM 内存**。
- 任一步失败（尤其百炼网络、ES/Milvus 不可用）触发 job **retry**，整篇 **重新读 MinIO、解析、切片**。

### 1.2 痛点

- 大 PDF/DOCX **解析 + StructuredChunker** 成本高，失败重试浪费 CPU 与时间。
- 切片已完成但 Embedding 中断时，**无法从中间态恢复**。

### 1.3 目标（本方案）

| 目标 | 说明 |
|------|------|
| 切片可恢复 | 切片结果写入 MinIO，进程/网络中断不丢 chunk 文本与 id |
| 阶段分离 | 阶段 A：解析+切片+写 artifact；阶段 B：读 artifact+向量+写索引 |
| 兼容现有 | 保留原文件 objectKey、双索引、Redis 队列抽象、权限与 published 语义 |
| 控制复杂度 | **不**在 MySQL 存每 chunk 大正文；元数据在 MySQL，正文在 MinIO JSON |

### 1.4 非目标（本期不做）

- 按 chunk 粒度的独立 Redis job（可二期）
- 解析结果（ParsedDocument blocks）单独落盘（可与 artifact 合并为一次写入）
- 改变 Hybrid 检索与 RAG 接口行为

---

## 2. 方案概览

```text
[原文件 MinIO] ──► 阶段 A（INGEST_PARSE）──► [chunk artifact MinIO]
                              │                        │
                              ▼                        ▼
                         MySQL: CHUNKED            阶段 B（INGEST_INDEX）
                              │                        │
                              └──────────► MySQL: READY + ES/Milvus
```

- **阶段 A**：读原文件 → `parseStructured` → `StructuredChunker`（或 DeterministicChunker 兜底）→ 将 `List<Chunk>` 序列化为 **Chunk Artifact** 上传 MinIO → 文档状态 **CHUNKED** → enqueue **阶段 B**。
- **阶段 B**：读 Chunk Artifact → `embed` → `IndexGenerationService.publish` → **READY**；失败只重试阶段 B。

Redis Stream 仍 **只携带 jobId**；job `type` 区分阶段（见 §5）。

---

## 3. MinIO 对象设计

### 3.1 Key 规则

与现有原文件并列，同租户前缀：

```text
{platform|tenant/{tenantId}}/knowledge/{uuid}/source/{fileName}     # 已有 objectKey
{platform|tenant/{tenantId}}/knowledge/{uuid}/chunks/{generation}.json   # 新增
```

- `{uuid}`：与 `KnowledgeDocument.objectKey` 中已有路径段一致（文档创建时的 UUID 段），**不**另造 id。
- `{generation}`：与索引 generation 对齐，建议 `{chunkStrategy}-v{strategyVersion}-{millis}`，与当前 `KnowledgeIndexingService` 中 generationKey 逻辑一致，便于 **replace 文档 / 重切** 时留新版本 artifact，旧 object 可异步 GC。

### 3.2 Content-Type

`application/json; charset=utf-8`

### 3.3 Chunk Artifact JSON Schema（v1）

```json
{
  "artifactVersion": 1,
  "documentId": 11,
  "tenantId": 6,
  "ownerUserId": null,
  "knowledgeKey": "optional-key",
  "knowledgeVersion": 1,
  "chunkStrategy": "GENERAL",
  "chunkStrategyVersion": 1,
  "sourceObjectKey": "tenant/6/knowledge/xxx/source/handbook.md",
  "sourceContentType": "text/markdown",
  "sourceFileName": "handbook.md",
  "createdAt": "2026-10-10T12:00:00Z",
  "chunks": [
    {
      "id": "sha256hex...",
      "ordinal": 0,
      "text": "..."
    }
  ]
}
```

- **id / ordinal / text** 与现有 `Chunk` record 一致；不向 artifact 存 vector（向量为阶段 B 产物，只进 Milvus）。
- 单 artifact 大小上限：建议配置项，默认 **32MB**；超限则 FAIL 并提示拆分文档（与配额策略一致）。

### 3.4 删除与覆盖

- 文档 **delete**：除删 source objectKey 外，删 `chunks/*` 下该文档所有 generation（或只删 DB 记录的 `chunkArtifactKey` 指向对象）。
- **replace 上传**：新文档 id 或新版本 → 新 artifact generation；索引 publish 前仍 `deleteDocument(tenantId, documentId)` 清 ES/Milvus。

---

## 4. MySQL 变更

### 4.1 DocumentStatus 枚举扩展

```text
UPLOADING → PARSING → CHUNKED → INDEXING → READY
                ↘ FAILED（任意阶段可进入，规则见 §6）
```

| 状态 | 含义 |
|------|------|
| UPLOADING | 已接上传，原文件已写 MinIO，可能尚未入队 |
| PARSING | 阶段 A 执行中（解析+切片+写 artifact） |
| CHUNKED | artifact 已落 MinIO，等待或正在阶段 B |
| INDEXING | 阶段 B 执行中（embed+双写） |
| READY | 索引成功（与现语义一致，仍须 published 才可搜） |
| FAILED | 重试耗尽 |

**兼容**：旧数据无 CHUNKED/INDEXING，仅 UPLOADING/PARSING/READY/FAILED；Recovery _runner 对 PARSING 且无 artifact 的 job 仍走 **合并单阶段** 或 **重新阶段 A**（见 §8）。

### 4.2 表字段（knowledge_document）

| 字段 | 类型 | 说明 |
|------|------|------|
| chunk_artifact_key | VARCHAR(512) NULL | 当前有效 artifact 的 MinIO key |
| chunk_artifact_generation | VARCHAR(128) NULL | 与 JSON 内逻辑 generation 一致，便于排查 |
| ingest_phase | VARCHAR(16) NULL | 可选：`PARSE` / `INDEX`，与 status 冗余时可省略 |

建议 **仅增加 `chunk_artifact_key`**，generation 可从 key 文件名解析。

---

## 5. 异步任务（Redis / PersistentJobQueue）

### 5.1 Job 类型

| type | 阶段 | 行为 |
|------|------|------|
| `KNOWLEDGE_INGEST_PARSE:{documentId}` | A | 读 source → 切片 → 写 artifact → status=CHUNKED → enqueue INDEX |
| `KNOWLEDGE_INGEST_INDEX:{documentId}` | B | 读 artifact → embed → publish 索引 → status=READY |

上传完成时 **只 enqueue PARSE**（与现 `KNOWLEDGE_INGEST:{id}` 替代关系见 §8）。

### 5.2 幂等

- **PARSE**：若 status 已是 CHUNKED/READY 且 artifact key 存在且 generation 匹配策略版本，可 skip 或仅 re-enqueue INDEX（管理接口「重建索引」）。
- **INDEX**：若 status 已是 READY 且索引 generation 已一致，complete job；若 CHUNKED 但 ES/Milvus 缺数据，允许重跑 INDEX。

### 5.3 Slot 锁

沿用 `tenantId + jobType` 去重；PARSE 与 INDEX **不同类型**，同一 document 可同时存在「PARSE 完成 + INDEX pending」，但 **不应** 两个 INDEX 并行——INDEX slot 仍为 `KNOWLEDGE_INGEST_INDEX:{documentId}`。

---

## 6. 核心流程伪代码

### 6.1 阶段 A — `ChunkArtifactService.materialize`

```text
function runParseJob(documentId, tenantId):
    doc = loadForUpdate(documentId)
    if doc.status not in (UPLOADING, PARSING, FAILED):
        return skip or reconcile
    doc.markParsing(); save(doc)

    bytes = objectStore.read(doc.objectKey)
    parsed = parser.parseStructured(doc.fileName, doc.contentType, bytes)
    chunks = structuredChunker.chunk(parsed, doc.id, doc.chunkStrategy, doc.chunkStrategyVersion)
           catch fallback deterministicChunker.chunk(parsed.plainText(), doc.id)

    artifact = buildArtifact(doc, chunks)
    artifactKey = resolveChunkKey(doc, generation)
    objectStore.put(artifactKey, json(artifact), "application/json")

    doc.setChunkArtifactKey(artifactKey)
    doc.markChunked(); save(doc)

    jobQueue.enqueue("KNOWLEDGE_INGEST_INDEX:" + documentId, tenantId)
    completeParseJob()
```

### 6.2 阶段 B — `KnowledgeIndexingService.publishFromArtifact`

```text
function runIndexJob(documentId, tenantId):
    doc = loadForUpdate(documentId)
    if doc.status != CHUNKED and doc.status != INDEXING:
        return reconcile
    doc.markIndexing(); save(doc)

    artifact = readAndParseJson(objectStore.read(doc.chunkArtifactKey))
    validateArtifactMatchesDocument(artifact, doc)

    vectors = embedding.embed(artifact.chunks.map(c => c.text))
    indexed = zip(artifact.chunks, vectors) -> IndexedChunk list

    indexGeneration.deleteDocument(indexTenantId, documentId)
    ok = indexGeneration.publish(indexTenantId, generationKey, indexed)
    if not ok: throw retry

    doc.markReady(); save(doc)
    completeIndexJob()
```

### 6.3 与现有 `publish` 关系

- 保留 `publish(bytes...)` 供 **单阶段兼容模式** 或测试；内部可 refactor 为 `parseAndChunk` + `publishFromArtifact` 组合。
- 生产配置 `gymmind.knowledge.ingest.two-phase=true` 时 Worker 只注册 PARSE/INDEX 两个 handler。

---

## 7. 失败、重试与恢复

| 失败点 | 文档状态 | Job | 恢复动作 |
|--------|----------|-----|----------|
| 解析/切片失败 | PARSING → retry 仍 PARSING，耗尽 FAILED | PARSE retry | 重跑 PARSE，**不读 artifact** |
| 写 artifact 失败 | PARSING | PARSE retry | 重跑 PARSE |
| artifact 已写，mark CHUNKED 前崩溃 | 可能 PARSING 但 MinIO 有 orphan | Recovery：检测 orphan artifact 或 re-PARSE | 对账任务或 PARSE 覆盖写 |
| Embedding/索引失败 | CHUNKED 或 INDEXING | INDEX retry | **只读 artifact**，不 re-parse |
| READY 后索引损坏 | READY | 管理端「重建索引」 | 仅 INDEX job |

**KnowledgeIndexRecoveryRunner** 扩展：

- `PARSING` 且无 artifact → enqueue PARSE  
- `CHUNKED` / `INDEXING` 且有 artifact → enqueue INDEX  
- `UPLOADING` 超时 → enqueue PARSE（与现逻辑类似）

---

## 8. 迁移与兼容

| 项 | 策略 |
|----|------|
| 旧 job `KNOWLEDGE_INGEST:{id}` | Worker 兼容：视为 **单阶段**，仍调用 monolithic `publish`；新上传仅发 PARSE |
| 配置开关 | `gymmind.knowledge.ingest.two-phase` 默认 **false**，灰度 tenant 或 dev 先 true |
| 数据库 | Flyway/Liquibase 增加 `chunk_artifact_key`；枚举 CHUNKED、INDEXING |
| 前端 | 文档列表可展示 CHUNKED/INDEXING；READY 前逻辑不变 |

---

## 9. 安全与权限

- artifact key 与 source 相同租户前缀；**禁止**跨 tenant 读 artifact。
- 下载 artifact 不对外开放 REST；仅 Worker 用 `KnowledgeObjectStore` 读。
- 删除文档时同步删 artifact，避免 MinIO 残留敏感 chunk。

---

## 10. 观测与运维

- 日志：PARSE/INDEX 分阶段耗时、chunk 数、artifact 字节大小、embed batch 失败原因。
- 指标：`ingest_parse_duration`, `ingest_index_duration`, `artifact_size_bytes`, `index_retry_count`。
- 管理 API（可选）：`POST /documents/{id}/reindex` 仅 enqueue INDEX（需 GYM_ADMIN）。

---

## 11. 测试要点

- 单元：`ChunkArtifact` 序列化/反序列化、generation key 规则、artifact 与 document 校验失败。
- 集成：PARSE 成功后 kill Worker → 重启 → INDEX 完成 READY；INDEX 失败 3 次不 re-parse（mock embed 失败）。
- 回归：two-phase=false 时行为与现网一致；RAG eval 语料 ingest 无 Leak/Recall 回退。

---

## 12. 工作量粗估（实现参考）

| 模块 | 内容 |
|------|------|
| 领域模型 | DocumentStatus、chunk_artifact_key、markChunked/markIndexing |
| ChunkArtifactService + DTO | JSON v1 读写 |
| KnowledgeIngestWorker | 拆分 PARSE / INDEX handler |
| KnowledgeIndexingService | publishFromArtifact |
| Recovery / 删除 / replace | artifact 生命周期 |
| 配置 + 测试 | 开关、IT、文档 |

---

## 13. 评审确认项

1. 是否接受新增状态 **CHUNKED、INDEXING**（前端/报表是否要展示）？
2. artifact **generation** 是否与 ES/Milvus deleteDocument 策略一致（always delete-by-documentId before publish）？
3. 默认 **two-phase 开关** 何时对生产 tenant 全开？
4. 单 artifact **32MB** 上限是否满足现网最大文档？

---

**请评审：回复「通过」或修改意见后，再进入编码计划与实现（Step 2）。**

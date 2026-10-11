# RAG 可靠性与召回优化修复方案

> 状态：**S3 已实现**（S1～S3；S4 Harness 对齐起待做）  
> 范围：索引发布、双写一致、入库恢复、检索权限与降级、评测/CI  
> 非目标：HyDE、多模态、按 chunk 独立 Redis job（二期）

---

## 0. 评审结论（已锁定）

| 项 | 决策 |
|----|------|
| **P0 索引发布** | **generation 指针**（不用 orphan 清理作为主方案） |
| **P1 受众字段** | **keyword 数组** `audienceRoles`（不用 bitmask） |
| **双路检索** | **`require-dual-recall: false`**（默认允许单路降级，须可观测） |

---

## 1. 问题汇总与优先级

| 优先级 | 问题 | 用户/系统影响 |
|--------|------|----------------|
| P0 | 先 delete 再 upsert，中断后文档索引空窗或半写 | 发布后短暂搜不到；ES/Milvus 不一致 |
| P0 | ES 与 Milvus 非原子双写 | 一路成功一路失败，Hybrid 结果漂移 |
| P0 | dev `recover-published` 重启全量重灌 | 启动慢、百炼/QPS 浪费 |
| P1 | 受众角色仅 MySQL 后过滤 | Top-K 被无权 chunk 占满，召回下降 |
| P1 | 单路检索失败静默降级 | 运维难发现，质量不可预期 |
| P1 | Harness 与 Search 路径差异 | eval 两条通道 pass 率不一致 |
| P2 | 检索超参、词面过滤、query 缓存硬编码 | 调参需改代码 |
| P2 | Rerank/MySQL 队列重试未对齐 | 行为不一致 |
| P2 | 无 CI SearchOnly eval | 回归靠手工 |

---

## 2. P0-1：索引发布 — **generation 指针**（已选）

### 2.1 目标

- 发布新版本索引时 **不先 deleteDocument 整篇**，避免空窗。
- 检索 **只读当前 generation** 的 chunk；旧 generation 异步 GC。
- 与现有 artifact 的 `{strategy}-v{version}-{millis}` 对齐，可共用同一 generation 字符串。

### 2.2 概念

```text
MySQL knowledge_document.index_generation = "GENERAL-v1-1739123456789"  // 当前可读指针
ES/Milvus 每条 chunk 带 indexGeneration = 同上
检索 filter: indexGeneration == 文档当前指针（按 documentId 批量或 join 内存 map）
旧 generation 的 chunk 仍在索引中，但对用户不可见，直到 GC 删除
```

### 2.3 发布流程（替换现 publishChunks 开头 delete）

```text
function publishChunks(document, chunks):
    generation = "document-{id}-{strategy}-v{ver}-{millis}"
    indexed = embed(chunks) with indexGeneration = generation on each chunk

    if !index.publishWithRollback(tenantId, indexed):   // P0-2 双写补偿
        return false

    // 原子切换指针（事务内）
    document.setIndexGeneration(generation)
    document.markReady() / save

    scheduleAsyncGc(tenantId, documentId, generation)   // 删 indexGeneration != generation 的 chunk
    return true
```

- **切换指针前**：检索仍用 **旧** `index_generation`（旧 chunk 仍可见）。
- **切换指针后**：仅新 generation 可见；旧数据后台删，删失败可通过对账 job 补删。

### 2.4 数据模型

| 位置 | 字段 | 说明 |
|------|------|------|
| `knowledge_document` | `index_generation` VARCHAR(128) NULL | 当前有效 generation；READY 且 published 时非空 |
| ES / Milvus chunk | `indexGeneration` keyword | 与 artifact generation 或 publish 时生成值一致 |
| `IndexedChunk` record | 扩展字段 | `indexGeneration`, `visibility`, `audienceRoles`（P1 同批写入） |

SQL 迁移：`docs/database/YYYY-MM-DD-knowledge-index-generation.sql`（实现阶段补充）。

### 2.5 检索侧

- `HybridRetriever` / ES / Milvus search：增加 filter  
  `indexGeneration == {doc.indexGeneration}`  
  实现方式（二选一，实现时定）：
  - **A**：召回后按 `documentId` 查 MySQL 当前指针再滤（简单，N 次 DB 或批量 `findByIdIn`）；
  - **B**：检索请求带 `Map<documentId, generation>`（Search 入口一次加载 searchable 文档的指针）。

- **未 READY / 无指针**：不参与检索（与现 published 语义一致）。

### 2.6 异步 GC

```text
function gcStaleGenerations(tenantId, documentId, keepGeneration):
    delete where documentId = X and indexGeneration != keepGeneration
    // ES delete_by_query + Milvus filter delete
```

- 触发：publish 成功后 enqueue 轻量 job 或同 INDEX job 末尾同步 GC（文档 chunk 数少时可同步）。
- 失败：日志 + 对账；**不影响** 指针已切换后的读路径。

### 2.7 与 orphan 方案对比（未采用）

| | generation 指针（已选） | orphan 清理 |
|--|-------------------------|-------------|
| 切换 | 改 MySQL 指针即生效 | 依赖 id 列表 diff |
| 存储 | 短期多 generation 共存 | 仅一套 id |
| 检索 | 必须 filter generation | 无需 generation 字段 |
| 改动 | mapping + 字段 + GC | listChunkIds + 删 orphan |

### 2.8 测试

- 发布 v2 后仅 v2 可被搜到；v1 chunk 在 GC 前存在但 filter 不可见。
- INDEX 失败：指针 **不** 更新，仍读 v1。
- Milvus 失败：本批 v2 ES 回滚（P0-2），指针不变。

---

## 3. P0-2：ES / Milvus 双写一致

### 3.1 目标

同批 **带同一 indexGeneration** 的 chunk：ES 成功、Milvus 失败时，不留下仅 ES 可见的新 generation 数据。

### 3.2 方案：同请求内补偿

```text
function publishWithRollback(tenantId, chunks):
    upsertAll(keyword, chunks)
    try:
        upsertAll(vector, chunks)
    catch ex:
        rollbackKeyword(chunks by id)   // 不 deleteDocument
        return false
    return true
```

- 指针切换 **仅在** `publishWithRollback == true` 之后执行。

### 3.3 对账 job（P1）

- 比较 ES/Milvus 同 `documentId + indexGeneration` 的 id 集合；不一致 → INDEX 重跑。

---

## 4. P0-3：收敛 `recover-published`

（不变，见原 §4）

```yaml
gymmind:
  search:
    recover-published: false
    recover-incomplete-ingest: true
    recover-index-on-start: false
```

dev 默认 `recover-published: false`。

---

## 5. P1-1：索引层受众 — **keyword 数组**（已选）

### 5.1 字段

| 字段 | ES | Milvus |
|------|-----|--------|
| `visibility` | keyword | VarChar |
| `audienceRoles` | keyword 多值 | JSON array 或 repeated（按 Milvus 能力选） |

- 值示例：`["GYM_ADMIN","COACH","MEMBER"]`，来自 `KnowledgeDocument.effectiveAudienceRoles()`。
- 平台全员文档：写入 **全部可搜角色** 或约定 `PLATFORM_ALL` 单值 + filter 特判。

### 5.2 检索 filter（与 MySQL 双保险）

```text
tenantId == T
AND (private 规则)
AND (audienceRoles 与 actor.roles 有交集 OR platformAdmin)
AND indexGeneration == document.index_generation
```

- **不采用 bitmask**（已否决）。

### 5.3 迁移

1. 新 mapping / 新 Milvus collection 或 schema 变更 + reload。
2. 全量 reindex；`index_generation` 与 chunk 字段一并写入。

---

## 6. P1-2：双路检索 — **`require-dual-recall: false`**（已选）

### 6.1 默认行为

| keyword | vector | 结果 |
|---------|--------|------|
| OK | OK | 正常 Hybrid |
| 失败 | OK | **200**，单路向量 + 降级标记 |
| OK | 失败 | **200**，单路关键词 + 降级标记 |
| 失败 | 失败 | **503** |

### 6.2 必须实现的可观测性（因默认 false）

- 响应头：`X-GymMind-Search-Degraded: keyword|vector|none`
- Progress / 日志：`degradedStages`
- 指标：`gymmind_search_degraded_total`

### 6.3 配置

```yaml
gymmind:
  search:
    require-dual-recall: false   # 全局默认；仅特殊环境可改 true
```

---

## 7. P1-3：Search 与 Harness 对齐

（不变：单入口 `KnowledgeSearchService.search`，eval diff citation ids）

---

## 8～10. P2 与 CI

（配置外置、query 缓存、rerank 重试、SearchOnly CI — 不变）

---

## 11. 实施顺序（按已选方案调整）

| 阶段 | 内容 | 工时 |
|------|------|------|
| **S1** | P0-1 **generation 指针** + MySQL 字段 + ES/Milvus 字段 + GC | 3～4d |
| **S1b** | P0-2 双写补偿 + 单测 | 含在 S1 |
| **S2** | P0-3 recover 配置 | 0.5d |
| **S3** | P1-2 降级 header/指标（false 默认） | 1d |
| **S4** | P1-3 Harness 对齐 | 1～2d |
| **S5** | P1-1 **audienceRoles keyword** + reindex | 3～5d |
| **S6** | P2 + CI | 2d |

**PR 建议**：S1+S2 → S3+S4 → S5 → S6。

---

## 12. 验收标准

- [ ] 新 generation 写入失败：DB 指针不变，检索仍见旧 generation。
- [ ] 指针切换成功后：仅新 generation 命中；旧 generation GC 后物理删除。
- [ ] `require-dual-recall=false` 且 ES 宕机：200 + degraded=keyword，503 不出现。
- [ ] eval PermissionLeak=0；受众 keyword filter 后负例题更稳。

---

## 13. 下一步

1. 输出 **编码计划**（文件清单、迁移 SQL、Milvus schema 变更步骤）。  
2. 评审通过后 **S1 实现**。

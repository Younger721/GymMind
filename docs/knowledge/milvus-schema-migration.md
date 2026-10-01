# Milvus 知识库 schema 迁移

当前知识库向量集合需要以下字段：`id`、`tenantId`、`documentId`、`ownerUserId`、`text` 和 `vector`。

`documentId` 用于把向量检索结果关联回租户内的知识文档。应用不会在启动时修改或删除已有集合；旧集合启用了固定 schema 时，继续向其中写入 `documentId` 会失败。

迁移步骤：

1. 备份当前集合中的知识数据，并确认原集合名称，例如 `gymmind_knowledge`。
2. 使用应用 `MilvusVectorIndex.collectionBody` 生成的新 schema 创建新集合，例如 `gymmind_knowledge_v2`。
3. 将 `GYMMIND_MILVUS_COLLECTION` 设置为新集合名称并重启后端。
4. 对现有知识文档执行一次全量入库，使新集合写入 `documentId`。
5. 验证租户内公开文档和私有文档都能完成检索后，再按运维备份策略处理旧集合。

不要在没有备份和检索验证的情况下直接删除旧集合。

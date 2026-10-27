# 知识库 DOCX OnlyOffice 在线编辑

> 状态：已实现  
> 关联：[2026-10-13-knowledge-upload-md-normalization.md](./2026-10-13-knowledge-upload-md-normalization.md)

## 目标

- `.docx` 在管理端 OnlyOffice 嵌入式编辑，保存覆盖 MinIO 原文件。
- 保存后自动 `unpublish`、状态 `UPLOADING`，走既有 ingest（规范化 MD → 切片 → 索引）。
- 仅 `READY` 后可再次发布。

## Docker

`docker-compose.yml` 服务 `onlyoffice`（端口默认 8088）。Document Server 通过 `host.docker.internal:8080` 访问宿主机 Spring Boot。

环境变量 `ONLYOFFICE_JWT_SECRET` 与 `gymmind.knowledge.onlyoffice.jwt-secret` 一致。

## API

| 方法 | 路径 | 鉴权 |
|------|------|------|
| GET | `/api/v1/knowledge/documents/{id}/editor-config` | Bearer |
| GET | `/api/v1/knowledge/documents/{id}/content?token&userId` | HMAC token |
| POST | `/api/v1/knowledge/documents/{id}/onlyoffice/callback?userId&session` | session token |

## 前端

- 列表「在线编辑」→ `/knowledge/:id/edit`
- 加载 `documentServerUrl/web-apps/apps/api/documents/api.js`，`DocEditor` 使用 JWT `token`。

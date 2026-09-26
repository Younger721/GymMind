# GymMind API 文档

运行时 OpenAPI 由 springdoc 自动生成：

- JSON：`http://localhost:8080/v3/api-docs`
- Swagger UI：`http://localhost:8080/swagger-ui/index.html`

所有业务 API 前缀为 `/api/v1`，受保护接口使用 JWT Bearer 认证（`Authorization: Bearer <token>`）。

响应统一使用 envelope：

```json
{
  "success": true,
  "data": {},
  "error": null,
  "traceId": "..."
}
```

## 会员与教练关系

会员资料与健身房归属分离，会员可以加入多个健身房。关系接口均使用当前 JWT 租户上下文：

| 方法 | 路径 | 角色 | 说明 |
|---|---|---|---|
| `POST` | `/api/v1/coach-relations` | 会员 | 申请选择的教练，正文包含 `coachId` |
| `POST` | `/api/v1/coach-relations/{id}/accept` | 教练 | 接受待处理申请 |
| `POST` | `/api/v1/coach-relations/{id}/reject` | 教练 | 拒绝待处理申请 |
| `POST` | `/api/v1/coach-relations/{id}/cancel` | 会员 | 取消待处理申请 |
| `POST` | `/api/v1/coach-relations/{id}/end` | 教练/会员 | 结束已建立关系 |
| `GET` | `/api/v1/coach-relations/pending` | 教练/会员管理员 | 查询当前租户待处理关系 |
| `GET` | `/api/v1/coach-relations/active` | 教练/会员管理员 | 查询当前租户有效关系 |

关系状态转换为 `PENDING -> ACTIVE`、`PENDING -> REJECTED`、`PENDING -> ENDED` 或 `ACTIVE -> ENDED`。服务端会验证会员和教练属于同一健身房，并对每个查询强制添加 `tenant_id` 条件。

导出静态 OpenAPI（应用启动后）：

```powershell
Invoke-RestMethod http://localhost:8080/v3/api-docs | ConvertTo-Json -Depth 100 | Out-File docs/api/openapi.json
```

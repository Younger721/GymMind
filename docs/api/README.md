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

导出静态 OpenAPI（应用启动后）：

```powershell
Invoke-RestMethod http://localhost:8080/v3/api-docs | ConvertTo-Json -Depth 100 | Out-File docs/api/openapi.json
```

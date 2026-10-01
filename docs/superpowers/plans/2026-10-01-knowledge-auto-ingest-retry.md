# 知识库上传自动解析重试计划

## 目标

上传文档后统一进入后台解析队列；解析异常由后台自动重试，最多 5 次。重试未完成前文档保持“解析中”，仅最终失败才显示“解析失败”。移除手动“重新解析”接口、客户端 API 和页面按钮。

## 实施步骤

1. 增加回归测试，覆盖首次解析失败会重新入队且文档仍为 `PARSING`，最终重试失败才为 `FAILED`；平台管理员上传也必须入队。
2. 修改 `KnowledgeDocument.markParsing()` 允许重试幂等，并在 `KnowledgeIngestWorker` 中按队列尝试次数区分中间失败和最终失败。
3. 调整上传用例，平台公共文档和健身房文档统一交由 `KnowledgeIngestWorker` 异步处理，不再因为平台文档而同步索引。
4. 删除文档用例、控制器、前端 API 和知识库页面中的手动重解析入口及对应过时测试。
5. 运行知识库相关后端测试、前端构建，并检查后端健康端点。

## 验证

```powershell
cd backend
.\mvnw.cmd "-Dtest=*Knowledge*,*Harness*,*Embedding*" test
cd ..\frontend
npm run build
```


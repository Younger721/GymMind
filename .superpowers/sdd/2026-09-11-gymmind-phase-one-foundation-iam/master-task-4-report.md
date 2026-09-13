# Task 4：操作审计落库

## 状态

已完成，提交：`feat: 建立租户级操作审计能力`

## 改动

- 新增 `AuditEvent`、`AuditResult` 和 `AuditRecorder` 端口。事件只接受动作、资源类型、资源 ID、结果、traceId 与白名单元数据；`actorId`、`tenantId` 始终从 `CurrentActorProvider.requireCurrent()` 获取。
- 新增 `OperationAudit` JPA 实体和只追加 `OperationAuditRepository`。实体字段全部不可更新，`@PreUpdate`/`@PreRemove` fail closed，未暴露更新或删除端口。
- 成功/失败审计在调用方事务内落库；拒绝审计通过 `DefaultRejectedAuditWriter` 的 `REQUIRES_NEW` 独立事务落库。
- 元数据仅允许 `reason`、`source`、`endpoint`、`httpStatus`、`requestId`、`changedFields`、`resourceName`，并拒绝密码、Token、健康资料、银行卡等敏感字段和值。
- 新增租户范围审计查询服务，查询条件只能来自当前租户，不接受调用方传入租户 ID。
- 新增单元测试覆盖身份来源、敏感字段、拒绝事件事务端口、只追加约束和租户查询边界。

## 测试

RED：

```text
backend\mvnw.cmd -q -f backend/pom.xml -Dtest=com.gymmind.audit.** test
Compilation failure: AuditEvent/OperationAudit/AuditRecorder 等类型不存在
```

GREEN：

```text
backend\mvnw.cmd -q -f backend/pom.xml -Dtest=com.gymmind.audit.** test
Tests run: 6, Failures: 0, Errors: 0

backend\mvnw.cmd -f backend/pom.xml test
Tests run: 123, Failures: 0, Errors: 0
BUILD SUCCESS
```

## 风险与边界

- 当前 Docker/Testcontainers 环境不稳定，未在本任务新增 MySQL IT；JPA 映射将由后续基础设施验收覆盖。
- 审计查询 API 控制器和分页将在路线图 Task 23 实现；本任务只提供应用端口和租户隔离查询服务。
- 拒绝事件要获得独立事务语义，生产调用必须通过 Spring 注入的 `RejectedAuditWriter`，不要自行 `new` 实现类。

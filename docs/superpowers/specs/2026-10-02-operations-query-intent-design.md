# 门店运营白名单查询意图设计

## 目标

让门店运营 AI 工具支持可组合的指标、时间范围和分组维度，同时禁止模型直接生成或执行任意 SQL。

## 设计

LLM 只输出 `gym_operations_metrics` 的 JSON 参数。参数包括：

- `metrics`：白名单指标，如 `UNIQUE_VISITORS`、`CHECK_IN_COUNT`、`ACTIVE_MEMBERS`、`BOOKINGS`、`ATTENDANCE_RATE`、`EXPIRING_MEMBERS`、`INACTIVE_MEMBERS`、`WORKOUT_MEMBERS`、`WORKOUT_COUNT`。
- `period`：`PREVIOUS_MONTH`、`PREVIOUS_PREVIOUS_MONTH`、`CURRENT_MONTH`、`LAST_7_DAYS`、`LAST_30_DAYS`，或通过 `from` 和 `to` 使用明确日期。
- `groupBy`：首期支持 `NONE`、`DAY`、`WEEK`、`MONTH`；未实现的维度拒绝执行。
- `from`、`to`：仅接受 ISO 日期，且由后端校验范围不超过 366 天。

后端从当前登录身份获取 `tenantId`，忽略任何租户参数。工具先校验角色、权限、参数枚举、日期范围和指标数量，再按指标选择固定 SQL 片段和参数绑定。SQL 只允许后端注册的表、字段和聚合表达式，查询默认限制结果数量和时间跨度。

未提供结构化参数时，保留对自然语言日期的兼容解析；模型需要查询数据库时应优先返回结构化参数。无效参数返回工具错误，不降级为猜测性回答。

## 数据流

```text
用户问题 -> LLM tool call -> JSON Schema 校验 -> tenant/role 校验
-> 安全 SQL 模板 -> OperationsMetricsContext -> LLM 中文回答
```

## 安全边界

- 不接受 `sql`、`querySql`、`rawQuery` 或任意表名、字段名。
- 只读工具最多执行两个调用。
- 结果只返回聚合值和限定数量的跟进名单。
- 每条查询记录工具、用户、租户、参数摘要、耗时和状态。

## 验收

- LLM 可以请求多个白名单指标和周/月分组。
- 伪造租户、任意 SQL、未知指标和过长日期范围被拒绝。
- 旧的自然语言“上个月”查询继续可用。
- RAG 开关不影响工具候选资格。

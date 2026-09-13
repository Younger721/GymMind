# GymMind 动作库与教学视频设计

## 目标

为每个独立健身房提供租户专属动作库和动作教学视频资源。动作、视频和对象键都必须带租户边界，不支持跨租户读取或关联。

## 数据模型

`Exercise` 为租户作用域实体，保存名称、分类、目标肌群、难度、器械、描述、动作步骤、常见错误、安全提示和标签。名称在同一租户内唯一。

`ExerciseVideo` 为租户作用域实体，保存动作 ID、标题、来源类型、平台、视频 URL、缩略图 URL、作者、时长和描述。来源类型固定为 `THIRD_PARTY` 或 `INTERNAL`：

- `THIRD_PARTY` 只允许绝对 `http`/`https` URL，不下载、不代理、不重新托管。
- `INTERNAL` 保存 MinIO 对象键，键格式为 `tenant/{tenantId}/exercise-videos/{videoId}/...`；对象存储通过端口抽象，当前功能不强依赖 MinIO 客户端。

动作和视频的查询必须同时使用 `tenant_id` 与资源 ID；视频关联的动作也必须属于当前租户。删除视频只删除租户内元数据，不影响第三方内容。

## 应用接口

- `ExerciseService.create/find/update`：仅 `GYM_ADMIN` 使用 `exercise:write` 或 `exercise:read`。
- `ExerciseVideoService.create/find/update/delete`：仅 `GYM_ADMIN` 使用 `video:write` 或 `video:read`。
- API 前缀固定为 `/api/v1/exercises` 和 `/api/v1/videos`，所有响应使用 `ApiResponse`。

## 校验与审计

名称、标题、分类、来源类型和动作关联必填；时长不能为负数；第三方来源必须通过 URI 解析校验 scheme；内部来源必须提供符合租户键前缀的对象键。所有写操作记录成功审计，越权和跨租户资源返回统一业务错误。

## 测试策略

先用应用服务单元测试覆盖租户边界、字段校验、第三方 URL、内部对象键和动作关联，再补 Controller 委托测试、权限目录初始化测试和 JPA 派生查询编译验证。普通测试不连接 MinIO；真实对象存储适配器留到知识与持久化任务阶段接入。

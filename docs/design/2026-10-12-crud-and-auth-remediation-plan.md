# CRUD / 鉴权 / 前后端一致性修复计划

> 状态：**P0～P3 已实现**（P4 仅 Agent 收口待定；运动视频功能产品暂缓）  
> 来源：2026-10-12 全项目 CRUD 扫描结论  
> 非目标：HyDE、RAG 调参、数据库全量清库（ destructive 操作需单独审批）  
> **运动视频**：当前不做该功能；不改造 `/api/v1/videos` CRUD/删除策略，后端代码与表结构暂保留。

---

## 1. 目标

- 消除 **Critical/High** 级租户隔离与 RBAC 缺口。
- 对齐 **前后端** 仍被引用的 CRUD/DELETE 路径。
- 补齐 **知识库/角色** 写路径的事务与数据一致性。
- 每项可独立 PR，便于回归与回滚。

---

## 2. 优先级与阶段

| 阶段 | 主题 | 风险 | 预估 |
|------|------|------|------|
| **P0** | 鉴权 + 教练分配 IDOR | 安全 | 1～2d |
| **P1** | 前后端 API 对齐 + 角色 PUT | 功能/安全 | 1d |
| **P2** | 知识库 CRUD 一致性 | 数据 | 1～2d |
| **P3** | REST 语义与校验统一 | 质量 | 2～3d |
| **P4** | 删除策略与 Agent 接口收口 | 产品 | 1～2d |

---

## 3. P0：鉴权与教练分配（必须先做）

### 3.1 `ActorAccess` 权限参数生效

**问题**：`requireGymAdmin(actor, permission)` 与 `requireExplicitGymAdmin` 均未调用 `actor.hasPermission(permission)`，传入的 `user:write` 等无效。

**方案**：

1. **`requireGymAdmin`**：在通过角色/租户校验后增加 `if (!actor.hasPermission(permission)) throw FORBIDDEN`（平台管理员白名单可保持 `isSuperAdmin` 短路，但需文档说明是否 bypass permission）。
2. **`requireExplicitGymAdmin`**：按类注释实现「仅显式授予的 permission」：`!actor.hasPermission(permission)` → FORBIDDEN；**不**再依赖 GYM_ADMIN 角色默认权限集。
3. 全库检索 `requireGymAdmin` / `requireExplicitGymAdmin` 调用点，区分：
   - 应走 **宽松**（仅角色）→ 改名为 `requireGymAdminRoleOnly` 或保留旧行为并显式命名，避免误用。
   - 应走 **严格**（permission）→ 改用修复后的方法。
4. **`DefaultUserAdministrationService`** 已有 `requireRolePermissionAccess`：对照避免双重标准。

**文件**：

- `backend/.../shared/security/ActorAccess.java`
- 所有调用方（约 15+ 类，见 grep `requireGymAdmin`）

**测试**：

- 新增 `ActorAccessTest`：GYM_ADMIN 无 `exercise:write` 时调用需 permission 的路径 → FORBIDDEN。
- 补 1～2 个集成测试（如 `ExerciseVideoService` 写操作）。

**验收**：

- [ ] 默认 dev 管理员账号在配置最小 permission 后，无法访问未授权写接口。
- [ ] 现有 CI / 主要模块单测通过；必要时更新测试里 mock actor 的 permission 集合。

**风险**：dev 默认 admin 若 permission 过全则无感；若过窄可能导致本地需调角色模板 — 在 `CONFIGURATION.md` 或 migration 说明中写清。

---

### 3.2 教练分配 `assign` 校验 member 归属

**问题**：`DefaultCoachAssignmentService.assign()` 未验证 `memberId` 属于 `tenantId`。

**方案**：

```text
assign():
  已有 coach findByTenantIdAndId
  新增 members.findByTenantIdAndId 或 memberships.existsByTenantIdAndMemberId
  不存在 → RESOURCE_NOT_FOUND 或 FORBIDDEN（与项目惯例统一）
```

**文件**：

- `backend/.../coach/application/DefaultCoachAssignmentService.java`
- 新增/扩展 `CoachAssignmentServiceTest`

**验收**：

- [ ] 跨租户 memberId 无法 assign。
- [ ] 本租户合法 member 仍可 assign；重复 assign 仍幂等。

---

## 4. P1：前后端对齐 + 角色权限 PUT

### 4.1 前端 dead / 错误 API

| 前端 | 现状 | 动作 |
|------|------|------|
| `frontend/src/api/workoutPlan.ts` | `DELETE /api/workout-plans/{id}` 无 v1、后端无 DELETE | 删除调用或改为后端真实语义（如 cancel/archive）；全局搜引用 |
| `frontend/src/api/image.ts` | `DELETE /api/knowledge/images/{id}` 无后端 | 删除或对接 MinIO/知识库现有能力；无需求则删 API 封装 |

**验收**：

- [ ] 前端构建无引用死链；相关页面手动点删无 404。

---

### 4.2 `RoleController.updatePermissions` null body

**方案**：

- `request == null` → `VALIDATION_FAILED`（与空权限意图区分）。
- 若需「清空权限」：显式 `{"permissions":[]}` + 二次确认或仅平台管理员（产品定）。
- 增加 `@Valid` DTO。

**文件**：`iam/api/RoleController.java`、`iam/application/*RolePermission*`

**测试**：PUT null body → 4xx；PUT `[]` 行为符合产品。

---

## 5. P2：知识库 CRUD

### 5.1 `upload()` 事务与补偿

**方案（择一，推荐 A）**：

- **A**：`upload` 加 `@Transactional`；对象存储失败则回滚 DB；DB 失败则删已写 object（try/finally 或 Outbox 简化版）。
- **B**：先 DB PENDING + 异步写 store（改动大，不首选）。

**文件**：`DefaultKnowledgeDocumentUseCase.java`

---

### 5.2 `delete()` 清理 chunk 表

**方案**：

- `delete()` 在 tombstone + index + store 之后调用 `KnowledgeChunkRepository.deleteByTenantIdAndDocumentId`（或软删标记，与 chunk 模型一致）。
- 确认与 `indexGeneration` GC 不冲突。

**验收**：

- [ ] 删文档后 DB chunk 行数归零（或 status=DELETED）。
- [ ] 检索不再命中该 documentId。

---

### 5.3 上传/更新校验（可选同 PR）

- `KnowledgeDocumentController.upload`：文件大小、类型、visibility 枚举校验。
- `update`：考虑 JSON body + `@Valid`（低优先级可放 P3）。

---

## 6. P3：REST 语义与校验（批量小 PR）

| 项 | 动作 |
|----|------|
| WorkoutPlan `PUT /{id}` | 拆路由：会员 `PUT /member/{id}`；教练/管理员 `PUT /coach/{id}` 或 PATCH 状态机 |
| PATCH 全量必填 | 文档标明「全量替换」或改为可选字段 + `@JsonProperty` |
| `TrainingController.create` | `@Valid` on command |
| `ExerciseVideoController.delete` | 204 无 body，或 200 + ApiResponse 二选一 |
| 教练 `unassign` | 0 行删除 → 404 或 204 幂等（产品定） |
| 平台 `BookingService.list` | 文档 + 可选强制 `tenantId` query 参数 |

---

## 7. P4：删除策略与 Agent 接口

| 项 | 动作 |
|----|------|
| ~~运动视频硬删~~ | **不做**（功能暂缓） |
| Agent CRUD | Controller 隐藏未实现端点，或返回 501 + 文档；仅保留 `updatePrompt` |
| 用户列表平台 `findAll` | 分页改为 DB 层 + 租户 filter 参数 |

---

## 8. PR 拆分建议

```text
PR1 (P0): ActorAccess + 调用点梳理 + 单测
PR2 (P0): Coach assign member 校验
PR3 (P1): Role PUT + 前端 workoutPlan/image API
PR4 (P2): Knowledge upload 事务 + delete chunk 清理
PR5 (P3): WorkoutPlan 路由拆分（可单独评审产品）
PR6+ : P3/P4 剩余项
```

---

## 9. 回归清单（每 PR）

- `mvn -f backend/pom.xml test`（相关模块 + 安全相关新增测试）
- 手动：GYM_ADMIN 登录 → 用户/教练/知识库/视频 各 1 次 CRUD
- 权限负例：无 permission 的 admin token → 写操作 403
- 前端：受影响页面 smoke

---

## 10. 下一步

评审本计划后回复 **「通过」** 再进入编码；优先实施 **PR1 + PR2**。

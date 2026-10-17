# 会员停卡 / 转卡审批 — 详细设计

> 状态：待评审  
> 日期：2026-10-14

## 1. 背景与目标

门店需要对**会员卡停卡（冻结）**与**转卡（权益转移）**建立可追溯的审批流，避免前台或会员直接改库。业务要求：

- 停卡、转卡均先进入**待审批**状态，经**健身房管理员（GYM_ADMIN）**通过或驳回后生效。
- **教练、会员**不能执行审批；**平台管理员**默认不参与本审批（见 §4.3）。

当前代码：`member_membership` 已有 `SUSPENDED` 枚举但未用于停卡流程；会员 `suspend` API 仅冻结**账号**而非**会籍卡**。

## 2. 业务规则（默认方案，可评审调整）

### 2.1 停卡（冻结）

对齐知识库样例《门店会员服务规则》中的「请假与延期」：

| 项 | 规则 |
|---|---|
| 适用卡 | 租户下状态为 `ACTIVE`、未过期（`expires_at` 为空或 ≥ 当前时间）的 `member_membership` |
| 申请时段 | `freezeStart`、`freezeEnd`（含起止日，闭区间），`freezeEnd >= freezeStart`，单次最长 **30** 天 |
| 频次 | **首版不实现**月度次数上限；预留 `reason` 与审计，后续可按套餐类型扩展 |
| 审批通过 | ① 会籍 `status` 置为 `SUSPENDED`（冻结期内不可预约/扣次）；② `expires_at` 顺延 `(freezeEnd - freezeStart + 1 天)`（无到期日的次卡仅改状态不顺延） |
| 审批驳回 | 申请单 `REJECTED`，会籍不变 |
| 冻结结束 | 定时任务或管理员「解冻」：若当前时间 > `freezeEnd` 且存在已批准的停卡记录，将 `SUSPENDED` 恢复为 `ACTIVE`（首版可用**审批通过时写入 plannedResumeAt**，由 `@Scheduled` 每日扫描执行，避免会员永久停卡） |

### 2.2 转卡

| 项 | 规则 |
|---|---|
| 转出 | 指定 `membershipId`，须为本租户、当前会员持有、`ACTIVE` 且可用 |
| 转入 | `targetMemberId` 须为**同一租户**下已入会会员（`member_tenant` 存在），且不能是转出会员本人 |
| 审批通过 | 同一 `member_membership` 行的 `member_id` 更新为 `targetMemberId`（不复制新行，保留订单/审计关联） |
| 审批驳回 | 申请单 `REJECTED`，会籍不变 |
| 在途限制 | 同一 `membershipId` 同时最多 **1** 条 `PENDING` 申请（停卡或转卡任一类型） |

### 2.3 谁可以发起申请

| 角色 | 停卡 | 转卡 |
|---|---|---|
| 会员（MEMBER） | 仅对自己的会籍 | 仅对自己的会籍 |
| 健身房管理员 | 可代任意本店会员提交 | 可代任意本店会员提交 |
| 教练 | 不允许 | 不允许 |

提交权限：`membership:change:request`（会员默认授予；管理员通过 `menu:operations` 扩展）。

## 3. 数据模型

### 3.1 新表 `membership_change_request`

租户级实体，继承 `TenantScopedEntity` 公共字段（`id`, `tenant_id`, `created_at`, `updated_at`）。

| 字段 | 类型 | 说明 |
|---|---|---|
| `type` | VARCHAR(16) | `FREEZE` \| `TRANSFER` |
| `status` | VARCHAR(16) | `PENDING` \| `APPROVED` \| `REJECTED` \| `CANCELLED` |
| `membership_id` | BIGINT | 关联 `member_membership.id` |
| `applicant_user_id` | BIGINT | 提交人用户 ID |
| `member_id` | BIGINT | 会籍所属会员（冗余，便于列表查询） |
| `target_member_id` | BIGINT NULL | 转卡目标会员；停卡为空 |
| `freeze_start` | DATE NULL | 停卡开始日 |
| `freeze_end` | DATE NULL | 停卡结束日 |
| `reason` | VARCHAR(500) NULL | 申请说明 |
| `reviewer_id` | BIGINT NULL | 审批人用户 ID |
| `reviewed_at` | INSTANT NULL | 审批时间 |
| `review_comment` | VARCHAR(500) NULL | 审批意见 |

索引建议：

- `(tenant_id, status, id DESC)` — 管理员待办列表  
- `(tenant_id, membership_id, status)` — 在途校验  
- `(tenant_id, member_id, id DESC)` — 会员「我的申请」

### 3.2 会籍表扩展（可选，首版推荐）

在 `member_membership` 增加：

| 字段 | 说明 |
|---|---|
| `freeze_until` | DATE NULL，审批通过后写入 `freeze_end`，解冻任务读后清空 |

若不增列，可从最新 `APPROVED` 的 FREEZE 申请推导；增列便于 entitlement 快速判断。

### 3.3 权限 catalog

| 权限码 | 说明 | 默认角色 |
|---|---|---|
| `membership:change:request` | 提交停卡/转卡申请 | MEMBER、GYM_ADMIN |
| `membership:change:read` | 查看申请列表/详情 | GYM_ADMIN、MEMBER（仅自己的） |
| `membership:change:approve` | 审批通过/驳回 | **仅 GYM_ADMIN** |
| `membership:change:cancel` | 取消自己的待审批申请 | MEMBER、GYM_ADMIN |

菜单：挂到 `menu:operations`，与现有 `membership:read/write` 同组。

## 4. API 设计

Base path：`/api/v1/membership-change-requests`

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| POST | `/` | 创建申请（body 含 `type` 及类型字段） | `membership:change:request` |
| GET | `/` | 列表；管理员可按 `status` 过滤；会员仅看自己相关 | `membership:change:read` |
| GET | `/{id}` | 详情 | `membership:change:read` + 租户/归属校验 |
| POST | `/{id}/cancel` | 取消 `PENDING` | 申请人或管理员 |
| POST | `/{id}/approve` | 通过 | **GYM_ADMIN + `membership:change:approve`** |
| POST | `/{id}/reject` | 驳回 | 同上 |

### 4.1 请求体示例

**停卡**

```json
{
  "type": "FREEZE",
  "membershipId": 1001,
  "freezeStart": "2026-10-20",
  "freezeEnd": "2026-10-25",
  "reason": "出差"
}
```

**转卡**

```json
{
  "type": "TRANSFER",
  "membershipId": 1001,
  "targetMemberId": 2002,
  "reason": "家人使用"
}
```

**审批**

```json
{ "comment": "材料齐全" }
```

### 4.2 响应 View

`MembershipChangeRequestView`：`id`, `tenantId`, `type`, `status`, `membershipId`, `memberId`, `targetMemberId`, `freezeStart`, `freezeEnd`, `reason`, `applicantUserId`, `reviewerId`, `reviewedAt`, `reviewComment`, `createdAt`。

### 4.3 审批鉴权（硬性）

```text
requireGymAdminStrictForApproval(actor):
  - actor.roles 必须包含 GYM_ADMIN
  - actor.tenantId 非空
  - actor.hasPermission("membership:change:approve")
  - 拒绝 PLATFORM_ADMIN（即使带租户上下文也不允许审批）
  - 审批人 userId 不得等于 applicantUserId（禁止自批）
```

实现方式：在 `ActorAccess` 新增 `requireGymAdminApproval(actor, permission)`，**不**调用现有的 `isSuperAdmin` 短路。

## 5. 领域逻辑伪代码

### 5.1 创建

```text
create(actor, command):
  tenantId = requireTenantId(actor)
  membership = load(tenantId, command.membershipId)
  assert membership usable
  assert no PENDING request for membershipId
  if actor is MEMBER:
    assert membership.memberId == actor's memberId in tenant
  if command.type == FREEZE:
    validate dates, max 30 days
  if command.type == TRANSFER:
    assert targetMember exists in tenant and != membership.memberId
  save request PENDING
  audit MEMBERSHIP_CHANGE_REQUESTED
```

### 5.2 审批通过

```text
approve(actor, id, comment):
  requireGymAdminStrictForApproval(actor)
  request = load PENDING
  if FREEZE:
    membership.suspendForFreeze(freezeStart, freezeEnd)  // status=SUSPENDED, extend expires_at, freeze_until
  if TRANSFER:
    membership.transferTo(targetMemberId)
  request.approve(actor.userId(), comment)
  audit MEMBERSHIP_CHANGE_APPROVED
```

### 5.3 审批驳回

```text
reject(actor, id, comment):
  requireGymAdminStrictForApproval(actor)
  request.reject(...)
  audit MEMBERSHIP_CHANGE_REJECTED
```

### 5.4 解冻任务（Scheduled）

```text
daily:
  for each membership where status=SUSPENDED and freeze_until < today:
    status = ACTIVE, clear freeze_until
```

### 5.5 与预约 entitlement 联动

`DefaultMembershipEntitlementPort` 已只选 `ACTIVE`；停卡后 `SUSPENDED` 自然无法约课/扣次。解冻后恢复。

## 6. 前端（建议范围）

在「门店运营 / 会员」或独立「会籍变更审批」页：

- 管理员：待审批列表、通过/驳回对话框。
- 会员端（Profile 或会员中心）：提交停卡/转卡、查看进度、取消待审批。

首版若仅后端，可先交付 API + 管理员最小页面。

## 7. 测试要点

- 会员只能对自己的会籍申请；不能审批。
- 教练提交/审批均 403。
- 平台管理员审批 403（严格模式）。
- 管理员不能审批自己提交的申请。
- 转卡：目标非本租户会员 → 校验失败。
- 停卡通过：到期日顺延、`SUSPENDED`；解冻任务恢复 `ACTIVE`。
- 同一卡重复 PENDING → CONFLICT。

## 8. 待确认问题（评审请回复）

1. **停卡语义**：是否采用「冻结期内 SUSPENDED + 到期顺延」？还是仅顺延、不改变 status？
2. **转卡对象**：是否只允许转给**本店已有会员**？是否需要支持「填写手机号由系统匹配」？
3. **平台管理员**：是否一律不能审批（本设计默认 **不能**）？
4. **首版范围**：是否包含会员端提交 UI，还是仅管理员代提 + 审批后台？
5. **月度停卡次数上限**：首版是否要做（样例规则 3/7 天）？

---

评审通过后进入《编码计划》与实现阶段。

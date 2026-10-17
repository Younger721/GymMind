# 会员停卡 / 转卡审批 — 编码计划

> 状态：已执行  
> 日期：2026-10-14

## 文件清单

| 模块 | 文件 |
|---|---|
| 领域 | `MembershipChangeRequest`、`MemberMembership` 扩展、`MembershipChangeType/Status` |
| 仓储 | `MembershipChangeRequestRepository` + JPA |
| 应用 | `DefaultMembershipChangeRequestService`、`MembershipFreezeResumeScheduler` |
| API | `MembershipChangeRequestController` |
| 权限 | `PermissionCatalog`、`ActorAccess.requireGymAdminApproval` |
| 前端 | `membershipChangeRequests.ts`、`MembershipChangeApprovalView.vue`、Profile 停卡入口 |
| 测试 | 领域 / 服务 / `ActorAccess` |

## 验证

```bash
cd backend && mvn -q test -Dtest=MembershipChangeRequest*,MemberMembershipFreezeTest,ActorAccessTest,SystemCatalogInitializerTest
```

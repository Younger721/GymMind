# 角色权限控制台 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为平台管理员提供角色权限配置页面，并通过现有 IAM 表和 Spring Security 让配置在登录权限中生效。

**Architecture:** 后端在 IAM application 层增加角色权限目录查询与整体替换用例，API 只允许平台管理员调用并保护平台管理员角色。前端新增权限管理 API 与页面，使用复选框矩阵保存角色权限；现有路由、菜单和按钮继续消费登录响应中的权限集合。

**Tech Stack:** JDK 17, Spring Boot, Spring Data JPA, Vue 3, TypeScript, Element Plus.

## Global Constraints

- 保留现有租户隔离和 Spring Security API 校验。
- 平台管理员角色不可被权限面板修改。
- 不增加单个用户例外授权。
- 使用现有 `sys_role`、`sys_permission`、`sys_role_permission` 表。

---

### Task 1: 后端角色权限应用服务

**Files:**
- Modify: `backend/src/main/java/com/gymmind/iam/application/UserAdministrationService.java`
- Modify: `backend/src/main/java/com/gymmind/iam/application/DefaultUserAdministrationService.java`
- Modify: `backend/src/main/java/com/gymmind/iam/domain/repository/PermissionRepository.java`
- Modify: `backend/src/main/java/com/gymmind/iam/domain/repository/RolePermissionRepository.java`
- Modify: `backend/src/main/java/com/gymmind/iam/infrastructure/persistence/SpringDataPermissionRepository.java`
- Modify: `backend/src/main/java/com/gymmind/iam/infrastructure/persistence/SpringDataRolePermissionRepository.java`
- Test: `backend/src/test/java/com/gymmind/iam/application/RolePermissionAdministrationServiceTest.java`

- [ ] 添加 `listPermissionCatalog(CurrentActor)` 和 `replaceRolePermissions(CurrentActor, RoleCode, Set<String>)` 接口及 view record。
- [ ] 为 repository 增加列出全部权限、按角色读取权限编码、删除角色关联和按权限编码批量查找能力。
- [ ] 在 service 中校验平台管理员、拒绝 `PLATFORM_ADMIN`、拒绝未知权限，并事务性替换关联。
- [ ] 编写服务测试覆盖成功保存、非平台管理员拒绝、平台角色保护和未知权限拒绝。

### Task 2: 后端 REST API

**Files:**
- Modify: `backend/src/main/java/com/gymmind/iam/api/RoleController.java`
- Create: `backend/src/main/java/com/gymmind/iam/api/response/RolePermissionCatalogResponse.java`
- Test: `backend/src/test/java/com/gymmind/iam/api/RolePermissionControllerTest.java`

- [ ] 增加 `GET /api/v1/roles/permissions` 返回角色、权限目录和当前关联。
- [ ] 增加 `PUT /api/v1/roles/{roleCode}/permissions` 接收 `{ "permissions": ["member:read"] }`。
- [ ] 在 controller 使用 `ActorAccess.requirePlatformAdmin(actor, "role:permission:write")`，并保持 service 二次校验。
- [ ] 编写 MockMvc 测试验证平台管理员成功、普通管理员 403、平台角色不可修改。

### Task 3: 前端 API 与权限管理页面

**Files:**
- Create: `frontend/src/api/rolePermissions.ts`
- Create: `frontend/src/views/business/RolePermissionView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/layouts/MainLayout.vue`
- Modify: `frontend/src/utils/displayLabels.ts`

- [ ] 定义 catalog、role permission view 和更新请求类型。
- [ ] 新增平台管理员专属路由 `/role-permissions`，页面按角色选择、权限分组和复选框展示。
- [ ] 保存时调用 PUT，成功后刷新 catalog 并提示“权限已保存”。
- [ ] 在平台控制台菜单加入入口，页面权限使用 `role:permission:read`。
- [ ] 平台管理员角色显示只读并禁用保存。

### Task 4: 验证与回归

**Files:**
- Modify: `frontend/src/api/auth.ts` only if response typing requires it.

- [ ] 运行后端 `./mvnw test` 或至少 `./mvnw -DskipTests compile`。
- [ ] 运行前端 `npm run build`。
- [ ] 检查登录权限仍能读取数据库角色权限，确认新页面路由和菜单只对平台管理员可见。
- [ ] 检查后端健康接口 `http://localhost:8080/actuator/health`。

# 角色权限管理重构 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将角色权限页面改造成角色列表、角色禁用/启用和树形权限配置，并让禁用角色不再产生有效权限。

**Architecture:** 后端在 `Role` 增加启用状态，角色管理服务提供状态切换并在分配、登录权限构建时过滤禁用角色；前端复用现有权限目录和保存接口，增加角色列表状态操作和 `el-tree` 权限弹窗。

**Tech Stack:** Spring Boot 3、JPA、JUnit、Vue 3、TypeScript、Element Plus、Vite。

## Global Constraints

- 平台管理员角色不可禁用、不可修改权限。
- 禁用角色保留配置，但不能分配给新用户，已有用户权限立即失效。
- 所有权限保存必须使用现有权限编码接口。

---

### Task 1: 角色状态领域模型与数据库映射

**Files:**
- Modify: `backend/src/main/java/com/gymmind/iam/domain/model/Role.java`
- Modify: role schema/migration file used by the project for `sys_role`
- Test: `backend/src/test/java/com/gymmind/iam/domain/RoleTest.java`

- [ ] 添加 `ACTIVE`/`DISABLED` 状态字段，默认 `ACTIVE`，提供 `disable()`, `activate()`, `isActive()`。
- [ ] 增加领域测试覆盖默认状态和幂等切换。
- [ ] 增加数据库列并设置非空默认值，确认现有数据迁移后为 ACTIVE。
- [ ] 运行角色领域测试和后端编译。

### Task 2: 后端角色管理接口与权限过滤

**Files:**
- Modify: `backend/src/main/java/com/gymmind/iam/application/UserAdministrationService.java`
- Modify: `backend/src/main/java/com/gymmind/iam/application/DefaultUserAdministrationService.java`
- Modify: `backend/src/main/java/com/gymmind/iam/api/RoleController.java`
- Modify: `backend/src/main/java/com/gymmind/iam/api/response/RoleResponse.java`
- Modify: user-role assignment and authentication permission query classes
- Test: role service/controller tests under `backend/src/test/java/com/gymmind/iam`

- [ ] 增加 `listRoles` 返回状态和权限数量。
- [ ] 增加 `disableRole(RoleCode)`、`enableRole(RoleCode)`，要求 `role:write`，拒绝 PLATFORM_ADMIN。
- [ ] 用户创建、邀请和换角色前校验目标角色为 ACTIVE。
- [ ] 登录权限构建过滤 DISABLED 角色，使已有用户权限立即失效。
- [ ] 增加状态接口 `PATCH /v1/roles/{roleCode}/disable` 和 `/enable`。
- [ ] 测试 403、平台角色保护、禁用角色不可分配及权限过滤。

### Task 3: 前端角色列表与状态操作

**Files:**
- Modify: `frontend/src/api/users.ts`
- Modify: `frontend/src/views/business/RolePermissionView.vue`
- Modify: `frontend/src/types/api.ts` if response typing requires it

- [ ] 增加角色状态接口调用。
- [ ] 用表格展示角色名、编码、状态、权限数量和操作列。
- [ ] 操作列提供权限管理、禁用/启用；禁用前弹出确认。
- [ ] 禁止在前端对 PLATFORM_ADMIN 显示禁用和编辑操作。
- [ ] 处理加载失败、保存失败和状态切换失败提示。

### Task 4: 树形权限配置弹窗

**Files:**
- Modify: `frontend/src/views/business/RolePermissionView.vue`
- Modify: `frontend/src/api/rolePermissions.ts` only if response typing needs expansion

- [ ] 建立菜单树到权限编码的静态映射，节点包含页面权限和操作权限。
- [ ] 使用 `el-dialog` + `el-tree show-checkbox` 展示树形目录。
- [ ] 打开弹窗时回显当前角色已有权限；保存时只提交权限编码集合。
- [ ] 父子节点联动，保存成功刷新角色列表和权限目录。
- [ ] 用前端构建检查模板类型和事件类型。

### Task 5: 集成验证

**Files:**
- Modify: relevant tests only when failures identify contract mismatches

- [ ] 运行后端角色相关测试和 `mvn test`/项目规定的构建命令。
- [ ] 运行 `frontend` 的 `npm run build`。
- [ ] 手动验证平台管理员：角色列表、禁用、启用、权限树回显和保存。
- [ ] 手动验证禁用角色不能新建/邀请用户，已有用户登录后权限失效。


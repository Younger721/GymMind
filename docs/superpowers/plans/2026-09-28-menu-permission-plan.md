# 菜单权限控制实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 让四个内置角色只通过菜单权限控制页面可见和菜单内功能，并修复角色权限页 500。

**Architecture:** 在现有 `sys_permission` 表中增加固定 `menu:*` 目录项。角色权限接口只返回菜单项；保存时按菜单映射展开现有接口权限，供现有后端服务鉴权。前端路由和侧栏统一消费菜单码，登录后的接口权限继续用于按钮和后端请求。

**Tech Stack:** Spring Boot 3.4、JPA、MySQL、Vue 3、TypeScript、Element Plus。

## Global Constraints

- 固定角色为 `PLATFORM_ADMIN`、`GYM_ADMIN`、`COACH`、`MEMBER`。
- 平台管理员权限固定全量且不可在页面修改。
- 健身房管理员只能管理当前健身房，并可看到当前健身房教练。
- 不增加用户级例外授权，不修改无关业务模块。

### Task 1: 后端菜单目录和角色展开

**Files:**
- Modify: `backend/src/main/java/com/gymmind/platform/bootstrap/PermissionCatalog.java`
- Modify: `backend/src/main/java/com/gymmind/platform/bootstrap/SystemCatalogInitializer.java`
- Modify: `backend/src/main/java/com/gymmind/iam/application/DefaultUserAdministrationService.java`
- Modify: `backend/src/main/java/com/gymmind/shared/security/CurrentActor.java`
- Test: `backend/src/test/java/com/gymmind/platform/bootstrap/SystemCatalogInitializerTest.java`
- Test: `backend/src/test/java/com/gymmind/iam/application/UserAdministrationServiceTest.java`
- Test: `backend/src/test/java/com/gymmind/shared/security/CurrentActorTest.java`

- [ ] 先写测试，确认目录包含菜单码、健身房管理员不再拥有未关联的接口权限、菜单保存会展开接口码。
- [ ] 增加菜单条目、默认角色菜单集合和菜单到接口权限的映射。
- [ ] 启动时补齐已有目录名称/审计时间并建立默认菜单关联，保留租户隔离。
- [ ] 让角色权限页目录只返回菜单条目，平台管理员仍固定全量。

### Task 2: 角色权限 API 和数据修复

**Files:**
- Modify: `backend/src/main/java/com/gymmind/iam/api/RoleController.java`
- Modify: `backend/src/main/java/com/gymmind/iam/domain/repository/PermissionRepository.java`
- Modify: `backend/src/main/java/com/gymmind/iam/infrastructure/persistence/JpaPermissionRepository.java`
- Modify: `backend/src/main/java/com/gymmind/iam/infrastructure/persistence/SpringDataPermissionRepository.java`
- Test: `backend/src/test/java/com/gymmind/iam/api/RolePermissionControllerTest.java`

- [ ] 保持 `/api/v1/roles/permissions` 和 `PUT /api/v1/roles/{roleCode}/permissions` 的平台管理员保护。
- [ ] 保存请求只接受菜单码，拒绝未知/操作权限码，并整体替换角色关联。
- [ ] 对历史 `role:permission:read/write` 零日期数据提供幂等修复路径。

### Task 3: 前端菜单显示和角色页面

**Files:**
- Modify: `frontend/src/api/rolePermissions.ts`
- Modify: `frontend/src/views/business/RolePermissionView.vue`
- Modify: `frontend/src/layouts/MainLayout.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/utils/permissions.ts`

- [ ] 先写或运行类型检查，确认 API 路径带 `/v1`。
- [ ] 角色权限树只展示菜单名称与层级，平台管理员只读。
- [ ] 侧栏和路由统一按菜单码判定，默认分配符合已确认的四角色范围。
- [ ] 页面内按钮仍使用菜单展开后的接口权限，避免菜单可见但接口 403。

### Task 4: 验证和启动

- [ ] 运行后端聚焦测试，再运行 Maven 编译/测试。
- [ ] 运行前端 `npm run build`。
- [ ] 启动后端 `localhost:8080` 和前端 `localhost:5173`，检查健康接口和角色权限接口。
- [ ] 检查工作区差异，只保留本任务必要修改。

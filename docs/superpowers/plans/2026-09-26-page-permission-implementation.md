# 页面与接口权限统一实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 统一前端页面权限与后端 Spring Security 接口权限，使页面可见角色与可操作接口保持一致，并保留租户数据隔离。

**Architecture:** 使用现有 JWT/CurrentActor/ActorAccess 作为后端身份基础，补充集中式权限目录；前端通过权限守卫控制路由、菜单和按钮，后端通过相同权限编码及 tenantId 校验保护接口。

**Tech Stack:** Spring Boot 3.4, Spring Security, JWT, Vue 3, Vue Router 4, TypeScript, JUnit 5.

## Global Constraints

- 使用 JDK 17。
- 平台管理员可访问全平台；健身房管理员仅访问自身 tenantId。
- 不删除或重置工作区已有用户改动。
- 前端隐藏不能替代后端接口校验。

### Task 1: 权限目录与角色映射

**Files:**
- Modify: `backend/src/main/java/com/gymmind/platform/bootstrap/PermissionCatalog.java`
- Modify: `frontend/src/utils/displayLabels.ts`
- Test: `backend/src/test/java/com/gymmind/platform/bootstrap/SystemCatalogInitializerTest.java`

- [ ] 增加页面/操作权限编码并确认角色默认权限覆盖平台管理员、健身房管理员、教练、会员。
- [ ] 运行权限目录测试。

### Task 2: 后端接口权限与数据范围

**Files:**
- Modify: 受影响的 `backend/src/main/java/com/gymmind/**/api/*Controller.java`
- Modify: 对应 application service
- Test: `backend/src/test/java/com/gymmind/shared/security/*Test.java` 及对应服务测试

- [ ] 为核心页面接口增加统一权限校验。
- [ ] 平台管理员走全平台范围，健身房管理员强制当前 tenantId，教练/会员强制自身或关联资源。
- [ ] 验证未授权返回 403、跨租户资源不可访问。

### Task 3: 前端路由、菜单与按钮权限

**Files:**
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/layouts/MainLayout.vue`
- Create/Modify: `frontend/src/utils/permissions.ts`
- Modify: 业务页面中的操作按钮

- [ ] 增加 `hasPermission`、路由守卫和 403 页面跳转。
- [ ] 菜单与按钮使用权限编码控制显示。
- [ ] 保证平台管理员和健身房管理员的用户管理页面显示符合数据范围。

### Task 4: 验证与启动

- [ ] 执行后端权限测试和编译。
- [ ] 执行前端构建。
- [ ] 启动后端并检查 `/actuator/health`。
- [ ] 手工确认四种角色的页面和接口访问边界。

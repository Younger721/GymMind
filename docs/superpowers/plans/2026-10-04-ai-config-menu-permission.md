# AI 配置菜单权限 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将门店 AI 配置页从普通 AI 中心菜单中拆出，确保只有平台管理员和健身房管理员可以看到和访问。

**Architecture:** 后端 `PermissionCatalog` 提供独立的 `menu:ai-config`，只把 Agent 管理操作映射到该菜单；前端使用同一个菜单代码驱动侧边栏、路由和角色权限树。已有角色权限通过增量目录初始化保留，管理员可在角色权限页选择新菜单。

**Tech Stack:** Spring Boot 3、Java 17、JUnit 5、Vue 3、TypeScript、Node test runner。

## Global Constraints

- 不执行数据库删除、清空、重置或重建操作。
- 保留共享工作区中其他任务的未提交改动，只提交本任务相关文件。
- 每次修改后使用 `<类型>: <中文说明>` 格式提交本地 Git commit。
- 遵循 TDD：先写能复现问题的失败测试，再写最小实现。

---

### Task 1: 后端菜单目录拆分

**Files:**
- Modify: `backend/src/main/java/com/gymmind/platform/bootstrap/PermissionCatalog.java`
- Test: `backend/src/test/java/com/gymmind/platform/bootstrap/SystemCatalogInitializerTest.java`

**Interfaces:**
- Produces menu code `menu:ai-config` and role-aware operation mapping consumed by the frontend catalog API and backend permission expansion.

- [ ] **Step 1: Write the failing tests**

在 `SystemCatalogInitializerTest` 中增加断言：菜单目录包含 `menu:ai-config`；健身房管理员默认菜单包含它；教练和会员默认菜单不包含它；`operationPermissionsFor(RoleCode.GYM_ADMIN, "menu:ai-config")` 包含 `agent:read`、`agent:write`、`agent:chat`；教练和会员对该菜单返回空集合；`menu:assistant` 不再包含 `agent:read` 或 `agent:write`。

- [ ] **Step 2: Run the focused test and verify RED**

Run from `backend`:

```powershell
./mvnw.cmd -Dtest=SystemCatalogInitializerTest test
```

Expected: the new assertions fail because the menu code is absent and Agent operations are still mapped to `menu:assistant`.

- [ ] **Step 3: Implement the minimal catalog change**

在 `MENU_ENTRIES` 中加入 `new MenuEntry("menu:ai-config", "AI configuration")`；从 `menu:assistant` 移除 `agent:read`、`agent:write`、`agent:chat`，新增 `menu:ai-config` 映射这三个操作；把 `menu:ai-config` 加入 `GYM_ADMIN` 和平台管理员的菜单范围，保持 `COACH`、`MEMBER` 范围不变。平台管理员仍通过现有全量逻辑拥有所有菜单。

- [ ] **Step 4: Run the focused test and verify GREEN**

```powershell
./mvnw.cmd -Dtest=SystemCatalogInitializerTest test
```

Expected: all assertions pass with exit code 0。

- [ ] **Step 5: Commit the backend catalog change**

```powershell
git add backend/src/main/java/com/gymmind/platform/bootstrap/PermissionCatalog.java backend/src/test/java/com/gymmind/platform/bootstrap/SystemCatalogInitializerTest.java
git commit -m "fix: 拆分AI配置菜单权限"
```

### Task 2: 前端菜单、路由与角色权限树

**Files:**
- Modify: `frontend/src/utils/menuPermissions.ts`
- Modify: `frontend/src/layouts/MainLayout.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/views/business/RolePermissionView.vue`
- Test: `frontend/tests/menuPermissions.test.mjs`

**Interfaces:**
- Consumes backend catalog code `menu:ai-config`.
- Produces `MENU_CODES.AI_CONFIG` with label `AI 配置` and path `/agents/manage`; all frontend visibility checks use this code.

- [ ] **Step 1: Write the failing frontend tests**

创建 `frontend/tests/menuPermissions.test.mjs`，测试 `FULL_MENU_CATALOG` 含有 `{ code: 'menu:ai-config', label: 'AI 配置', path: '/agents/manage' }`，且 `menuCodeForOperation('agent:write')` 返回 `menu:ai-config`。测试还应确认 `MENU_CODES.AI_CONFIG` 与该目录项一致。

- [ ] **Step 2: Run the focused test and verify RED**

```powershell
node --test frontend/tests/menuPermissions.test.mjs
```

Expected: test fails because `MENU_CODES.AI_CONFIG` and the independent catalog item do not exist。

- [ ] **Step 3: Implement the minimal frontend change**

在 `MENU_CODES` 增加 `AI_CONFIG: 'menu:ai-config'`，在智能分组的 `FULL_MENU_CATALOG` 加入 AI 配置条目；删除 `MainLayout.vue` 中把 AI 配置拼接为 `menu:assistant` 的临时逻辑；将 `/agents/manage` 的路由元数据改为 `MENU_CODES.AI_CONFIG`；在操作权限前缀映射中先匹配 `agent:` 到 `MENU_CODES.AI_CONFIG`；在 `RolePermissionView.vue` 的 `GYM_ADMIN` 边界集合中加入 `menu:ai-config`，不加入教练和会员集合。

- [ ] **Step 4: Run the focused frontend test and type/build checks**

```powershell
node --test frontend/tests/menuPermissions.test.mjs
npm --prefix frontend run build
```

Expected: the focused test passes and Vite build exits 0。

- [ ] **Step 5: Commit the frontend change**

```powershell
git add frontend/src/utils/menuPermissions.ts frontend/src/layouts/MainLayout.vue frontend/src/router/index.ts frontend/src/views/business/RolePermissionView.vue frontend/tests/menuPermissions.test.mjs
git commit -m "fix: 修正AI配置菜单显示范围"
```

### Task 3: 验证增量初始化和现有权限行为

**Files:**
- Test: `backend/src/test/java/com/gymmind/platform/bootstrap/SystemCatalogInitializerTest.java`
- Test: `frontend/tests/operationAccess.test.mjs`

**Interfaces:**
- Verifies that adding the catalog entry does not delete existing role links and that Agent operations require the new menu boundary.

- [ ] **Step 1: Add regression assertions**

在后端初始化测试中保留并扩展现有“不会调用 deleteByRoleId”的断言，确保新增菜单只通过 `link` 增量补齐；在前端操作权限测试中增加：拥有 `menu:assistant` 但没有 `menu:ai-config` 时不能使用 `agent:write`，同时拥有两者和 `agent:write` 时才允许健身房管理员使用。

- [ ] **Step 2: Run the complete focused regression set**

```powershell
./mvnw.cmd -Dtest=SystemCatalogInitializerTest,UserAdministrationServiceTest,TenantRolePermissionAdministrationTest test
node --test frontend/tests/menuPermissions.test.mjs frontend/tests/operationAccess.test.mjs
```

Expected: all selected backend and frontend tests pass with exit code 0。

- [ ] **Step 3: Inspect the final diff and commit test updates**

```powershell
git diff --check
git status --short
git add backend/src/test/java/com/gymmind/platform/bootstrap/SystemCatalogInitializerTest.java frontend/tests/operationAccess.test.mjs
git commit -m "test: 增加AI配置菜单权限回归测试"
```

### Task 4: Final verification

**Files:**
- No new files; inspect only the files changed by Tasks 1–3.

- [ ] **Step 1: Run backend and frontend verification commands**

```powershell
./mvnw.cmd -DskipTests compile
./mvnw.cmd -Dtest=SystemCatalogInitializerTest,UserAdministrationServiceTest,TenantRolePermissionAdministrationTest test
npm --prefix frontend run build
node --test frontend/tests/menuPermissions.test.mjs frontend/tests/operationAccess.test.mjs
```

- [ ] **Step 2: Confirm scope and clean task files**

```powershell
git diff --check
git status --short
git log -3 --oneline
```

确认本任务的提交只包含上述文件；若工作区仍有其他任务改动，保留并在结果中说明，不将其加入提交。

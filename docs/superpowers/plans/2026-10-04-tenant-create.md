# 租户创建流程 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让平台后台新建租户时由后端生成唯一租户编码，管理员邮箱和密码保持空白并由操作者填写。

**Architecture:** 平台后台创建请求移除 `tenantCode`，服务层生成 `gym-` 加 8 位随机小写字母数字编码并在仓储层检查冲突。公开注册接口继续使用用户主动填写的租户编码。两个平台前端入口共享不发送编码、邮箱密码初始为空的表单约束。

**Tech Stack:** Spring Boot 3.4, Spring Data JPA, JUnit 5, Vue 3, TypeScript, Element Plus, Vitest.

## Global Constraints

- 平台后台创建接口不接受客户端生成的租户编码。
- 公开注册接口继续要求用户填写租户编码。
- 管理员邮箱和密码不提供默认值，创建时仍为必填。
- 数据库唯一约束继续作为租户编码最终保护，不能执行删除或重建数据操作。
- 每个完成的代码、配置或测试修改必须提交本地 Git，提交信息使用 `fix:` 或 `test:` 中文说明。

---

### Task 1: 后端生成租户编码

**Files:**
- Modify: `backend/src/main/java/com/gymmind/tenancy/api/request/CreateTenantRequest.java`
- Modify: `backend/src/main/java/com/gymmind/tenancy/application/command/CreateTenantCommand.java`
- Modify: `backend/src/main/java/com/gymmind/tenancy/application/DefaultPlatformTenantService.java`
- Test: `backend/src/test/java/com/gymmind/tenancy/application/DefaultPlatformTenantServiceTest.java`

**Interfaces:**
- `CreateTenantCommand` no longer carries `tenantCode` for platform creation; it carries tenant name, administrator email, password, and display name.
- `DefaultPlatformTenantService.create` generates the code before `Tenant.create` and returns it through the existing `TenantView`.

- [ ] **Step 1: Add failing service tests**

Add tests that submit a platform create command without a code and assert that the saved tenant code matches `gym-[a-z0-9]{8}`, and that a generated code already returned by `existsByCode` causes another generated value to be tried. Add a test that blank administrator email and password are rejected by validation/service rules.

- [ ] **Step 2: Run the focused backend tests**

Run `mvn -q "-Dtest=DefaultPlatformTenantServiceTest" test` from `backend`.

Expected: the new tests fail because the command still requires `tenantCode` and the service still validates it.

- [ ] **Step 3: Remove platform request code and implement generation**

Change `CreateTenantRequest` and `CreateTenantCommand` to omit `tenantCode` for this endpoint. In `DefaultPlatformTenantService`, generate with `UUID.randomUUID().toString().replace("-", "").substring(0, 8)` prefixed by `gym-`, check `tenantRepository.existsByCode`, retry up to five times, and throw `BusinessException(ErrorCode.CONFLICT)` after five collisions. Keep administrator email/password required and preserve the existing username conflict checks.

- [ ] **Step 4: Run the focused tests again**

Run `mvn -q "-Dtest=DefaultPlatformTenantServiceTest" test` from `backend`.

Expected: PASS, including code format, collision retry, and required administrator credentials.

- [ ] **Step 5: Commit the backend change**

Run `git add backend/src/main/java/com/gymmind/tenancy/api/request/CreateTenantRequest.java backend/src/main/java/com/gymmind/tenancy/application/command/CreateTenantCommand.java backend/src/main/java/com/gymmind/tenancy/application/DefaultPlatformTenantService.java backend/src/test/java/com/gymmind/tenancy/application/DefaultPlatformTenantServiceTest.java` and commit with `fix: 后端生成租户编码`.

### Task 2: 平台前端表单

**Files:**
- Modify: `frontend/src/views/business/TenantSettingsView.vue`
- Modify: `frontend/src/views/business/PlatformView.vue`
- Test: `frontend/tests/tenantCreateForm.test.mjs`

**Interfaces:**
- Both platform creation entry points send `{ tenantName, adminEmail, adminPassword, adminDisplayName }` and never send `tenantCode`.
- `adminEmail` and `adminPassword` initialize to empty strings and are required before submit.

- [ ] **Step 1: Add failing form tests**

Add tests that load the form helpers or extracted payload builder and assert the payload has no `tenantCode`, and that empty email/password are rejected while a fully populated form is accepted.

- [ ] **Step 2: Run the focused frontend test**

Run `npm test -- --run frontend/tests/tenantCreateForm.test.mjs` from the repository root if the frontend test script is available.

Expected: FAIL because both current views still render and validate `tenantCode`, and one path treats email as optional.

- [ ] **Step 3: Remove the code field and clear credential defaults**

Remove the create-form tenant code input and state from `TenantSettingsView.vue`; remove the `tenantCode` field definition from `PlatformView.vue`'s `tenantFields`. Require trimmed `tenantName`, `adminEmail`, `adminPassword`, and `adminDisplayName` in both submit paths. Keep the existing tenant list `code` column so the generated value remains visible after creation.

- [ ] **Step 4: Run frontend tests and type/build checks**

Run the focused test, then run the existing frontend build command from `frontend/package.json`.

Expected: PASS with no TypeScript errors and no request payload containing `tenantCode`.

- [ ] **Step 5: Commit the frontend change**

Run `git add frontend/src/views/business/TenantSettingsView.vue frontend/src/views/business/PlatformView.vue frontend/tests/tenantCreateForm.test.mjs` and commit with `fix: 调整租户创建表单`.

### Task 3: Full verification

**Files:**
- Verify: `backend/src/main/java/com/gymmind/tenancy/application/DefaultPlatformTenantService.java`
- Verify: `frontend/src/views/business/TenantSettingsView.vue`
- Verify: `frontend/src/views/business/PlatformView.vue`

- [ ] **Step 1: Run backend compile and focused tests**

Run `mvn -q -DskipTests compile` and the focused tenancy service tests from `backend`.

- [ ] **Step 2: Run frontend tests/build**

Run the frontend test and build commands recorded in `frontend/package.json`.

- [ ] **Step 3: Inspect the final diff**

Run `git diff --check` and `git status --short`; ensure only this task's files are staged for the final verification commit and retain unrelated worktree changes unstaged.

- [ ] **Step 4: Commit verification-only adjustments if needed**

If verification requires a code or test adjustment, commit it with a `fix:` or `test:` message; otherwise leave the two implementation commits as the final history.

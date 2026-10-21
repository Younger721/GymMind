# 每家门店限制一个 Agent Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将每个租户可用的 Agent 数量固定为 1，并让门店及平台配额页面一致显示。

**Architecture:** 后端以常量 1 作为有效上限，不再依赖历史 `max_agents` 值；租户配额 DTO 统一返回上限 1，平台更新 API 拒绝其它值。门店页继续显示配额 DTO，平台页把 Agent 上限字段固定成只读 1。

**Tech Stack:** Java 17、Spring Boot 3.4、JUnit 5、Mockito、Vue 3、TypeScript、Element Plus、Vite。

## Global Constraints

- 每个门店（租户）最多配置 1 条 Agent 记录，启用和停用记录都占用名额。
- 历史 `max_agents` 大于 1 时，应用有效上限和返回页面的上限仍为 1。
- 不删除、合并或改写已有 Agent 数据；现有提示词编辑保持可用。
- 平台管理员不能将 Agent 配额更新为非 1；AI 调用、Token、知识文档配额继续可配置。
- 不新增数据库唯一索引或迁移；创建接口保持当前关闭状态。
- 共享工作区内已有其它任务的改动必须保留，提交只包含本计划相关文件。

---

### Task 1: 固定后端租户 Agent 上限

**Files:**
- Modify: `backend/src/main/java/com/gymmind/tenancy/domain/model/TenantQuota.java`
- Modify: `backend/src/main/java/com/gymmind/tenancy/application/TenantQuotaView.java`
- Modify: `backend/src/main/java/com/gymmind/tenancy/application/DefaultTenantQuotaService.java`
- Test: `backend/src/test/java/com/gymmind/tenancy/application/DefaultTenantQuotaServiceTest.java`

**Interfaces:**
- `TenantQuota.defaults(Long tenantId)` creates new quotas with `maxAgents = 1`.
- `TenantQuotaView.of(TenantQuota quota, long usedAgents, long usedAiCalls, long usedAiTokens, long usedDocs)` reports `maxAgents = 1` and preserves the supplied usage count.
- `DefaultTenantQuotaService.ensureAgentCreationAllowed(Long tenantId)` checks `agents.countByTenantId(tenantId)` against the fixed limit 1 after verifying the AI module is enabled.
- `DefaultTenantQuotaService.updateForPlatform(CurrentActor actor, Long tenantId, UpdateTenantQuotaCommand command)` accepts `command.maxAgents() == 1` and rejects any other value before mutating or saving the quota.

- [ ] **Step 1: Add failing quota tests**

In `DefaultTenantQuotaServiceTest`, add tests that verify a legacy quota set to 5 is reported as 1 while `usedAgents` stays unchanged; one existing Agent blocks creation even when the persisted quota is 5; a platform quota update with `maxAgents = 2` throws `BusinessException` without saving; and a platform update with `maxAgents = 1` still saves changed AI call, Token, and document quotas. Also assert `TenantQuota.defaults(7L).getMaxAgents()` is 1.

Build fixtures with the existing `service(TenantQuota quota, JdbcTemplate jdbc)` helper. Mock `TenantAgentRepository.countByTenantId(7L)` as `1L` for the cap test. For platform updates, create an actor with role `PLATFORM_ADMIN` and permission `platform:quota:write`, mock `TenantQuotaRepository.findByTenantId(7L)` to return the quota, and verify the repository does not save after rejection. For the accepted update test, mock `save` to return its argument, `JdbcTemplate.queryForObject` to return `0L`, and `KnowledgeDocumentRepository.findByTenantId(7L)` to return an empty list; assert the changed non-Agent quota values are present in the returned view.

- [ ] **Step 2: Run the quota tests and confirm the new assertions fail**

Run from `backend/` with JDK 17:

```powershell
$env:JAVA_HOME='D:\java\jdk-17.0.14'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn '-Dtest=DefaultTenantQuotaServiceTest' test
```

Expected: the new assertions fail because current defaults, DTO output, and service checks still use the configured value 5.

- [ ] **Step 3: Implement the one-Agent quota behavior**

Set the `TenantQuota.maxAgents` default to 1. In `TenantQuotaView.of`, return 1 for `maxAgents` while retaining the supplied `usedAgents`. Add a named constant `MAX_AGENTS_PER_TENANT = 1` to `DefaultTenantQuotaService`; compare `countByTenantId` to that constant in `ensureAgentCreationAllowed`. Before calling `quota.update(...)` in `updateForPlatform`, reject `command.maxAgents() != 1` with `BusinessException(ErrorCode.VALIDATION_FAILED)`.

- [ ] **Step 4: Run quota and tenant provisioning tests**

Run:

```powershell
mvn '-Dtest=DefaultTenantQuotaServiceTest' test
```

Expected: all selected tests pass; other quota values remain updateable. `AgentProvisioningContributor` remains unchanged and continues to skip seeding when `countByTenantId` is greater than zero.

- [ ] **Step 5: Commit the backend task**

Stage only `TenantQuota.java`, `TenantQuotaView.java`, `DefaultTenantQuotaService.java`, and `DefaultTenantQuotaServiceTest.java`, preserving existing unrelated changes in the modified service file. Commit with:

```text
fix: 固定每家门店 Agent 数量上限为一
```

### Task 2: Display the fixed limit in platform quota settings

**Files:**
- Modify: `frontend/src/views/business/PlatformView.vue`
- Verify: `frontend/src/views/AgentManageView.vue` (it already renders `usedAgents / maxAgents` from the API and should need no change)

**Interfaces:**
- The platform form initializes and loads `quotaForm.maxAgents` as `'1'`.
- The “最大 Agent” form control displays `1` and is disabled.
- `saveQuota()` always sends `maxAgents: 1`, independent of stale local form state.
- The store Agent page continues to use `TenantQuotaView.maxAgents`; the backend DTO now makes that value 1.

- [ ] **Step 1: Fix the platform quota form**

Set `quotaForm.maxAgents` to `'1'`. Replace the editable Agent input with a disabled input that displays the literal value `1`. In `loadQuota()`, set `quotaForm.maxAgents = '1'` instead of copying `data.maxAgents`. In `saveQuota()`, send the numeric constant `maxAgents: 1` while leaving the other quota fields sourced from `quotaForm`.

- [ ] **Step 2: Build the frontend**

Run from `frontend/`:

```powershell
npx vite build
```

Expected: Vite completes successfully and the PlatformView changes compile. The door-level Agent page should show `usedAgents / 1` from the updated API response.

- [ ] **Step 3: Review the diff and commit the platform UI task**

Inspect the `PlatformView.vue` diff and stage only the Agent quota changes, keeping its pre-existing tenant-context and layout edits unstaged. Commit with:

```text
fix: 固定平台 Agent 配额展示为一
```

### Task 3: Final verification

**Files:**
- Verify: the four backend files from Task 1 and `frontend/src/views/business/PlatformView.vue` from Task 2.

- [ ] **Step 1: Run the quota unit tests and frontend build again after the final edits**

Run the JDK 17 Maven test command from Task 1 and `npx vite build` from `frontend/`. Expected: both pass.

- [ ] **Step 2: Check commits and the shared workspace**

Run `git diff --check`, `git status --short`, and `git log -2 --oneline`. Confirm both feature commits contain only their listed files. Leave all unrelated pre-existing modified and untracked files untouched and report that they remain in the workspace.

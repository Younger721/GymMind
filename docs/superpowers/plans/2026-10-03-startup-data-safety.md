# Startup Data Safety Implementation Plan

> **For agentic workers:** Execute the tasks inline with test-first verification and commit only the files belonging to this fix.

**Goal:** Prevent application restarts from deleting custom role permissions, reprocessing already-published knowledge documents, resetting existing development passwords, or losing background jobs by default in Docker.

**Architecture:** Startup catalog initialization becomes additive and idempotent. Knowledge recovery only restores interrupted documents, while Docker uses the existing MySQL-backed queue. Development password synchronization becomes explicit opt-in. Existing external index and object-store integrations remain unchanged.

**Tech Stack:** Spring Boot, Java, JUnit 5, Mockito, MySQL, Elasticsearch, Milvus, MinIO.

## Global Constraints

- Do not delete or reset existing business data.
- Preserve unrelated uncommitted work in the shared checkout.
- Follow repository commit format: `fix: 中文说明`.

---

### Task 1: Protect startup role permissions

**Files:**
- Modify: `backend/src/main/java/com/gymmind/platform/bootstrap/SystemCatalogInitializer.java`
- Test: `backend/src/test/java/com/gymmind/platform/bootstrap/SystemCatalogInitializerTest.java`

- [ ] Add a regression test proving configured role links are preserved and `deleteByRoleId` is never called.
- [ ] Run the focused test and observe the expected failure.
- [ ] Remove destructive role-link deletion and retain additive catalog expansion.
- [ ] Run the focused test and the existing catalog tests.

### Task 2: Make knowledge recovery restart-safe

**Files:**
- Modify: `backend/src/main/java/com/gymmind/knowledge/application/KnowledgeIndexRecoveryRunner.java`
- Test: `backend/src/test/java/com/gymmind/knowledge/application/KnowledgeIndexRecoveryRunnerTest.java`

- [ ] Add a regression test proving published READY documents are not requeued on every restart.
- [ ] Run the focused test and observe the expected failure.
- [ ] Recover only `UPLOADING` and `PARSING` documents.
- [ ] Run focused knowledge tests.

### Task 3: Make Docker background jobs persistent and password reset opt-in

**Files:**
- Modify: `backend/src/main/resources/application-docker.yml`
- Modify: `backend/src/main/java/com/gymmind/platform/bootstrap/DevelopmentPasswordInitializer.java`
- Test: `backend/src/test/java/com/gymmind/platform/bootstrap/DevelopmentPasswordInitializerTest.java`

- [ ] Add a test proving an existing demo account password is unchanged unless the explicit reset flag is enabled.
- [ ] Run the focused test and observe the expected failure.
- [ ] Add `gymmind.job-queue: ${GYMMIND_JOB_QUEUE:mysql}` to Docker configuration.
- [ ] Add `gymmind.dev.reset-demo-password: ${GYMMIND_RESET_DEMO_PASSWORD:false}` and gate password reset on it.
- [ ] Run focused bootstrap tests and compile the backend.

### Task 4: Verify, review, and commit

- [ ] Review the diff and confirm no unrelated files are staged.
- [ ] Run focused tests and the backend Maven test/compile command.
- [ ] Commit with `fix: 防止项目重启刷新业务状态`.
- [ ] Confirm the worktree status and report any unrelated remaining changes.

# GymMind 动作库与教学视频 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 GymMind 租户提供隔离的动作库与教学视频元数据管理。

**Architecture:** `exercise` 模块采用现有的 api/application/domain/infrastructure 分层。动作与视频均继承 `TenantScopedEntity`，所有读取通过 `tenantId + id` 的仓储方法完成；视频来源分为第三方 URL 和内部 MinIO 对象键，第三方内容不下载或代理。

**Tech Stack:** Java 17、Spring Boot 3.4.12、Spring Data JPA、Jakarta Validation、JUnit 5、Mockito。

## Global Constraints

- 产品与 API 前缀固定为 `GymMind` 和 `/api/v1`。
- 每个租户是一家独立健身房；动作、视频和对象键严禁跨租户互通。
- 所有业务写入通过 `AuditRecorder` 记录租户审计。
- 只用显式 `git add` 暂存本任务文件；不自动推送。
- 每个独立小功能使用中文 Conventional Commit。
- 第三方视频仅保存合法 HTTP/HTTPS 链接和许可缩略图，不下载或重新托管。

---

### Task 1: 租户动作库

**Files:**
- Create: `backend/src/main/java/com/gymmind/exercise/{api,application,domain/model,domain/repository,infrastructure/persistence}/Exercise*.java`
- Create: `backend/src/test/java/com/gymmind/exercise/application/ExerciseServiceTest.java`
- Modify: `backend/src/main/java/com/gymmind/platform/bootstrap/PermissionCatalog.java`
- Modify: `backend/src/main/java/com/gymmind/platform/bootstrap/SystemCatalogInitializer.java`
- Modify: `backend/src/test/java/com/gymmind/platform/bootstrap/SystemCatalogInitializerTest.java`

**Interfaces:**
- Produces: `ExerciseService.create(CurrentActor, CreateExerciseCommand)`, `find(CurrentActor, Long)`, and `update(CurrentActor, Long, ExerciseUpdateCommand)`.
- Produces: `ExerciseRepository.findByTenantIdAndId(Long, Long)` and tenant-local name lookup.

- [ ] **Step 1: Write failing application tests**

```java
assertThat(service.create(admin(11L), command).tenantId()).isEqualTo(11L);
assertThatThrownBy(() -> service.find(admin(12L), exerciseId))
        .isInstanceOf(BusinessException.class);
```

- [ ] **Step 2: Run the focused test and confirm the missing service fails**

Run: `backend\mvnw.cmd -q -f backend/pom.xml -Dtest=com.gymmind.exercise.application.ExerciseServiceTest test`

- [ ] **Step 3: Implement domain entity, JPA repository, service and protected controller**

```java
@Entity
@Table(name = "exercise", uniqueConstraints = @UniqueConstraint(
        name = "uk_exercise_tenant_name", columnNames = {"tenant_id", "name"}))
public class Exercise extends TenantScopedEntity {
    // name, category, targetMuscle, difficulty, equipment and safety fields
}
```

- [ ] **Step 4: Add and link `exercise:read` / `exercise:write` for `GYM_ADMIN`**

- [ ] **Step 5: Run focused tests and full unit suite**

Run: `backend\mvnw.cmd -q -f backend/pom.xml test`

- [ ] **Step 6: Commit the feature**

```powershell
git add -- backend/src/main/java/com/gymmind/exercise backend/src/test/java/com/gymmind/exercise backend/src/main/java/com/gymmind/platform/bootstrap/PermissionCatalog.java backend/src/main/java/com/gymmind/platform/bootstrap/SystemCatalogInitializer.java backend/src/test/java/com/gymmind/platform/bootstrap/SystemCatalogInitializerTest.java
git commit -m "feat: 实现租户动作库"
```

### Task 2: 动作教学视频元数据

**Files:**
- Create: `backend/src/main/java/com/gymmind/exercise/{api,application,domain/model,domain/repository,infrastructure/persistence}/ExerciseVideo*.java`
- Create: `backend/src/test/java/com/gymmind/exercise/application/ExerciseVideoServiceTest.java`
- Modify: Task 1 动作仓储和权限目录文件

**Interfaces:**
- Consumes: `ExerciseRepository.findByTenantIdAndId(Long, Long)`。
- Produces: `ExerciseVideoService.create/find/delete` and `/api/v1/videos`.

- [ ] **Step 1: Write failing tests for URL scheme, tenant object-key prefix and action ownership**

```java
assertThatThrownBy(() -> service.create(admin(11L), thirdParty("ftp://host/video")))
        .isInstanceOf(BusinessException.class);
assertThatThrownBy(() -> service.create(admin(11L), internal("tenant/12/exercise-videos/a.mp4")))
        .isInstanceOf(BusinessException.class);
```

- [ ] **Step 2: Run the focused test and confirm it fails**

Run: `backend\mvnw.cmd -q -f backend/pom.xml -Dtest=com.gymmind.exercise.application.ExerciseVideoServiceTest test`

- [ ] **Step 3: Implement source-type validation and tenant-scoped JPA persistence**

```java
if (sourceType == INTERNAL && !objectKey.startsWith("tenant/" + tenantId + "/exercise-videos/")) {
    throw new BusinessException(ErrorCode.VALIDATION_FAILED);
}
```

- [ ] **Step 4: Add and link `video:read` / `video:write` for `GYM_ADMIN`**

- [ ] **Step 5: Run focused tests, full unit tests and package verification**

Run: `backend\mvnw.cmd -q -f backend/pom.xml test`

Run: `backend\mvnw.cmd -q -f backend/pom.xml -DskipTests package`

- [ ] **Step 6: Commit the feature**

```powershell
git add -- backend/src/main/java/com/gymmind/exercise backend/src/test/java/com/gymmind/exercise backend/src/main/java/com/gymmind/platform/bootstrap/PermissionCatalog.java backend/src/main/java/com/gymmind/platform/bootstrap/SystemCatalogInitializer.java backend/src/test/java/com/gymmind/platform/bootstrap/SystemCatalogInitializerTest.java
git commit -m "feat: 实现动作教学视频管理"
```

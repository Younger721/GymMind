# 课程与权限缺陷修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 Milvus 知识库字段、取消报名复报、前端角色操作显示、课程时间转换和非法日期筛选的五个已确认缺陷。

**Architecture:** 保留现有租户隔离、业务角色边界和菜单作为页面入口的设计。后端只修复数据状态、输入解析和索引 schema；前端把菜单可见性与角色范围组合为控件显示条件，并在 API 边界把浏览器本地时间转换成 UTC。

**Tech Stack:** Java 17, Spring Boot, JUnit 5, Mockito, Vue 3, TypeScript, Element Plus, Vite, Milvus REST API。

## Global Constraints

- 不新增细粒度后端权限码拦截；后端继续只校验登录、租户和业务角色边界。
- 会员报名必须继续使用当前租户内有效套餐、容量和重复报名校验。
- 取消报名后只能在课程开始前重新报名；已开始、已取消或已结束课程不得恢复报名。
- 不在应用启动时自动删除或重建已有 Milvus 集合，避免无备份时丢失知识库数据。
- 保持现有 API 路径和课程、报名响应字段兼容。

---

### Task 1: 修复 Milvus 知识库 collection schema

**Files:**
- Modify: `backend/src/main/java/com/gymmind/knowledge/infrastructure/search/MilvusVectorIndex.java:29-40`
- Modify: `backend/src/test/java/com/gymmind/knowledge/infrastructure/search/MilvusVectorIndexTest.java:20-65`
- Create: `docs/knowledge/milvus-schema-migration.md`

**Interfaces:**
- `collectionBody(String)` 必须声明 `documentId` 为 `Int64` 非主键字段。
- `insertBody`、`OUTPUT_FIELDS` 和 `parseSearchResponse` 继续使用同一个 `documentId` 字段。

- [ ] **Step 1: Write the failing test**

在 `MilvusVectorIndexTest` 中断言 schema 的字段列表包含 `documentId`，插入行携带文档 ID，搜索输出字段包含 `documentId`，并让解析响应将 `documentId` 映射回 `IndexedChunk`。

- [ ] **Step 2: Run the focused test to verify the current defect**

Run: `./mvnw.cmd -q "-Dtest=MilvusVectorIndexTest" test`

Expected: 当前版本在 schema 或 output fields 断言处失败；失败确认后再改实现。

- [ ] **Step 3: Add the schema field**

在 `collectionBody` 的 fields 中加入：

```java
field("documentId", "Int64", false, Map.of()),
```

保持 `enableDynamicField` 为 `false`，让 schema 与 `insertBody` 的字段集合一致。

- [ ] **Step 4: Document existing collection migration**

新增迁移说明，明确旧的 `gymmind_knowledge` 集合不能仅靠 `has` 检查自动补字段。部署时创建带 `documentId` 的新集合，设置 `GYMMIND_MILVUS_COLLECTION` 指向新集合，然后重新执行知识库全量入库；确认新集合可检索后再按备份策略处理旧集合。文档不得包含自动删除数据的启动逻辑。

- [ ] **Step 5: Run the focused test**

Run: `./mvnw.cmd -q "-Dtest=MilvusVectorIndexTest" test`

Expected: 该测试全部通过。

---

### Task 2: 支持取消报名后重新报名

**Files:**
- Modify: `backend/src/main/java/com/gymmind/booking/domain/model/Booking.java:45-57`
- Modify: `backend/src/main/java/com/gymmind/booking/application/DefaultBookingService.java:81-107`
- Modify: `backend/src/test/java/com/gymmind/booking/application/BookingServiceTest.java:102-171`

**Interfaces:**
- 新增 `Booking.reactivate()`，只允许 `CANCELLED -> CONFIRMED`。
- `DefaultBookingService.book` 对 `CONFIRMED`/`PENDING` 保持幂等返回；对 `CANCELLED` 重新校验课程、扣留套餐次数、占用课程名额，再恢复原记录。

- [ ] **Step 1: Write the failing regression test**

构造已取消报名、仍可报名的课程和有效套餐 mock，调用 `book`，断言返回状态为 `CONFIRMED`，调用一次 `reserve`，并保存原报名记录；同时覆盖已满课程和无有效套餐时仍拒绝恢复。

- [ ] **Step 2: Run the focused booking tests**

Run: `./mvnw.cmd -q "-Dtest=BookingServiceTest" test`

Expected: 新增的取消后重新报名测试失败，现有重复报名和取消测试保持可运行。

- [ ] **Step 3: Implement the state transition**

在 `Booking` 中增加只接受 `CANCELLED` 状态的 `reactivate()`。在 `book` 中把现有记录分支改为：

1. 活跃报名直接返回；
2. 取消报名先查询课程并执行 `ensureBookable`；
3. 调用 `entitlements.reserve` 和 `reserveCourseCapacity`；
4. 容量失败时释放套餐次数；
5. 调用 `reactivate()` 并保存原记录。

不要创建第二条记录，以继续满足现有租户、会员、课程唯一约束。

- [ ] **Step 4: Run booking and course tests**

Run: `./mvnw.cmd -q "-Dtest=BookingServiceTest,CourseServiceTest" test`

Expected: 所有测试通过，取消后重新报名不会重复占用名额或套餐次数。

---

### Task 3: 恢复前端角色范围判断

**Files:**
- Modify: `frontend/src/utils/operationAccess.ts:8-26`
- Modify: `frontend/tests/operationAccess.test.mjs:5-15`
- Modify: `frontend/src/views/business/TrainingView.vue:63-73` (仅在编译暴露类型问题时调整)

**Interfaces:**
- `canUseOperation` 返回 `菜单权限存在 && (allowedRoles 为空或当前角色命中 allowedRoles)`。
- `canUseTenantOperation` 保持平台管理员无租户上下文时不可使用租户操作的现有行为，并复用修正后的角色判断。

- [ ] **Step 1: Fix the regression test harness if needed**

让 `operationAccess.test.mjs` 使用可被 Node 直接解析的菜单映射导入方式，保留三组矩阵：缺菜单权限为 false、角色不匹配为 false、菜单和角色均匹配为 true。

- [ ] **Step 2: Run the frontend role matrix test**

Run: `node --test tests/operationAccess.test.mjs`

Expected: 当前代码的角色不匹配用例失败；该失败确认 `allowedRoles` 被忽略。

- [ ] **Step 3: Implement the role check**

在 `canUseOperation` 中使用传入的 `roles` 和 `allowedRoles`，不恢复细粒度权限码判断，也不把平台管理员强行加入租户操作角色列表。

- [ ] **Step 4: Build the frontend**

Run: `npm run build`

Expected: 类型检查和 Vite 构建通过；训练页中仅 `GYM_ADMIN` 的食物库等控件不再向教练或会员显示。

---

### Task 4: 修复课程时间的本地时间与 UTC 转换

**Files:**
- Create: `frontend/src/utils/courseDateTime.mjs`
- Modify: `frontend/src/views/business/CourseCenterView.vue:63-71,163-170`
- Create: `frontend/tests/courseDateTime.test.mjs`

**Interfaces:**
- `toUtcIso(localDateTime: string): string`：把日期选择器产生的本地 `YYYY-MM-DDTHH:mm:ss` 转成 UTC ISO 字符串。
- `toLocalDateTime(iso: string): string`：把后端 UTC ISO 字符串转成日期选择器需要的本地格式。

- [ ] **Step 1: Write timezone regression tests**

在 `courseDateTime.test.mjs` 中固定 `TZ=Asia/Shanghai` 运行，断言本地 `2026-09-29T09:00:00` 转成 `2026-09-29T01:00:00.000Z`，再转回仍为 `09:00:00`。

- [ ] **Step 2: Run the timezone test**

Run: `$env:TZ='Asia/Shanghai'; node --test tests/courseDateTime.test.mjs`

Expected: 当前尚未实现 helper，测试失败。

- [ ] **Step 3: Add explicit conversion helpers**

将日期选择器的 `value-format` 改为 `YYYY-MM-DDTHH:mm:ss`，保存时对 `startsAt`/`endsAt` 调用 `toUtcIso`，编辑课程时对后端值调用 `toLocalDateTime` 后再填入表单。禁止继续给本地字符串直接追加字面量 `Z`。

- [ ] **Step 4: Run frontend verification**

Run: `$env:TZ='Asia/Shanghai'; node --test tests/courseDateTime.test.mjs; npm run build`

Expected: 时间转换测试和前端构建均通过。

---

### Task 5: 将非法课程日期筛选转换为 400

**Files:**
- Modify: `backend/src/main/java/com/gymmind/course/api/CourseController.java:22-25,85-93`
- Create or modify: `backend/src/test/java/com/gymmind/course/api/CourseControllerTest.java`

**Interfaces:**
- `parseDate` 对 ISO instant、ISO local date 继续兼容；两种解析都失败时抛出 `BusinessException(ErrorCode.VALIDATION_FAILED)`。
- `GET /api/v1/courses?from=bad-date` 和 `to=bad-date` 返回统一的 400 错误响应。

- [ ] **Step 1: Write controller regression tests**

使用现有 Spring MVC 测试风格，为非法 `from` 和非法 `to` 各增加一个请求断言，验证状态为 400，且不会调用 `CourseService.list`。

- [ ] **Step 2: Run the controller tests**

Run: `./mvnw.cmd -q "-Dtest=CourseControllerTest" test`

Expected: 当前实现返回 500 或抛出未处理日期异常，新增断言失败。

- [ ] **Step 3: Normalize parse failures**

捕获 `DateTimeParseException`，转成 `BusinessException(ErrorCode.VALIDATION_FAILED)`；保留日期范围结束日使用次日 UTC 零点的现有语义。

- [ ] **Step 4: Run course API tests**

Run: `./mvnw.cmd -q "-Dtest=CourseControllerTest,CourseServiceTest" test`

Expected: 非法日期为 400，合法 instant 和日期筛选行为不变。

---

### Task 6: 集成验证

**Files:**
- No source changes.

- [ ] **Step 1: Run focused backend regression tests**

Run: `./mvnw.cmd -q "-Dtest=MilvusVectorIndexTest,BookingServiceTest,CourseControllerTest,CourseServiceTest,SystemCatalogInitializerTest" test`

Expected: 新增和受影响测试全部通过。

- [ ] **Step 2: Run frontend verification**

Run: `npm run build`

Expected: `vue-tsc` 和 Vite 构建退出码为 0。

- [ ] **Step 3: Run the full backend suite and classify remaining failures**

Run: `./mvnw.cmd test`

Expected: 五个缺陷对应的测试不再失败；若仍有旧的细粒度权限断言失败，单独记录为测试契约更新项，不把它们混入本次业务缺陷修复。

- [ ] **Step 4: Manual acceptance check**

在 `http://localhost:5173/courses` 验证：本地 09:00 创建课程后刷新仍显示 09:00；会员取消后可以再次报名；教练和会员不会看到不属于其角色的训练操作控件。启用 Milvus 的环境额外验证新集合能完成一次建表、入库和查询。

## Self-review

- 五个用户点名的缺陷均有独立任务和回归验证。
- 未引入新的后端细粒度权限拦截，符合当前菜单权限设计。
- Milvus 旧集合迁移被单独写明，避免仅修改 Java schema 却让线上旧集合继续失败。
- 课程时间转换覆盖创建和编辑两个方向，避免只修创建仍在编辑时错位。


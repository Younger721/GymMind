# 角色与会员关系实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 支持单用户单角色、会员跨多个健身房，以及会员发起教练申请、教练确认的多对多关系。

**Architecture:** 保留现有租户隔离和角色体系。将会员资料与健身房归属拆分为 `member_profile` 与 `member_tenant`，将现有直接分配模型改为带状态的 `coach_member_relation`。应用服务负责校验同租户归属和角色权限，控制器提供会员申请、教练接受/拒绝、取消和结束关系的接口。

**Tech Stack:** Java 17, Spring Boot 3.4, Spring Data JPA, MySQL 8, JUnit 5, Mockito.

## Global Constraints

- 每个用户只能拥有一个角色：`PLATFORM_ADMIN`、`GYM_ADMIN`、`COACH` 或 `MEMBER`。
- 平台管理员使用平台上下文；普通角色必须使用租户上下文。
- 会员可以加入多个健身房；教练只能属于一个健身房。
- 教练与会员的关系必须带 `tenant_id`，且会员和教练必须属于同一个租户。
- 会员发起关系申请，教练只能接受或拒绝，不能主动发起绑定。
- 所有跨租户读取和写入必须被拒绝。

## 文件地图

- `backend/src/main/java/com/gymmind/member/domain/model/Member.java`: 全局会员资料实体，移除直接租户归属。
- `backend/src/main/java/com/gymmind/member/domain/model/MemberTenant.java`: 会员加入健身房的关系实体。
- `backend/src/main/java/com/gymmind/member/domain/repository/MemberTenantRepository.java` 及其 Spring Data 实现：租户归属查询。
- `backend/src/main/java/com/gymmind/coach/domain/model/CoachAssignment.java`: 改为状态化的会员教练关系实体，或重命名为 `CoachMemberRelation` 并同步引用。
- `backend/src/main/java/com/gymmind/coach/application/CoachAssignmentService.java` 与 `DefaultCoachAssignmentService.java`: 替换管理员直接 assign 为申请、确认、拒绝、取消、结束流程。
- `backend/src/main/java/com/gymmind/coach/api/*`: 暴露关系流程接口并返回状态。
- `backend/src/main/java/com/gymmind/platform/bootstrap/SystemCatalogInitializer.java`: 补齐会员申请和教练确认权限。
- `backend/src/main/resources/db/migration/*` 或项目实际数据库迁移位置：迁移会员租户字段并创建关系表；若项目继续使用 Hibernate update，则补充等价的数据库文档和迁移脚本。
- `backend/src/test/java/com/gymmind/member/*`、`backend/src/test/java/com/gymmind/coach/*`: 单元、应用和集成测试。

### Task 1: 建立跨健身房会员归属模型

**Files:**
- Create: `backend/src/main/java/com/gymmind/member/domain/model/MemberTenant.java`
- Create/modify: `backend/src/main/java/com/gymmind/member/domain/repository/MemberTenantRepository.java` and its infrastructure implementation
- Modify: `backend/src/main/java/com/gymmind/member/domain/model/Member.java` and member services/repositories
- Test: `backend/src/test/java/com/gymmind/member/domain/model/MemberTenantTest.java`, `backend/src/test/java/com/gymmind/member/application/MemberServiceTest.java`

**Interfaces:** `MemberTenant.create(memberId, tenantId)`; repository methods `findByTenantIdAndMemberId`, `existsByTenantIdAndMemberId`, and `findAllByMemberId`. Existing member CRUD must accept a member identity while tenant-specific operations resolve through `MemberTenant`.

- [ ] 写测试：验证同一会员可创建两个不同租户归属，同一租户不能重复；验证无归属时租户业务访问失败。
- [ ] 运行 `./mvnw -q -Dtest=MemberTenantTest,MemberServiceTest test`，确认新测试先失败。
- [ ] 实现 `MemberTenant`、唯一约束 `(member_id, tenant_id)` 和租户归属查询；将会员状态等租户相关字段移到关系实体。
- [ ] 更新会员服务的创建、查询、修改、删除逻辑，所有租户操作先验证 `MemberTenant`。
- [ ] 运行同一命令确认通过，再运行 `./mvnw -q -DskipTests compile`。
- [ ] 提交：`git add backend/src/main/java backend/src/test/java && git commit -m "feat: support members across tenants"`。

### Task 2: 将教练关系改为状态化申请流程

**Files:**
- Modify: `backend/src/main/java/com/gymmind/coach/domain/model/CoachAssignment.java` or create `CoachMemberRelation.java`
- Modify: coach repository interfaces and Spring Data repository
- Modify: `backend/src/main/java/com/gymmind/coach/application/CoachAssignmentService.java` and `DefaultCoachAssignmentService.java`
- Test: `backend/src/test/java/com/gymmind/coach/application/CoachAssignmentServiceTest.java` and domain tests

**Interfaces:** `request(CurrentActor actor, Long coachId)`, `accept(CurrentActor actor, Long relationId)`, `reject(CurrentActor actor, Long relationId)`, `cancel(CurrentActor actor, Long relationId)`, `end(CurrentActor actor, Long relationId)`, and `canAccess(CurrentActor actor, Long memberId)`. Relation statuses are `PENDING`, `ACTIVE`, `REJECTED`, `ENDED`.

- [ ] 写失败测试：会员只能为自己发起申请；教练不能主动申请；教练只能处理发给自己的申请；跨租户申请被拒绝；重复待处理或有效关系不重复创建。
- [ ] 运行 `./mvnw -q -Dtest=CoachAssignmentServiceTest test` 确认失败。
- [ ] 增加状态枚举、申请人和时间字段；实现会员归属、教练归属和当前用户身份校验。
- [ ] 实现接受、拒绝、取消、结束的合法状态转换，并保留审计事件。
- [ ] 修改 `canAccess` 只认当前租户下 `ACTIVE` 关系；管理员继续拥有本租户访问权，平台管理员走平台权限规则。
- [ ] 运行服务测试并执行 coach、workout、AI 相关测试集，确认既有调用适配完成。
- [ ] 提交：`git add backend/src/main/java backend/src/test/java && git commit -m "feat: add coach member request workflow"`。

### Task 3: 暴露关系 API 并同步权限目录

**Files:**
- Modify/create: `backend/src/main/java/com/gymmind/coach/api/*`
- Modify: `backend/src/main/java/com/gymmind/platform/bootstrap/SystemCatalogInitializer.java`
- Modify: `frontend/src/api/coaches.ts`, `frontend/src/api/members.ts` and relevant views
- Test: controller tests under `backend/src/test/java/com/gymmind/coach/api`

**Interfaces:** `POST /api/v1/coach-relations` for member request; `POST /api/v1/coach-relations/{id}/accept`, `/reject`, `/cancel`, `/end`; `GET` endpoints for pending and active relations. Responses expose relation id, tenant id, coach id, member id, status and timestamps.

- [ ] 写控制器测试覆盖四角色权限、状态码和请求校验。
- [ ] 运行 controller tests 确认失败。
- [ ] 实现 API、DTO、错误映射和权限目录项：`coach-relation:request/read/accept/reject/manage`，并按角色分配。
- [ ] 更新前端 API 封装和会员选教练、教练处理申请的页面状态，不允许教练出现“主动绑定”操作。
- [ ] 运行后端 API 测试和 `npm run build`。
- [ ] 提交：`git add backend frontend && git commit -m "feat: expose coach relation workflow"`。

### Task 4: 数据迁移、隔离验证和回归测试

**Files:**
- Create/modify: database migration/schema documentation
- Modify: `docs/租户隔离与权限矩阵.md` and API docs
- Test: integration tests under `backend/src/integrationTest/java`

**Interfaces:** Migration must preserve existing member records by creating one `member_tenant` row for each former `member_profile.tenant_id`; existing active assignments become `ACTIVE` relations only when both sides belong to the same tenant.

- [ ] 写迁移测试：旧会员租户字段正确回填，旧绑定关系正确迁移，重复关系被唯一约束阻止。
- [ ] 写隔离测试：租户 A 的会员、教练和关系不能被租户 B 读取或修改；平台管理员可读取平台聚合接口但普通租户详情仍受限。
- [ ] 执行集成测试和完整后端测试：`./mvnw -q test`。
- [ ] 执行前端构建：`npm run build`。
- [ ] 更新权限矩阵、API 文档和项目状态文档，明确会员跨健身房及申请流程。
- [ ] 提交：`git add backend frontend docs && git commit -m "test: verify tenant and role relationship rules"`。

## 完成标准

- 单用户只能拥有一个角色的约束和现有 IAM 测试保持通过。
- 一个会员可以加入多个健身房，且每个健身房有独立归属状态。
- 会员发起申请，教练只能接受或拒绝，关系状态转换受权限和租户限制。
- 教练与会员在同一租户内支持多对多关系。
- 平台管理员、健身房管理员、教练、会员的访问边界有自动化测试覆盖。

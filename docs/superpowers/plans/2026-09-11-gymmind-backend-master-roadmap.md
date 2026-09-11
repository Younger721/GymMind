# GymMind 后端全量重建 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将旧后端重建为可启动、可测试的 GymMind 多租户智能健身房 Spring Boot 后端，完整覆盖已确认的 P0、P1、P2 需求。

**Architecture:** 采用 feature-first 模块化单体，每个业务模块内部按 `api/application/domain/infrastructure` 分层；MySQL 是业务与索引任务的唯一事实源，所有租户数据从 JWT 主体建立边界。RAG 固定使用 Elasticsearch 全文检索和 Milvus 向量检索，经 RRF、百炼重排、MySQL 授权复核后生成带引用回答。

**Tech Stack:** Java 17、Spring Boot 3.4.x、Maven、Spring Security、Spring Data JPA、MySQL 8、Redis 7、Elasticsearch 9.4.2、Milvus 2.4.17、etcd、MinIO、Spring AI 1.0.x、阿里云百炼、JUnit 5、Testcontainers、MockWebServer、springdoc-openapi。

## Global Constraints

- 产品名和品牌名固定为 `GymMind`，API 前缀固定为 `/api/v1`。
- 一个 `Tenant` 代表一家独立、个性化健身房；不存在连锁、总部、门店层级或跨租户共享。
- 数据库固定为 MySQL 8，库名必须精确为 `gymmind`；不使用 PostgreSQL、pgvector 或 Flyway。
- 开发环境 Hibernate 使用 `ddl-auto=update`，测试使用 `create-drop`，生产只允许 `validate`。
- 删除旧库前必须只读确认目标名为 `gymmind`；重置脚本必须拒绝空库名、系统库和其他库名。
- 平台管理员只能管理租户状态和查看脱敏汇总，不能读取租户会员、训练、知识正文或私有会话。
- 公开注册一次性创建租户和首个 `GYM_ADMIN`；`COACH` 与 `MEMBER` 只能由租户管理员创建或邀请。
- 知识可见性只有 `TENANT` 与 `PRIVATE_USER`；平台管理员不能搜索租户知识正文。
- RAG 必须同时使用 Elasticsearch 与 Milvus，不得退化为 pgvector 或单一 VectorStore 实现。
- 百炼模型固定为 Chat/结构化生成 `qwen3.7-plus`、意图分类 `qwen3.8-flash`、1024 维 Embedding `qwen3.7-text-embedding`、Rerank `qwen3.7-text-rerank`。
- 百炼基础地址固定为 `https://dashscope.aliyuncs.com/compatible-mode/v1`；密钥只读取 `DASHSCOPE_API_KEY`，不得写入源码、配置默认值、测试、日志或 Git。
- 套餐、订单和缴费只记录业务事实，不接真实支付渠道，不保存银行卡数据，不发起扣款。
- 复用现有 Redis、Elasticsearch、Milvus、etcd 与 MinIO，不新增消息队列。
- 所有功能与修复遵循红-绿 TDD；每个可独立审查的小功能单独提交，显式暂存文件，禁止 `git add .`，不自动推送。
- 普通单元测试不得依赖 Docker 或付费模型；真实基础设施测试使用单独的 `integration` profile。

---

## File Structure

```text
backend/
├── pom.xml
├── src/main/java/com/gymmind/
│   ├── GymMindApplication.java
│   ├── shared/{api,error,config,security,persistence,async}/
│   ├── platform/{api,application,domain,infrastructure}/
│   ├── tenancy/{api,application,domain,infrastructure}/
│   ├── iam/{api,application,domain,infrastructure}/
│   ├── member/{api,application,domain,infrastructure}/
│   ├── coach/{api,application,domain,infrastructure}/
│   ├── course/{api,application,domain,infrastructure}/
│   ├── booking/{api,application,domain,infrastructure}/
│   ├── membership/{api,application,domain,infrastructure}/
│   ├── exercise/{api,application,domain,infrastructure}/
│   ├── workout/{api,application,domain,infrastructure}/
│   ├── nutrition/{api,application,domain,infrastructure}/
│   ├── analytics/{api,application,domain,infrastructure}/
│   ├── audit/{api,application,domain,infrastructure}/
│   ├── knowledge/{api,application,domain,infrastructure}/
│   ├── ai/{api,application,domain,infrastructure}/
│   ├── search/{api,application,domain,infrastructure}/
│   └── mcp/{api,application,domain,infrastructure}/
├── src/main/resources/{application.yml,application-dev.yml,application-prod.yml}/
├── src/test/java/com/gymmind/
└── src/integrationTest/java/com/gymmind/
scripts/{reset-gymmind-db.ps1,reset-gymmind-db.sh,verify-backend.ps1}
docs/{api,database,operations}/
```

模块间只通过应用服务或领域端口协作。Controller 不得直接访问其他模块 Repository，AI 与 MCP 不得绕过应用服务、租户检查或资源授权。

## Delivery Sequence

### Task 1: 可启动的 Spring Boot 基线

**Files:** `backend/pom.xml`、`backend/src/main/java/com/gymmind/GymMindApplication.java`、`backend/src/main/java/com/gymmind/shared/**`、`backend/src/main/resources/application*.yml`、对应测试。

**Interfaces:** 产生统一的 `ApiResponse<T>`、`ErrorCode`、`BusinessException`、`Clock`、JPA 审计配置、`/actuator/health` 和 OpenAPI 基线，供所有后续模块使用。

- [ ] 先提交会失败的上下文、配置绑定、统一响应和错误映射测试，确认旧工程不能满足新契约。
- [ ] 删除旧 Java 实现并建立最小 Spring Boot 3.4.x/Java 17 工程，使 `mvn test` 和 `spring-boot:run` 在不连接外部中间件的 `test` 配置下通过。
- [ ] 显式暂存本任务文件并提交 `chore: 重建Spring Boot后端基础工程`。

### Task 2: 数据库安全重置与结构护栏

**Files:** `scripts/reset-gymmind-db.ps1`、`scripts/reset-gymmind-db.sh`、`backend/src/test/java/com/gymmind/database/DatabaseResetGuardTest.java`、`docs/database/README.md`。

**Interfaces:** `DatabaseResetGuard.requireExactDatabase(String)` 只接受精确值 `gymmind`；脚本先查询 `SELECT DATABASE()`/`SHOW DATABASES LIKE 'gymmind'`，再显式执行 `DROP DATABASE IF EXISTS gymmind` 和 `CREATE DATABASE gymmind CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci`。

- [ ] 用参数化测试证明空值、大小写变体、系统库和任意其他库名全部拒绝。
- [ ] 在获得本机凭据后先执行只读目标核验，再运行有人工确认的重置；凭据只能来自环境变量或交互输入。
- [ ] 提交 `chore: 增加gymmind数据库安全重置工具`。

### Task 3: 租户、IAM、JWT 与 RBAC

**Files:** `backend/src/main/java/com/gymmind/{tenancy,iam,shared/security}/**` 及同包测试。

**Interfaces:** `CurrentActor(userId, tenantId, roles, permissions, tokenVersion, tokenId)`；`TenantRegistrationService.register(RegisterTenantCommand)`；`TokenService.issue/refresh/revoke`；`ResourceAuthorizationService`；固定角色 `PLATFORM_ADMIN/GYM_ADMIN/COACH/MEMBER`。

- [ ] 先覆盖注册事务、密码散列、访问/刷新令牌、Redis 撤销、租户停用、角色权限矩阵和跨租户 IDOR 的失败测试。
- [ ] 分小提交实现租户模型、用户/角色/权限、注册登录、JWT 过滤器、刷新/退出、邀请和方法级授权。
- [ ] 运行 IAM 全套测试与 MySQL Testcontainers 隔离测试，提交边界采用第一阶段详细计划中的提交名称。

### Task 4: 操作审计落库

**Files:** `backend/src/main/java/com/gymmind/audit/**` 及同包测试。

**Interfaces:** `AuditRecorder.record(AuditEvent)`；actorId 与 tenantId 只能从 `CurrentActor` 补充，事件仅接受动作、资源类型、资源 ID、结果、traceId 和白名单元数据。关键成功事件与业务写入同事务，拒绝事件使用独立事务。

- [ ] 测试 actor/tenant 不可由调用方伪造、密码/Token/健康资料/银行卡字段不可入库、关键审计失败使业务回滚、另一租户无法查询。
- [ ] 实现不可更新/删除的 `OperationAudit` 和审计端口，供所有后续业务写操作调用。
- [ ] 提交 `feat: 建立租户级操作审计能力`。

### Task 5: 会员、教练与负责关系

**Files:** `backend/src/main/java/com/gymmind/{member,coach}/**`、对应测试和 OpenAPI 契约。

**Interfaces:** `MemberService.create/update/suspend/find`、`CoachService.create/update/find`、`CoachAssignmentService.assign/unassign/canAccess`；所有命令从 `CurrentActor` 取 tenantId。

- [ ] 测试租户管理员 CRUD、会员本人读取、教练仅能读取有效分配会员、手机号/会员号租户内唯一和跨租户拒绝。
- [ ] 实现 `/api/v1/members`、`/api/v1/coaches`、`/api/v1/coaches/{coachId}/members`，并记录身份敏感操作审计。
- [ ] 分别提交 `feat: 实现租户会员管理`、`feat: 实现教练与会员负责关系`。

### Task 6: 课程目录与排期

**Files:** `backend/src/main/java/com/gymmind/course/**` 及同包测试。

**Interfaces:** `CourseService`、`CourseCapacityManager`；课程保存有效教练、容量、已预订数、时段、地点、类型、状态和 `@Version`。

- [ ] 测试课程时段、教练归属、缩容下限、已开始课程变更和跨租户拒绝。
- [ ] 实现 `/api/v1/courses`，取消课程保留历史且不物理删除。
- [ ] 提交 `feat: 实现课程排期管理`。

### Task 7: 套餐、订单与缴费记录

**Files:** `backend/src/main/java/com/gymmind/membership/**` 及同包测试。

**Interfaces:** `MembershipPackageService`、`MemberMembershipService`、`OrderService`、`PaymentRecordService.recordOfflinePayment`；金额使用 `BigDecimal` 和明确币种，缴费 API 不包含支付网关操作。

- [ ] 测试次数/期限权益、订单状态机、重复凭证、幂等缴费、退款更正记录和跨租户隔离。
- [ ] 实现 `/api/v1/membership-packages`、`/api/v1/memberships`、`/api/v1/orders`、`/api/v1/payment-records`。
- [ ] 分别提交 `feat: 实现会员套餐与权益`、`feat: 实现订单和缴费记录`。

### Task 8: 预约、签到与权益核销

**Files:** `backend/src/main/java/com/gymmind/booking/**` 及同包测试。

**Interfaces:** `BookingService.book/cancel/confirm`、`CheckInService.checkIn`、`MembershipEntitlementPort.reserve/release/consume`；预约状态固定为 `PENDING/CONFIRMED/CANCELLED/COMPLETED/NO_SHOW`，容量使用 `Course.@Version` 和 `reservedCount` 防超卖。

- [ ] 测试 20 个并发请求争抢 10 个名额恰好 10 个成功、重复预约幂等、租户取消时限、签到窗口、爽约、容量与权益同事务、跨租户拒绝。
- [ ] 实现 `/api/v1/bookings`、`/api/v1/check-ins`；定时爽约任务逐租户恢复上下文，缺少上下文 fail closed。
- [ ] 分别提交 `feat: 实现防超卖课程预约状态机`、`feat: 实现签到核销与自动爽约`。

### Task 9: 动作与教学视频

**Files:** `backend/src/main/java/com/gymmind/exercise/**` 及同包测试。

**Interfaces:** `ExerciseService`、`ExerciseVideoService`、`ExerciseTemplateSeeder`；平台模板只在租户创建时复制，此后记录必须属于该租户。

- [ ] 测试动作标签、训练限制、租户模板复制、合法第三方 URL、MinIO 租户对象键和跨租户拒绝。
- [ ] 实现 `/api/v1/exercises`、`/api/v1/videos`，不下载或托管无授权第三方内容。
- [ ] 分别提交 `feat: 实现租户动作库`、`feat: 实现动作教学视频管理`。

### Task 10: 训练计划与训练记录

**Files:** `backend/src/main/java/com/gymmind/workout/**` 及同包测试。

**Interfaces:** `WorkoutPlanService`、`WorkoutPlanValidator`、`WorkoutRecordService`；计划使用 `WorkoutPlan`/`WorkoutPlanItem` 规范化表，记录使用 `WorkoutRecord`/`WorkoutSet`。

- [ ] 测试动作归属、组次/重量/休息范围、会员健康限制、教练负责关系、计划发布和训练记录所有权。
- [ ] 实现 `/api/v1/workout-plans` 与 `/api/v1/workout-records`，保留 `saveValidatedDraft` 给 AI 模块调用。
- [ ] 分别提交 `feat: 实现结构化训练计划`、`feat: 实现训练完成记录`。

### Task 11: 营养资料、计划与记录

**Files:** `backend/src/main/java/com/gymmind/nutrition/**` 及同包测试。

**Interfaces:** `NutritionCalculator.calculateBmi/calculateBmr/calculateTdee/calculateMacros`、`NutritionPlanService`、`NutritionRecordService`；确定性 Java 公式是计算唯一来源。

- [ ] 用边界值和已知样例测试 BMI、Mifflin-St Jeor BMR、活动系数 TDEE、宏量营养合计及权限。
- [ ] 实现 `/api/v1/nutrition/profiles`、`foods`、`plans`、`records`。
- [ ] 分别提交 `feat: 实现确定性营养计算`、`feat: 实现营养计划与记录`。

### Task 12: Dashboard 与经营周报数据

**Files:** `backend/src/main/java/com/gymmind/analytics/**` 及同包测试。

**Interfaces:** `DashboardQueryService`、`WeeklyMetricsService`；租户指标由业务模块查询端口确定性聚合，平台汇总只能返回租户数量、活跃状态和脱敏 AI 成本。

- [ ] 测试会员数、活跃率、预约率、签到率、收入记录、续费率的可复算性和时间区间；测试平台管理员不能下钻明细。
- [ ] 实现 `/api/v1/analytics/dashboard` 与 `/weekly-metrics`，不得直接跨模块访问 Repository。
- [ ] 提交 `feat: 实现租户经营看板`。

### Task 13: 知识文档、MinIO 与持久摄取任务

**Files:** `backend/src/main/java/com/gymmind/knowledge/**`、`backend/src/main/java/com/gymmind/shared/async/**` 及测试。

**Interfaces:** `KnowledgeDocumentUseCase.upload/reindex/delete`、`ObjectStoragePort`、`PersistentJobQueue.claimNext/advance/retry/complete`；Worker 只接收 jobId/documentId，任务租约使用 MySQL 8 `FOR UPDATE SKIP LOCKED`。

- [ ] 测试 `DocumentStatus` 状态机、`TENANT/PRIVATE_USER` 策略、魔数/大小校验、`tenant/{tenantId}/knowledge/...` 对象键、租约恢复和指数退避。
- [ ] 实现上传返回 202、MinIO 存储、MySQL 文档/Chunk/Job 主记录、启动恢复和 tombstone 删除。
- [ ] 分别提交 `feat: 建立知识文档与可见范围模型`、`feat: 接入租户隔离的MinIO文档存储`、`feat: 增加持久化摄取任务和租约恢复`。

### Task 14: 文档解析、分块和百炼 Embedding

**Files:** `backend/src/main/java/com/gymmind/knowledge/infrastructure/parser/**`、`backend/src/main/java/com/gymmind/ai/infrastructure/bailian/BailianEmbeddingAdapter.java` 及测试。

**Interfaces:** `DocumentParserRegistry.parse`、`DeterministicChunker.chunk`、`EmbeddingGateway.embed(List<String>)`；每个向量强制恰为 1024 维。

- [ ] 测试 PDF、DOCX、HTML、Markdown、TXT、空文档、稳定 Chunk ID、边界重叠、批处理与错误响应。
- [ ] 用 MockWebServer 接入 `qwen3.7-text-embedding`，CI 不调用真实付费 API。
- [ ] 分别提交 `feat: 实现文档解析与确定性分块`、`feat: 接入百炼1024维文本向量`。

### Task 15: Elasticsearch 与 Milvus 双索引

**Files:** `backend/src/main/java/com/gymmind/knowledge/infrastructure/{elasticsearch,milvus}/**` 及 integration tests。

**Interfaces:** `KeywordIndexPort.upsert/search/delete`、`VectorIndexPort.upsert/search/delete`；两侧使用相同的确定性 chunkId、tenantId、ownerUserId、visibility、documentId 和 generation。

- [ ] 在 ES 9.4.2 smartcn 容器测试全文、租户/私有过滤、bulk upsert/delete；在 Milvus 2.4.17 测试 1024 维 COSINE/HNSW、scalar filter、upsert/delete。
- [ ] 测试新 generation 只有双侧校验成功才发布，单侧失败保留旧 generation，删除先 tombstone 后异步清理。
- [ ] 分别提交 `feat: 建立ES全文索引适配器`、`feat: 建立Milvus向量索引适配器`、`feat: 实现双索引generation发布与清理`。

### Task 16: RRF 混合检索与百炼重排

**Files:** `backend/src/main/java/com/gymmind/ai/domain/{HybridRetriever,RrfFusion,ContextBudgeter}.java`、`BailianRerankAdapter.java` 及测试。

**Interfaces:** ES Top 30 与 Milvus Top 30 并行；`RrfFusion` 使用 `sum(1/(60+rank))` 去重融合 Top 20；`RerankGateway` 调用 `qwen3.7-text-rerank`；MySQL 最终复核后返回 5-8 段且每文档最多 2 段。

- [ ] 测试 RRF 分数、稳定同分排序、单路降级、双路失败 503、MySQL 授权失败 fail closed、上下文 Token 预算和引用上限。
- [ ] 实现 `/api/v1/knowledge/search`，平台管理员直接拒绝正文搜索。
- [ ] 分别提交 `feat: 实现RRF混合召回和降级`、`feat: 接入百炼重排与上下文预算`。

### Task 17: AI 对话、用量、长期记忆与训练计划

**Files:** `backend/src/main/java/com/gymmind/ai/**` 及同包测试。

**Interfaces:** `ChatModelGateway.complete`；意图使用 `qwen3.8-flash`，回答和结构化草案使用 `qwen3.7-plus`；`AiUsageLog` 记录模型、Token、延迟、状态和 providerRequestId，不记录密钥或完整正文。

- [ ] 用 MockWebServer 测试模型路由、超时、429/5xx 重试、结构化响应、引用、Prompt Injection 边界、Token 审计和租户会话隔离。
- [ ] 实现 `/api/v1/ai/chat`、长期记忆的显式写入/删除、AI 训练计划草案；草案只有通过 `WorkoutPlanValidator` 和权限检查后才保存。
- [ ] 分别提交 `feat: 完成带来源引用的RAG问答`、`feat: 增加AI用量与错误审计`、`feat: 实现租户隔离的AI长期记忆`、`feat: 实现AI训练计划生成与业务校验`。

### Task 18: AI 私教、会员分析、运营分析和周报

**Files:** `backend/src/main/java/com/gymmind/ai/application/**`、`backend/src/main/java/com/gymmind/analytics/**` 及测试。

**Interfaces:** AI 只解释确定性指标；`CoachAssistantService` 先用 `CoachAssignmentService` 复核负责关系；`WeeklyReportService` 总结 `WeeklyMetricsService` 的冻结快照。

- [ ] 测试会员本人、负责教练、租户管理员的字段级权限；测试周报重跑幂等、指标不被模型改写和失败可重试。
- [ ] 实现 `/api/v1/ai/member-analysis`、`coach-assistant`、`operation-analysis`、`weekly-reports`。
- [ ] 分别提交 `feat: 实现AI会员与私教助手`、`feat: 实现AI运营分析与周报`。

### Task 19: 流失预测与推荐系统

**Files:** `backend/src/main/java/com/gymmind/analytics/domain/ChurnRiskCalculator.java`、`backend/src/main/java/com/gymmind/ai/application/RecommendationService.java` 及测试。

**Interfaces:** 风险分数由可解释规则产生；推荐候选由目标、限制、历史和动作标签确定，AI 仅增强排序和说明。

- [ ] 测试规则权重、缺失数据、健康禁忌硬过滤、租户隔离、结果可解释和 AI 不可越过候选集。
- [ ] 实现 `/api/v1/analytics/churn-risks` 与 `/api/v1/ai/recommendations`。
- [ ] 分别提交 `feat: 实现可解释流失风险评估`、`feat: 实现安全约束下的个性化推荐`。

### Task 20: Web 搜索、合规导入与 MCP 工具

**Files:** `backend/src/main/java/com/gymmind/{search,mcp}/**` 及同包测试。

**Interfaces:** URL 导入只允许 HTTP/HTTPS 公网地址，DNS 解析后拒绝内网、环回、链路本地和受控重定向；MCP tool 每次执行重新取得 `CurrentActor` 并调用同一应用服务。

- [ ] 测试 SSRF、重定向、robots/访问失败、文章元数据、第三方视频只存链接，以及每个 MCP tool 的允许/拒绝矩阵。
- [ ] 实现 `/api/v1/search/articles`、`/import`、视频搜索元数据和 `/api/v1/mcp` 工具目录/调用。
- [ ] 分别提交 `feat: 实现安全的网页搜索与知识导入`、`feat: 实现受RBAC保护的MCP工具`。

### Task 21: 审计查询、OpenAPI、运行脚本与端到端验收

**Files:** `backend/src/main/java/com/gymmind/audit/api/**`、`docs/api/openapi.json`、`docs/operations/README.md`、`scripts/verify-backend.ps1`、启动脚本、端到端测试。

**Interfaces:** 一条验证命令完成 Java 17 校验、构建、单元测试、可选集成测试、应用启动、健康检查、租户注册、登录和核心 API 冒烟。

- [ ] 实现 `/api/v1/audit/operations` 与 `/api/v1/audit/ai-usage`；租户管理员只看当前租户，平台管理员只看平台事件和脱敏 AI 汇总。
- [ ] 生成并校验 OpenAPI，确认所有受保护 API 声明 bearerAuth、所有响应使用统一 envelope。
- [ ] 使用空 `gymmind` 库启动并验证幂等初始化；验证两个租户之间 MySQL、Redis、MinIO、ES、Milvus、AI 与 MCP 均不互通。
- [ ] 在本地安全注入密钥后分别人工验证一次 Chat、Embedding 和 Rerank；输出中不得出现密钥。
- [ ] 提交 `docs: 完善后端运行与验收指南` 和 `test: 增加GymMind端到端验收`。

## Phase Gates

每个 Task 结束前必须满足：

1. 本任务新增测试先看到与目标行为对应的失败，再实现到绿色。
2. 运行本任务测试和受影响模块回归测试；共享基础变更运行全部单元测试。
3. `git diff --check` 无错误，`git status --short` 只显示已知用户文件和本任务文件。
4. 搜索 `sk-`、`DASHSCOPE_API_KEY=`、JWT 实际密钥、数据库密码，确认没有秘密进入已暂存内容。
5. 只显式暂存本任务文件，每个可独立审查功能单独提交，不推送。

## Completion Criteria

- [ ] Java 17 下 `mvn clean verify` 通过，应用可启动且 `/actuator/health` 为 `UP`。
- [ ] 空 `gymmind` 库能由 Hibernate 建表并幂等初始化；无 Flyway 元数据表。
- [ ] 全部 P0、P1、P2 API 有契约、授权、验证、测试和审计。
- [ ] 两个独立租户在所有存储、缓存、异步、AI 和 MCP 链路均无法互读。
- [ ] ES+Milvus 混合检索、RRF、百炼重排、MySQL 复核、引用、删除和 generation 重建通过真实中间件测试。
- [ ] 支付仅为记录；平台管理员仅看脱敏汇总；AI 结构化输出不经业务校验无法落库。
- [ ] 每个小功能都有对应 Git 提交，用户原有未提交文件未被暂存、覆盖或删除。

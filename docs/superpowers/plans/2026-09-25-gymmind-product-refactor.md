# GymMind 产品重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 GymMind 收敛为“基于 SaaS 架构与 RAG 的智能健身房管理平台”，以共享数据库/共享表 + `tenant_id` 为隔离基础，形成平台管理员、租户管理员、教练、会员四类角色，以及会员运营 + AI 助手的完整业务闭环。

**Architecture:** 保留现有 Spring Boot + Vue 3 架构、租户/配额/RAG/Agent/会员/教练/课程/预约/训练/审计模块。前端按角色提供独立工作台；后端通过认证上下文统一确定租户，不信任请求体中的 `tenantId`。Agent 从通用搭建中心调整为三个预置场景助手，并复用现有工具调用、知识检索、用量记录和配额能力。

**Tech Stack:** Java 17+, Spring Boot 3, Spring Security/JWT, Spring Data JPA, PostgreSQL/MySQL 现有配置, Vue 3, TypeScript, Element Plus, Pinia, ECharts, RAG 检索组件。

## Global Constraints

- 业务表必须含 `tenant_id`；平台级租户、套餐和全局配置表不强制挂租户。
- 所有租户读写、RAG 检索、Agent 调用、AI 用量和审计查询必须使用当前租户上下文。
- 平台管理员不能通过普通租户接口读取跨租户明细；跨租户统计只允许平台专用接口。
- 保留当前工作区未提交改动，不执行 reset、checkout 或覆盖式重写。
- 一期移出主导航和核心演示：营养、外部文章/视频搜索、社区、挑战、个人进度、通用 Agent 自由编排。
- 动作库保留，作为训练计划、训练记录和 RAG/AI 工具的基础数据。
- AI 只能生成建议或草案；训练计划保存、风险指标计算和权限判断由后端完成。

## 目标模块和文件边界

- 后端租户与安全边界：`backend/src/main/java/com/gymmind/shared/security`、`backend/src/main/java/com/gymmind/tenancy`、各业务模块 repository/service。
- 后端业务闭环：`member`、`coach`、`course`、`booking`、`workout`、`exercise`、`analytics`、`audit`。
- 后端 AI：`agent`、`ai`、`knowledge`、`ai/harness`；增加预置助手配置和业务上下文组装，不重写现有 RAG 管线。
- 前端导航与工作台：`frontend/src/layouts/MainLayout.vue`、`frontend/src/router/index.ts`、`frontend/src/stores/auth.ts`、`frontend/src/views/business` 与相关 API 文件。
- 文档：本计划完成后同步更新 `FitSaaS_智能健身房管理与运营平台_开发设计文档.md`、`README.md` 和角色/接口说明。

### Task 1: 建立目标角色、权限和租户上下文契约

**Files:**
- Modify: `backend/src/main/java/com/gymmind/shared/security/CurrentActor.java`
- Modify: `backend/src/main/java/com/gymmind/shared/security/JwtAuthenticationFilter.java`
- Modify: `backend/src/main/java/com/gymmind/shared/security/PlatformTenantContextFilter.java`
- Modify: `backend/src/main/java/com/gymmind/shared/security/SecurityConfig.java`
- Modify: `backend/src/main/java/com/gymmind/iam/domain/model/UserAccount.java`
- Test: `backend/src/test/java/com/gymmind/shared/security/*Test.java`

**Interfaces:**
- `CurrentActor` 必须暴露 `userId()`, `tenantId()`, `roles()`, `hasPermission(String)` 和 `isPlatformAdmin()`。
- 普通租户请求缺少租户上下文时返回统一 403；平台管理员上下文为空时只能访问平台接口。
- 请求体、查询参数和路径中的 `tenantId` 不得覆盖认证上下文。

- [ ] 编写测试：四类角色的权限矩阵、租户用户无上下文拒绝、平台管理员不能调用租户业务接口、跨租户 ID 访问返回 403/无资源。
- [ ] 运行 `cd backend; .\mvnw.cmd -q -Dtest="*Security*Test" test`，确认新增测试先失败。
- [ ] 实现统一 actor/tenant 解析与接口级访问规则，兼容当前 JWT 字段和已有登录流程。
- [ ] 重新运行安全测试，并补充认证过滤器回归测试。

### Task 2: 强制业务数据隔离并补齐索引

**Files:**
- Modify: `backend/src/main/java/com/gymmind/*/domain/model/*.java` 中仍缺少租户约束的业务实体
- Modify: `backend/src/main/java/com/gymmind/*/infrastructure/persistence/*.java`
- Create: `docs/database/tenant-isolation-indexes.sql`
- Test: 各业务模块 repository/service 隔离测试

**Interfaces:**
- 租户 repository 使用 `findByTenantIdAndId`、`findAllByTenantId...` 或等效封装；服务层只传入认证得到的 tenantId。
- 新增记录由服务/实体从 `CurrentActor` 注入租户，不接受前端指定租户。

- [ ] 扫描实体、SQL、JPQL 和原生查询，列出缺失 `tenant_id`、无租户条件和跨表查询风险。
- [ ] 为 member、coach、course、booking、workout、exercise、knowledge、agent、AI usage、audit 等表补字段、联合唯一键和索引。
- [ ] 为每类资源添加 A 租户创建、B 租户读取/修改/删除、列表不串租户的测试。
- [ ] 执行完整后端测试和数据库 schema 校验；记录迁移兼容策略，不删除现有数据。

### Task 3: 收敛平台端与租户端后台

**Files:**
- Modify: `backend/src/main/java/com/gymmind/tenancy/api/PlatformTenantController.java`
- Modify: `backend/src/main/java/com/gymmind/tenancy/api/PlatformQuotaController.java`
- Modify: `backend/src/main/java/com/gymmind/tenancy/api/PlatformStatsController.java`
- Modify: `backend/src/main/java/com/gymmind/tenancy/api/TenantSettingsController.java`
- Modify: `frontend/src/views/business/PlatformView.vue`
- Modify: `frontend/src/views/business/TenantSettingsView.vue`
- Modify: `frontend/src/api/tenancy.ts`

**Interfaces:**
- 平台 API：租户新增、冻结/启用、额度更新、模块开关、全局统计、平台审计。
- 租户 API：自身设置、员工/角色、AI 助手配置、租户审计；禁止传入任意 tenantId。

- [ ] 将平台管理页面拆成“租户、配额与模块、全局统计、平台审计”四个明确区域。
- [ ] 将租户设置页面改为“门店设置、员工角色、AI 服务配置、租户额度只读/使用量”。
- [ ] 为冻结租户、超配额、关闭 AI 模块增加明确错误码和前端空/错误状态。
- [ ] 验证平台管理员与租户管理员分别登录后的菜单和 API 权限。

### Task 4: 重做租户业务闭环

**Files:**
- Modify: `frontend/src/layouts/MainLayout.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/views/business/OperationsView.vue`
- Modify: `frontend/src/views/business/MembershipManageView.vue`
- Modify: `frontend/src/views/business/TrainingView.vue`
- Modify: `frontend/src/views/business/AnalyticsHubView.vue`
- Modify: `frontend/src/api/members.ts`, `coaches.ts`, `courses.ts`, `bookings.ts`, `workouts.ts`, `analytics.ts`

**Interfaces:**
- 租户菜单：运营概览、会员、教练、课程排期、预约签到、训练管理、风险预警、AI 服务配置、员工与角色、租户审计。
- 教练菜单：我的会员、训练计划、训练记录、教练助手、课程日程。
- 会员菜单：我的课程、预约签到、我的训练、健身助手。

- [ ] 删除/隐藏营养、搜索、社区、挑战、个人进度和通用 Agent 管理的主导航入口，保留兼容重定向但不在一期菜单展示。
- [ ] 将 `OperationsView` 改为租户运营概览：会员总数、活跃率、今日预约、签到率、待跟进风险会员。
- [ ] 将会员、教练、课程、预约、训练页面统一为同一租户工作台视觉和状态模型。
- [ ] 在前端路由守卫增加角色权限判断，不满足权限时进入 403 页面或回到角色首页。
- [ ] 为列表加载、空数据、保存失败、配额不足和租户被冻结设计可验证状态。

### Task 5: 将 Agent 重构为三个预置智能助手

**Files:**
- Modify: `backend/src/main/java/com/gymmind/agent/application/DefaultAgentManagementService.java`
- Modify: `backend/src/main/java/com/gymmind/agent/application/DefaultAgentChatService.java`
- Modify: `backend/src/main/java/com/gymmind/ai/harness/*`
- Modify: `backend/src/main/java/com/gymmind/ai/api/*`
- Modify: `frontend/src/views/AssistantView.vue`
- Modify: `frontend/src/views/AgentManageView.vue`
- Modify: `frontend/src/api/agent.ts`, `ai.ts`, `harness.ts`

**Interfaces:**
- 预置类型：`MEMBER_ASSISTANT`、`COACH_ASSISTANT`、`OPERATIONS_ASSISTANT`。
- 租户配置：启用状态、显示名称、提示词补充、允许的知识库分类、允许的工具集合。
- 聊天接口必须带 assistant type，由后端根据 actor 角色、租户和业务资源组装上下文。
- 计划生成返回 draft；只有教练/管理员确认后调用确定性保存接口。

- [ ] 为新租户自动创建三个预置助手配置，旧有自由 Agent 数据保持可读并提供迁移映射。
- [ ] 实现会员助手的租户知识库检索与引用；禁止检索其他租户文档。
- [ ] 实现教练助手读取会员目标、训练记录、动作库并输出结构化计划草案。
- [ ] 实现运营助手读取后端统计指标并输出风险解释，不允许 AI 自己计算或直接写业务表。
- [ ] 统一记录调用次数、token、延迟、失败原因，并接入租户配额/限流。
- [ ] 前端将通用 Agent 管理改为“AI 服务配置”和按角色展示的助手入口。

### Task 6: 会员活跃度与流失预警产品化

**Files:**
- Modify: `backend/src/main/java/com/gymmind/analytics/application/DefaultChurnRiskService.java`
- Modify: `backend/src/main/java/com/gymmind/analytics/application/DefaultDashboardQueryService.java`
- Create/Modify: analytics DTO、repository 和 controller
- Modify: `frontend/src/views/business/AnalyticsHubView.vue`

**Interfaces:**
- 风险指标由后端规则计算：最近签到、近 30 天签到、预约取消/参与、课程参与、会员卡剩余时间。
- 风险结果包含等级、指标明细、解释状态和建议，不把 AI 文本作为唯一判断依据。

- [ ] 编写边界测试：无签到、刚注册、卡已过期、数据不足、正常活跃和高风险组合。
- [ ] 实现可解释的风险评分/分级和租户范围查询。
- [ ] 将风险名单接入租户首页、会员详情和运营助手。
- [ ] 增加“已跟进/待跟进”操作日志，支持租户管理员查看处理状态。

### Task 7: 统一异常、返回体、校验、审计和限流

**Files:**
- Modify: `backend/src/main/java/com/gymmind/common/*`
- Modify: `backend/src/main/java/com/gymmind/*/api/*Request.java`
- Modify: `backend/src/main/java/com/gymmind/audit/*`
- Modify: `backend/src/main/resources/application*.yml`
- Test: controller、validation、quota、rate-limit tests

**Interfaces:**
- 统一返回：`{ code, message, data, traceId }`。
- 统一错误码至少包含：未登录、无权限、租户被冻结、租户上下文缺失、资源不存在、额度不足、租户限流、参数校验失败、AI 服务失败。
- 限流 key 使用 `tenantId + endpoint`，平台端接口使用平台维度；超限返回 429 和可读错误信息。

- [ ] 增加全局异常处理、参数校验和 traceId；移除 controller/service 中重复裸 `try-catch`。
- [ ] 为创建会员、课程、文档、Agent 和 AI 调用接入额度检查。
- [ ] 为敏感操作记录 actor、tenant、时间、动作、资源和结果。
- [ ] 增加限流测试、配额测试、统一错误体测试和审计回归测试。

### Task 8: 更新文档、验收和答辩材料

**Files:**
- Modify: `FitSaaS_智能健身房管理与运营平台_开发设计文档.md`
- Modify: `README.md`
- Create: `docs/租户隔离与权限矩阵.md`
- Create: `docs/答辩演示流程.md`
- Create: `docs/database/tenant-isolation-indexes.sql`

- [ ] 删除文档中与一期定位冲突的个人 App、社区、挑战和通用 Agent 主线描述。
- [ ] 补充共享数据库/共享表隔离方案、租户上下文流程、权限矩阵、配额与限流说明。
- [ ] 补充三个预置助手的 RAG 数据流、工具白名单、调用计量和日志隔离说明。
- [ ] 编写 10 分钟演示：平台建租户 → 租户建员工/会员 → 预约签到 → 教练生成计划 → 会员 RAG 问答 → 运营风险预警 → A/B 租户隔离验证。
- [ ] 执行前端构建、后端完整测试、租户隔离测试、关键页面手工验收，并记录结果。

## 验收标准

- A 租户用户无法通过列表、详情、导出、RAG、Agent、AI 用量或审计接口看到 B 租户数据。
- 平台管理员可以创建/冻结租户、修改配额、开关模块并查看全局聚合统计。
- 租户管理员只能管理自己的员工、会员、教练、课程、预约和 AI 配置。
- 教练可基于会员业务数据生成训练计划草案；会员可进行带知识库引用的健身问答。
- 运营首页能展示真实业务指标和可解释的会员流失风险。
- 超过租户配额或租户级限流阈值时，接口返回统一错误体，不产生部分写入。
- 前端主导航不再出现营养、社区、挑战、外部搜索和通用 Agent 管理等非核心模块。
- `mvn test` 与 `npm run build` 通过，关键页面具备 loading、empty、error、forbidden 状态。

## 分阶段交付顺序

1. 安全契约与租户隔离（Task 1-2）。
2. 平台/租户后台收敛（Task 3）。
3. 会员运营闭环与角色工作台（Task 4、6）。
4. 预置 Agent/RAG 服务化（Task 5）。
5. 工程治理与统一体验（Task 7）。
6. 文档、演示和最终验收（Task 8）。

每个任务完成后先运行对应测试，再进入下一个任务；禁止把所有页面和后端改动堆到最后一次验证。

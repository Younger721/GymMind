# GymMind 多租户智能健身房后端重建设计

## 1. 文档状态

- 日期：2026-09-11
- 状态：已由用户确认，进入实施计划阶段
- 需求来源：`FitSaaS_智能健身房管理与运营平台_开发设计文档.md`
- 产品名称与品牌名称：`GymMind`
- 旧后端回退基线：Git 提交 `bfd5c40`

本设计以用户在会话中的确认结果为最高优先级。需求资料中的 PostgreSQL、pgvector、连锁门店和真实收费建议均被后续确认覆盖。

## 2. 目标与范围

重建一个可启动、可测试、可持续扩展的 Spring Boot 后端，为多家彼此独立的个性化健身房提供 SaaS 能力。

本次范围包含原需求清单中的全部 P0、P1 和 P2：

1. 登录、注册、JWT、RBAC、租户管理和租户个性化配置。
2. 会员、教练、课程、预约、签到、套餐、订单和缴费记录。
3. 动作、教学视频、训练计划、训练记录和营养管理。
4. Dashboard、经营周报、会员分析和运营分析。
5. AI 对话、AI 私教、AI 训练计划、长期记忆、流失风险和推荐系统。
6. 文档知识库、网页文章、搜索导入及 RAG。
7. AI 使用量、Token、延迟、错误和操作审计。
8. 受同一权限体系保护的 MCP 工具能力。

套餐、订单和缴费仅记录业务事实，不接入真实支付渠道，不产生真实扣款。

## 3. 明确不做

- 不建设微服务体系。
- 不引入 Kafka、RabbitMQ 或其他新消息中间件。
- 不使用 PostgreSQL、pgvector 或 Flyway。
- 不保留旧数据库结构或迁移旧业务数据。
- 不实现品牌总部、连锁门店、跨店预约或跨租户共享。
- 不兼容旧前端 API；后续前端以新 OpenAPI 契约重构。
- 不允许 AI 直接写数据库或替代确定性业务计算。
- 不下载或重新托管无授权的第三方视频。

## 4. 总体架构

后端采用 Java 17、Spring Boot 3.x 和 Maven 构建模块化单体。所有 HTTP API 使用 `/api/v1` 前缀，统一生成 OpenAPI 文档。

代码采用纵向业务模块，每个模块内部按 `api`、`application`、`domain`、`infrastructure` 分层：

```text
com.gymmind
├── GymMindApplication
├── shared
│   ├── api
│   ├── error
│   ├── config
│   ├── security
│   ├── persistence
│   └── async
├── platform
├── tenancy
├── iam
├── member
├── coach
├── course
├── booking
├── membership
├── exercise
├── workout
├── nutrition
├── knowledge
├── ai
├── analytics
├── audit
└── mcp
```

模块间只通过应用服务或明确的端口接口协作，禁止 Controller 直接访问其他模块 Repository，禁止 AI 服务绕过业务服务和权限检查。

## 5. 技术基线

- Java：17
- Spring Boot：3.4.x 稳定版
- Spring AI：1.0.x 稳定版，使用 BOM 管理版本
- 关系数据库：MySQL 8，数据库名固定为 `gymmind`
- ORM：Spring Data JPA / Hibernate
- 数据库结构：Hibernate 开发环境 `ddl-auto=update`，测试环境 `create-drop`
- 缓存与短租约：现有 Redis 7
- 本地热点缓存：Caffeine，仅用于明确允许的短期数据
- 全文检索：现有 Elasticsearch 9.4.2 与 smartcn
- 向量检索：现有 Milvus 2.4.17 与 etcd
- 文件存储：现有 MinIO
- 文档解析：PDFBox、Apache POI、Jsoup 和纯文本/Markdown 解析器
- API 文档：springdoc-openapi
- 测试：JUnit 5、Spring Boot Test、Spring Security Test、Testcontainers、MockWebServer

Elasticsearch Java 客户端必须与 9.4.2 服务端对齐。Milvus Java SDK 使用与 2.4.17 服务端兼容的 2.4.x 最新补丁版，并通过真实容器集成测试验证筛选、upsert 和 delete。

## 6. 数据库管理

用户明确要求删除旧 `gymmind` 数据库并重新设计。实施时必须执行以下安全边界：

1. 只允许操作准确名称为 `gymmind` 的数据库。
2. 删除前再次查询并输出目标数据库名，不触碰 MySQL 系统库或其他项目库。
3. 通过显式重置脚本执行 `DROP DATABASE gymmind` 与重新创建。
4. 重置脚本默认要求人工确认；自动化测试不得连接本机开发库。
5. 建表由 Hibernate 实体模型完成，不使用 Flyway。
6. 角色、权限、平台管理员和租户动作模板由幂等初始化器写入。
7. 生产配置禁止 `create`、`create-drop`，只能使用 `validate` 或经人工控制的 `update`。

不使用迁移工具的代价是缺少可审计的结构演进历史。因此每次实体结构变更必须同步更新数据库设计文档、结构快照测试和重置脚本说明。

## 7. 租户模型

一个 Tenant 就是一家独立健身房。GymMind 平台承载多个租户，但租户之间没有品牌、门店或业务关系。

每个租户可以独立配置：

- 健身房名称、Logo、联系方式和介绍。
- 营业时间、预约规则和取消规则。
- 课程、教练、会员、套餐、动作、视频和营养数据。
- 知识库、AI 提示词参数和推荐偏好。

平台管理员只能：

- 创建、查看、启用和停用租户。
- 查看租户数量、活跃状态和脱敏的平台汇总。
- 查看平台级故障与 AI 成本汇总。

平台管理员不能读取租户会员明细、训练数据、知识库正文或私有 AI 会话。

## 8. 租户隔离实现

租户上下文只能从服务端验证后的 JWT Principal 建立，业务 API 不接受客户端传入的 `tenantId` 决定数据范围。

隔离在多个层级同时执行：

1. MySQL：所有租户业务表包含非空 `tenant_id`，唯一约束必须包含 `tenant_id`，Repository 查询必须显式带租户条件。
2. 应用服务：写入前校验资源租户与当前主体租户一致，并执行角色和资源关系检查。
3. Redis：键固定包含环境、版本、tenantId、主体或可见范围。
4. MinIO：对象键使用 `tenant/{tenantId}/...` 命名空间。
5. Elasticsearch：文档写入 `tenant_id`，所有查询使用不可移除的 filter。
6. Milvus：向量记录写入 `tenant_id`，所有检索表达式强制过滤。
7. AI/MCP：每次工具调用重新获取 CurrentActor 并执行权限判断。
8. 异步任务：任务表持久化 tenantId，工作线程恢复租户上下文后才能处理。

任何租户上下文缺失、关系库复核失败或权限状态不明确的情况均 fail closed。

## 9. 身份、注册与 RBAC

角色固定为：

- `PLATFORM_ADMIN`
- `GYM_ADMIN`
- `COACH`
- `MEMBER`

权限通过 `User -> Role -> Permission` 配置，并使用方法级鉴权。核心表包括 `sys_tenant`、`sys_user`、`sys_role`、`sys_permission`、`sys_user_role` 和 `sys_role_permission`。

公开注册流程：

1. 用户提交健身房资料和管理员账户。
2. 一个事务创建 Tenant、首位 GYM_ADMIN、角色绑定和租户默认设置。
3. 初始化该租户的权限、动作模板和基础业务配置。
4. 返回访问令牌与刷新令牌。

教练和会员只能由当前租户 GYM_ADMIN 创建或邀请。平台管理员由本地安全初始化配置创建，不能通过公开注册获得。

JWT 至少携带 userId、tenantId、角色、tokenVersion 和唯一 tokenId。退出登录、禁用用户、禁用租户和修改关键权限时，通过 Redis 撤销会话或提升 tokenVersion，使已有令牌失效。

## 10. 业务模块

### 10.1 会员与教练

- Member 保存会员编号、状态、联系方式、目标、经验、偏好、最近签到等业务信息。
- MemberProfile 保存体测、健康提示和训练限制。
- Coach 保存简介、专长、证书、年限和状态。
- CoachMemberAssignment 明确教练负责的会员关系，教练只能访问有效分配关系中的会员。

### 10.2 课程、预约与签到

- Course 保存教练、容量、时段、地点、类型和状态。
- CourseBooking 状态为 `PENDING`、`CONFIRMED`、`CANCELLED`、`COMPLETED`、`NO_SHOW`。
- CheckIn 保存会员、课程、时间、类型和操作人。
- 预约容量使用数据库约束与带版本的并发控制，禁止超卖。
- 取消时限、签到窗口和自动爽约规则由租户设置控制。

### 10.3 套餐、订单与缴费记录

- MembershipPackage 定义次数、有效期和适用课程。
- MemberMembership 记录会员已购套餐及剩余权益。
- Order 记录应收金额、状态和业务来源。
- PaymentRecord 记录线下或人工确认的缴费事实、方式和凭证编号。
- 系统不连接支付网关，不保存银行卡信息，不发起真实支付。

### 10.4 动作与视频

- Exercise 与 ExerciseVideo 为一对多关系。
- 动作模板在租户创建时复制，之后只属于该租户，不存在运行时全局共享动作。
- 第三方视频只保存合法 URL、平台、作者和允许使用的缩略图。
- 租户自有视频可以上传 MinIO。

### 10.5 训练

- WorkoutPlan、WorkoutPlanItem 使用规范化关系，不把计划主体存成不可查询的大 JSON。
- WorkoutRecord 与 WorkoutSet 记录实际训练完成情况。
- AI 计划先生成结构化草案，经过动作 ID、组数、次数、休息、日期、会员限制和权限校验后，才能由应用服务保存。

### 10.6 营养

- NutritionProfile、NutritionFood、NutritionPlan、NutritionMeal 和 NutritionRecord 构成营养模块。
- BMI、BMR、TDEE、宏量营养等计算由确定性 Java 领域服务完成。
- AI 只能解释结果和生成建议，不能改变计算公式或伪造营养数据。

### 10.7 分析、预测和推荐

- Dashboard 指标全部由业务数据确定性聚合。
- AI 周报只总结已计算指标。
- 流失风险先使用可解释规则基线，再由 AI 生成原因说明和触达建议。
- 推荐系统以会员目标、限制、历史和动作标签生成候选，AI 仅参与解释和排序增强。
- 所有风险和推荐输出都标明辅助性质，不能作为唯一经营决策。

## 11. RAG 知识库

### 11.1 固定技术路线

RAG 必须同时使用：

- Elasticsearch：中文全文检索、关键词匹配和元数据过滤。
- Milvus：Embedding 向量相似度检索。

不使用 pgvector。MySQL 是文档元数据、Chunk 主记录和索引任务的唯一事实源；MinIO 保存原文件；Redis 只做缓存和短租约。

### 11.2 可见范围

知识可见范围只有两级：

- `TENANT`：当前健身房内共享。
- `PRIVATE_USER`：仅上传者本人可见。

查询条件恒为当前 tenantId，并进一步满足租户共享或当前用户私有。不存在门店共享、跨租户共享或平台管理员正文访问。

### 11.3 摄取流程

```text
文件上传
  -> MinIO 保存原件
  -> MySQL 创建 Document 与持久化 IngestionJob
  -> @Async 工作线程领取任务
  -> 文件解析与文本清洗
  -> 确定性分块
  -> 百炼 Embedding
  -> Elasticsearch bulk upsert
  -> Milvus bulk upsert
  -> 双索引校验
  -> 发布当前 generation 并标记 READY
```

文档状态包括 `UPLOADING`、`QUEUED`、`PARSING`、`CHUNKING`、`EMBEDDING`、`INDEXING`、`READY`、`FAILED`、`DELETE_PENDING` 和 `DELETED`。

任务表记录阶段、进度、尝试次数、错误码、下次重试时间、租约和 generation。应用启动时扫描未完成任务恢复处理，不依赖新增消息队列。

Chunk 使用基于 tenantId、documentId、generation 和 chunkIndex 的确定性 ID。ES 与 Milvus 使用相同 Chunk ID 幂等 upsert。新 generation 的两侧索引均校验成功后才切换可见版本；失败时旧版本继续可用。

### 11.4 检索流程

```text
问题规范化
  -> 权限与意图判断
  -> ES 与 Milvus 并行召回
  -> 按 Chunk ID 去重
  -> RRF 融合
  -> 百炼 Rerank
  -> MySQL 对租户、可见范围、状态和 generation 最终复核
  -> 上下文压缩与 Token 预算
  -> LLM 生成带引用的回答
```

默认候选为 ES Top 30 与 Milvus Top 30，RRF 常数 60，融合后 Top 20 进入重排，最终提供 5 至 8 个片段，每个文档最多 2 个片段。所有参数可配置并记录到实验日志。

### 11.5 删除、重建和降级

- 删除先在 MySQL 写 tombstone，使检索复核立即屏蔽，再异步清理 ES、Milvus、MinIO 和 Chunk。
- 重建创建 generation+1，不原地覆盖；验证通过后切换当前 generation。
- Embedding 或 Milvus 失败时允许 ES-only，并标记降级。
- ES 失败时允许 Milvus-only，正文从 MySQL 补充，并标记降级。
- Redis 失败时绕过缓存。
- ES 与 Milvus 均失败时，知识搜索返回明确的 503；AI 对话只能明确说明没有知识库依据后给出通用建议。
- MySQL 或权限复核失败时禁止降级，直接拒绝请求。

缓存键必须包含 schemaVersion、tenantId、userId 或可见范围、queryHash、corpusGeneration、embeddingModel、检索参数版本和 promptVersion，禁止仅按问题文本缓存。

## 12. 百炼模型配置

兼容接口基础地址：`https://dashscope.aliyuncs.com/compatible-mode/v1`

- 主聊天和结构化生成：`qwen3.7-plus`
- 低成本意图分类：`qwen3.8-flash`
- Embedding：`qwen3.7-text-embedding`
- 向量维度：1024
- Rerank：`qwen3.7-text-rerank`

Chat、Embedding 和 Rerank 使用独立配置项、超时、重试和熔断策略。Milvus collection 固定校验 1024 维；模型或维度改变时必须创建新 collection 并重建索引。

API Key 只能通过 `.env` 或进程环境变量 `DASHSCOPE_API_KEY` 注入，不得出现在源码、测试样例、日志、异常信息、设计文档或 Git 历史中。由于用户曾在会话中发送密钥，项目配置完成后应在百炼控制台轮换该密钥。

## 13. AI 服务与 MCP

AI 调用链统一为：

```text
Controller
  -> AI Application Service
  -> Intent / Business Data / Authorized Tools
  -> RAG / Context Builder
  -> Model Gateway
  -> Structured Response Parser
  -> Business Validator
  -> Application Service 持久化或返回
```

AI 功能包含对话、私教助手、训练计划、会员分析、运营分析、周报、长期记忆、流失解释和推荐说明。

长期记忆只保存经过用户行为或明确确认产生的结构化事实，不保存完整无限聊天历史。Prompt 每次只检索与当前问题相关的摘要记忆。

MCP 作为同一进程中的受控工具接口提供动作、视频、知识库、会员资料、训练历史和统计查询。MCP 不获得超级权限，每次调用都复用正常 API 的租户、角色和资源关系检查。

## 14. API 约定

- 基础路径：`/api/v1`
- 分页：`page` 从 0 开始，`size` 有最大值限制。
- 时间：服务端存 UTC，API 返回 ISO-8601，前端负责本地化显示。
- 金额：使用十进制定点数和明确币种，不使用浮点数。
- 响应：统一 success、code、message、data、traceId 和 timestamp。
- 错误：使用正确 HTTP 状态码，业务错误码稳定且可文档化。
- 幂等：上传、预约、缴费记录和 AI 计划保存支持幂等键。
- 版本：旧前端不做兼容，后续前端依据 OpenAPI 生成类型与客户端。

主要资源路径：

```text
/api/v1/auth
/api/v1/platform/tenants
/api/v1/tenant/settings
/api/v1/users
/api/v1/roles
/api/v1/members
/api/v1/coaches
/api/v1/courses
/api/v1/bookings
/api/v1/check-ins
/api/v1/membership-packages
/api/v1/memberships
/api/v1/orders
/api/v1/payment-records
/api/v1/exercises
/api/v1/videos
/api/v1/workout-plans
/api/v1/workout-records
/api/v1/nutrition
/api/v1/knowledge/documents
/api/v1/knowledge/search
/api/v1/ai
/api/v1/analytics
/api/v1/audit
/api/v1/mcp
```

## 15. 安全设计

- BCrypt 密码散列。
- JWT 访问令牌与可撤销刷新令牌。
- 数据库角色/权限与方法级授权。
- 租户、教练负责会员、会员本人三层资源授权。
- 请求参数、状态机和文件大小/类型校验。
- 上传文件使用魔数检测、随机对象键和隔离解析。
- 网页导入防 SSRF，只允许 HTTP/HTTPS、公网地址和受控重定向。
- RAG 内容视为不可信数据，系统提示与工具权限不能被文档内容覆盖。
- 日志脱敏，不记录密码、令牌、API Key、完整健康资料或知识正文。
- 管理修改、身份变更、缴费记录和 AI 工具调用均写操作审计。
- 登录、AI、上传和搜索接口应用租户与用户维度限流。

## 16. 测试策略

所有功能和修复遵循测试先行。最低测试层级：

1. 领域单元测试：状态机、金额、营养公式、训练计划校验、风险规则和推荐候选。
2. Repository 测试：tenantId 条件、唯一约束、分页和并发锁。
3. Security 测试：四类角色允许/拒绝矩阵、跨租户 IDOR、教练负责关系和会员本人边界。
4. Controller 契约测试：状态码、统一响应、校验错误和 OpenAPI。
5. MySQL 集成测试：Testcontainers，不使用 H2 替代 MySQL 语义。
6. RAG 集成测试：ES/Milvus/MinIO/Redis 容器验证租户过滤、混合召回、删除和重建。
7. AI 网关测试：使用 MockWebServer 验证模型、维度、超时、重试、Token 日志和结构化输出，不在测试中调用付费 API。
8. 启动冒烟测试：最小配置启动、健康检查、注册租户、登录和核心 API。

完成标准包括：构建通过、全部测试通过、无密钥进入 Git、OpenAPI 可访问、`gymmind` 新库可初始化、基础设施健康、后端健康检查通过，以及至少一个真实百炼 Chat/Embedding/Rerank 的人工集成验证。

## 17. 实施与 Git 规则

重建在当前 `backend` 目录中进行，以需求清单为准，不保留旧 API 兼容层。旧代码可从 `bfd5c40` 恢复。

每个可独立审查的小功能必须单独提交：

- 先写能正确失败的测试。
- 实现最小代码使测试通过。
- 运行该功能测试与受影响的回归测试。
- 只暂存本功能明确涉及的文件，禁止 `git add .`。
- 使用 `feat:`、`fix:`、`test:`、`refactor:`、`docs:` 或 `chore:` 中文提交信息。
- 不把多个无关功能捆绑到一个提交。
- 不自动推送远程仓库，除非用户明确要求。

所有新增代码注释使用中文，但只在业务规则或非直观实现处添加，避免逐行复述代码。

## 18. 实施顺序

1. 保存旧后端基线和设计/计划。
2. 重建 Spring Boot 工程、配置、健康检查和测试基线。
3. 安全重置 `gymmind` 数据库并建立 Hibernate 模型。
4. 实现 Tenant、IAM、JWT、RBAC、邀请与隔离测试。
5. 实现会员、教练、课程、预约、签到。
6. 实现套餐、订单和缴费记录。
7. 实现动作、视频、训练和营养。
8. 实现 Dashboard、周报和审计。
9. 实现知识库摄取、ES+Milvus 索引、检索、删除和重建。
10. 接入百炼 Chat、Embedding、Rerank、AI 用量与长期记忆。
11. 实现 AI 训练计划、私教、会员/运营分析、流失风险和推荐。
12. 实现网页搜索/导入和受控 MCP 工具。
13. 完善启动脚本、完整集成测试、OpenAPI 和验收文档。

每个阶段结束时系统都必须保持可构建、可测试；需要外部中间件的测试通过 profile 与 Testcontainers 隔离，不能让日常单元测试因 Docker 未启动而全部失败。

## 19. 验收标准

- 新检出项目使用 Java 17 和项目说明可完成构建。
- MySQL `gymmind` 可从空库生成完整结构并幂等初始化基础数据。
- 多个租户可独立注册并配置，任何租户均无法读取另一租户的数据。
- 平台管理员无法读取租户会员明细或知识正文。
- 会员、教练、课程、预约、签到、套餐、记录、训练和营养形成完整业务闭环。
- Dashboard 指标可以从业务数据复算。
- 文档能完成上传、解析、分块、ES 与 Milvus 索引、混合检索、删除和重建。
- RAG 结果严格受租户与个人可见范围约束，并返回来源引用和降级状态。
- 百炼四类模型配置各司其职，1024 维向量经过校验。
- AI 结构化输出未经业务校验不能保存。
- 长期记忆、风险预测、推荐和 MCP 均执行相同授权规则。
- 关键操作与 AI 用量可审计，日志不泄露密钥或敏感正文。
- 每个独立小功能都有对应测试、验证证据和 Git 提交。

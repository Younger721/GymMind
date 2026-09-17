# FitSaaS 智能健身房管理与运营平台

> 基于 SaaS、多租户、大语言模型与 RAG 的智能健身房管理与会员服务 Web 系统

## 1. 项目定位

FitSaaS 是一个面向健身房、健身工作室及其工作人员的 SaaS Web 平台。

系统在会员管理、教练管理、课程预约、训练记录、健身动作、教学视频和经营数据的基础上，引入：

- 大语言模型（LLM）
- RAG 私有知识库
- AI 会员分析
- AI 私教助手
- AI 训练计划生成
- AI 运营分析
- 健身动作/教学视频资源
- 数据可视化
- SaaS 多租户架构

核心原则：

> 传统业务负责确定性数据和流程，AI 负责理解、分析、推荐和生成。

---

## 2. 用户角色

| 角色 | 核心职责 |
|---|---|
| 平台管理员 | 管理 SaaS 平台、租户和系统配置 |
| 健身房管理员 | 管理本健身房会员、教练、课程、套餐、订单和经营数据 |
| 教练 | 管理会员、训练计划、训练记录和预约 |
| 会员 | 查看课程、预约、训练计划、训练记录和 AI 助手 |

### 多租户关系

```text
SaaS Platform
│
├── Gym A
│   ├── Admin
│   ├── Coaches
│   └── Members
│
├── Gym B
│   ├── Admin
│   ├── Coaches
│   └── Members
│
└── Gym C
    ├── Admin
    ├── Coaches
    └── Members
```

核心业务表统一包含 `tenant_id`，实现租户数据隔离。

---

# 3. 总体功能架构

```text
                         FitSaaS
                            │
       ┌────────────────────┼────────────────────┐
       │                    │                    │
   SaaS基础能力          健身业务             AI智能能力
       │                    │                    │
   多租户                  会员管理             AI健身助手
   RBAC                    教练管理             RAG知识库
   JWT                     课程管理             AI会员分析
   数据隔离                套餐管理             AI私教助手
   操作日志                预约签到             AI训练计划
                           训练记录             AI运营分析
                           动作库               AI周报
                           视频库
                           营养管理
       │                    │                    │
       └────────────────────┼────────────────────┘
                            │
                       数据分析平台
                            │
                     Dashboard / ECharts
```

---

# 4. 核心功能

## 4.1 SaaS 平台管理

平台管理员：

- 登录
- 租户管理
- 创建健身房
- 启用/禁用租户
- 平台用户管理
- 系统配置
- 操作日志
- AI 使用量统计
- Token 使用量统计

租户核心字段：

```text
Tenant
├── id
├── name
├── logo
├── address
├── phone
├── status
├── plan
├── created_at
└── updated_at
```

---

## 4.2 用户与 RBAC

角色：

```text
PLATFORM_ADMIN
GYM_ADMIN
COACH
MEMBER
```

权限示例：

```text
member:view
member:create
member:update
member:delete

coach:view
coach:create
coach:update

course:view
course:create
course:book

workout:view
workout:create

ai:chat
ai:plan
ai:analysis
```

推荐权限关系：

```text
User → Role → Permission
```

---

## 4.3 会员管理

支持：

- 添加/编辑/删除会员
- 会员搜索
- 会员详情
- 会员状态
- 训练记录
- 预约记录
- 购买套餐
- 活跃度
- AI 会员分析

会员信息：

```text
Member
├── 基本信息
├── 健身目标
├── 健身经验
├── 训练偏好
├── 当前套餐
├── 私教教练
├── 最近签到
└── 会员状态
```

---

## 4.4 教练管理

支持：

- 添加教练
- 编辑教练
- 教练详情
- 擅长方向
- 教练简介
- 预约时间
- 教练课程
- 教练会员
- 教练业绩

```text
Coach
├── id
├── user_id
├── tenant_id
├── specialty
├── introduction
├── experience_years
├── certificate
└── status
```

---

## 4.5 课程管理

课程类型：

- 团体课
- 私教课
- 体验课
- 训练课程

```text
Course
├── id
├── tenant_id
├── coach_id
├── name
├── type
├── description
├── capacity
├── duration
├── start_time
├── end_time
├── location
└── status
```

支持：

- 创建课程
- 修改课程
- 删除课程
- 课程预约
- 取消预约
- 人数限制
- 签到

---

## 4.6 预约与签到

预约状态：

```text
PENDING
CONFIRMED
CANCELLED
COMPLETED
NO_SHOW
```

签到记录：

```text
CheckIn
├── member_id
├── tenant_id
├── course_id
├── check_in_time
└── type
```

这些数据用于后续会员活跃度、流失风险和运营分析。

---

# 5. 健身动作与视频资源

## 5.1 动作库

```text
Exercise
├── id
├── name
├── category
├── target_muscle
├── difficulty
├── equipment
├── description
├── steps
├── common_mistakes
├── safety_notes
└── tags
```

分类：

- 胸部
- 背部
- 肩部
- 手臂
- 腿部
- 臀部
- 核心
- 有氧
- 拉伸

---

## 5.2 健身视频库

视频功能定位：

> 健身动作教学视频资源库，而不是视频网站。

```text
Exercise
   │
   ├── Video 1
   ├── Video 2
   └── Video 3
```

```text
ExerciseVideo
├── id
├── exercise_id
├── title
├── platform
├── video_url
├── thumbnail_url
├── author
├── duration
├── description
├── source_type
└── created_at
```

支持：

```text
YouTube
Bilibili
Official
Internal
Other
```

第三方视频建议只保存：

- 标题
- 原始 URL
- 平台
- 作者
- 缩略图（在允许的情况下）
- 动作关联

点击后跳转原平台。

不要未经授权下载或重新托管第三方视频；健身房自有版权视频可以上传对象存储。

---

# 6. 训练计划与训练记录

## 6.1 训练计划

```text
WorkoutPlan
├── id
├── tenant_id
├── member_id
├── coach_id
├── name
├── goal
├── start_date
├── end_date
├── status
└── description
```

计划项目：

```text
WorkoutPlanItem
├── plan_id
├── exercise_id
├── day_of_week
├── sets
├── reps
├── weight
├── rest_seconds
└── notes
```

## 6.2 训练记录

```text
WorkoutRecord
├── id
├── member_id
├── plan_id
├── workout_date
├── duration
├── calories
├── notes
└── status
```

```text
WorkoutSet
├── record_id
├── exercise_id
├── set_number
├── weight
├── reps
├── duration
└── completed
```

统计：

- 训练次数
- 每周训练频率
- 总训练量
- 动作次数
- 重量变化
- 训练时长
- 连续训练天数

---

# 7. 营养管理

## 营养档案

```text
NutritionProfile
├── member_id
├── goal
├── calories_target
├── protein_target
├── carbohydrate_target
├── fat_target
└── notes
```

## 食物数据库

```text
NutritionFood
├── id
├── name
├── calories
├── protein
├── carbohydrate
├── fat
└── serving_size
```

## 营养计划

```text
NutritionPlan
├── id
├── member_id
├── calories_target
├── protein_target
├── carbohydrate_target
├── fat_target
└── description
```

AI 可以生成饮食建议，但热量、营养值等确定性计算由后端完成。

---

# 8. RAG 健身知识库

## 8.1 知识来源

支持：

- PDF
- Word
- Markdown
- TXT
- 网页文章
- 健身房内部资料
- 教练上传资料

## 8.2 数据流程

```text
文件上传
   ↓
Document
   ↓
文本解析
   ↓
文本清洗
   ↓
Chunk
   ↓
Embedding
   ↓
pgvector
```

## 8.3 RAG 查询

```text
用户问题
   ↓
Intent 判断
   ↓
Metadata Filter
   ↓
Vector Search
   ↓
Top K
   ↓
Rerank
   ↓
Context Compression
   ↓
LLM
   ↓
答案
```

知识库文档状态：

```text
UPLOADING
PARSING
CHUNKING
EMBEDDING
COMPLETED
FAILED
```

文档解析和向量化使用异步任务，避免阻塞 HTTP 请求。

---

# 9. Web 健身知识搜索

系统可以接入外部搜索服务。

用户搜索：

> 新手力量训练注意事项

返回：

```text
搜索结果
├── Article A
├── Article B
├── Article C
├── Article D
└── Article E
```

结果包含：

- 标题
- 来源
- 作者
- 发布时间
- 摘要
- URL

用户可以：

```text
查看原文
收藏
加入知识库
```

---

# 10. 文章导入知识库

```text
Web Search
    ↓
搜索结果
    ↓
用户选择文章
    ↓
Fetch Article
    ↓
HTML Cleaning
    ↓
Text Extraction
    ↓
Chunk
    ↓
Embedding
    ↓
Vector DB
    ↓
健身房知识库
```

注意：

- 遵守网站 Terms、robots 和访问限制
- 优先使用公开 API
- 不绕过登录、付费墙或访问控制
- 不批量抓取无授权内容

---

# 11. AI 健身助手

会员可以使用：

> AI Fitness Assistant

支持：

### 知识问答

> 什么是渐进式超负荷？

### 训练建议

> 今天应该练什么？

### 动作查询

> 如何正确完成卧推？

### 视频推荐

> 给我推荐卧推教学视频。

### 计划解释

> 为什么今天安排这几个动作？

---

# 12. AI 私教助手

教练输入：

> 这个会员最近训练频率比较低，帮我设计一个恢复训练方案。

AI 获取：

```text
会员资料
+
历史训练记录
+
训练目标
+
动作数据库
+
RAG 健身知识
```

生成训练建议。

推荐架构：

```text
Coach AI
   ↓
MemberService
   ↓
WorkoutHistoryService
   ↓
ExerciseService
   ↓
RagService
   ↓
LLM
   ↓
Structured Workout Plan
```

---

# 13. AI 训练计划生成

完整流程：

```text
用户请求
   ↓
获取会员资料
   ↓
获取历史训练
   ↓
获取训练目标
   ↓
查询动作
   ↓
RAG 查询知识
   ↓
LLM 生成结构化方案
   ↓
后端校验
   ↓
保存 WorkoutPlan
```

AI 不直接写数据库。

后端校验：

- exerciseId
- sets
- reps
- restSeconds
- 日期
- 用户权限
- 业务规则

---

# 14. AI 会员分析

会员详情页面可以展示：

```text
最近 30 天
签到次数：4
上月签到：11
最近签到：18 天前
训练频率：下降
```

AI 分析：

```text
会员活跃度：低

可能存在流失风险。

主要变化：
1. 最近训练频率下降
2. 长时间未签到
3. 近期预约取消增加

建议：
1. 主动联系会员
2. 推荐低门槛恢复训练
3. 推荐近期适合的课程
```

AI 是辅助判断，不应作为唯一的经营决策依据。

---

# 15. AI 运营分析

管理员可以询问：

> 为什么这个月会员续费率下降？

后端先统计：

```text
会员数据
+
签到数据
+
预约数据
+
订单数据
+
套餐数据
```

再交给 LLM 分析。

AI 输出：

```text
本月续费率较上月下降 7.2%。

主要变化：
1. 新会员留存下降
2. 私教预约取消率上升
3. 部分会员连续超过 14 天未签到

建议：
1. 对低活跃会员进行主动触达
2. 为新会员提供入门训练计划
3. 针对高风险会员推荐体验课程
```

---

# 16. Dashboard

管理员：

```text
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│ 会员总数 │ │ 活跃会员 │ │ 本月收入 │ │ 续费率   │
│  1286    │ │   824    │ │ ¥86,400  │ │ 78.6%    │
└──────────┘ └──────────┘ └──────────┘ └──────────┘

会员增长趋势
────────────────────

课程预约统计
────────────────────

会员活跃度
────────────────────

AI 运营分析
────────────────────
```

ECharts：

- 会员增长
- 收入趋势
- 签到趋势
- 课程预约
- 教练业绩
- 会员活跃度
- 流失趋势
- 训练次数
- 套餐销售

---

# 17. AI 周报

每周自动生成：

```text
健身房运营周报

会员数
新增会员
流失会员
活跃会员
签到次数
课程预约
私教课程
收入
```

AI 自动总结：

- 本周经营情况
- 会员活跃变化
- 课程情况
- 收入变化
- 需要关注的问题
- 下周建议

---

# 18. AI 长期记忆

不要把完整聊天历史全部放入 Prompt。

使用：

```text
UserMemory
├── member_id
├── key
├── value
├── source
└── updated_at
```

例如：

```text
goal = muscle_gain
preferred_training = strength
preferred_time = evening
fitness_level = beginner
```

每次只检索和当前问题相关的记忆。

---

# 19. AI Tool Calling

使用 Spring AI Tool Calling 时可以封装：

```text
searchFitnessArticles()
searchExercises()
searchVideos()
searchKnowledgeBase()
getMemberProfile()
getWorkoutHistory()
getMemberStatistics()
generateWorkoutPlan()
```

LLM 只调用必要工具。

如果后期需要，可以增加 MCP：

```text
Fitness MCP Server

Tools:
├── search_exercise
├── search_video
├── search_article
├── search_knowledge
├── get_member_profile
├── get_workout_history
└── get_statistics
```

MCP 建议作为 P2 功能，不要影响 MVP 进度。

---

# 20. Token 低成本设计

核心原则：

> 不让 LLM 负责所有事情。

推荐：

```text
用户问题
   ↓
Intent
   ↓
结构化数据库查询
   ↓
Metadata Filter
   ↓
Vector Retrieval
   ↓
Top K
   ↓
Rerank
   ↓
Context Compression
   ↓
LLM
```

不推荐：

```text
用户问题
 ↓
LLM
 ↓
Search
 ↓
LLM
 ↓
Search
 ↓
LLM
```

推荐：

```text
用户问题
 ↓
后端判断是否需要搜索
 ↓
一次 Search
 ↓
后端过滤 Top 5~10
 ↓
一次 LLM 总结
```

其他优化：

- Redis Cache
- 搜索结果缓存
- Embedding 缓存
- Prompt 模板
- 摘要记忆
- 小模型做分类/提取
- 强模型处理复杂任务
- 记录 Token 与延迟

---

# 21. 技术栈

## 后端

```text
Java 17+
Spring Boot 3.x
Spring Web
Spring Security
Spring Data JPA
Spring AI
Maven
Lombok
Validation
Jackson
```

## 前端

```text
Vue 3
Vite
TypeScript
Element Plus
Pinia
Vue Router
Axios
ECharts
```

## 数据库

```text
PostgreSQL
pgvector
Redis
```

## 文件存储

```text
MinIO
```

## 文档解析

```text
Apache PDFBox
Apache POI
Jsoup
```

---

# 22. 后端目录结构

```text
fitsaas-backend
├── controller
├── service
├── repository
├── entity
├── dto
├── vo
├── mapper
├── security
├── config
├── exception
├── common
├── tenant
├── member
├── coach
├── course
├── workout
├── nutrition
├── knowledge
├── video
├── analytics
├── ai
│   ├── chat
│   ├── rag
│   ├── tools
│   ├── memory
│   └── prompt
└── FitSaasApplication.java
```

---

# 23. 前端目录结构

```text
fitsaas-web
├── src
│   ├── api
│   ├── assets
│   ├── components
│   ├── layouts
│   ├── router
│   ├── stores
│   ├── types
│   ├── utils
│   ├── views
│   │   ├── auth
│   │   ├── dashboard
│   │   ├── member
│   │   ├── coach
│   │   ├── course
│   │   ├── workout
│   │   ├── nutrition
│   │   ├── knowledge
│   │   ├── video
│   │   ├── ai
│   │   └── system
│   └── App.vue
└── package.json
```

---

# 24. 数据库核心表

```text
sys_tenant
sys_user
sys_role
sys_permission
sys_user_role
sys_role_permission

member
member_profile
coach

course
course_booking
check_in

membership_package
member_membership
order
payment

exercise
exercise_video

workout_plan
workout_plan_item
workout_record
workout_set

nutrition_profile
nutrition_food
nutrition_plan
nutrition_meal
nutrition_record

knowledge_document
knowledge_chunk
knowledge_category
knowledge_tag

conversation
conversation_message
user_memory
user_preference

web_article
search_history

ai_usage_log
weekly_report
operation_log
```

---

# 25. 关键数据关系

```text
Tenant
 │
 ├── User
 │    └── Role
 │
 ├── Member
 │    ├── WorkoutPlan
 │    │    └── WorkoutPlanItem
 │    ├── WorkoutRecord
 │    │    └── WorkoutSet
 │    ├── Booking
 │    └── NutritionProfile
 │
 ├── Coach
 │    └── Course
 │
 ├── Exercise
 │    └── ExerciseVideo
 │
 └── KnowledgeDocument
      └── KnowledgeChunk
```

---

# 26. API 设计

## Auth

```http
POST /api/auth/login
POST /api/auth/register
POST /api/auth/logout
GET  /api/auth/me
```

## Members

```http
GET    /api/members
GET    /api/members/{id}
POST   /api/members
PUT    /api/members/{id}
DELETE /api/members/{id}

GET /api/members/{id}/workouts
GET /api/members/{id}/bookings
GET /api/members/{id}/analysis
```

## Coaches

```http
GET    /api/coaches
POST   /api/coaches
PUT    /api/coaches/{id}
DELETE /api/coaches/{id}
```

## Courses

```http
GET  /api/courses
POST /api/courses
PUT  /api/courses/{id}
DELETE /api/courses/{id}

POST /api/courses/{id}/book
POST /api/courses/{id}/cancel
POST /api/courses/{id}/check-in
```

## Workout

```http
GET  /api/exercises
GET  /api/exercises/{id}

GET  /api/workout-plans
POST /api/workout-plans
POST /api/workout-plans/ai-generate

POST /api/workout-records
GET  /api/workout-records
```

## Video

```http
GET    /api/videos
GET    /api/videos/{id}
POST   /api/videos
PUT    /api/videos/{id}
DELETE /api/videos/{id}
```

## Knowledge

```http
GET    /api/knowledge/documents
POST   /api/knowledge/documents/upload
DELETE /api/knowledge/documents/{id}
POST   /api/knowledge/documents/{id}/reindex
GET    /api/knowledge/search
```

## Search

```http
GET /api/search/articles
GET /api/search/videos
```

## AI

```http
POST /api/ai/chat
POST /api/ai/workout-plan
POST /api/ai/member-analysis
POST /api/ai/operation-analysis
POST /api/ai/weekly-report
```

---

# 27. 页面规划

```text
/login
/register

/dashboard

/members
/members/:id

/coaches
/coaches/:id

/courses
/courses/:id

/workout
/workout/plans
/workout/records
/workout/exercises

/videos
/videos/:id

/nutrition

/knowledge
/knowledge/upload
/knowledge/:id

/search

/ai/assistant
/ai/member-analysis
/ai/operation-analysis

/reports

/settings
```

不同角色通过 RBAC 控制菜单和页面访问。

---

# 28. AI Service 架构

不要让 Controller 直接调用 LLM。

推荐：

```text
Controller
    ↓
AI Service
    ↓
Intent Service
    ↓
Business Data Service
    ↓
RAG Service / Tool Service
    ↓
Context Builder
    ↓
LLM Service
    ↓
Response Parser
    ↓
Business Validation
    ↓
VO
```

例如：

```text
WorkoutPlanController
        ↓
WorkoutPlanAIService
        ↓
MemberService
        ↓
WorkoutHistoryService
        ↓
ExerciseService
        ↓
RagService
        ↓
PromptBuilder
        ↓
ChatModel
        ↓
JSON Parser
        ↓
WorkoutPlanValidator
        ↓
WorkoutPlan
```

---

# 29. AI 输出结构化

不要让 AI 直接返回不可解析的大段文本。

训练计划示例：

```json
{
  "goal": "muscle_gain",
  "days": [
    {
      "day": "MONDAY",
      "exercises": [
        {
          "exerciseId": 1,
          "sets": 4,
          "reps": 10,
          "restSeconds": 90
        }
      ]
    }
  ]
}
```

后端进行：

- JSON Schema 校验
- exerciseId 校验
- 数值范围校验
- 日期校验
- 权限校验

再保存数据库。

---

# 30. 安全设计

必须实现：

- BCrypt 密码加密
- JWT
- RBAC
- Tenant 数据隔离
- 参数校验
- SQL 注入防护
- XSS 基础防护
- 文件类型校验
- 文件大小限制
- API 权限校验
- AI Tool 权限校验
- 操作日志

尤其注意：

> 一个租户不能读取另一个租户的会员、订单、训练记录和知识库。

---

# 31. AI 权限

```text
Member AI
    ↓
只能读取当前会员允许的数据

Coach AI
    ↓
只能读取自己负责的会员

Gym Admin AI
    ↓
只能读取当前 Tenant 数据

Platform Admin
    ↓
可以读取平台级统计
```

AI Tool 必须经过权限校验。

---

# 32. 异步任务

建议异步：

```text
PDF 解析
网页抓取
Embedding
批量向量化
AI 周报
复杂统计
```

第一版使用：

```java
@Async
```

即可。

不建议一开始引入 Kafka/RabbitMQ 等复杂基础设施。

---

# 33. Token 与 AI 使用日志

```text
AIUsageLog
├── user_id
├── tenant_id
├── model
├── request_type
├── input_tokens
├── output_tokens
├── total_tokens
├── latency
├── success
└── created_at
```

用于：

- AI 成本分析
- Token 优化
- 性能分析
- 毕设实验

---

# 34. 毕设创新点

## 创新点 1：SaaS 多租户

支持一个平台管理多个健身房，并实现租户级数据隔离。

## 创新点 2：RAG 健身知识库

将健身房内部资料、教练资料和合法获取的外部知识与 AI 结合。

## 创新点 3：业务数据 + RAG 融合

```text
会员数据
+
训练数据
+
预约数据
+
知识库
+
LLM
=
个性化 AI
```

## 创新点 4：AI 会员运营

根据签到、预约、训练和套餐等数据辅助分析会员活跃度和潜在流失风险。

## 创新点 5：AI + 视频资源

根据训练目标、动作、难度和会员情况推荐动作与教学视频。

## 创新点 6：低 Token RAG

研究：

```text
普通 RAG
vs
Metadata Filter + Rerank + Compression
```

比较：

- Token
- 延迟
- 回答质量
- 成本

---

# 35. 毕设实验

## Experiment 1：RAG 优化

Baseline：

```text
大量文档 → LLM
```

优化：

```text
Vector Retrieval
+
Rerank
+
Context Compression
```

比较：

- 输入 Token
- 输出 Token
- 延迟
- 回答准确率

## Experiment 2：记忆优化

```text
完整聊天历史
vs
Summary Memory
```

比较 Token 消耗。

## Experiment 3：个性化效果

```text
无会员信息
vs
加入会员资料 + 训练历史
```

比较 AI 训练计划质量。

## Experiment 4：AI 会员分析

使用模拟会员数据测试：

```text
高活跃
普通
低活跃
高流失风险
```

比较分析结果与规则基线。

---

# 36. MVP 范围

## P0：必须完成

```text
1. 登录注册
2. RBAC
3. 多租户
4. 会员管理
5. 教练管理
6. 课程管理
7. 预约签到
8. 健身动作库
9. 视频资源库
10. 训练计划
11. 训练记录
12. Dashboard
13. AI Chat
14. RAG 知识库
15. AI 训练计划
```

## P1：推荐完成

```text
16. AI 会员分析
17. AI 私教助手
18. 营养管理
19. Web 健身文章搜索
20. 文章导入知识库
21. AI 运营分析
22. AI 周报
23. Token 使用统计
```

## P2：有时间再做

```text
24. AI 长期记忆
25. MCP
26. 自动流失预测
27. 推荐系统
28. 多门店
29. SaaS 套餐/计费模拟
```

---

# 37. 推荐开发阶段

## Phase 1：基础架构

- Spring Boot
- Vue 3
- PostgreSQL
- Redis
- JWT
- Swagger/OpenAPI
- 统一异常处理
- 统一响应
- Layout

## Phase 2：SaaS 核心

- Tenant
- User
- Role
- Permission
- RBAC
- 多租户隔离

## Phase 3：健身业务

- Member
- Coach
- Course
- Booking
- CheckIn
- Exercise
- Video

## Phase 4：训练系统

- WorkoutPlan
- WorkoutRecord
- WorkoutSet
- Dashboard

## Phase 5：RAG

- 文件上传
- PDF/Word 解析
- Chunk
- Embedding
- pgvector
- Retrieval
- RAG Chat

## Phase 6：AI

- AI Chat
- AI 训练计划
- AI 会员分析
- AI 私教助手

## Phase 7：Web Search

- 文章搜索
- 视频搜索
- 文章详情
- 导入知识库

## Phase 8：分析与优化

- AI 周报
- Token 统计
- Redis Cache
- 异步任务
- Prompt 优化
- RAG 优化
- 性能测试

---

# 38. 最终业务闭环

```text
健身房管理员
      ↓
创建会员
      ↓
会员预约课程
      ↓
会员签到
      ↓
教练记录训练
      ↓
产生训练数据
      ↓
AI 分析会员
      ↓
AI 生成训练计划
      ↓
关联健身动作
      ↓
推荐教学视频
      ↓
会员完成训练
      ↓
产生新的训练数据
      ↓
再次 AI 分析
```

知识闭环：

```text
教练上传资料
      ↓
健身房知识库
      ↓
RAG
      ↓
AI 私教助手
      ↓
训练计划
      ↓
会员训练
```

最终形成：

> **业务数据 → 知识库 → AI → 业务应用 → 新业务数据**

---

# 39. 最终产品定位

**产品名：FitSaaS**

**中文名：智能健身房管理与运营平台**

**一句话介绍：**

> FitSaaS 是一个基于 SaaS 多租户架构、大语言模型和 RAG 技术构建的智能健身房管理与会员服务 Web 平台，为健身房提供会员管理、课程预约、训练管理、教学视频、知识库、AI 私教和智能运营分析等功能。

**核心技术关键词：**

```text
Spring Boot
Vue 3
PostgreSQL
pgvector
Redis
MinIO
Spring Security
Spring AI
RAG
LLM
Tool Calling
SaaS
Multi-Tenancy
RBAC
ECharts
```

---

# 40. 开发原则

1. 先完成传统业务，再加入 AI。
2. 使用模块化单体，不要一开始微服务化。
3. AI 不负责确定性计算。
4. AI 输出尽可能结构化。
5. RAG 只检索相关内容。
6. 第三方视频保存合法链接和元数据，不随意搬运。
7. 严格执行租户数据隔离。
8. 文档解析和向量化采用异步处理。
9. 记录 Token、延迟、错误率。
10. MCP、多 Agent 等高级能力放到 P2。
11. 先保证核心业务完整，再追求 AI 功能数量。
12. 论文重点围绕 SaaS 多租户、RAG、AI 个性化和 Token 优化展开。

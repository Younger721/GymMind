# 三大 AI 功能详细设计（2026-10-12）

## 1. RAG 健身知识助手

- **分类**：`KnowledgeCategory` 枚举映射文档 `category` 字段（8 类 + GENERAL）。
- **演示数据**：`dev` 配置下 `FitnessKnowledgeDemoInitializer` 按 `DEMO_FITNESS:{CATEGORY}` 幂等导入 classpath 文档。
- **问答路径**：Harness → `knowledge_search` → ES/Milvus → `KnowledgeAnswerFormatter` + 前端 citations。

## 2. AI 训练计划

- **接口**：`POST /api/v1/ai/workout-plan-drafts` 扩展 `goal`、`sessionsPerWeek`、`experience`、`includeHistory`。
- **Prompt**：`WorkoutPlanPromptComposer` 组装结构化字段与 `WorkoutHistorySummary`。
- **保存**：草案校验后 `POST /api/v1/workout-plans/drafts`。
- **范围修正（2026-10-12）**：仅 **会员** 角色 + `ai:plan` 可生成；教练/店长代生成已关闭。前端入口：`/training/ai-plan`（`PlanGeneratorView`）。

## 3. 会员活跃度与流失预警

- **列表**：`GET /api/v1/analytics/member-risks` 聚合签到、预约、完课、会籍。
- **评分**：`ChurnRiskCalculator` 基于 `ChurnSignals` 可解释规则（0～100 分）。
- **试算**：`POST /api/v1/analytics/churn-risks` 与列表使用同一信号模型。
- **范围修正（2026-10-12）**：仅 **店长（GYM_ADMIN）**；前端独立页隐藏，店长在 AI 助手通过 Function Call 工具 **`member_churn_risk_list`** 查库（与汇总工具 `gym_operations_metrics` 分工）。

# AI 回答路径路由 — 编码计划（P1）

> 设计基线：`2026-10-12-ai-answer-path-routing.md`（已通过）  
> 默认决策：L3 店长模糊问句 `HYBRID`；无知识 `DISCLOSE`；首期无 L2 LLM 分类。

## 文件清单

| 操作 | 路径 |
|------|------|
| 新增 | `ai/domain/AnswerPath.java` |
| 新增 | `ai/domain/RouteConfidence.java` |
| 新增 | `ai/domain/AnswerPathPlan.java` |
| 新增 | `ai/domain/AnswerPathRuleEngine.java` |
| 新增 | `ai/domain/AnswerPathRouter.java` |
| 修改 | `ai/harness/config/AiHarnessProperties.java` |
| 修改 | `ai/harness/application/DefaultAiHarnessService.java` |
| 修改 | `ai/harness/application/AiToolOrchestrator.java` |
| 新增 | `test/.../AnswerPathRouterTest.java` |
| 修改 | `test/.../AutomaticKnowledgeRetrievalTest.java` |
| 修改 | `test/.../AiToolOrchestratorTest.java`（preferredToolIds） |

## 任务拆解

1. 领域模型与规则引擎（L1 关键词 + L3 FALLBACK + clamp）。
2. 配置项 `routing-mode`、`empty-knowledge-policy`、`hybrid-on-ambiguous`。
3. Harness `chat` 接入 plan：条件 RAG、条件 NO_CONTEXT、动态 answerRules、route 进度事件。
4. Orchestrator 支持 `preferredToolIds` 过滤。
5. 单元测试与既有 Harness 测试调整。

## 不在 P1

- L2 LLM 分类、DB 字段、规则外置 YAML、前端改版。

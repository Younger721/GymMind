# GymMind 后端模块分层规则：编排 / 插件面 / 领域服务 / 平台底座

> 状态：已确认（维持现有顶级包结构，本文固化分层与依赖规则，新模块一律照此归位）
> 关联：[2026-10-30-ocr-agent-ready-design.md](./2026-10-30-ocr-agent-ready-design.md)、[2026-10-30-computer-use-agent-design.md](./2026-10-30-computer-use-agent-design.md)
> 日期：2026-10-30

## 1. 四层模型

| 层 | 包 | 职责 | 允许依赖 |
|---|---|---|---|
| **L2 编排层**（AI 大脑） | `ai`、`agent` | 对话编排、模型网关、会话与记忆、用量计量；决定"何时调谁、结果怎么用" | L1、L0、L-1 |
| **L1 插件面**（工具注册表） | `mcp` 工具注册、`ai.harness.capability.AiCapabilityRegistry`、HarnessPlugin | 把 L0 能力适配成 agent 可见的**工具签名**（`ocr_extract`、`knowledge_search`、`web_search`…），带权限与开关 | L0、L-1 |
| **L0 领域服务层**（能力实现） | `knowledge`、`ocr`、`search`、`member`、`workout`、`booking`、`course`、`membership`… | 业务能力的完整实现：REST、存储、配额、领域模型；**不知道 agent 的存在** | L-1 |
| **L-1 平台底座** | `shared`、`iam`、`tenancy`、`audit` | 租户隔离、身份权限、审计、通用错误与持久化基类 | 仅 L-1 |

核心心智：**agent 与 knowledge/ocr 的"级别差"由调用关系表达，不由包嵌套表达**。agent 只看见 L1 插件面上的工具名；L0 引擎住在自己的顶级包里。

## 2. 依赖铁律

1. **只准上层调下层**：L2→L1→L0→L-1，任何反向依赖（如 knowledge import agent）都是架构违例。
2. **L2 对 L0 只依赖端口**：编排层只能 import L0 模块暴露的接口（如 `KnowledgeSearchService`、`OcrService`），不得触碰其 infrastructure 实现（adapter/entity 不得出现在编排层 import 里）。
3. **agent 可见的 L0 能力必须在 L1 插件面注册工具**：一个能力若要让 agent 用，须在 `McpToolService` / `AiCapabilityRegistry` 注册，带权限码与 `TenantAgent.enabledTools` 开关；**禁止在编排服务里为某个业务能力写死私有调用旁路**。
   - 现状偏差：`DefaultAgentChatService` 直调 `KnowledgeSearchService` 属历史双轨，M3 工具化收敛时一并归位到插件面。
4. **L0 模块之间允许横向调用，但必须走对方的应用层接口**（如 `ocr` → `knowledge` 的 `KnowledgeObjectStore`/`KnowledgeDocumentUseCase`），禁止伸进对方 infrastructure。

## 3. 新能力放哪（放置决策树）

```text
新能力 X
├─ 只有编排层用、无自己的 REST/存储/配额（纯模型侧工具，如联网搜索 websearch）
│    → 嵌套在 ai/infrastructure/<x>            【websearch 即此类，无需迁移】
├─ 有独立领域面：自己的 REST、存储、配额，或被多个主人共享
│    （agent 要用、前端要用、其他模块也要用）
│    → 顶级领域模块 com.gymmind.<x>            【knowledge、ocr 即此类；
│                                                未来 computeruse 同此】
└─ 只是某个 L0 模块的内部实现细节
     → 该模块 infrastructure 子包              【knowledge 的 parser/storage 即此类】
```

**永远不要**：因为"它看起来很 AI"就把领域模块塞进 `agent/` 或 `ai/` 包——多主人能力挂到单一主人名下必然产生 `agent→knowledge→ocr→agent` 类依赖环。

## 4. AI 独立抽离的终局镜像

抽离那天，L2+L1 与"纯 AI 工具型 L0"整包搬走组成 `gymmind-agent` 服务，其余留在主服务经远程 MCP 调用：

| 模块 | 现层 | 抽离后 |
|---|---|---|
| `ai` / `agent` / `mcp` | L2 / L1 | **随 agent 服务搬迁** |
| `ocr`、`websearch`、未来 `computeruse` | L0（纯 AI 工具型） | **随 agent 服务搬迁** |
| `knowledge`、`search` | L0（有独立领域面） | 留主服务；agent 经远程 MCP 调用，工具签名不变 |
| 业务模块（member/workout/...）、`iam`/`tenancy`/`audit` | L0 / L-1 | 留主服务 |

这就是当前顶级包布局被称为"终局镜像"的原因：今天的一级包 = 明天的服务边界，抽离 = 搬包 + 加服务间鉴权。

## 5. 遗留说明

- 仓库根部的 `controller` / `dto` / `entity` / `repository` / `service` / `common` / `config` 等旧包为早期遗留代码：**冻结不新增**，触碰时顺手迁移到对应领域模块，不做专项重构。
- 可选后续（不做强制）：引入 ArchUnit 单测把 §2 的依赖方向固化为 `layeredArchitecture()` 规则，CI 阻断违例。

## 6. 规则速查（给 AI 助手与新人）

> 加新能力前问三个问题：它有自己的 REST/存储/配额吗？它被 agent 之外的主人用吗？它是某个模块的内部细节吗？
> 三个"是"→ 顶级领域模块；三个"否"→ `ai/infrastructure` 嵌套工具；中间态 → 该归属模块的 infrastructure。
> 然后无论放哪，只要 agent 要用，就去 L1 插件面注册工具——**包位置和插件身份是两件事。**

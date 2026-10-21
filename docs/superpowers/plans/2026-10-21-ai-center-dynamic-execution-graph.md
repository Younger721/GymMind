# AI 中心官网风格动态执行流程图实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 AI 中心流程图改成由真实 SSE 驱动、具有 AgentCore 官网点阵画布、节点状态过渡和连线流光效果的动态视图。

**Architecture:** 继续用 `HarnessProgressEvent` 列表作为唯一状态来源；工具的 `tool_start` / `tool_result` 更新同一节点。流程布局保持真实事件顺序的单链，CSS 负责节点入场和状态过渡，活动边显示移动光点，所有运行效果在减少动态偏好下关闭。

**Tech Stack:** Vue 3、TypeScript、原生 CSS、Node.js `node:test`、Vite。

## Global Constraints

- 复用已有 `HarnessProgressEvent` 和 SSE 协议，不改后端。
- 每个节点只反映真实系统事件；不得生成模拟 Agent、并行波次、依赖闸门或隐藏思维链。
- 节点按首次事件顺序保持单链；连线只表达时间顺序。
- 保留详情、折叠、错误状态、会话历史和局部横向滚动。
- 所有动画遵守 `prefers-reduced-motion: reduce`。
- 不添加运行时依赖。

---

### Task 1: 开始步骤的活动状态与回归测试

**Files:**
- Modify: `frontend/src/utils/aiExecutionGraph.ts`
- Test: `frontend/tests/aiExecutionGraph.test.mjs`

**Interfaces:**
- `projectExecutionGraph(steps)` 维持现有返回类型；对每条边，目标节点状态为 `STARTED` 或 `RUNNING` 时 `active: true`，终态时 `active: false`。
- `upsertExecutionStep` 继续用归一化后的 `executionStepId` 合并工具开始与结果事件。

- [x] **Step 1: 写开始态活动边测试**

```js
test('marks the incoming edge active while a tool has started but not finished', () => {
  const graph = projectExecutionGraph([
    { pluginId: 'answer_router', stage: 'route_decision', status: 'COMPLETED', message: '路由完成' },
    { pluginId: 'gym_operations_metrics', stage: 'tool_start', status: 'STARTED', message: '正在查询' }
  ])

  assert.equal(graph.edges[0].active, true)
})
```

- [x] **Step 2: 运行测试确认失败**

运行：`node --test frontend/tests/aiExecutionGraph.test.mjs`
预期：新增断言失败，因为现有投影只把 `RUNNING` 作为活动状态。

- [x] **Step 3: 最小修改投影函数**

```ts
active: target.status === 'STARTED' || target.status === 'RUNNING'
```

- [x] **Step 4: 重跑投影测试**

运行：`node --test frontend/tests/aiExecutionGraph.test.mjs`
预期：全部通过；工具 `tool_start` / `tool_result` 仍只投影一个最终节点。

### Task 2: 将流程图动效改成官网视觉语言

**Files:**
- Modify: `frontend/src/views/AssistantView.vue`
- Test: `frontend/tests/aiExecutionGraph.test.mjs`

**Interfaces:**
- 继续从 `projectExecutionGraph(msg.processSteps)` 读取图数据，详情键由 `executionStepId` 生成。
- 不新增演示计时器；状态只由实际 SSE 回调更新。

- [x] **Step 1: 写视图动效契约测试**

在现有 AI 中心模板契约测试中增加以下断言：

```js
assert.match(source, /process-node-enter/)
assert.match(source, /process-edge-particle/)
assert.match(source, /prefers-reduced-motion: reduce/)
```

- [x] **Step 2: 运行测试确认视觉契约失败**

运行：`node --test frontend/tests/aiExecutionGraph.test.mjs`
预期：缺少 `process-node-enter` 与 `process-edge-particle` 时测试失败。

- [x] **Step 3: 更新流程图 CSS 动效**

- 保留现有深色点阵背景，并提高微弱点阵在深色主题下的可见度。
- 给新建 `.process-node` 添加短暂淡入和轻微缩放；保留节点尺寸，避免入场改变滚动宽度。
- 给 `.process-node` 的状态边框、光晕和标记增加约 180ms 过渡，`STARTED` 与 `RUNNING` 都使用活动蓝色。
- 将 `.process-graph-edge.active::before` 改成名为 `process-edge-particle` 的小型发光光点，沿当前边从起点移动到终点；静止边保持中性灰，活动边显示蓝色底线。
- 在 `@media (prefers-reduced-motion: reduce)` 下停用节点入场与光点动画，并立即呈现终态样式。
- 不更改节点顺序、文案、服务端协议、权限逻辑或对话缓存格式。

- [x] **Step 4: 重跑定向测试**

运行：`node --test frontend/tests/aiExecutionGraph.test.mjs frontend/tests/sseStream.test.mjs`
预期：所有流程图与 SSE 测试通过。

- [x] **Step 5: 构建前端资源并检查页面服务（Vite 构建通过；HTTP 200；完整类型检查已知被知识库编辑器错误阻断）**

运行：`npx vite build`（工作目录 `frontend`）。
预期：Vite 构建成功。另运行 `Invoke-WebRequest http://127.0.0.1:5173/` 确认开发页可访问。
完整 `npm run build` 当前会在无关的 `KnowledgeDocEditorView.vue` 两处类型错误处失败；若仍失败，记录原始错误，不扩展本任务修改范围。

### Task 3: 检查并提交

**Files:**
- Commit only: `frontend/src/utils/aiExecutionGraph.ts`
- Commit only: `frontend/src/views/AssistantView.vue`
- Commit only: `frontend/tests/aiExecutionGraph.test.mjs`
- Commit only: `docs/superpowers/plans/2026-10-21-ai-center-dynamic-execution-graph.md`

- [x] **Step 1: 检查本次指定文件差异**

运行：`git diff --check` 和 `git diff -- frontend/src/utils/aiExecutionGraph.ts frontend/src/views/AssistantView.vue frontend/tests/aiExecutionGraph.test.mjs docs/superpowers/plans/2026-10-21-ai-center-dynamic-execution-graph.md`。
确认没有修改后端、添加依赖或夹带其他对话改动。

- [ ] **Step 2: 提交本计划涉及的实现文件**

```bash
git add frontend/src/utils/aiExecutionGraph.ts frontend/src/views/AssistantView.vue frontend/tests/aiExecutionGraph.test.mjs docs/superpowers/plans/2026-10-21-ai-center-dynamic-execution-graph.md
git commit -m "feat: 增强 AI 流程图动态效果"
```

- [ ] **Step 3: 核对提交和工作区**

运行：`git show --stat --oneline HEAD` 和 `git status --short`。确认提交只含当前流程图任务文件，并记录其他已有未提交内容仍在原处。

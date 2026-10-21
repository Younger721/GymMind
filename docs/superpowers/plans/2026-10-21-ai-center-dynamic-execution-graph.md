# AI 中心动态执行流程图实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 AI 中心当前的执行步骤列表改成由真实 SSE 事件驱动、可查看步骤详情的动态流程图。

**Architecture:** 新增一个无 UI 依赖的流程图投影函数，将已有进度事件按稳定键合并并投影为有序节点和连线；AssistantView 使用该投影渲染流程图，继续复用现有 SSE、历史持久化和详情折叠行为。

**Tech Stack:** Vue 3、TypeScript、原生 SVG/CSS、Node.js `node:test`。

## Global Constraints

- 每个节点代表真实 `HarnessProgressEvent`，不代表 Agent 或模型思维链。
- 节点与连线只表示首次收到事件的顺序，不暗示数据依赖或并行关系。
- 不改后端协议，不添加画布依赖。
- 保留流程折叠、节点详情、错误状态与历史回看。
- 移动端横向滚动限制在流程图区域内，不得使页面整体溢出。

---

### Task 1: 执行事件到图结构的投影

**Files:**
- Create: `frontend/src/utils/aiExecutionGraph.mjs`
- Create: `frontend/src/utils/aiExecutionGraph.mjs.d.ts`
- Test: `frontend/tests/aiExecutionGraph.test.mjs`

**Interfaces:**
- `upsertExecutionStep(steps, event)` 使用 `pluginId + stage` 定位节点；首次出现时追加，后续更新原位置。
- `projectExecutionGraph(steps)` 返回按步骤顺序排列的节点数组及相邻节点连线；RUNNING 节点前的连线标记为活动状态。

- [x] **Step 1: 写更新与投影测试**

```js
test('updates an existing stage in place and preserves event order', () => {
  const first = { pluginId: 'knowledge_search', stage: 'recall', status: 'STARTED', message: '开始' }
  const next = { pluginId: 'tool_a', stage: 'tool_start', status: 'RUNNING', message: '查询中' }
  const updated = { ...first, status: 'COMPLETED', message: '完成' }
  const steps = upsertExecutionStep(upsertExecutionStep([], first), next)
  const result = upsertExecutionStep(steps, updated)

  assert.deepEqual(result.map(step => step.message), ['完成', '查询中'])
  assert.deepEqual(projectExecutionGraph(result).edges, [
    { id: 'knowledge_search-recall->tool_a-tool_start', source: 'knowledge_search-recall', target: 'tool_a-tool_start', active: true }
  ])
})
```

- [x] **Step 2: 运行 `node --test tests/aiExecutionGraph.test.mjs` 确认因导出缺失而失败**
- [x] **Step 3: 实现最小的 upsert 和有序边投影函数**
- [x] **Step 4: 重跑该测试确认通过**

### Task 2: 将 AI 中心步骤卡片改为动态流程图

**Files:**
- Modify: `frontend/src/views/AssistantView.vue`
- Test: `frontend/tests/aiExecutionGraph.test.mjs`

**Interfaces:**
- 视图使用 Task 1 的 `projectExecutionGraph` 构造节点和相邻连线。
- 详情展开仍按既有 `processStepKey` 存取；事件回调使用 `upsertExecutionStep` 更新步骤。

- [x] **Step 1: 增加视图契约测试，验证执行过程使用图节点/连线容器并继续绑定 SSE 进度更新**
- [x] **Step 2: 运行 `node --test tests/aiExecutionGraph.test.mjs` 确认视图契约测试失败**
- [x] **Step 3: 替换纵向步骤列表为节点卡、顺序连接线和运行态样式；保留详情、折叠、持久化与错误路径**
- [x] **Step 4: 定向单测通过；完整 `npm run build` 被既有 KnowledgeDocEditorView 类型错误阻断**

### Task 3: 提交并检查工作区

**Files:**
- Commit only: `frontend/src/utils/aiExecutionGraph.mjs`
- Commit only: `frontend/src/utils/aiExecutionGraph.mjs.d.ts`
- Commit only: `frontend/tests/aiExecutionGraph.test.mjs`
- Commit only: `frontend/src/views/AssistantView.vue`

- [ ] **Step 1: 检查本次文件 diff 与 `git diff --check`**
- [ ] **Step 2: 运行定向单测及 Vite 构建，并记录完整构建阻断**
- [ ] **Step 3: 仅暂存上述四个功能文件并提交 `feat: 将 AI 执行过程改为动态流程图`**
- [ ] **Step 4: 检查提交内容及工作区，记录其他未提交改动仍保持原样**

import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { mergeExecutionHistory, projectExecutionGraph, upsertExecutionStep } from '../src/utils/aiExecutionGraph.ts'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

test('updates a stage in place and marks the incoming edge to a running step', () => {
  const first = { pluginId: 'knowledge_search', stage: 'recall', status: 'STARTED', message: '开始检索' }
  const second = { pluginId: 'gym_operations_metrics', stage: 'tool_start', status: 'RUNNING', message: '正在查询' }
  const updated = { ...first, status: 'COMPLETED', message: '检索完成' }
  const steps = upsertExecutionStep(upsertExecutionStep([], first), second)
  const graph = projectExecutionGraph(upsertExecutionStep(steps, updated))

  assert.deepEqual(graph.nodes.map(node => node.message), ['检索完成', '正在查询'])
  assert.deepEqual(graph.edges, [{
    id: 'knowledge_search:recall->gym_operations_metrics:tool_execution',
    source: 'knowledge_search:recall',
    target: 'gym_operations_metrics:tool_execution',
    reached: true,
    active: true
  }])
})

test('merges tool_start and tool_result into one completed query node', () => {
  const steps = upsertExecutionStep([], {
    pluginId: 'gym_operations_metrics', stage: 'tool_start', status: 'STARTED', message: '正在查询门店运营数据'
  })
  const updated = upsertExecutionStep(steps, {
    pluginId: 'gym_operations_metrics', stage: 'tool_result', status: 'COMPLETED', message: '查询完成', resultCount: 1
  })
  const graph = projectExecutionGraph(updated)

  assert.equal(graph.nodes.length, 1)
  assert.equal(graph.nodes[0].status, 'COMPLETED')
  assert.equal(graph.nodes[0].message, '查询完成')
  assert.equal(graph.nodes[0].resultCount, 1)
})

test('does not animate edges after all steps have settled', () => {
  const graph = projectExecutionGraph([
    { pluginId: 'answer_router', stage: 'route_decision', status: 'COMPLETED', message: '路由完成' },
    { pluginId: 'knowledge_search', stage: 'plugin_complete', status: 'FAILED', message: '检索失败' }
  ])

  assert.equal(graph.edges[0].active, false)
})

test('marks the incoming edge active while a tool has started but not finished', () => {
  const graph = projectExecutionGraph([
    { pluginId: 'answer_router', stage: 'route_decision', status: 'COMPLETED', message: '路由完成' },
    { pluginId: 'gym_operations_metrics', stage: 'tool_start', status: 'STARTED', message: '正在查询' }
  ])

  assert.equal(graph.edges[0].active, true)
})

test('settles stale active steps after a successful response so the graph cannot look busy', () => {
  const graph = projectExecutionGraph([
    { pluginId: 'answer_router', stage: 'route_decision', status: 'COMPLETED', message: '路由完成' },
    { pluginId: 'platform_ai_admin', stage: 'tool_start', status: 'STARTED', message: '正在查询平台 AI 管理数据' },
    { pluginId: 'platform_ai_admin', stage: 'plugin_complete', status: 'COMPLETED', message: '查询完成' }
  ], false)

  assert.equal(graph.nodes[1].status, 'COMPLETED')
  assert.equal(graph.nodes[1].message, '查询平台 AI 管理数据')
  assert.equal(graph.edges.some(edge => edge.active), false)
  assert.equal(graph.edges[0].reached, true)
})

test('settles a step that was still active when the response failed', () => {
  const graph = projectExecutionGraph([
    { pluginId: 'knowledge_search', stage: 'recall', status: 'RUNNING', message: '正在检索知识库' }
  ], false, true)

  assert.equal(graph.nodes[0].status, 'FAILED')
  assert.equal(graph.nodes[0].message, '正在检索知识库')
})

test('merges graph state onto matching server history in order, including repeated messages', () => {
  const steps = [{ pluginId: 'knowledge_search', stage: 'plugin_complete', status: 'COMPLETED', message: '检索完成' }]
  const messages = mergeExecutionHistory(
    [
      { role: 'assistant', content: '相同回答' },
      { role: 'assistant', content: '相同回答' }
    ],
    [
      { role: 'assistant', content: '相同回答', processSteps: steps },
      { role: 'assistant', content: '相同回答', failed: true }
    ]
  )

  assert.deepEqual(messages.map(message => [Boolean(message.processSteps), Boolean(message.failed)]), [[true, false], [false, true]])
})

test('AI center renders the graph while retaining SSE step details', () => {
  const source = fs.readFileSync(path.join(root, 'src/views/AssistantView.vue'), 'utf8')

  assert.match(source, /class="process-graph"/)
  assert.match(source, /process-node-enter/)
  assert.match(source, /process-edge-flow/)
  assert.match(source, /<svg[^>]*class="process-graph-edge/)
  assert.match(source, /class="process-edge-base/)
  assert.match(source, /class="process-edge-flow/)
  assert.match(source, /:class="\{ 'is-on': graph\.edges\[nodeIndex\]\.reached, active: graph\.edges\[nodeIndex\]\.active \}"/)
  assert.match(source, /stroke-dashoffset:-28px/)
  assert.match(source, /prefers-reduced-motion: reduce/)
  assert.match(source, /\.process-graph \{[^}]*flex-direction:column/)
  assert.doesNotMatch(source, /\.process-graph-edge::after/)
  assert.match(source, /\.process-node \{[^}]*background:#f8fafc/)
  assert.match(source, /\.process-node\.process-completed \{[^}]*background:#dbeafe/)
  assert.match(source, /\.process-node\.process-started, \.process-node\.process-running \{[^}]*background:#112744/)
  assert.match(source, /\.process-node\.process-running \.process-node-message/)
  assert.match(source, /background-color:#0c121c;\s*background-image:radial-gradient/)
  assert.match(source, /const processSummary = \(steps: HarnessProgressEvent\[\], isProcessing = false\)/)
  assert.match(source, /projectExecutionGraph\(msg\.processSteps, msg\.processActive === true, msg\.failed\)/)
  assert.match(source, /upsertExecutionStep\(message\.processSteps, progress\)/)
  assert.match(source, /const isNewStep = !message\.processSteps\.some/)
  assert.match(source, /sessionMessagesKey\(sessionId\.value\)/)
  assert.match(source, /process-details/)
})

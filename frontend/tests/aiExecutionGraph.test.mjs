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
    id: 'knowledge_search:recall->gym_operations_metrics:tool_start',
    source: 'knowledge_search:recall',
    target: 'gym_operations_metrics:tool_start',
    active: true
  }])
})

test('does not animate edges after all steps have settled', () => {
  const graph = projectExecutionGraph([
    { pluginId: 'answer_router', stage: 'route_decision', status: 'COMPLETED', message: '路由完成' },
    { pluginId: 'knowledge_search', stage: 'plugin_complete', status: 'FAILED', message: '检索失败' }
  ])

  assert.equal(graph.edges[0].active, false)
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
  assert.match(source, /projectExecutionGraph\(msg\.processSteps\)/)
  assert.match(source, /upsertExecutionStep\(message\.processSteps, progress\)/)
  assert.match(source, /const isNewStep = !message\.processSteps\.some/)
  assert.match(source, /sessionMessagesKey\(sessionId\.value\)/)
  assert.match(source, /process-details/)
})

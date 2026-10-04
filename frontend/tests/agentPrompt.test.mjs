import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const viewSource = fs.readFileSync(path.join(root, 'src/views/AgentManageView.vue'), 'utf8')
const apiSource = fs.readFileSync(path.join(root, 'src/api/agent.ts'), 'utf8')

test('agent management saves only the tenant prompt', () => {
  assert.match(apiSource, /updatePrompt\(/)
  assert.match(viewSource, /agentApi\.updatePrompt\(/)
  assert.doesNotMatch(viewSource, /agentApi\.create\(/)
  assert.doesNotMatch(viewSource, /agentApi\.disable\(/)
  assert.doesNotMatch(viewSource, /agentApi\.activate\(/)
  assert.match(viewSource, /systemPrompt: form\.systemPrompt\.trim\(\)/)
})

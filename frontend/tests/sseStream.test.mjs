import test from 'node:test'
import assert from 'node:assert/strict'
import { consumeSseStream } from '../src/utils/sseStream.ts'

test('stops reading after a done event even when the server closes abruptly', async () => {
  const encoder = new TextEncoder()
  let reads = 0
  let cancelled = false
  const reader = {
    async read() {
      reads += 1
      if (reads === 1) {
        return { value: encoder.encode('event:done\ndata:{"answer":"ok"}\n\n'), done: false }
      }
      throw new TypeError('terminated')
    },
    async cancel() {
      cancelled = true
    }
  }
  const events = []

  await consumeSseStream(reader, (event) => events.push(event))

  assert.deepEqual(events, [{ type: 'done', data: '{"answer":"ok"}' }])
  assert.equal(reads, 1)
  assert.equal(cancelled, true)
})

test('passes progress events to the SSE consumer before the final answer', async () => {
  const encoder = new TextEncoder()
  const reader = {
    async read() {
      return { value: encoder.encode('event:progress\ndata:{"pluginId":"knowledge_search","stage":"vector_recall","status":"RUNNING","message":"向量检索中"}\n\nevent:done\ndata:{"answer":"ok"}\n\n'), done: false }
    },
    async cancel() {}
  }
  const events = []

  await consumeSseStream(reader, event => events.push(event))

  assert.equal(events[0].type, 'progress')
  assert.equal(JSON.parse(events[0].data).message, '向量检索中')
  assert.equal(events[1].type, 'done')
})

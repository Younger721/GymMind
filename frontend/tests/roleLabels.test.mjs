import test from 'node:test'
import assert from 'node:assert/strict'

const { toDisplayRole } = await import('../src/utils/roleLabels.mjs')

test('converts built-in role names to Chinese while preserving role codes', () => {
  const role = toDisplayRole({ code: 'GYM_ADMIN', name: 'Gym administrator' })
  assert.deepEqual(role, { code: 'GYM_ADMIN', name: '健身房管理员' })
})

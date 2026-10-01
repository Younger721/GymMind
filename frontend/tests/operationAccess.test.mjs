import test from 'node:test'
import assert from 'node:assert/strict'
import { canUseOperation } from '../src/utils/operationAccess.ts'

test('operation requires its menu and allowed role', () => {
  assert.equal(canUseOperation(['menu:training'], ['GYM_ADMIN'], 'exercise:write', ['GYM_ADMIN']), false)
  assert.equal(canUseOperation(['menu:training', 'exercise:write'], ['COACH'], 'exercise:write', ['GYM_ADMIN']), false)
  assert.equal(canUseOperation(['menu:training', 'exercise:write'], ['GYM_ADMIN'], 'exercise:write', ['GYM_ADMIN']), true)
})

test('tenant operation access excludes platform administrators without tenant context', async () => {
  const access = await import('../src/utils/operationAccess.ts')
  assert.equal(typeof access.canUseTenantOperation, 'function')
  assert.equal(access.canUseTenantOperation(true, ['ai:plan'], ['PLATFORM_ADMIN'], 'ai:plan', ['GYM_ADMIN', 'COACH', 'MEMBER']), false)
  assert.equal(access.canUseTenantOperation(false, ['ai:plan'], ['COACH'], 'ai:plan', ['GYM_ADMIN', 'COACH', 'MEMBER']), true)
})

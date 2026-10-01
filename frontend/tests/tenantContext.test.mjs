import test from 'node:test'
import assert from 'node:assert/strict'
import { isTenantRequestCurrent } from '../src/utils/tenantContext.ts'

test('tenant settings response is current only for the active tenant and request version', () => {
  assert.equal(isTenantRequestCurrent(2, 2, 3, 3), true)
  assert.equal(isTenantRequestCurrent(1, 2, 3, 3), false)
  assert.equal(isTenantRequestCurrent(2, 2, 2, 3), false)
})

test('tenant settings cannot be saved before the active tenant has loaded', () => {
  assert.equal(isTenantRequestCurrent(null, 2, 0, 0), false)
})

import test from 'node:test'
import assert from 'node:assert/strict'
import { buildTenantCreatePayload, isTenantCreateFormValid } from '../src/utils/tenantCreate.mjs'

test('builds a platform tenant payload without a client tenant code', () => {
  const payload = buildTenantCreatePayload({
    tenantCode: 'client-supplied',
    tenantName: ' East Gym ',
    adminEmail: ' admin@example.com ',
    adminPassword: 'password123',
    adminDisplayName: ' Admin '
  })

  assert.deepEqual(payload, {
    tenantName: 'East Gym',
    adminEmail: 'admin@example.com',
    adminPassword: 'password123',
    adminDisplayName: 'Admin'
  })
  assert.equal('tenantCode' in payload, false)
})

test('requires administrator email and password', () => {
  assert.equal(isTenantCreateFormValid({ tenantName: 'Gym', adminDisplayName: 'Admin' }), false)
  assert.equal(isTenantCreateFormValid({
    tenantName: 'Gym',
    adminEmail: 'admin@example.com',
    adminPassword: 'password123',
    adminDisplayName: 'Admin'
  }), true)
})

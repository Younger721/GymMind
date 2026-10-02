import test from 'node:test'
import assert from 'node:assert/strict'
import { equipmentStatusLabel, equipmentStatusType, summarizeEquipmentInventory } from '../src/utils/equipmentPresentation.mjs'

test('maps equipment request statuses to Chinese labels and tag types', () => {
  assert.equal(equipmentStatusLabel('WAITING_RECEIVE'), '待确认')
  assert.equal(equipmentStatusType('PENDING'), 'warning')
  assert.equal(equipmentStatusType('REJECTED'), 'danger')
  assert.equal(equipmentStatusLabel('UNKNOWN'), 'UNKNOWN')
})

test('summarizes inventory quantities without mutating source rows', () => {
  const rows = [{ deliveredQuantity: 5, receivedQuantity: 4, usableQuantity: 4 }, { deliveredQuantity: 2, receivedQuantity: 1, usableQuantity: 1 }]
  assert.deepEqual(summarizeEquipmentInventory(rows), { deliveredQuantity: 7, receivedQuantity: 5, usableQuantity: 5 })
  assert.deepEqual(rows[0], { deliveredQuantity: 5, receivedQuantity: 4, usableQuantity: 4 })
})

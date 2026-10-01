import test from 'node:test'
import assert from 'node:assert/strict'
import { toLocalDateTime, toUtcIso } from '../src/utils/courseDateTime.mjs'

test('converts local course time to UTC and back', () => {
  const local = '2026-09-29T09:00:00'
  assert.equal(toLocalDateTime(toUtcIso(local)), local)
})

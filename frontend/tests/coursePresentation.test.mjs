import test from 'node:test'
import assert from 'node:assert/strict'
import {
  courseStatus,
  groupCoursesByTenant,
  groupBookingsByStatus,
  hasActiveBooking,
  bookingStatusLabel
} from '../src/utils/coursePresentation.mjs'

const NOW = new Date('2026-10-01T10:00:00.000Z')

const course = (overrides = {}) => ({
  id: 1,
  tenantId: 10,
  tenantName: '东城健身房',
  startsAt: '2026-10-01T12:00:00.000Z',
  endsAt: '2026-10-01T13:00:00.000Z',
  capacity: 10,
  reservedCount: 2,
  status: 'SCHEDULED',
  ...overrides
})

test('derives course states from lifecycle, time and member booking', () => {
  assert.equal(courseStatus(course(), [], NOW, 'MEMBER').label, '可报名')
  assert.equal(courseStatus(course(), [{ courseId: 1, status: 'CONFIRMED' }], NOW, 'MEMBER').label, '已报名')
  assert.equal(courseStatus(course({ startsAt: '2026-10-01T09:00:00.000Z', endsAt: '2026-10-01T11:00:00.000Z' }), [], NOW, 'GYM_ADMIN').label, '进行中')
  assert.equal(courseStatus(course({ startsAt: '2026-09-30T09:00:00.000Z', endsAt: '2026-09-30T11:00:00.000Z' }), [], NOW, 'MEMBER').label, '已结束')
  assert.equal(courseStatus(course({ status: 'CANCELLED' }), [], NOW, 'MEMBER').label, '已取消')
  assert.equal(courseStatus(course({ reservedCount: 10 }), [], NOW, 'GYM_ADMIN').label, '已满')
})

test('only non-cancelled bookings count as active member bookings', () => {
  assert.equal(hasActiveBooking(1, [{ courseId: 1, status: 'CANCELLED' }]), false)
  assert.equal(hasActiveBooking(1, [{ courseId: 1, status: 'CONFIRMED' }]), true)
})

test('groups courses into gym containers with display names', () => {
  const groups = groupCoursesByTenant([
    course({ id: 1, tenantId: 10, tenantName: '东城健身房' }),
    course({ id: 2, tenantId: 20, tenantName: '西城健身房' }),
    course({ id: 3, tenantId: 10, tenantName: '东城健身房' })
  ])
  assert.deepEqual(groups.map(group => [group.tenantId, group.name, group.courses.length]), [
    [10, '东城健身房', 2],
    [20, '西城健身房', 1]
  ])
})

test('groups roster bookings by status and translates labels', () => {
  const grouped = groupBookingsByStatus([
    { id: 1, status: 'PENDING' },
    { id: 2, status: 'CONFIRMED' },
    { id: 3, status: 'NO_SHOW' }
  ])
  assert.equal(grouped.find(group => group.status === 'PENDING').label, '待确认')
  assert.equal(grouped.find(group => group.status === 'CONFIRMED').count, 1)
  assert.equal(grouped.find(group => group.status === 'NO_SHOW').label, '未出席')
  assert.equal(bookingStatusLabel('CANCELLED'), '已取消')
})

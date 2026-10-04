const BOOKING_STATUS_LABELS = {
  PENDING: '待确认',
  CONFIRMED: '已报名',
  CANCELLED: '已取消',
  COMPLETED: '已完成',
  NO_SHOW: '未出席'
}

export function hasActiveBooking(courseId, bookings = []) {
  return bookings.some(booking => Number(booking.courseId) === Number(courseId) && booking.status !== 'CANCELLED')
}

export function courseStatus(course, bookings = [], now = new Date(), role = '') {
  if (course.status === 'CANCELLED') return { code: 'CANCELLED', label: '已取消' }
  const start = new Date(course.startsAt).getTime()
  const end = new Date(course.endsAt).getTime()
  const current = new Date(now).getTime()
  if (!Number.isFinite(start) || !Number.isFinite(end)) return { code: 'UNKNOWN', label: '待定' }
  if (end <= current || course.status === 'COMPLETED') return { code: 'ENDED', label: '已结束' }
  if (start <= current) return { code: 'IN_PROGRESS', label: '进行中' }
  if (role === 'MEMBER' && hasActiveBooking(course.id, bookings)) return { code: 'BOOKED', label: '已报名' }
  if (Number(course.reservedCount ?? 0) >= Number(course.capacity ?? 0)) return { code: 'FULL', label: '已满' }
  return { code: 'BOOKABLE', label: '可报名' }
}

export function groupCoursesByTenant(courses = []) {
  const groups = new Map()
  for (const course of courses) {
    const tenantId = Number(course.tenantId)
    if (!groups.has(tenantId)) {
      groups.set(tenantId, {
        tenantId,
        name: course.tenantName || `健身房 #${tenantId}`,
        courses: []
      })
    }
    groups.get(tenantId).courses.push(course)
  }
  return [...groups.values()]
}

export function bookingStatusLabel(status) {
  return BOOKING_STATUS_LABELS[status] || status || '未知'
}

export function groupBookingsByStatus(bookings = []) {
  const groups = new Map()
  for (const booking of bookings) {
    if (!groups.has(booking.status)) {
      groups.set(booking.status, {
        status: booking.status,
        label: bookingStatusLabel(booking.status),
        count: 0,
        bookings: []
      })
    }
    const group = groups.get(booking.status)
    group.count += 1
    group.bookings.push(booking)
  }
  return [...groups.values()]
}

export const bookingStatusLabels = BOOKING_STATUS_LABELS

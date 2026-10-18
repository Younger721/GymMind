/** 操作日志表格列与中文展示 */

export const OPERATION_AUDIT_COLUMN_KEYS = [
  'tenantId',
  'actorId',
  'action',
  'resourceType',
  'resourceId',
  'result',
  'createdAt'
] as const

export const OPERATION_AUDIT_COLUMN_LABELS: Record<string, string> = {
  tenantId: '租户 ID',
  actorId: '用户 ID',
  action: '操作',
  resourceType: '资源类型',
  resourceId: '资源 ID',
  result: '结果',
  createdAt: '时间'
}

const AUDIT_RESULT_LABELS: Record<string, string> = {
  SUCCESS: '成功',
  REJECTED: '已拒绝',
  FAILURE: '失败'
}

const AUDIT_ACTION_LABELS: Record<string, string> = {
  BOOKING_CREATED: '创建预约',
  BOOKING_CANCELLED: '取消预约',
  BOOKING_CONFIRMED: '确认预约',
  BOOKING_REACTIVATED: '恢复预约',
  BOOKING_NO_SHOW: '标记未到课',
  COURSE_CREATED: '创建课程',
  COURSE_UPDATED: '更新课程',
  COURSE_CANCELLED: '取消课程',
  WORKOUT_RECORD_CREATED: '新增训练记录',
  WORKOUT_PLAN_CREATED: '创建训练计划',
  WORKOUT_PLAN_UPDATED: '更新训练计划',
  WORKOUT_PLAN_PUBLISHED: '发布训练计划',
  CHECKIN_RECORDED: '签到',
  MEMBER_CREATED: '创建会员',
  MEMBER_UPDATED: '更新会员',
  ORDER_CREATED: '创建订单',
  PAYMENT_RECORDED: '记录缴费'
}

const AUDIT_RESOURCE_TYPE_LABELS: Record<string, string> = {
  BOOKING: '预约',
  COURSE: '课程',
  WORKOUT_RECORD: '训练记录',
  WORKOUT_PLAN: '训练计划',
  MEMBER: '会员',
  COACH: '教练',
  ORDER: '订单',
  PAYMENT: '缴费记录',
  CHECKIN: '签到'
}

export function formatAuditResult(value: unknown): string {
  if (value == null) return '—'
  const key = String(value)
  return AUDIT_RESULT_LABELS[key] ?? key
}

export function formatAuditAction(value: unknown): string {
  if (value == null) return '—'
  const key = String(value)
  return AUDIT_ACTION_LABELS[key] ?? key
}

export function formatAuditResourceType(value: unknown): string {
  if (value == null) return '—'
  const key = String(value)
  return AUDIT_RESOURCE_TYPE_LABELS[key] ?? key
}

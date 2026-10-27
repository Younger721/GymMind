/** 用户状态、角色等枚举的中文展示 */
export const USER_STATUS_LABELS: Record<string, string> = {
  ACTIVE: '正常',
  DISABLED: '已禁用'
}

export const ROLE_LABELS: Record<string, string> = {
  PLATFORM_ADMIN: '平台管理员',
  GYM_ADMIN: '场馆管理员',
  COACH: '教练',
  MEMBER: '会员'
}

export const DEFAULT_COLUMN_LABELS: Record<string, string> = {
  id: 'ID',
  tenantId: '租户ID',
  actorId: '用户 ID',
  result: '结果',
  username: '用户名',
  email: '邮箱',
  displayName: '显示名',
  status: '状态',
  roles: '角色'
}

export function formatRoleList(value: unknown): string {
  if (!Array.isArray(value)) return value == null ? '—' : String(value)
  return value.map(r => ROLE_LABELS[String(r)] ?? String(r)).join('、') || '—'
}

export function formatUserStatus(value: unknown): string {
  if (value == null) return '—'
  const key = String(value)
  return USER_STATUS_LABELS[key] ?? key
}

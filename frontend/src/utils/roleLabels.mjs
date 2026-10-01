export const ROLE_LABELS = {
  PLATFORM_ADMIN: '平台管理员',
  GYM_ADMIN: '健身房管理员',
  COACH: '教练',
  MEMBER: '会员'
}

export function toDisplayRole(role) {
  return { ...role, name: ROLE_LABELS[role.code] ?? role.name }
}

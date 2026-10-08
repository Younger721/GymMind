import { useAuthStore } from '@/stores/auth'

/**
 * Menu permissions are the only permissions used to decide whether a page is
 * visible or routable. Operation permissions remain an implementation detail
 * of the API layer and are expanded from these menu selections by the backend.
 */
export const MENU_CODES = {
  DASHBOARD: 'menu:dashboard',
  ASSISTANT: 'menu:assistant',
  AI_CONFIG: 'menu:ai-config',
  AGENTS: 'menu:ai-config',
  KNOWLEDGE: 'menu:knowledge',
  OPERATIONS: 'menu:operations',
  COURSES: 'menu:courses',
  EQUIPMENT: 'menu:equipment',
  TRAINING: 'menu:training',
  ANALYTICS: 'menu:analytics',
  ADMIN: 'menu:admin',
  AUDIT: 'menu:audit',
  AI_USAGE: 'menu:ai-usage',
  TENANT_SETTINGS: 'menu:tenant-settings',
  PLATFORM: 'menu:platform',
  ROLE_PERMISSIONS: 'menu:role-permissions',
  PROFILE: 'menu:profile'
} as const

export type MenuCode = typeof MENU_CODES[keyof typeof MENU_CODES]

export interface MenuCatalogEntry {
  code: MenuCode
  label: string
  group: string
  path: string
}

/** The fixed menu set exposed by the four built-in roles. */
export const FULL_MENU_CATALOG: readonly MenuCatalogEntry[] = [
  { code: MENU_CODES.DASHBOARD, label: '首页', group: '概览', path: '/dashboard' },
  { code: MENU_CODES.ASSISTANT, label: 'AI 中心', group: '智能', path: '/assistant' },
  { code: MENU_CODES.AI_CONFIG, label: 'AI 配置', group: '智能', path: '/agents/manage' },
  { code: MENU_CODES.KNOWLEDGE, label: 'AI 资料库', group: '智能', path: '/knowledge' },
  { code: MENU_CODES.COURSES, label: '课程中心', group: '门店运营', path: '/courses' },
  { code: MENU_CODES.EQUIPMENT, label: '器械管理', group: '门店运营', path: '/equipment' },
  { code: MENU_CODES.TRAINING, label: '训练管理', group: '门店运营', path: '/training' },
  { code: MENU_CODES.ADMIN, label: '用户与角色', group: '系统管理', path: '/admin' },
  { code: MENU_CODES.AUDIT, label: '日志管理', group: '系统管理', path: '/audit' },
  { code: MENU_CODES.AI_USAGE, label: 'AI 用量', group: '系统管理', path: '/ai-usage' },
  { code: MENU_CODES.TENANT_SETTINGS, label: '租户设置', group: '系统管理', path: '/tenant-settings' },
  { code: MENU_CODES.PLATFORM, label: '平台控制台', group: '系统管理', path: '/platform' },
  { code: MENU_CODES.ROLE_PERMISSIONS, label: '角色权限', group: '系统管理', path: '/role-permissions' },
  { code: MENU_CODES.PROFILE, label: '账号设置', group: '系统管理', path: '/profile' },
]

/** Menus exposed in the current product shell. The permission editor uses the full catalog above. */
const VISIBLE_MENU_CODES: ReadonlySet<MenuCode> = new Set([
  MENU_CODES.DASHBOARD,
  MENU_CODES.ASSISTANT,
  MENU_CODES.AI_CONFIG,
  MENU_CODES.KNOWLEDGE,
  MENU_CODES.ADMIN,
  MENU_CODES.AUDIT,
  MENU_CODES.AI_USAGE,
  MENU_CODES.COURSES,
  MENU_CODES.EQUIPMENT,
  MENU_CODES.TRAINING,
  MENU_CODES.ROLE_PERMISSIONS,
  MENU_CODES.TENANT_SETTINGS,
  MENU_CODES.PROFILE
])

export const MENU_CATALOG: readonly MenuCatalogEntry[] = FULL_MENU_CATALOG.filter(item => VISIBLE_MENU_CODES.has(item.code))

const labels = new Map(FULL_MENU_CATALOG.map(item => [item.code, item.label]))

export function menuLabel(code: string, fallback?: string): string {
  return labels.get(code as MenuCode) ?? fallback ?? code
}

export function hasMenuPermission(code: string): boolean {
  const auth = useAuthStore()
  return auth.isPlatformAdmin || auth.permissions.includes(code)
}

/** Map legacy endpoint permissions to the menu that owns the endpoint. */
const OPERATION_MENU_PREFIXES: ReadonlyArray<readonly [string, MenuCode]> = [
  ['platform:', MENU_CODES.PLATFORM],
  ['tenant:', MENU_CODES.TENANT_SETTINGS],
  ['user:', MENU_CODES.ADMIN],
  ['role:permission:', MENU_CODES.ROLE_PERMISSIONS],
  ['role:', MENU_CODES.ADMIN],
  ['audit:', MENU_CODES.ADMIN],
  ['ai:usage:', MENU_CODES.AI_USAGE],
  ['member:', MENU_CODES.OPERATIONS],
  ['coach:', MENU_CODES.OPERATIONS],
  ['coach-relation:', MENU_CODES.OPERATIONS],
  ['course:', MENU_CODES.COURSES],
  ['equipment:', MENU_CODES.EQUIPMENT],
  ['membership:', MENU_CODES.OPERATIONS],
  ['payment:', MENU_CODES.OPERATIONS],
  ['order:', MENU_CODES.OPERATIONS],
  ['booking:', MENU_CODES.COURSES],
  ['checkin:', MENU_CODES.OPERATIONS],
  ['workout:', MENU_CODES.TRAINING],
  ['workout-record:', MENU_CODES.TRAINING],
  ['nutrition:', MENU_CODES.TRAINING],
  ['knowledge:', MENU_CODES.KNOWLEDGE],
  ['search:', MENU_CODES.KNOWLEDGE],
  ['analytics:', MENU_CODES.ANALYTICS],
  ['agent:', MENU_CODES.AI_CONFIG],
  ['ai:', MENU_CODES.ASSISTANT]
]

export function menuCodeForOperation(permission: string): MenuCode | undefined {
  return OPERATION_MENU_PREFIXES.find(([prefix]) => permission.startsWith(prefix))?.[1]
}

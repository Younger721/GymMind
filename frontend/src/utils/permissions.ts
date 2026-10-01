import { useAuthStore } from '@/stores/auth'
import { menuCodeForOperation } from '@/utils/menuPermissions'

export function hasPermission(permission: string): boolean {
  const auth = useAuthStore()
  if (auth.isPlatformAdmin) return true
  const menuCode = menuCodeForOperation(permission)
  if (menuCode) return auth.permissions.includes(menuCode)
  return auth.permissions.includes(permission)
}

/**
 * Check the expanded backend permission for an operation.
 *
 * `hasPermission` intentionally treats a menu as the page boundary. Controls
 * that call a role-scoped endpoint need the expanded operation claim instead,
 * otherwise a training menu would make every training action appear usable.
 */
export function hasOperationPermission(permission: string): boolean {
  const auth = useAuthStore()
  return auth.isPlatformAdmin || auth.permissions.includes(permission)
}

export function canAccess(permission: string, roles: string[] = []): boolean {
  const auth = useAuthStore()
  return hasPermission(permission) && (roles.length === 0 || roles.some(role => auth.roles.includes(role)))
}

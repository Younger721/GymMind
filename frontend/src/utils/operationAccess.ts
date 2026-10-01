/**
 * Frontend visibility guard for role-scoped mutations.
 *
 * The backend expands selected menu permissions into operation claims. A
 * control is usable only when both the operation claim and the business role
 * boundary are present.
 */
export function canUseOperation(
  permissions: readonly string[],
  roles: readonly string[],
  operation: string,
  allowedRoles: readonly string[]
): boolean {
  const menu = menuCodeForOperation(operation)
  const roleAllowed = allowedRoles.length === 0 || roles.some(role => allowedRoles.includes(role))
  return Boolean(menu && permissions.includes(menu) && roleAllowed)
}

export function canUseTenantOperation(
  isPlatformAdmin: boolean,
  permissions: readonly string[],
  roles: readonly string[],
  operation: string,
  allowedRoles: readonly string[]
): boolean {
  return !isPlatformAdmin && canUseOperation(permissions, roles, operation, allowedRoles)
}
import { menuCodeForOperation } from '@/utils/menuPermissions'

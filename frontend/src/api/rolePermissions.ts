import client from './client'

export interface PermissionCatalogItem { code: string; name: string; roles: string[] }
export interface PermissionUpdate { permissions: string[] }

export const rolePermissionsApi = {
  catalog: () => client.get<PermissionCatalogItem[]>('/v1/roles/permissions', { skipErrorToast: true }),
  update: (role: string, permissions: string[]) => client.put(`/v1/roles/${role}/permissions`, { permissions }, { skipErrorToast: true })
}

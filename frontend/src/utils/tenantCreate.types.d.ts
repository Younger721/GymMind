declare module '@/utils/tenantCreate.mjs' {
  export function isTenantCreateFormValid(form?: Record<string, unknown>): boolean
  export function buildTenantCreatePayload(form?: Record<string, unknown>): {
    tenantName: string
    adminEmail: string
    adminPassword: string
    adminDisplayName: string
  }
}

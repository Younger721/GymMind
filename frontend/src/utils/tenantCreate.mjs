const TENANT_CREATE_KEYS = ['tenantName', 'adminEmail', 'adminPassword', 'adminDisplayName']

export function isTenantCreateFormValid(form = {}) {
  return TENANT_CREATE_KEYS.every(key => String(form[key] ?? '').trim().length > 0)
}

export function buildTenantCreatePayload(form = {}) {
  return {
    tenantName: String(form.tenantName ?? '').trim(),
    adminEmail: String(form.adminEmail ?? '').trim(),
    adminPassword: String(form.adminPassword ?? ''),
    adminDisplayName: String(form.adminDisplayName ?? '').trim()
  }
}

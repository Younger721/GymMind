export function isTenantRequestCurrent(
  requestTenantId: number | null,
  activeTenantId: number | null,
  requestVersion: number,
  currentVersion: number
): boolean {
  return requestTenantId != null
    && requestTenantId === activeTenantId
    && requestVersion === currentVersion
}

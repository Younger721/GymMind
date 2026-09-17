import apiClient from './client'
import type { ApiResponse } from '@/types/api'
import type { PageResponse } from '@/types/page'

export const tenancyApi = {
  platformStats() {
    return apiClient.get<any, ApiResponse<unknown>>('/v1/platform/stats')
  },
  listTenants(page = 0, size = 20) {
    return apiClient.get<any, ApiResponse<PageResponse<unknown>>>('/v1/platform/tenants', { params: { page, size } })
  },
  createTenant(body: Record<string, unknown>) {
    return apiClient.post('/v1/platform/tenants', body)
  },
  activateTenant(id: number) {
    return apiClient.patch(`/v1/platform/tenants/${id}/activate`)
  },
  disableTenant(id: number) {
    return apiClient.patch(`/v1/platform/tenants/${id}/disable`)
  },
  getTenantQuota(tenantId: number) {
    return apiClient.get<any, ApiResponse<unknown>>(`/v1/platform/tenants/${tenantId}/quota`)
  },
  updateTenantQuota(tenantId: number, body: Record<string, unknown>) {
    return apiClient.put(`/v1/platform/tenants/${tenantId}/quota`, body)
  },
  getMyQuota() {
    return apiClient.get<any, ApiResponse<unknown>>('/v1/tenant/quota')
  },
  getSettings() {
    return apiClient.get<any, ApiResponse<unknown>>('/v1/tenant/settings')
  },
  updateSettings(body: Record<string, unknown>) {
    return apiClient.put('/v1/tenant/settings', body)
  }
}

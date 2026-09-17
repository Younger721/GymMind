import apiClient from './client'
import type { ApiResponse } from '@/types/api'

export const auditApi = {
  operations() {
    return apiClient.get<any, ApiResponse<unknown[]>>('/v1/audit/operations')
  },
  aiUsage() {
    return apiClient.get<any, ApiResponse<unknown[]>>('/v1/audit/ai-usage')
  }
}

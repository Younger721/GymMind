import apiClient from './client'
import type { ApiResponse } from '@/types/api'

export const mcpApi = {
  tools() {
    return apiClient.get<any, ApiResponse<unknown[]>>('/v1/mcp/tools')
  },
  invoke(body: { name: string; input: string }) {
    return apiClient.post<any, ApiResponse<unknown>>('/v1/mcp/invoke', body)
  }
}

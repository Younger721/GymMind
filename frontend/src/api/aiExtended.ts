import apiClient from './client'
import type { ApiResponse } from '@/types/api'

export const aiExtendedApi = {
  memberAnalysis(body: Record<string, unknown>) {
    return apiClient.post<any, ApiResponse<unknown>>('/v1/ai/member-analysis', body)
  },
  operationAnalysis(from: string, to: string) {
    return apiClient.post<any, ApiResponse<unknown>>('/v1/ai/operation-analysis', null, { params: { from, to } })
  },
  recommend(body: { goal: string; healthConstraints?: string[] }) {
    return apiClient.post<any, ApiResponse<unknown[]>>('/v1/ai/recommendations', body)
  },
  coachAssistant(body: Record<string, unknown>) {
    return apiClient.post<any, ApiResponse<unknown>>('/v1/ai/coach-assistant', body)
  },
  listMemories() {
    return apiClient.get<any, ApiResponse<unknown[]>>('/v1/ai/memories')
  },
  writeMemory(body: Record<string, unknown>) {
    return apiClient.post('/v1/ai/memories', body)
  },
  deleteMemory(type: string, key: string) {
    return apiClient.delete('/v1/ai/memories', { params: { type, key } })
  }
}

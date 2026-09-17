import apiClient from './client'
import type { ApiResponse } from '@/types/api'

export const workoutPlansApi = {
  list() {
    return apiClient.get<any, ApiResponse<unknown[]>>('/v1/workout-plans')
  },
  find(id: number) {
    return apiClient.get<any, ApiResponse<unknown>>(`/v1/workout-plans/${id}`)
  },
  create(body: Record<string, unknown>) {
    return apiClient.post('/v1/workout-plans', body)
  },
  saveDraft(body: Record<string, unknown>) {
    return apiClient.post('/v1/workout-plans/drafts', body)
  },
  publish(id: number) {
    return apiClient.post(`/v1/workout-plans/${id}/publish`)
  },
  aiDraft(body: { memberId: number | null; request: string }) {
    return apiClient.post<any, ApiResponse<unknown>>('/v1/ai/workout-plan-drafts', body)
  },
  aiSave(body: { memberId: number | null; request: string }) {
    return apiClient.post<any, ApiResponse<unknown>>('/v1/ai/workout-plan', body)
  }
}

export const workoutRecordsApi = {
  create(body: Record<string, unknown>) {
    return apiClient.post('/v1/workout-records', body)
  }
}

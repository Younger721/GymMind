import apiClient from './client'
import type { ApiResponse } from '@/types/api'

export interface WorkoutPlanItemView {
  id: number
  exerciseId: number
  exerciseName: string
  dayOfWeek: number
  phase: 'WARMUP' | 'MAIN' | 'STRETCH'
  sortOrder: number
  sets: number
  reps: number
  restSeconds: number
  weight: number
  notes?: string
}

export interface WorkoutPlanView {
  id: number
  tenantId: number
  memberId: number
  coachId?: number
  name: string
  goal: string
  startDate: string
  endDate: string
  status: string
  description: string
  items: WorkoutPlanItemView[]
}

export const workoutPlansApi = {
  list() {
    return apiClient.get<any, ApiResponse<WorkoutPlanView[]>>('/v1/workout-plans')
  },
  find(id: number) {
    return apiClient.get<any, ApiResponse<WorkoutPlanView>>(`/v1/workout-plans/${id}`)
  },
  create(body: Record<string, unknown>) {
    return apiClient.post('/v1/workout-plans', body)
  },
  saveDraft(body: Record<string, unknown>) {
    return apiClient.post('/v1/workout-plans/drafts', body)
  },
  createMember(body: Record<string, unknown>) {
    return apiClient.post('/v1/workout-plans/member', body)
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

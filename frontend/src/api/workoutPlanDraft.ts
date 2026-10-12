import apiClient from './client'
import type { ApiResponse } from '@/types/api'

export interface WorkoutPlanTarget {
  memberId: number
  memberNumber?: string
  name?: string
  displayName?: string
  username?: string
  email?: string
}

export interface WorkoutPlanItemDraft {
  exerciseId: number
  dayOfWeek: number
  sets: number
  reps: number
  restSeconds: number
  weight: number
  notes?: string
}

export interface WorkoutPlanDraft {
  tenantId?: number | null
  memberId: number
  coachId?: number | null
  name: string
  goal: string
  startDate: string
  endDate: string
  description: string
  items: WorkoutPlanItemDraft[]
}

export interface WorkoutPlanHistory {
  included: boolean
  recordCount: number
  summary?: string
}

export interface WorkoutPlanDraftResponse {
  plan: WorkoutPlanDraft
  history?: WorkoutPlanHistory
}

export const workoutPlanDraftApi = {
  getTargets() {
    return apiClient.get<any, ApiResponse<WorkoutPlanTarget[] | { targets?: WorkoutPlanTarget[] }>>(
      '/v1/ai/workout-plan-targets'
    )
  },

  generate(body: {
    memberId?: number | null
    request?: string
    includeHistory?: boolean
    goal?: string
    sessionsPerWeek?: number
    experience?: string
  }) {
    return apiClient.post<any, ApiResponse<WorkoutPlanDraftResponse | WorkoutPlanDraft>>(
      '/v1/ai/workout-plan-drafts',
      body
    )
  },

  save(plan: WorkoutPlanDraft) {
    return apiClient.post<any, ApiResponse<{ id?: number; memberId?: number }>>(
      '/v1/workout-plans/drafts',
      plan
    )
  }
}

import apiClient from './client'
import type { ApiResponse } from '@/types/api'

export interface Exercise {
  id: number
  tenantId: number
  name: string
  category: string
  targetMuscle: string
  difficulty: string
  equipment: string
  description: string
  steps: string
  commonMistakes: string
  safetyNotes: string
  tags: string
  active: boolean
}

export const exercisesApi = {
  list() {
    return apiClient.get<any, ApiResponse<Exercise[]>>('/v1/exercises')
  },
  find(id: number) {
    return apiClient.get<any, ApiResponse<unknown>>(`/v1/exercises/${id}`)
  },
  create(body: Record<string, unknown>) {
    return apiClient.post('/v1/exercises', body)
  },
  update(id: number, body: Record<string, unknown>) {
    return apiClient.patch(`/v1/exercises/${id}`, body)
  },
  setActive(id: number, active: boolean) {
    return apiClient.patch(`/v1/exercises/${id}/status`, { active })
  }
}

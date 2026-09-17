import apiClient from './client'
import type { ApiResponse } from '@/types/api'

export const nutritionApi = {
  createPlan(body: Record<string, unknown>) {
    return apiClient.post('/v1/nutrition/plans', body)
  },
  createRecord(body: Record<string, unknown>) {
    return apiClient.post('/v1/nutrition/records', body)
  },
  createFood(body: Record<string, unknown>) {
    return apiClient.post('/v1/nutrition/foods', body)
  },
  createProfile(body: Record<string, unknown>) {
    return apiClient.post<any, ApiResponse<unknown>>('/v1/nutrition/profiles', body)
  }
}

import apiClient from './client'
import type { ApiResponse } from '@/types/api'

export const analyticsApi = {
  dashboard(from: string, to: string) {
    return apiClient.get<any, ApiResponse<unknown>>('/v1/analytics/dashboard', { params: { from, to } })
  },
  weeklyMetrics(weekStart: string) {
    return apiClient.get<any, ApiResponse<unknown>>('/v1/analytics/weekly-metrics', { params: { weekStart } })
  },
  weeklyReport(weekStart: string) {
    return apiClient.get<any, ApiResponse<unknown>>('/v1/analytics/weekly-report', { params: { weekStart } })
  },
  aiWeeklyReport(weekStart: string) {
    return apiClient.get<any, ApiResponse<unknown>>('/v1/ai/weekly-reports', { params: { weekStart } })
  },
  churnRisk(body: {
    daysSinceLastWorkout: number
    workoutsLast30Days: number
    workoutsPrevious30Days: number
    membershipDaysRemaining: number
    membershipExpiring: boolean
  }) {
    return apiClient.post<any, ApiResponse<unknown>>('/v1/analytics/churn-risks', body)
  }
}

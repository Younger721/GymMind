import apiClient from './client'

export interface DashboardSummary {
  todayOverview: {
    date: string
    workoutCount: number
    totalVolume: number
    caloriesConsumed: number
    caloriesTarget: number
    proteinConsumed: number
    proteinTarget: number
  }
  weeklyStats: {
    workoutDays: number
    totalVolume: number
    avgCalories: number
    totalWorkouts: number
  }
  knowledgeStats: {
    totalDocuments: number
    successDocuments: number
    processingDocuments: number
    failedDocuments: number
  }
  recentWorkouts: Array<{
    date: string
    workoutCount: number
    totalVolume: number
  }>
  weightTrend: Array<{
    date: string
    weight: number
  }>
}

export function getDashboardSummary() {
  return apiClient.get<DashboardSummary>('/dashboard/summary')
}

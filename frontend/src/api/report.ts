import apiClient from './client'

export interface WeeklyReport {
  startDate: string
  endDate: string
  workoutSummary: {
    totalDays: number
    totalWorkouts: number
    totalVolume: number
    avgVolumePerDay: number
    mostFrequentExercise: string
    personalRecords: Array<{
      exerciseName: string
      weight: number
      reps: number
      achievedDate: string
    }>
  }
  nutritionSummary: {
    avgCalories: number
    avgProtein: number
    avgCarbs: number
    avgFats: number
    calorieTarget: number
    proteinTarget: number
    daysOnTarget: number
    totalDays: number
  }
  aiAnalysis: string
  suggestions: string[]
}

export function getWeeklyReport(startDate: string, endDate: string) {
  return apiClient.get<WeeklyReport>('/report/weekly', {
    params: { startDate, endDate }
  })
}

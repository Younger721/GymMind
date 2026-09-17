import axios from './client'

export interface GeneratePlanRequest {
  goal: string
  durationWeeks: number
  workoutsPerWeek: number
  difficulty: string
  equipment: string
  sessionDuration: number
  focusAreas?: string[]
  injuries?: string[]
  preferredTime?: string
}

export interface Exercise {
  name: string
  category: string
  muscleGroup: string
  sets: number
  reps: number
  restTime: string
  notes?: string
}

export interface DayPlan {
  dayNumber: number
  dayName: string
  isRestDay: boolean
  isCompleted: boolean
  exercises: Exercise[]
  notes?: string
}

export interface WeekPlan {
  weekNumber: number
  weekTitle: string
  days: DayPlan[]
}

export interface PlanStats {
  totalWorkouts: number
  completedWorkouts: number
  remainingWorkouts: number
  completionRate: number
  currentWeek: number
}

export interface WorkoutPlanResponse {
  id: number
  planName: string
  description: string
  goal: string
  difficulty: string
  durationWeeks: number
  workoutsPerWeek: number
  startDate: string
  endDate: string
  status: string
  isAiGenerated: boolean
  createdAt: string
  weeks: WeekPlan[]
  stats: PlanStats
}

export const workoutPlanApi = {
  generatePlan: (request: GeneratePlanRequest) =>
    axios.post<WorkoutPlanResponse>('/api/workout-plans/generate', request),

  getUserPlans: () =>
    axios.get<WorkoutPlanResponse[]>('/api/workout-plans'),

  getPlanById: (planId: number) =>
    axios.get<WorkoutPlanResponse>(`/api/workout-plans/${planId}`),

  updatePlanStatus: (planId: number, status: string) =>
    axios.put(`/api/workout-plans/${planId}/status`, null, { params: { status } }),

  deletePlan: (planId: number) =>
    axios.delete(`/api/workout-plans/${planId}`)
}

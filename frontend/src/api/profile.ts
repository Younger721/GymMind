import apiClient from './client'

export interface UserProfile {
  id: number
  userId: number
  height?: number
  weight?: number
  targetWeight?: number
  bodyFatPercentage?: number
  fitnessGoal?: string
  experienceLevel?: string
  weeklyWorkoutDays?: number
  dailyWorkoutMinutes?: number
  availableEquipment?: string[]
  dietaryPreference?: string
  foodAllergies?: string[]
  foodDislikes?: string[]
  healthConditions?: string
  injuries?: string
  createdAt: string
  updatedAt: string
}

export interface UpdateProfileRequest {
  height?: number
  weight?: number
  targetWeight?: number
  bodyFatPercentage?: number
  fitnessGoal?: string
  experienceLevel?: string
  weeklyWorkoutDays?: number
  dailyWorkoutMinutes?: number
  availableEquipment?: string[]
  dietaryPreference?: string
  foodAllergies?: string[]
  foodDislikes?: string[]
  healthConditions?: string
  injuries?: string
}

export const profileApi = {
  getProfile() {
    return apiClient.get<any, { data: UserProfile }>('/user/profile')
  },

  updateProfile(data: UpdateProfileRequest) {
    return apiClient.put<any, { data: UserProfile }>('/user/profile', data)
  }
}

import apiClient from './client'
import type { ApiResponse } from '@/types/api'

export interface ProfileOverview {
  userId: number
  username: string
  email: string
  displayName: string
  status: string
  from: string
  to: string
  checkInCount: number
  member: {
    id: number
    fullName: string
    memberNumber: string
    status: string
    checkInToken: string
  } | null
  membership: {
    id: number
    packageName: string
    membershipType: string
    startsAt: string
    expiresAt: string
    status: string
  } | null
}

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
  getOverview(from?: string, to?: string) {
    return apiClient.get<any, ApiResponse<ProfileOverview>>('/v1/profile/overview', {
      params: { from, to }
    })
  },

  getProfile() {
    return apiClient.get<any, { data: UserProfile }>('/user/profile')
  },

  updateProfile(data: UpdateProfileRequest) {
    return apiClient.put<any, { data: UserProfile }>('/user/profile', data)
  }
}

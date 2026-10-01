import apiClient from './client'
import type { ApiResponse } from '@/types/api'

export interface MemberRiskView {
  memberId: number
  memberName: string
  lastCheckInAt?: string
  checkInsLast30Days: number
  bookingsLast30Days: number
  courseParticipationsLast30Days: number
  membershipDaysRemaining: number
  level: 'LOW' | 'MEDIUM' | 'HIGH'
  score: number
  reasons: string[]
}

export const memberRisksApi = {
  list() { return apiClient.get<any, ApiResponse<MemberRiskView[]>>('/v1/analytics/member-risks') }
}

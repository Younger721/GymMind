import apiClient from './client'
import type { ApiResponse } from '@/types/api'

export interface TenantOperationsSummary {
  from: string
  to: string
  periodLabel: string
  totalMembers: number
  activeMembers: number
  uniqueVisitors: number
  checkInPersonTimes: number
  previousUniqueVisitors?: number | null
  previousCheckInPersonTimes?: number | null
  uniqueVisitorsChangeRate?: number | string | null
  checkInPersonTimesChangeRate?: number | string | null
}

export interface TenantOperationsTrendPoint {
  bucket: string
  day?: string | null
  uniqueVisitors: number
  checkInPersonTimes: number
}

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
  tenantOperationsSummary(from: string, to: string, comparePrevious = true) {
    return apiClient.get<any, ApiResponse<TenantOperationsSummary>>('/v1/analytics/tenant-operations/summary', {
      params: { from, to, comparePrevious }
    })
  },
  tenantOperationsTrend(from: string, to: string, groupBy: 'day' | 'week' | 'month' = 'day') {
    return apiClient.get<any, ApiResponse<TenantOperationsTrendPoint[]>>('/v1/analytics/tenant-operations/trend', {
      params: { from, to, groupBy }
    })
  },
  churnRisk(body: {
    daysSinceLastCheckIn: number
    checkInsLast30Days: number
    checkInsPrevious30Days: number
    bookingsLast30Days: number
    courseParticipationsLast30Days: number
    membershipDaysRemaining: number
    membershipExpiring: boolean
  }) {
    return apiClient.post<any, ApiResponse<unknown>>('/v1/analytics/churn-risks', body)
  }
}

/** 格式化为 API 所需的 YYYY-MM-DD */
export function formatApiDate(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

export function previousCalendarMonthRange(reference = new Date()): { from: string; to: string; label: string } {
  const first = new Date(reference.getFullYear(), reference.getMonth() - 1, 1)
  const last = new Date(reference.getFullYear(), reference.getMonth(), 0)
  return {
    from: formatApiDate(first),
    to: formatApiDate(last),
    label: `${first.getFullYear()}年${first.getMonth() + 1}月`
  }
}

export function currentMonthToDateRange(reference = new Date()): { from: string; to: string; label: string } {
  const first = new Date(reference.getFullYear(), reference.getMonth(), 1)
  return {
    from: formatApiDate(first),
    to: formatApiDate(reference),
    label: `${first.getFullYear()}年${first.getMonth() + 1}月至今`
  }
}

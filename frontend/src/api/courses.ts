import apiClient from './client'
import type { ApiResponse } from '@/types/api'

export type CourseLifecycleStatus = 'SCHEDULED' | 'CANCELLED' | 'COMPLETED'

export interface CourseView {
  id: number
  tenantId: number
  tenantName?: string
  coachId: number
  coachName?: string
  title: string
  type: string
  description?: string
  startsAt: string
  endsAt: string
  capacity: number
  reservedCount: number
  location: string
  status: CourseLifecycleStatus
}

export interface CoursePayload {
  tenantId?: number
  coachId?: number
  title: string
  type: string
  description?: string
  startsAt: string
  endsAt: string
  capacity: number
  location: string
}

export const coursesApi = {
  list(params?: { scope?: 'all' | 'mine' | 'managed'; from?: string; to?: string; type?: string; coachId?: number; availableOnly?: boolean }) {
    return apiClient.get<any, ApiResponse<CourseView[]>>('/v1/courses', { params })
  },
  find(id: number) {
    return apiClient.get<any, ApiResponse<CourseView>>(`/v1/courses/${id}`)
  },
  create(body: CoursePayload) {
    return apiClient.post<any, ApiResponse<CourseView>>('/v1/courses', body)
  },
  update(id: number, body: CoursePayload) {
    return apiClient.patch<any, ApiResponse<void>>(`/v1/courses/${id}`, body)
  },
  cancel(id: number) {
    return apiClient.post<any, ApiResponse<void>>(`/v1/courses/${id}/cancel`)
  }
}

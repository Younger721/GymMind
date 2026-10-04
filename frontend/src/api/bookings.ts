import apiClient from './client'
import type { ApiResponse } from '@/types/api'

export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED' | 'NO_SHOW'

export interface BookingView {
  id: number
  tenantId?: number
  memberId: number
  memberName?: string
  memberPhone?: string
  courseId: number
  status: BookingStatus
  checkedInAt?: string
}

export const bookingsApi = {
  list(courseId?: number) {
    return apiClient.get<any, ApiResponse<BookingView[]>>('/v1/bookings', { params: courseId ? { courseId } : undefined })
  },
  book(body: { memberId?: number; courseId: number }) {
    return apiClient.post<any, ApiResponse<BookingView>>('/v1/bookings', body)
  },
  cancel(id: number) {
    return apiClient.post<any, ApiResponse<BookingView>>(`/v1/bookings/${id}/cancel`)
  },
  confirm(id: number) {
    return apiClient.post<any, ApiResponse<BookingView>>(`/v1/bookings/${id}/confirm`)
  }
}

export const checkInsApi = {
  checkIn(body: { memberId: number; courseId: number }) {
    return apiClient.post('/v1/check-ins', body)
  }
}

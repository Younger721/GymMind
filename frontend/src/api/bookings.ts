import apiClient from './client'
import type { ApiResponse } from '@/types/api'

export const bookingsApi = {
  list() {
    return apiClient.get<any, ApiResponse<unknown[]>>('/v1/bookings')
  },
  book(body: { memberId: number; courseId: number }) {
    return apiClient.post('/v1/bookings', body)
  },
  cancel(id: number) {
    return apiClient.post(`/v1/bookings/${id}/cancel`)
  },
  confirm(id: number) {
    return apiClient.post(`/v1/bookings/${id}/confirm`)
  }
}

export const checkInsApi = {
  checkIn(body: { memberId: number; courseId: number }) {
    return apiClient.post('/v1/check-ins', body)
  }
}

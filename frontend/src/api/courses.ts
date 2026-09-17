import apiClient from './client'
import type { ApiResponse } from '@/types/api'

export const coursesApi = {
  list() {
    return apiClient.get<any, ApiResponse<unknown[]>>('/v1/courses')
  },
  find(id: number) {
    return apiClient.get<any, ApiResponse<unknown>>(`/v1/courses/${id}`)
  },
  create(body: Record<string, unknown>) {
    return apiClient.post('/v1/courses', body)
  },
  update(id: number, body: Record<string, unknown>) {
    return apiClient.patch(`/v1/courses/${id}`, body)
  },
  cancel(id: number) {
    return apiClient.post(`/v1/courses/${id}/cancel`)
  }
}

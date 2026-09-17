import apiClient from './client'
import type { ApiResponse } from '@/types/api'

export const exercisesApi = {
  list() {
    return apiClient.get<any, ApiResponse<unknown[]>>('/v1/exercises')
  },
  find(id: number) {
    return apiClient.get<any, ApiResponse<unknown>>(`/v1/exercises/${id}`)
  },
  create(body: Record<string, unknown>) {
    return apiClient.post('/v1/exercises', body)
  },
  update(id: number, body: Record<string, unknown>) {
    return apiClient.patch(`/v1/exercises/${id}`, body)
  }
}

export const videosApi = {
  find(id: number) {
    return apiClient.get<any, ApiResponse<unknown>>(`/v1/videos/${id}`)
  },
  create(body: Record<string, unknown>) {
    return apiClient.post('/v1/videos', body)
  },
  remove(id: number) {
    return apiClient.delete(`/v1/videos/${id}`)
  }
}

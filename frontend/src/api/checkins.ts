import apiClient from './client'

export const checkinsApi = {
  scan(token: string, courseId?: number) {
    return apiClient.post('/v1/check-ins/scan', { token, ...(courseId ? { courseId } : {}) })
  }
}

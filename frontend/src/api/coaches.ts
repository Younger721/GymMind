import apiClient from './client'
import type { ApiResponse } from '@/types/api'

export const coachesApi = {
  find(id: number) {
    return apiClient.get<any, ApiResponse<unknown>>(`/v1/coaches/${id}`)
  },
  create(body: { coachNumber: string; fullName: string; phone: string; userId?: number }) {
    return apiClient.post('/v1/coaches', body)
  },
  update(id: number, body: { fullName: string; phone: string }) {
    return apiClient.patch(`/v1/coaches/${id}`, body)
  },
  assignMember(coachId: number, memberId: number) {
    return apiClient.post(`/v1/coaches/${coachId}/members/${memberId}`)
  },
  unassignMember(coachId: number, memberId: number) {
    return apiClient.delete(`/v1/coaches/${coachId}/members/${memberId}`)
  }
}

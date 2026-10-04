import apiClient from './client'
import type { ApiResponse } from '@/types/api'

export interface CoachView {
  id: number
  tenantId: number
  userId?: number
  coachNumber: string
  fullName: string
  phone: string
  status: string
}

export const coachesApi = {
  find(id: number) {
    return apiClient.get<any, ApiResponse<CoachView>>(`/v1/coaches/${id}`)
  },
  list(tenantId?: number) {
    return apiClient.get<any, ApiResponse<CoachView[]>>('/v1/coaches', { params: tenantId ? { tenantId } : undefined })
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
  },
  relations(status: 'pending' | 'active' = 'active') {
    return apiClient.get(`/v1/coach-relations/${status}`)
  },
  acceptRelation(id: number) { return apiClient.post(`/v1/coach-relations/${id}/accept`) },
  rejectRelation(id: number) { return apiClient.post(`/v1/coach-relations/${id}/reject`) },
  cancelRelation(id: number) { return apiClient.post(`/v1/coach-relations/${id}/cancel`) },
  endRelation(id: number) { return apiClient.post(`/v1/coach-relations/${id}/end`) }
}

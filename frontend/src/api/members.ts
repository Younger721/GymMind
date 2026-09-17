import apiClient from './client'
import type { ApiResponse } from '@/types/api'
import type { PageResponse } from '@/types/page'

export interface MemberView {
  id: number
  memberNumber?: string
  fullName?: string
  phone?: string
  status?: string
  [key: string]: unknown
}

export const membersApi = {
  list(q?: string, page = 0, size = 20) {
    return apiClient.get<any, ApiResponse<PageResponse<MemberView>>>('/v1/members', { params: { q, page, size } })
  },
  find(id: number) {
    return apiClient.get<any, ApiResponse<MemberView>>(`/v1/members/${id}`)
  },
  me() {
    return apiClient.get<any, ApiResponse<MemberView>>('/v1/members/me')
  },
  create(body: { memberNumber: string; fullName: string; phone: string; userId?: number }) {
    return apiClient.post<any, ApiResponse<MemberView>>('/v1/members', body)
  },
  update(id: number, body: { fullName: string; phone: string }) {
    return apiClient.patch(`/v1/members/${id}`, body)
  },
  suspend(id: number) {
    return apiClient.post(`/v1/members/${id}/suspend`)
  }
}

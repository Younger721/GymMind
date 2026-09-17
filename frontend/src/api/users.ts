import apiClient from './client'
import type { ApiResponse } from '@/types/api'
import type { PageResponse } from '@/types/page'

export const usersApi = {
  list(page = 0, size = 20) {
    return apiClient.get<any, ApiResponse<PageResponse<unknown>>>('/v1/users', { params: { page, size } })
  },
  create(body: Record<string, unknown>) {
    return apiClient.post('/v1/users', body)
  },
  changeStatus(userId: number, status: string) {
    return apiClient.patch(`/v1/users/${userId}/status`, { status })
  },
  replaceRoles(userId: number, roles: string[]) {
    return apiClient.put(`/v1/users/${userId}/roles`, { roles })
  },
  listRoles() {
    return apiClient.get<any, ApiResponse<unknown[]>>('/v1/roles')
  },
  invite(body: Record<string, unknown>) {
    return apiClient.post('/v1/users/invitations', body)
  },
  acceptInvitation(body: Record<string, unknown>) {
    return apiClient.post<any, ApiResponse<unknown>>('/v1/auth/invitations/accept', body)
  }
}

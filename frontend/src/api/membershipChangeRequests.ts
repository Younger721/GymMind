import apiClient from './client'

export type MembershipChangeType = 'FREEZE' | 'TRANSFER'
export type MembershipChangeStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED'

export interface MembershipChangeRequest {
  id: number
  tenantId: number
  type: MembershipChangeType
  status: MembershipChangeStatus
  membershipId: number
  memberId: number
  targetMemberId?: number | null
  freezeStart?: string | null
  freezeEnd?: string | null
  reason?: string | null
  applicantUserId: number
  reviewerId?: number | null
  reviewedAt?: string | null
  reviewComment?: string | null
  createdAt: string
}

export const membershipChangeRequestsApi = {
  list(status?: MembershipChangeStatus) {
    return apiClient.get<MembershipChangeRequest[]>('/v1/membership-change-requests', {
      params: status ? { status } : undefined
    })
  },
  create(body: {
    type: MembershipChangeType
    membershipId: number
    freezeStart?: string
    freezeEnd?: string
    targetMemberId?: number
    reason?: string
  }) {
    return apiClient.post<MembershipChangeRequest>('/v1/membership-change-requests', body)
  },
  approve(id: number, comment?: string) {
    return apiClient.post<MembershipChangeRequest>(`/v1/membership-change-requests/${id}/approve`, { comment })
  },
  reject(id: number, comment?: string) {
    return apiClient.post<MembershipChangeRequest>(`/v1/membership-change-requests/${id}/reject`, { comment })
  },
  cancel(id: number) {
    return apiClient.post<MembershipChangeRequest>(`/v1/membership-change-requests/${id}/cancel`)
  }
}

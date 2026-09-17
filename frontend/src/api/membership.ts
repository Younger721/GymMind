import apiClient from './client'
import type { ApiResponse } from '@/types/api'

export const membershipApi = {
  createPackage(body: Record<string, unknown>) {
    return apiClient.post('/v1/membership-packages', body)
  },
  findPackage(id: number) {
    return apiClient.get<any, ApiResponse<unknown>>(`/v1/membership-packages/${id}`)
  },
  grantMembership(body: Record<string, unknown>) {
    return apiClient.post('/v1/memberships', body)
  },
  createOrder(body: Record<string, unknown>) {
    return apiClient.post('/v1/orders', body)
  },
  recordPayment(body: Record<string, unknown>) {
    return apiClient.post('/v1/payment-records', body)
  }
}

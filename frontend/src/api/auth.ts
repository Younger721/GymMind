import apiClient from './client'
import type { ApiResponse } from '@/types/api'

export interface LoginRequest {
  email: string
  password: string
}

/** 租户注册（创建健身房 + 管理员账号） */
export interface RegisterRequest {
  tenantCode: string
  tenantName: string
  email: string
  password: string
  displayName: string
}

export interface AuthResponseData {
  userId: number
  tenantId: number | null
  email: string
  displayName: string
  roles: string[]
  permissions: string[]
  accessToken: string
  accessExpiresAt: string
  refreshToken: string
  refreshExpiresAt: string
}

export const authApi = {
  login(data: LoginRequest) {
    return apiClient.post<any, ApiResponse<AuthResponseData>>('/v1/auth/login', data)
  },

  register(data: RegisterRequest) {
    return apiClient.post<any, ApiResponse<AuthResponseData>>('/v1/auth/register', data)
  },

  logout(refreshToken: string) {
    return apiClient.post<any, ApiResponse<void>>('/v1/auth/logout', { refreshToken })
  }
}

import apiClient from './client'
import type { ApiResponse } from '@/types/api'

export interface AiUsageRecord {
  tenantId: number
  userId: number
  model: string
  provider?: string
  feature?: string
  inputTokens?: number | null
  outputTokens?: number | null
  totalTokens?: number | null
  latencyMs: number
  providerRequestId?: string | null
  status: string
  usageSource?: string
  estimatedCost?: number | null
  currency?: string | null
  createdAt?: string | null
}

export interface AiUsageSummary {
  calls: number
  inputTokens: number
  outputTokens: number
  totalTokens: number
  estimatedCost: number
  averageLatencyMs: number
  successRate: number
}

export interface AiUsagePage {
  content: AiUsageRecord[]
  page: number
  size: number
  totalElements: number
  summary: AiUsageSummary
}

export interface AiUsageTrendPoint {
  date: string
  calls: number
  inputTokens: number
  outputTokens: number
  totalTokens: number
  estimatedCost: number
}

export const auditApi = {
  operations() {
    return apiClient.get<any, ApiResponse<unknown[]>>('/v1/audits')
  },
  aiUsage(params: Record<string, unknown> = {}) {
    return apiClient.get<any, ApiResponse<AiUsagePage>>('/v1/audits/ai-usage/page', { params })
  },
  aiUsageSummary(params: Record<string, unknown> = {}) {
    return apiClient.get<any, ApiResponse<AiUsageSummary>>('/v1/audits/ai-usage/summary', { params })
  },
  aiUsageTrend(params: Record<string, unknown> = {}) {
    return apiClient.get<any, ApiResponse<AiUsageTrendPoint[]>>('/v1/audits/ai-usage/trend', { params })
  }
}

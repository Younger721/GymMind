import apiClient from './client'
import type { ApiResponse } from '@/types/api'

export interface AiModelPricing {
  id?: number
  provider: string
  model: string
  inputPricePerMillion: number
  outputPricePerMillion: number
  currency: string
  enabled: boolean
}

export const aiPricingApi = {
  list() {
    return apiClient.get<any, ApiResponse<AiModelPricing[]>>('/v1/platform/ai-model-pricing')
  },
  upsert(payload: AiModelPricing) {
    return apiClient.put<any, ApiResponse<AiModelPricing>>('/v1/platform/ai-model-pricing', payload)
  }
}

import apiClient from './client'
import type { ApiResponse } from '@/types/api'

export interface AgentView {
  id: number
  tenantId: number
  name: string
  description: string
  systemPrompt: string
  status: 'ACTIVE' | 'DISABLED'
  knowledgeEnabled: boolean
  enabledTools: string[]
  createdByUserId?: number
}

export interface TenantQuotaView {
  tenantId: number
  maxAgents: number
  usedAgents: number
  maxAiCallsPerMonth: number
  usedAiCallsThisMonth: number
  maxAiTokensPerMonth: number
  usedAiTokensThisMonth: number
  maxKnowledgeDocuments: number
  usedKnowledgeDocuments: number
  aiModuleEnabled: boolean
  knowledgeModuleEnabled: boolean
}

export interface AgentChatResponse {
  answer: string
  sources: string[]
  citations: Array<{ chunkId: string; documentId: string; text: string }>
}

export const agentApi = {
  list(options?: { silent?: boolean }) {
    return apiClient.get<any, ApiResponse<AgentView[]>>('/v1/agents', {
      skipErrorToast: options?.silent
    })
  },

  updatePrompt(id: number, payload: { systemPrompt: string }) {
    return apiClient.put<any, { data: AgentView }>(`/v1/agents/${id}/prompt`, payload)
  },

  chat(agentId: number, sessionId: string, question: string) {
    return apiClient.post<any, { data: AgentChatResponse }>(`/v1/agents/${agentId}/chat`, {
      sessionId,
      question
    })
  },

  quota() {
    return apiClient.get<any, { data: TenantQuotaView }>('/v1/tenant/quota')
  }
}

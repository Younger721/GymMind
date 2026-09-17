import apiClient from './client'
import type { ApiResponse } from '@/types/api'

export interface HarnessPluginDescriptor {
  id: string
  name: string
  description: string
  category: string
  permission: string
  requiresTenant: boolean
  defaultEnabled: boolean
}

export interface HarnessConfigView {
  enabledPlugins: string[]
  availablePlugins: HarnessPluginDescriptor[]
  systemPrompt: string
  platformScope: boolean
}

export interface HarnessChatResponse {
  answer: string
  sources: string[]
  citations: Array<{ chunkId?: string; documentId?: string; text?: string }>
}

export const harnessApi = {
  getConfig() {
    return apiClient.get<any, ApiResponse<HarnessConfigView>>('/v1/ai/harness/config')
  },

  updateConfig(body: { enabledPlugins: string[]; systemPrompt: string }) {
    return apiClient.put<any, ApiResponse<HarnessConfigView>>('/v1/ai/harness/config', body)
  },

  getPlatformConfig() {
    return apiClient.get<any, ApiResponse<HarnessConfigView>>('/v1/platform/ai/harness/config')
  },

  updatePlatformConfig(body: { enabledPlugins: string[]; systemPrompt: string }) {
    return apiClient.put<any, ApiResponse<HarnessConfigView>>('/v1/platform/ai/harness/config', body)
  },

  chat(body: { sessionId: string; question: string; plugins?: string[] }) {
    return apiClient.post<any, ApiResponse<HarnessChatResponse>>('/v1/ai/harness/chat', body)
  }
}

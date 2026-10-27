import apiClient, { buildAuthenticatedHeaders } from './client'
import type { ApiResponse } from '@/types/api'
import { consumeSseStream } from '@/utils/sseStream'

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
  availableCapabilities?: Array<{
    id: string
    name: string
    description: string
    type: string
    permission?: string | null
    requiresTenant: boolean
    allowedRoles: string[]
  }>
}

export interface HarnessChatResponse {
  answer: string
  sources: string[]
  citations: Array<{ chunkId?: string; documentId?: string; text?: string; excerpt?: string; documentName?: string; documentVersion?: number; knowledgeType?: string; knowledgeKey?: string; effectiveFrom?: string | null; effectiveTo?: string | null }>
}

export interface HarnessProgressDetail {
  kind: 'ACTOR' | 'SCOPE' | 'CHUNK' | 'DOCUMENT' | string
  title: string
  documentId?: string | null
  chunkId?: string | null
  documentName?: string | null
  excerpt?: string | null
  rank?: number | null
}

export interface HarnessProgressEvent {
  pluginId: string
  stage: string
  status: 'STARTED' | 'RUNNING' | 'COMPLETED' | 'FAILED'
  message: string
  resultCount?: number | null
  details?: HarnessProgressDetail[]
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
  },

  async stream(body: { sessionId: string; question: string; plugins?: string[]; history?: Array<{ role: string; content: string }> }, onToken: (token: string) => void, onDone?: (result: HarnessChatResponse) => void, onProgress?: (event: HarnessProgressEvent) => void) {
    const controller = new AbortController()
    const timeout = window.setTimeout(() => controller.abort(), 120000)
    const response = await fetch('/api/v1/ai/harness/chat/stream', {
      method: 'POST',
      headers: buildAuthenticatedHeaders({
        'Content-Type': 'application/json',
        Accept: 'text/event-stream'
      }),
      body: JSON.stringify(body),
      signal: controller.signal
    })
    if (!response.ok || !response.body) throw new Error('stream failed')
    try {
      await consumeSseStream(response.body.getReader(), ({ type: eventType, data }) => {
        if (eventType === 'error') {
          let message = data
          try {
            const payload = JSON.parse(data) as { message?: string; code?: string; traceId?: string }
            message = payload.message || message
            if (payload.code || payload.traceId) {
              const parts = [message]
              if (payload.code) parts.push(`错误码：${payload.code}`)
              if (payload.traceId) parts.push(`追踪号：${payload.traceId}`)
              message = parts.join('；')
            }
          } catch { /* plain text error */ }
          throw new Error(message)
        }
        if (eventType === 'done') {
          onDone?.(JSON.parse(data) as HarnessChatResponse)
        } else if (eventType === 'progress' || eventType === 'tool_decision' || eventType === 'tool_start' || eventType === 'tool_result') {
          onProgress?.(JSON.parse(data) as HarnessProgressEvent)
        } else if (eventType === 'token' || !eventType) {
          try {
            const parsed = JSON.parse(data)
            if (parsed && typeof parsed === 'object' && 'answer' in parsed) onDone?.(parsed as HarnessChatResponse)
            else onToken(data)
          } catch { onToken(data) }
        }
      })
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') throw new Error('AI 响应超时，请稍后重试')
      throw error
    } finally { window.clearTimeout(timeout) }
  }
}

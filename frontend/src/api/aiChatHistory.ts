import apiClient from './client'
import type { ApiResponse } from '@/types/api'
export interface ChatSession { sessionKey: string; roleContext: string; title: string; updatedAt: string }
export interface ChatMessage { role: 'user' | 'assistant'; content: string; sequence: number; createdAt: string }
export const aiChatHistoryApi = {
  list() { return apiClient.get<any, ApiResponse<ChatSession[]>>('/v1/ai/chat/sessions') },
  create(sessionKey: string, title?: string) { return apiClient.post<any, ApiResponse<ChatSession>>('/v1/ai/chat/sessions', { sessionKey, title }) },
  messages(sessionKey: string) { return apiClient.get<any, ApiResponse<ChatMessage[]>>(`/v1/ai/chat/sessions/${encodeURIComponent(sessionKey)}/messages`) },
  archive(sessionKey: string) { return apiClient.delete<any, ApiResponse<void>>(`/v1/ai/chat/sessions/${encodeURIComponent(sessionKey)}`) }
}

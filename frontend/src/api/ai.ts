import { harnessApi } from './harness'
import type { ApiResponse } from '@/types/api'

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

export interface ChatRequest {
  message: string
  history?: ChatMessage[]
  agentId?: number
  sessionId?: string
}

export interface SourceReference {
  documentId: number
  documentName: string
  chunkIndex: number
  excerpt: string
  category: string
  sourceType: string
}

export interface ChatResponse {
  answer: string
  sources: SourceReference[] | string[]
  tokenUsage?: number
  responseTime?: number
  hasReliableSource?: boolean
}

interface AiChatView {
  answer: string
  sources?: string[]
  citations?: Array<{ chunkId?: string; documentId?: string; text?: string }>
}

export const aiApi = {
  async chat(request: ChatRequest): Promise<ApiResponse<ChatResponse>> {
    const sessionId = request.sessionId || `web-${Date.now()}`
    const response = await harnessApi.chat({
      sessionId,
      question: request.message,
      plugins: undefined
    })
    return wrapHarnessResponse(response)
  }
}

function wrapHarnessResponse(response: ApiResponse<{ answer: string; sources?: string[]; citations?: AiChatView['citations'] }>): ApiResponse<ChatResponse> {
  return wrapChatResponse({
    ...response,
    data: {
      answer: response.data.answer,
      sources: response.data.sources,
      citations: response.data.citations
    }
  })
}

function wrapChatResponse(response: ApiResponse<AiChatView>): ApiResponse<ChatResponse> {
  const view = response.data
  const citations = view.citations || []
  const sources: SourceReference[] = citations.map((c, index) => ({
    documentId: index,
    documentName: c.documentId || c.chunkId || '知识库',
    chunkIndex: index,
    excerpt: c.text || '',
    category: 'knowledge',
    sourceType: 'RAG'
  }))

  if (sources.length === 0 && view.sources?.length) {
    view.sources.forEach((s, index) => {
      sources.push({
        documentId: index,
        documentName: s,
        chunkIndex: index,
        excerpt: s,
        category: 'knowledge',
        sourceType: 'RAG'
      })
    })
  }

  return {
    ...response,
    data: {
      answer: view.answer,
      sources,
      hasReliableSource: sources.length > 0,
      responseTime: 0,
      tokenUsage: 0
    }
  }
}

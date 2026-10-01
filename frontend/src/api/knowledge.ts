import apiClient from './client'
import type { ApiResponse } from '@/types/api'

/** 与后端 KnowledgeDocumentView 对齐 */
export interface KnowledgeDocumentView {
  id: number
  tenantId: number | null
  ownerUserId: number | null
  fileName: string
  objectKey: string
  visibility: string
  status: string
  category?: string
  published?: boolean
  chunkStrategy: string
  chunkStrategyVersion: number
  audienceRoles: string[]
}

export const knowledgeApi = {
  /** 上传文档；平台管理员由后端保存为平台公共文档，健身房管理员绑定当前租户。 */
  uploadDocument(file: File, visibility = 'TENANT', category = 'GENERAL', audienceRoles: string[] = []) {
    const formData = new FormData()
    formData.append('file', file)
    const params = new URLSearchParams({ visibility, category })
    audienceRoles.forEach(role => params.append('audienceRoles', role))
    return apiClient.post<any, ApiResponse<KnowledgeDocumentView>>(
      `/v1/knowledge/documents?${params.toString()}`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    )
  },

  listDocuments() {
    return apiClient.get<any, ApiResponse<KnowledgeDocumentView[]>>('/v1/knowledge/documents')
  },

  getDocument(id: number) {
    return apiClient.get<any, ApiResponse<KnowledgeDocumentView>>(`/v1/knowledge/documents/${id}`)
  },

  updateDocument(id: number, visibility: 'TENANT' | 'PRIVATE_USER', audienceRoles: string[] = []) {
    const params = new URLSearchParams({ visibility })
    audienceRoles.forEach(role => params.append('audienceRoles', role))
    return apiClient.patch<any, ApiResponse<KnowledgeDocumentView>>(
      `/v1/knowledge/documents/${id}?${params.toString()}`
    )
  },

  deleteDocument(id: number) {
    return apiClient.delete(`/v1/knowledge/documents/${id}`)
  },

  setPublication(id: number, published: boolean) {
    return apiClient.patch(`/v1/knowledge/documents/${id}/publication`, null, { params: { published } })
  }
}

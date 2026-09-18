import apiClient from './client'
import type { ApiResponse } from '@/types/api'

/** 与后端 KnowledgeDocumentView 对齐 */
export interface KnowledgeDocumentView {
  id: number
  tenantId: number
  ownerUserId: number
  fileName: string
  objectKey: string
  visibility: string
  status: string
}

export const knowledgeApi = {
  /** 上传文档（绑定当前租户，后端自动解析） */
  uploadDocument(file: File, visibility = 'TENANT') {
    const formData = new FormData()
    formData.append('file', file)
    return apiClient.post<any, ApiResponse<KnowledgeDocumentView>>(
      `/v1/knowledge/documents?visibility=${visibility}`,
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

  updateDocument(id: number, visibility: 'TENANT' | 'PRIVATE_USER') {
    return apiClient.patch<any, ApiResponse<KnowledgeDocumentView>>(
      `/v1/knowledge/documents/${id}?visibility=${visibility}`
    )
  },

  deleteDocument(id: number) {
    return apiClient.delete(`/v1/knowledge/documents/${id}`)
  },

  reindexDocument(id: number) {
    return apiClient.post(`/v1/knowledge/documents/${id}/reindex`)
  }
}

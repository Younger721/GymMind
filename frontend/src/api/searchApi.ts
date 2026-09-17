import apiClient from './client'
import type { ApiResponse } from '@/types/api'

export const searchApi = {
  searchVideos(q: string) {
    return apiClient.get<any, ApiResponse<unknown[]>>('/v1/search/videos', { params: { q } })
  },
  searchArticles(q: string) {
    return apiClient.get<any, ApiResponse<unknown[]>>('/v1/search/articles', { params: { q } })
  },
  importArticle(body: { url: string; title: string; summary?: string }) {
    return apiClient.post('/v1/search/import', body)
  },
  saveArticle(body: { url: string; title: string; summary?: string }) {
    return apiClient.post('/v1/search/articles', body)
  },
  searchKnowledge(q: string) {
    return apiClient.get<any, ApiResponse<unknown[]>>('/v1/knowledge/search', { params: { q } })
  }
}

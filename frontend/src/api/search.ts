import apiClient from './client'

export interface SearchResult {
  title: string
  url: string
  snippet: string
  source: string
  publishedDate: string
  relevanceScore: number
}

export interface SearchResponse {
  results: SearchResult[]
  totalResults: number
  query: string
}

export interface ImportRequest {
  title: string
  url: string
  category?: string
}

export const searchApi = {
  search(query: string) {
    return apiClient.get<any, { data: SearchResponse }>('/search/web', {
      params: { q: query }
    })
  },

  importFromWeb(request: ImportRequest) {
    return apiClient.post('/search/import', request)
  }
}

import axios from './client'

export interface KnowledgeImage {
  id: number
  userId: number
  documentId: number
  imageName: string
  imageUrl: string
  description: string
  category: string
  detectedAction: string
  muscleGroup: string
  difficulty: string
  status: string
  errorMessage?: string
  createdAt: string
  vectorId: string
  esId: string
}

export interface ImageUploadResponse {
  imageId: number
  imageName: string
  status: string
  message: string
}

export const imageApi = {
  uploadImage: (file: File, category?: string, documentId?: number) => {
    const formData = new FormData()
    formData.append('file', file)
    if (category) formData.append('category', category)
    if (documentId) formData.append('documentId', documentId.toString())

    return axios.post<ImageUploadResponse>('/api/knowledge/images/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  getUserImages: () =>
    axios.get<KnowledgeImage[]>('/api/knowledge/images'),

  getImageById: (imageId: number) =>
    axios.get<KnowledgeImage>(`/api/knowledge/images/${imageId}`),

  deleteImage: (imageId: number) =>
    axios.delete(`/api/knowledge/images/${imageId}`)
}

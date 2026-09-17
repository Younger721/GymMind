import apiClient from '@/api/client'
import type { AxiosRequestConfig } from 'axios'

// 统一的请求函数
export default function request<T = any>(config: AxiosRequestConfig): Promise<T> {
  return apiClient(config)
}

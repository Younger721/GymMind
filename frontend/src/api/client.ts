import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'

declare module 'axios' {
  interface AxiosRequestConfig {
    /** 为 true 时不弹出全局错误 Toast（由调用方自行处理） */
    skipErrorToast?: boolean
  }
}
import { ElMessage } from 'element-plus'
import router from '@/router'

const apiClient: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json'
  }
})

apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

function extractMessage(data: unknown): string {
  if (data && typeof data === 'object' && 'message' in data) {
    const msg = (data as { message?: string }).message
    if (msg) return msg
  }
  return '请求失败'
}

apiClient.interceptors.response.use(
  (response: AxiosResponse) => response.data,
  (error) => {
    const skipToast = error.config?.skipErrorToast === true

    if (error.response) {
      const { status, data } = error.response
      const message = extractMessage(data)

      if (!skipToast) {
        switch (status) {
          case 401:
            ElMessage.error('登录已过期，请重新登录')
            localStorage.removeItem('token')
            localStorage.removeItem('refreshToken')
            router.push('/login')
            break
          case 403:
            ElMessage.error('没有权限执行此操作')
            break
          case 404:
            ElMessage.error('接口不存在，请检查前后端版本')
            break
          case 500:
            ElMessage.error(message || '服务器错误')
            break
          default:
            ElMessage.error(message)
        }
      }
    } else if (!skipToast) {
      if (error.request) {
        ElMessage.error('无法连接后端，请确认 http://localhost:8080 已启动')
      } else {
        ElMessage.error('请求失败')
      }
    }

    return Promise.reject(error)
  }
)

export default apiClient

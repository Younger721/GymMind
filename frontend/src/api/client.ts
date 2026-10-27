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

function hasPlatformAdminRole(): boolean {
  try {
    const raw = localStorage.getItem('roles')
    const roles = raw ? JSON.parse(raw) : []
    return Array.isArray(roles) && roles.includes('PLATFORM_ADMIN')
  } catch {
    return false
  }
}

function readPlatformContextTenantId(): number | null {
  const raw = localStorage.getItem('platformContextTenantId')
  const value = raw ? Number(raw) : NaN
  return Number.isSafeInteger(value) && value > 0 ? value : null
}

/** 与 axios 拦截器一致：Bearer + 平台管理员顶栏门店 X-Tenant-Id */
export function buildAuthenticatedHeaders(extra?: Record<string, string>): Record<string, string> {
  const headers: Record<string, string> = { ...(extra ?? {}) }
  const token = localStorage.getItem('token')
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  const contextTenantId = readPlatformContextTenantId()
  if (hasPlatformAdminRole() && contextTenantId != null) {
    headers['X-Tenant-Id'] = String(contextTenantId)
  }
  return headers
}

apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    if (config.headers) {
      // Authentication endpoints must establish identity before any tenant
      // context is applied. A stale platform context header would otherwise
      // be rejected by PlatformTenantContextFilter as an unauthenticated
      // request (403), preventing login after switching sessions.
      if (isAuthRequest(config.url)) {
        delete config.headers['X-Tenant-Id']
        return config
      }
      const contextTenantId = readPlatformContextTenantId()
      if (hasPlatformAdminRole() && contextTenantId != null) {
        config.headers['X-Tenant-Id'] = String(contextTenantId)
      } else {
        delete config.headers['X-Tenant-Id']
      }
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

function isAuthRequest(url: string | undefined): boolean {
  if (!url) return false
  return /\/v1\/auth\/(login|register|refresh)/.test(url)
}

function isOnLoginPage(): boolean {
  return router.currentRoute.value.path === '/login'
    || router.currentRoute.value.path === '/register'
}

/** 并行 401/403 时只弹一次「请重新登录」，避免首页多接口同时失败刷屏 */
let sessionExpiredNoticeShown = false

export function resetSessionExpiredNotice() {
  sessionExpiredNoticeShown = false
}

function handleSessionExpired() {
  if (sessionExpiredNoticeShown || isOnLoginPage()) {
    return
  }
  sessionExpiredNoticeShown = true
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  ElMessage.error('登录已过期，请重新登录')
  router.push('/login')
}

function shouldSuppressAuthErrorToast(): boolean {
  return sessionExpiredNoticeShown || isOnLoginPage()
}

apiClient.interceptors.response.use(
  (response: AxiosResponse) => response.data,
  (error) => {
    const skipToast = error.config?.skipErrorToast === true
    const requestUrl = error.config?.url as string | undefined

    if (error.response) {
      const { status, data } = error.response
      const message = extractMessage(data)

      if (!skipToast) {
        switch (status) {
          case 401:
            if (isAuthRequest(requestUrl)) {
              // 登录/注册失败：优先展示后端中文 message
              ElMessage.error(message || '账号或密码错误')
            } else {
              handleSessionExpired()
            }
            break
          case 403:
            if (data && typeof data === 'object' && 'code' in data
              && (data as { code?: string }).code === 'TENANT_CONTEXT_REQUIRED') {
              if (!shouldSuppressAuthErrorToast()) {
                ElMessage.warning('请先选择租户')
              }
              break
            }
            if (data && typeof data === 'object' && 'code' in data
              && (data as { code?: string }).code === 'UNAUTHENTICATED') {
              handleSessionExpired()
              break
            }
            // 未登录/登出后 Spring 常返回 403，与 401 一并收敛，避免重复 Toast
            if (shouldSuppressAuthErrorToast() || !localStorage.getItem('token')) {
              break
            }
            ElMessage.error('没有权限执行此操作')
            break
          case 404:
            // 业务级资源不存在：未映射的接口在本后端会落到 500 INTERNAL_ERROR，
            // 因此 404 一定是业务资源缺失，直接展示后端提示，避免误报接口不存在
            ElMessage.error(message || '资源不存在')
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

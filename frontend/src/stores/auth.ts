import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, type LoginRequest, type RegisterRequest } from '@/api/auth'
import { resetSessionExpiredNotice } from '@/api/client'
import { ElMessage } from 'element-plus'
import router from '@/router'

const PLATFORM_CONTEXT_TENANT_KEY = 'platformContextTenantId'

function persistAuth(
  accessToken: string,
  refreshToken: string,
  userId: number,
  displayName: string,
  email: string,
  tenantId: number | null,
  roles: string[],
  permissions: string[]
) {
  resetSessionExpiredNotice()
  localStorage.setItem('token', accessToken)
  localStorage.setItem('refreshToken', refreshToken)
  localStorage.setItem('userId', String(userId))
  localStorage.setItem('username', displayName)
  localStorage.setItem('email', email)
  if (tenantId != null) {
    localStorage.setItem('tenantId', String(tenantId))
  } else {
    localStorage.removeItem('tenantId')
  }
  localStorage.setItem('roles', JSON.stringify(roles))
  localStorage.setItem('permissions', JSON.stringify(permissions))
}

function readTenantId(): number | null {
  const raw = localStorage.getItem('tenantId')
  return raw ? Number(raw) : null
}

function readPlatformContextTenantId(): number | null {
  const raw = localStorage.getItem(PLATFORM_CONTEXT_TENANT_KEY)
  const value = raw ? Number(raw) : NaN
  return Number.isSafeInteger(value) && value > 0 ? value : null
}

function readRoles(): string[] {
  try {
    const raw = localStorage.getItem('roles')
    return raw ? (JSON.parse(raw) as string[]) : []
  } catch {
    return []
  }
}

function readPermissions(): string[] {
  try {
    const raw = localStorage.getItem('permissions')
    return raw ? (JSON.parse(raw) as string[]) : []
  } catch {
    return []
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const refreshToken = ref<string | null>(localStorage.getItem('refreshToken'))
  const userId = ref<number | null>(Number(localStorage.getItem('userId')) || null)
  const username = ref<string | null>(localStorage.getItem('username'))
  const email = ref<string | null>(localStorage.getItem('email'))
  const tenantId = ref<number | null>(readTenantId())
  const platformContextTenantId = ref<number | null>(readPlatformContextTenantId())
  const roles = ref<string[]>(readRoles())
  const permissions = ref<string[]>(readPermissions())

  // A token without the role claims persisted by the current login response is
  // an old or incomplete session. It must not gain platform administrator
  // access as a fallback.
  const isAuthenticated = computed(() => !!token.value && roles.value.length > 0)
  const isPlatformAdmin = computed(() => {
    return tenantId.value == null && roles.value.includes('PLATFORM_ADMIN')
  })
  const isTenantUser = computed(() => tenantId.value != null)
  const hasPlatformTenantContext = computed(() => isPlatformAdmin.value && platformContextTenantId.value != null)

  function setPlatformContextTenantId(value: number | null) {
    if (!isPlatformAdmin.value || value == null) {
      platformContextTenantId.value = null
      localStorage.removeItem(PLATFORM_CONTEXT_TENANT_KEY)
      return
    }
    const normalized = Number(value)
    if (!Number.isSafeInteger(normalized) || normalized <= 0) {
      platformContextTenantId.value = null
      localStorage.removeItem(PLATFORM_CONTEXT_TENANT_KEY)
      return
    }
    platformContextTenantId.value = normalized
    localStorage.setItem(PLATFORM_CONTEXT_TENANT_KEY, String(normalized))
  }

  function applyAuthResponse(data: {
    userId: number
    tenantId?: number | null
    username?: string
    email: string
    displayName: string
    roles?: string[]
    permissions?: string[]
    accessToken: string
    refreshToken: string
  }) {
    const loginName = data.username?.trim() || data.displayName
    token.value = data.accessToken
    refreshToken.value = data.refreshToken
    userId.value = data.userId
    username.value = loginName
    email.value = data.email
    tenantId.value = data.tenantId ?? null
    roles.value = data.roles ?? []
    permissions.value = data.permissions ?? []
    // A new login must choose a tenant explicitly instead of inheriting the
    // previous platform administrator's tenant context.
    platformContextTenantId.value = null
    localStorage.removeItem(PLATFORM_CONTEXT_TENANT_KEY)
    persistAuth(
      data.accessToken,
      data.refreshToken,
      data.userId,
      loginName,
      data.email,
      tenantId.value,
      roles.value,
      permissions.value
    )
  }

  async function login(credentials: LoginRequest) {
    try {
      const response = await authApi.login(credentials)
      if (!response.success || !response.data) {
        ElMessage.error(response.message || '登录失败')
        throw new Error(response.message || '登录失败')
      }
      applyAuthResponse(response.data)
      ElMessage.success('登录成功')
      router.push('/dashboard')
    } catch (error: unknown) {
      console.error('Login failed:', error)
      const axiosError = error as {
        response?: { status?: number; data?: { message?: string } }
      }
      const status = axiosError.response?.status
      const apiMessage = axiosError.response?.data?.message
      if (status === 401) {
        ElMessage.error(apiMessage || '账号或密码错误')
      } else if (status === 400) {
        ElMessage.error(apiMessage || '请填写有效的账号和密码')
      } else if (!(error instanceof Error && error.message)) {
        ElMessage.error(apiMessage || '登录失败，请稍后重试')
      }
      throw error
    }
  }

  async function register(data: RegisterRequest) {
    try {
      const response = await authApi.register(data)
      if (!response.success || !response.data) {
        throw new Error(response.message || '注册失败')
      }
      applyAuthResponse(response.data)
      ElMessage.success('注册成功')
      router.push('/dashboard')
    } catch (error) {
      console.error('Registration failed:', error)
      throw error
    }
  }

  async function logout() {
    try {
      if (refreshToken.value) {
        await authApi.logout(refreshToken.value)
      }
    } catch (error) {
      console.error('Logout request failed:', error)
    } finally {
      token.value = null
      refreshToken.value = null
      userId.value = null
      username.value = null
      email.value = null
      tenantId.value = null
      platformContextTenantId.value = null
      roles.value = []
      permissions.value = []
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('userId')
      localStorage.removeItem('username')
      localStorage.removeItem('email')
      localStorage.removeItem('tenantId')
      localStorage.removeItem(PLATFORM_CONTEXT_TENANT_KEY)
      localStorage.removeItem('roles')
      localStorage.removeItem('permissions')
      ElMessage.success('已退出登录')
      router.push('/login')
    }
  }

  return {
    token,
    refreshToken,
    userId,
    username,
    email,
    tenantId,
    platformContextTenantId,
    hasPlatformTenantContext,
    roles,
    permissions,
    isPlatformAdmin,
    isTenantUser,
    isAuthenticated,
    setPlatformContextTenantId,
    login,
    register,
    logout
  }
})

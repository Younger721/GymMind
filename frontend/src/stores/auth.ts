import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, type LoginRequest, type RegisterRequest } from '@/api/auth'
import { ElMessage } from 'element-plus'
import router from '@/router'

function persistAuth(
  accessToken: string,
  refreshToken: string,
  userId: number,
  displayName: string,
  email: string,
  tenantId: number | null,
  roles: string[]
) {
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
}

function readTenantId(): number | null {
  const raw = localStorage.getItem('tenantId')
  return raw ? Number(raw) : null
}

function readRoles(): string[] {
  try {
    const raw = localStorage.getItem('roles')
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
  const roles = ref<string[]>(readRoles())

  const isAuthenticated = computed(() => !!token.value)
  const isPlatformAdmin = computed(() => {
    if (tenantId.value != null) return false
    if (roles.value.includes('PLATFORM_ADMIN')) return true
    // 旧会话未持久化 roles 时：无 tenantId 且已登录视为平台管理员
    return !!token.value
  })
  const isTenantUser = computed(() => tenantId.value != null)

  function applyAuthResponse(data: {
    userId: number
    tenantId?: number | null
    email: string
    displayName: string
    roles?: string[]
    accessToken: string
    refreshToken: string
  }) {
    token.value = data.accessToken
    refreshToken.value = data.refreshToken
    userId.value = data.userId
    username.value = data.displayName
    email.value = data.email
    tenantId.value = data.tenantId ?? null
    roles.value = data.roles ?? []
    persistAuth(
      data.accessToken,
      data.refreshToken,
      data.userId,
      data.displayName,
      data.email,
      tenantId.value,
      roles.value
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
      roles.value = []
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('userId')
      localStorage.removeItem('username')
      localStorage.removeItem('email')
      localStorage.removeItem('tenantId')
      localStorage.removeItem('roles')
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
    roles,
    isPlatformAdmin,
    isTenantUser,
    isAuthenticated,
    login,
    register,
    logout
  }
})

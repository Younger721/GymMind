import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

/** 区分平台管理员与租户用户，避免调用租户专属 API 触发 403 */
export function useTenantContext() {
  const authStore = useAuthStore()

  const isPlatformAdmin = computed(() => authStore.isPlatformAdmin)
  const isTenantUser = computed(() => authStore.isTenantUser)

  const tenantOnlyHint =
    '当前为平台管理员账号，无法使用门店租户功能。请前往「平台管理」，或使用注册页创建/登录租户账号。'

  return { isPlatformAdmin, isTenantUser, tenantOnlyHint }
}

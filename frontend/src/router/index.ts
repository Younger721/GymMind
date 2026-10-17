import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { hasMenuPermission, MENU_CODES } from '@/utils/menuPermissions'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/knowledge/:id/edit',
      name: 'knowledge-doc-edit',
      component: () => import('@/views/KnowledgeDocEditorView.vue'),
      meta: { requiresAuth: true, menuPermission: MENU_CODES.KNOWLEDGE, fullscreen: true }
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: '/assistant' },
        { path: 'dashboard', name: 'dashboard', component: () => import('@/views/DashboardView.vue'), meta: { menuPermission: MENU_CODES.DASHBOARD } },
        { path: 'assistant', name: 'assistant', component: () => import('@/views/AssistantView.vue'), meta: { menuPermission: MENU_CODES.ASSISTANT } },
        { path: 'knowledge', name: 'knowledge', component: () => import('@/views/KnowledgeView.vue'), meta: { menuPermission: MENU_CODES.KNOWLEDGE } },
        { path: 'plan', name: 'plan', redirect: '/assistant', meta: { menuPermission: MENU_CODES.ASSISTANT } },
        { path: 'operations', name: 'operations', redirect: '/courses', meta: { menuPermission: MENU_CODES.COURSES } },
        { path: 'check-ins', name: 'check-ins', redirect: '/courses', meta: { menuPermission: MENU_CODES.COURSES } },
        { path: 'membership', name: 'membership', redirect: '/courses', meta: { menuPermission: MENU_CODES.COURSES } },
        { path: 'training', name: 'training', component: () => import('@/views/business/TrainingView.vue'), meta: { menuPermission: MENU_CODES.TRAINING } },
        { path: 'analytics', name: 'analytics', component: () => import('@/views/business/AnalyticsHubView.vue'), meta: { menuPermission: MENU_CODES.ANALYTICS } },
        { path: 'member-risks', redirect: '/assistant', meta: { menuPermission: MENU_CODES.ANALYTICS } },
        { path: 'training/ai-plan', name: 'training-ai-plan', component: () => import('@/views/PlanGeneratorView.vue'), meta: { menuPermission: MENU_CODES.TRAINING } },
        { path: 'admin', name: 'admin', component: () => import('@/views/business/AdminView.vue'), meta: { menuPermission: MENU_CODES.ADMIN } },
        { path: 'audit', name: 'audit', component: () => import('@/views/business/AuditLogView.vue'), meta: { menuPermission: MENU_CODES.AUDIT } },
        { path: 'ai-usage', name: 'ai-usage', component: () => import('@/views/business/AiUsageView.vue'), meta: { menuPermission: MENU_CODES.AI_USAGE } },
        { path: 'tenant-settings', name: 'tenant-settings', component: () => import('@/views/business/TenantSettingsView.vue'), meta: { menuPermission: MENU_CODES.TENANT_SETTINGS } },
        { path: 'platform', name: 'platform', redirect: '/dashboard' },
        { path: 'role-permissions', name: 'role-permissions', component: () => import('@/views/business/RolePermissionView.vue'), meta: { menuPermission: MENU_CODES.ROLE_PERMISSIONS } },
        { path: 'profile', name: 'profile', component: () => import('@/views/ProfileView.vue'), meta: { menuPermission: MENU_CODES.PROFILE } },
        { path: 'plan-generator', redirect: '/assistant', meta: { menuPermission: MENU_CODES.ASSISTANT } },
        { path: 'agents/manage', name: 'agents-manage', component: () => import('@/views/AgentManageView.vue'), meta: { menuPermission: MENU_CODES.AI_CONFIG } },
        { path: 'ai-tools', redirect: '/assistant' },
        { path: 'search', redirect: '/knowledge' },
        { path: 'members', redirect: '/courses' },
        { path: 'courses', name: 'courses', component: () => import('@/views/business/CourseCenterView.vue'), meta: { menuPermission: MENU_CODES.COURSES } },
        { path: 'membership-changes', name: 'membership-changes', component: () => import('@/views/business/MembershipChangeApprovalView.vue'), meta: { menuPermission: MENU_CODES.OPERATIONS } },
        { path: 'equipment', name: 'equipment', component: () => import('@/views/business/EquipmentManagementView.vue'), meta: { menuPermission: MENU_CODES.EQUIPMENT } },
        { path: 'bookings', redirect: '/courses' },
        { path: 'exercises', name: 'exercises', redirect: '/training', meta: { menuPermission: MENU_CODES.TRAINING } },
        { path: 'workouts', redirect: '/training' },
        { path: 'nutrition', redirect: '/training' },
        { path: 'reports', redirect: '/analytics' },
        { path: ':pathMatch(.*)*', redirect: '/dashboard' }
      ]
    }
  ]
})

router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore()
  const requiresAuth = to.matched.some(r => r.meta.requiresAuth)

  if (requiresAuth && !authStore.isAuthenticated) {
    next('/login')
    return
  }
  if (!requiresAuth && authStore.isAuthenticated && (to.path === '/login' || to.path === '/register')) {
    next('/dashboard')
    return
  }
  const menuPermission = to.matched
    .map(record => record.meta.menuPermission as string | undefined)
    .find(Boolean)
  if (requiresAuth && menuPermission && !hasMenuPermission(menuPermission)) {
    const fallback = MENU_CODES.DASHBOARD
    if (to.path !== '/dashboard' && hasMenuPermission(fallback)) {
      next('/dashboard')
    } else {
      next(false)
    }
    return
  }
  next()
})

export default router

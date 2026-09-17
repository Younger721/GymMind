import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

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
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', name: 'dashboard', component: () => import('@/views/DashboardView.vue') },
        { path: 'assistant', name: 'assistant', component: () => import('@/views/AssistantView.vue') },
        { path: 'knowledge', name: 'knowledge', component: () => import('@/views/KnowledgeView.vue') },
        { path: 'plan', name: 'plan', component: () => import('@/views/PlanGeneratorView.vue') },
        { path: 'agents/manage', name: 'agent-manage', component: () => import('@/views/AgentManageView.vue') },
        { path: 'ai-tools', name: 'ai-tools', component: () => import('@/views/business/AiToolsView.vue') },
        { path: 'operations', name: 'operations', component: () => import('@/views/business/OperationsView.vue') },
        { path: 'membership', name: 'membership', component: () => import('@/views/business/MembershipManageView.vue') },
        { path: 'training', name: 'training', component: () => import('@/views/business/TrainingView.vue') },
        { path: 'analytics', name: 'analytics', component: () => import('@/views/business/AnalyticsHubView.vue') },
        { path: 'search', name: 'search', component: () => import('@/views/business/SearchToolsView.vue') },
        { path: 'admin', name: 'admin', component: () => import('@/views/business/AdminView.vue') },
        { path: 'tenant-settings', name: 'tenant-settings', component: () => import('@/views/business/TenantSettingsView.vue') },
        { path: 'platform', name: 'platform', component: () => import('@/views/business/PlatformView.vue') },
        { path: 'profile', name: 'profile', component: () => import('@/views/ProfileView.vue') },
        { path: 'plan-generator', redirect: '/plan' },
        { path: 'members', redirect: '/operations' },
        { path: 'courses', redirect: '/operations' },
        { path: 'bookings', redirect: '/operations' },
        { path: 'exercises', redirect: '/training' },
        { path: 'workouts', redirect: '/training' },
        { path: 'nutrition', redirect: '/training' },
        { path: 'reports', redirect: '/analytics' },
        { path: ':pathMatch(.*)*', redirect: '/dashboard' }
      ]
    }
  ]
})

router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()
  const requiresAuth = to.matched.some(r => r.meta.requiresAuth)

  if (requiresAuth && !authStore.isAuthenticated) {
    next('/login')
  } else if (!requiresAuth && authStore.isAuthenticated && (to.path === '/login' || to.path === '/register')) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router

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
        {
          path: '',
          redirect: '/dashboard'
        },
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/DashboardView.vue')
        },
        {
          path: 'assistant',
          name: 'assistant',
          component: () => import('@/views/AssistantView.vue')
        },
        {
          path: 'knowledge',
          name: 'knowledge',
          component: () => import('@/views/KnowledgeView.vue')
        },
        {
          path: 'search',
          name: 'search',
          component: () => import('@/views/SearchView.vue')
        },
        {
          path: 'workout',
          name: 'workout',
          component: () => import('@/views/WorkoutView.vue')
        },
        {
          path: 'nutrition',
          name: 'nutrition',
          component: () => import('@/views/NutritionView.vue')
        },
        {
          path: 'progress',
          name: 'progress',
          component: () => import('@/views/ProgressView.vue')
        },
        {
          path: 'report',
          name: 'report',
          component: () => import('@/views/ReportView.vue')
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/ProfileView.vue')
        },
        {
          path: 'plan-generator',
          name: 'plan-generator',
          component: () => import('@/views/PlanGeneratorView.vue')
        },
        {
          path: 'social',
          name: 'social',
          component: () => import('@/views/SocialView.vue')
        },
        {
          path: 'challenges',
          name: 'challenges',
          component: () => import('@/views/ChallengeView.vue')
        },
        {
          path: 'exercise-library',
          name: 'exercise-library',
          component: () => import('@/views/ExerciseLibraryView.vue')
        }
      ]
    }
  ]
})

// Navigation guard
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)

  if (requiresAuth && !authStore.isAuthenticated) {
    next('/login')
  } else if (!requiresAuth && authStore.isAuthenticated && (to.path === '/login' || to.path === '/register')) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router

<template>
  <el-container class="main-layout">
    <el-aside :width="isCollapsed ? '64px' : '240px'" class="sidebar">
      <div class="sidebar-header">
        <router-link to="/dashboard" class="logo-link">
          <svg class="logo-mark" width="28" height="28" viewBox="0 0 28 28" fill="none" aria-hidden="true">
            <rect x="4" y="5" width="6" height="18" rx="1.5" fill="currentColor"/>
            <rect x="18" y="5" width="6" height="18" rx="1.5" fill="currentColor"/>
            <rect x="9" y="11" width="10" height="3" rx="1" fill="currentColor"/>
          </svg>
          <span v-if="!isCollapsed" class="logo-text">GymMind</span>
        </router-link>
      </div>

      <nav class="sidebar-nav" aria-label="主导航">
        <el-menu
          :default-active="$route.path"
          router
          class="sidebar-menu"
          :collapse="isCollapsed"
          background-color="transparent"
        >
          <el-menu-item index="/dashboard">
            <el-icon><HomeFilled /></el-icon>
            <template #title>仪表板</template>
          </el-menu-item>
          <el-menu-item index="/plan-generator">
            <el-icon><Calendar /></el-icon>
            <template #title>训练计划</template>
          </el-menu-item>
          <el-menu-item index="/exercise-library">
            <el-icon><VideoPlay /></el-icon>
            <template #title>动作库</template>
          </el-menu-item>
          <el-menu-item index="/social">
            <el-icon><ChatDotSquare /></el-icon>
            <template #title>社区动态</template>
          </el-menu-item>
          <el-menu-item index="/challenges">
            <el-icon><Trophy /></el-icon>
            <template #title>挑战赛</template>
          </el-menu-item>
          <el-menu-item index="/assistant">
            <el-icon><ChatDotRound /></el-icon>
            <template #title>AI 助手</template>
          </el-menu-item>
          <el-menu-item index="/knowledge">
            <el-icon><Reading /></el-icon>
            <template #title>知识库</template>
          </el-menu-item>
          <el-menu-item index="/search">
            <el-icon><Search /></el-icon>
            <template #title>智能搜索</template>
          </el-menu-item>
          <el-menu-item index="/report">
            <el-icon><Document /></el-icon>
            <template #title>周报分析</template>
          </el-menu-item>
          <el-menu-item index="/profile">
            <el-icon><User /></el-icon>
            <template #title>个人设置</template>
          </el-menu-item>
        </el-menu>
      </nav>

      <div class="sidebar-footer">
        <button
          type="button"
          class="collapse-btn"
          :aria-label="isCollapsed ? '展开侧边栏' : '收起侧边栏'"
          @click="isCollapsed = !isCollapsed"
        >
          <el-icon v-if="isCollapsed"><DArrowRight /></el-icon>
          <el-icon v-else><DArrowLeft /></el-icon>
        </button>
      </div>
    </el-aside>

    <el-container class="main-content">
      <el-header class="top-header">
        <div class="header-left">
          <h1 class="page-title">{{ pageTitle }}</h1>
          <p v-if="pageSubtitle" class="page-subtitle">{{ pageSubtitle }}</p>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click" @command="handleCommand">
            <button type="button" class="user-trigger">
              <el-avatar :size="32" class="user-avatar">
                {{ authStore.username?.charAt(0).toUpperCase() }}
              </el-avatar>
              <span class="username">{{ authStore.username }}</span>
              <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人设置
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="page-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const isCollapsed = ref(false)

const pageMeta: Record<string, { title: string; subtitle?: string }> = {
  '/dashboard': { title: '仪表板', subtitle: '今日训练与营养概览' },
  '/plan-generator': { title: '训练计划', subtitle: '生成个性化训练方案' },
  '/exercise-library': { title: '动作库', subtitle: '专业健身动作视频指导' },
  '/social': { title: '社区动态', subtitle: '分享训练心得与进展' },
  '/challenges': { title: '挑战赛', subtitle: '参与健身挑战，追踪排名' },
  '/assistant': { title: 'AI 助手', subtitle: '基于知识库的智能健身咨询' },
  '/knowledge': { title: '知识库', subtitle: '管理健身文档与资料' },
  '/search': { title: '智能搜索', subtitle: '跨文档语义检索' },
  '/report': { title: '周报分析', subtitle: '每周训练数据回顾' },
  '/profile': { title: '个人设置', subtitle: '账户与身体数据管理' }
}

const pageTitle = computed(() => pageMeta[route.path]?.title || 'GymMind')
const pageSubtitle = computed(() => pageMeta[route.path]?.subtitle)

const handleCommand = async (command: string) => {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '退出确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await authStore.logout()
    } catch {
      /* cancelled */
    }
  } else if (command === 'profile') {
    router.push('/profile')
  }
}
</script>

<style scoped>
.main-layout {
  min-height: 100dvh;
  background: var(--canvas);
}

.sidebar {
  background: var(--surface);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  transition: width var(--transition-base);
  position: relative;
}

.sidebar-header {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 var(--space-4);
  border-bottom: 1px solid var(--border);
}

.logo-link {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  text-decoration: none;
  color: var(--ink);
  min-width: 0;
}

.logo-mark {
  flex-shrink: 0;
  color: var(--ink);
}

.logo-text {
  font-family: var(--font-serif);
  font-size: var(--text-lg);
  font-weight: var(--weight-medium);
  letter-spacing: -0.02em;
  white-space: nowrap;
}

.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-3) var(--space-2);
}

.sidebar-menu {
  border: none;
}

.sidebar-menu :deep(.el-menu-item) {
  height: 40px;
  line-height: 40px;
  margin-bottom: 2px;
  border-radius: var(--radius-sm);
  color: var(--ink-secondary);
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  transition: all var(--transition-base);
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background: var(--gray-100) !important;
  color: var(--ink) !important;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: var(--ink) !important;
  color: var(--ink-inverse) !important;
  font-weight: var(--weight-semibold);
}

.sidebar-menu :deep(.el-menu-item .el-icon) {
  font-size: 18px;
}

.sidebar-footer {
  padding: var(--space-3);
  border-top: 1px solid var(--border);
}

.collapse-btn {
  width: 100%;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  color: var(--ink-secondary);
  cursor: pointer;
  transition: all var(--transition-base);
}

.collapse-btn:hover {
  background: var(--gray-100);
  color: var(--ink);
}

.collapse-btn:active {
  transform: scale(0.98);
}

.main-content {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.top-header {
  height: auto;
  min-height: 64px;
  background: var(--surface);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-8);
  gap: var(--space-4);
}

.header-left {
  min-width: 0;
}

.page-title {
  font-family: var(--font-serif);
  font-size: var(--text-2xl);
  font-weight: var(--weight-medium);
  color: var(--ink);
  margin: 0;
  letter-spacing: -0.02em;
  line-height: 1.2;
}

.page-subtitle {
  font-size: var(--text-sm);
  color: var(--ink-secondary);
  margin: var(--space-1) 0 0;
  max-width: none;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2) var(--space-3);
  background: transparent;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-base);
  font-family: inherit;
}

.user-trigger:hover {
  background: var(--gray-100);
  border-color: var(--gray-300);
}

.user-avatar {
  background: var(--ink);
  color: var(--ink-inverse);
  font-weight: var(--weight-semibold);
  font-size: var(--text-sm);
}

.username {
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--ink);
}

.dropdown-icon {
  color: var(--ink-tertiary);
  font-size: 12px;
}

.page-content {
  background: var(--canvas);
  padding: var(--space-8);
  overflow-y: auto;
  flex: 1;
}

:deep(.el-dropdown-menu__item) {
  padding: var(--space-3) var(--space-4);
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
}

@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: var(--z-fixed);
  }

  .page-content {
    padding: var(--space-4);
  }

  .username {
    display: none;
  }

  .page-subtitle {
    display: none;
  }
}
</style>

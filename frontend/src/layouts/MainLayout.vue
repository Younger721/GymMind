<template>
  <div class="shell">
    <aside class="sidebar">
      <router-link to="/dashboard" class="brand">GymMind</router-link>

      <nav v-for="group in menuGroups" :key="group.title" class="menu-group">
        <div class="group-title">{{ group.title }}</div>
        <router-link
          v-for="item in group.items"
          :key="item.path"
          :to="item.path"
          class="menu-link"
          :class="{ active: isActive(item.path) }"
        >
          {{ item.label }}
        </router-link>
      </nav>
    </aside>

    <div class="main">
      <header class="topbar">
        <span class="page-hint">{{ currentTitle }}</span>
        <div class="user-area">
          <el-select
            v-if="authStore.isPlatformAdmin"
            v-model="selectedTenantId"
            size="small"
            placeholder="选择门店租户"
            style="width: 160px"
            @change="onTenantChange"
          >
            <el-option
              v-for="t in tenantOptions"
              :key="t.id"
              :label="t.label"
              :value="t.id"
            />
          </el-select>
          <span class="username">{{ authStore.username }}</span>
          <el-button size="small" text @click="handleLogout">退出</el-button>
        </div>
      </header>
      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { tenancyApi } from '@/api/tenancy'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const authStore = useAuthStore()
const tenantOptions = ref<{ id: number; label: string }[]>([])
const selectedTenantId = ref<number | null>(authStore.platformContextTenantId)

async function loadTenants() {
  if (!authStore.isPlatformAdmin) return
  try {
    const res = await tenancyApi.listTenants(0, 100)
    const items = (res.data?.content ?? []) as Array<{ id: number; name?: string; code?: string }>
    tenantOptions.value = items.map(t => ({
      id: t.id,
      label: t.name || t.code || `租户 #${t.id}`
    }))
    if (!selectedTenantId.value && tenantOptions.value.length > 0) {
      selectedTenantId.value = tenantOptions.value[0].id
      authStore.setPlatformContextTenantId(selectedTenantId.value)
    }
  } catch {
    /* 平台管理接口失败时不阻塞导航 */
  }
}

function onTenantChange(id: number | null) {
  authStore.setPlatformContextTenantId(id)
}

onMounted(loadTenants)

const menuGroups = [
  {
    title: '概览',
    items: [{ path: '/dashboard', label: '首页' }]
  },
  {
    title: '智能',
    items: [
      { path: '/assistant', label: 'AI 助手' },
      { path: '/knowledge', label: '知识库' },
      { path: '/plan', label: '训练计划' },
      { path: '/agents/manage', label: 'Agent' },
      { path: '/ai-tools', label: 'AI 工具箱' }
    ]
  },
  {
    title: '门店运营',
    items: [
      { path: '/operations', label: '会员·课程·预约' },
      { path: '/membership', label: '会籍·支付' },
      { path: '/training', label: '训练·营养' }
    ]
  },
  {
    title: '分析搜索',
    items: [
      { path: '/analytics', label: '数据分析' },
      { path: '/search', label: '搜索导入' }
    ]
  },
  {
    title: '系统管理',
    items: [
      { path: '/admin', label: '用户·审计' },
      { path: '/tenant-settings', label: '租户设置' },
      { path: '/platform', label: '平台管理' },
      { path: '/profile', label: '账号设置' }
    ]
  }
]

const flatItems = menuGroups.flatMap(g => g.items)

const currentTitle = computed(() => {
  const hit = flatItems.find(i => isActive(i.path))
  return hit?.label ?? 'GymMind'
})

function isActive(path: string) {
  return route.path === path || route.path.startsWith(path + '/')
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning' })
    await authStore.logout()
  } catch {
    /* 取消 */
  }
}
</script>

<style scoped>
.shell {
  min-height: 100vh;
  display: flex;
}

.sidebar {
  width: 220px;
  flex-shrink: 0;
  background: var(--surface);
  border-right: 1px solid var(--border);
  padding: 16px 12px;
  position: sticky;
  top: 0;
  height: 100vh;
  overflow-y: auto;
}

.brand {
  display: block;
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
  text-decoration: none;
  padding: 4px 8px 16px;
  letter-spacing: -0.02em;
}

.brand:hover {
  color: var(--primary);
  text-decoration: none;
}

.menu-group {
  margin-bottom: 16px;
}

.group-title {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  padding: 0 8px 6px;
}

.menu-link {
  display: block;
  padding: 8px 10px;
  border-radius: 8px;
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
  margin-bottom: 2px;
  transition: background 0.15s, color 0.15s;
}

.menu-link:hover {
  background: var(--bg);
  color: var(--text);
  text-decoration: none;
}

.menu-link.active {
  background: var(--primary-soft);
  color: var(--primary);
}

.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.topbar {
  height: 52px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--surface);
  border-bottom: 1px solid var(--border);
  position: sticky;
  top: 0;
  z-index: 10;
}

.page-hint {
  font-size: 14px;
  font-weight: 600;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 8px;
}

.username {
  font-size: 13px;
  color: var(--text-muted);
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.content {
  flex: 1;
  max-width: 1100px;
  width: 100%;
  margin: 0 auto;
  padding: 24px 24px 40px;
}

@media (max-width: 900px) {
  .shell {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    height: auto;
    position: static;
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
    padding: 12px;
  }

  .brand {
    width: 100%;
    padding-bottom: 8px;
  }

  .menu-group {
    display: contents;
  }

  .group-title {
    display: none;
  }

  .menu-link {
    display: inline-block;
    margin: 0;
  }

  .username {
    display: none;
  }
}
</style>

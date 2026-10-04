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
            :model-value="authStore.platformContextTenantId"
            class="tenant-context-select"
            filterable
            placeholder="选择门店上下文"
            size="small"
            @visible-change="onTenantSelectVisible"
            @change="onTenantContextChange"
          >
            <el-option
              v-for="tenant in tenants"
              :key="tenant.id"
              :label="`${tenant.name}（ID: ${tenant.id}）`"
              :value="Number(tenant.id)"
              :disabled="tenant.status !== 'ACTIVE'"
            />
          </el-select>
          <span class="username">{{ authStore.username }}</span>
          <el-button size="small" text @click="handleLogout">退出</el-button>
        </div>
      </header>
      <main class="content">
        <router-view :key="tenantContextKey" />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessageBox } from 'element-plus'
import { hasMenuPermission, MENU_CATALOG } from '@/utils/menuPermissions'
import { tenancyApi } from '@/api/tenancy'

const route = useRoute()
const authStore = useAuthStore()
const tenantContextKey = computed(() => authStore.isPlatformAdmin
  ? `platform:${authStore.platformContextTenantId ?? 'none'}`
  : `tenant:${authStore.tenantId ?? 'none'}`)
const tenants = ref<Array<{ id: number; name: string; status: string }>>([])
const tenantsLoaded = ref(false)
const allMenuGroups = ['概览', '智能', '门店运营', '系统管理'].map(title => ({
  title,
    items: title === '智能'
      ? [...MENU_CATALOG.filter(item => item.group === title), { code: 'menu:assistant', label: 'AI 配置', group: '智能', path: '/agents/manage' }]
      : MENU_CATALOG.filter(item => item.group === title)
}))

const menuGroups = computed(() => {
  return allMenuGroups.map(group => ({
    ...group,
    items: group.items.filter(item => hasMenuPermission(item.code))
  })).filter(group => group.items.length > 0)
})

const flatItems = computed(() => menuGroups.value.flatMap(g => g.items))

const currentTitle = computed(() => {
  const hit = flatItems.value.find(i => isActive(i.path))
  return hit?.label ?? 'GymMind'
})

function isActive(path: string) {
  return route.path === path || route.path.startsWith(path + '/')
}

async function loadTenants() {
  if (tenantsLoaded.value) return
  try {
    const response = await tenancyApi.listTenants(0, 200)
    const data = (response as any)?.data
    const rows = Array.isArray(data?.content) ? data.content : Array.isArray(data) ? data : []
    tenants.value = rows.map((tenant: Record<string, unknown>) => ({
      id: Number(tenant.id),
      name: String(tenant.name || tenant.code || `门店 ${tenant.id}`),
      status: String(tenant.status || '')
    }))
      .filter((tenant: { id: number; status: string }) => Number.isSafeInteger(tenant.id)
        && tenant.id > 0
        && tenant.status === 'ACTIVE')
      .sort((a: { id: number }, b: { id: number }) => a.id - b.id)
    const selectedTenantId = authStore.platformContextTenantId
    if (tenants.value.length > 0
      && (selectedTenantId == null || !tenants.value.some(tenant => tenant.id === selectedTenantId))) {
      authStore.setPlatformContextTenantId(tenants.value[0].id)
    }
    tenantsLoaded.value = true
  } catch {
    // Keep the selector available so an invalid or disabled saved context can
    // still be cleared and the list can be retried.
  }
}

function onTenantSelectVisible(visible: boolean) {
  if (visible) void loadTenants()
}

function onTenantContextChange(value: number | null | undefined) {
  const nextTenantId = value == null ? tenants.value[0]?.id : Number(value)
  if (nextTenantId != null) authStore.setPlatformContextTenantId(nextTenantId)
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning' })
    await authStore.logout()
  } catch {
    /* 取消 */
  }
}
onMounted(() => {
  if (authStore.isPlatformAdmin) void loadTenants()
})
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

.tenant-context-select {
  width: 230px;
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

  .tenant-context-select {
    width: 190px;
  }
}
</style>

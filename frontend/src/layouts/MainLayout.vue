<template>
  <div class="shell">
    <aside class="sidebar">
      <router-link to="/dashboard" class="brand">
        <span class="brand-mark" aria-hidden="true">G</span>
        <span class="brand-text">
          <strong>GymMind</strong>
          <span class="brand-sub">AI PLATFORM</span>
        </span>
      </router-link>

      <nav v-for="group in menuGroups" :key="group.title" class="menu-group">
        <div class="group-title">{{ group.title }}</div>
        <router-link
          v-for="item in group.items"
          :key="item.path"
          :to="item.path"
          class="menu-link"
          :class="{ active: isActive(item.path) }"
        >
          {{ itemTitle(item) }}
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <div class="sync-box">
          <span class="gm-pill gm-pill--ok">
            <span class="gm-pill-dot" />
            Cluster Sync
          </span>
          <span class="sync-state">READY</span>
        </div>
      </div>
    </aside>

    <div class="main">
      <header class="topbar">
        <div class="topbar-left">
          <span class="page-hint">{{ currentTitle }}</span>
        </div>
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
import { hasMenuPermission, MENU_CATALOG, MENU_CODES, type MenuCatalogEntry } from '@/utils/menuPermissions'
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
  items: MENU_CATALOG.filter(item => item.group === title)
}))

const menuGroups = computed(() => {
  return allMenuGroups.map(group => ({
    ...group,
    items: group.items.filter(item => hasMenuPermission(item.code))
  })).filter(group => group.items.length > 0)
})

const flatItems = computed(() => menuGroups.value.flatMap(g => g.items))

function itemTitle(item: MenuCatalogEntry) {
  if (!authStore.isPlatformAdmin && item.code === MENU_CODES.TENANT_SETTINGS) return '额度使用'
  return item.label
}

const currentTitle = computed(() => {
  const hit = flatItems.value.find(i => isActive(i.path))
  return hit ? itemTitle(hit) : 'GymMind'
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
    // 保留选择器以便重试加载门店列表
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
  width: var(--sidebar-width);
  flex-shrink: 0;
  background: var(--surface-muted);
  border-right: 1px solid var(--border);
  padding: 16px 12px 12px;
  position: sticky;
  top: 0;
  height: 100vh;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  padding: 6px 8px 18px;
  margin-bottom: 4px;
}

.brand-mark {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-weight: 800;
  font-size: 16px;
  color: #052e16;
  background: linear-gradient(145deg, var(--primary), #16a34a);
  box-shadow: 0 4px 16px var(--primary-glow);
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}

.brand-text strong {
  font-size: 15px;
  font-weight: 700;
  color: var(--text);
  letter-spacing: -0.02em;
}

.brand-sub {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.14em;
  color: var(--text-dim);
  margin-top: 3px;
}

.brand:hover .brand-text strong {
  color: var(--primary);
}

.menu-group {
  margin-bottom: 18px;
}

.group-title {
  font-size: 10px;
  font-weight: 700;
  color: var(--text-dim);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  padding: 0 10px 8px;
}

.menu-link {
  display: block;
  padding: 9px 12px;
  border-radius: 10px;
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
  margin-bottom: 3px;
  transition: background 0.15s, color 0.15s;
}

.menu-link:hover {
  background: var(--surface-hover);
  color: var(--text);
  text-decoration: none;
}

.menu-link.active {
  background: var(--primary);
  color: #052e16;
  font-weight: 600;
  box-shadow: 0 4px 14px var(--primary-glow);
}

.sidebar-footer {
  margin-top: auto;
  padding: 12px 8px 4px;
  border-top: 1px solid var(--border);
}

.sync-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px;
  border-radius: var(--radius-sm);
  background: var(--surface);
  border: 1px solid var(--border);
}

.sync-state {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--primary);
}

.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.topbar {
  height: var(--topbar-height);
  padding: 0 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(15, 22, 32, 0.85);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border);
  position: sticky;
  top: 0;
  z-index: 10;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.page-hint {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}

.user-area {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
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
  max-width: var(--content-max);
  width: 100%;
  margin: 0 auto;
  padding: 28px 28px 48px;
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

  .sidebar-footer {
    display: none;
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

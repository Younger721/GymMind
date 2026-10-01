<template>
  <div>
    <h2 class="simple-page-title">用户与审计</h2>
    <p class="simple-page-desc">用户管理、角色与操作审计</p>

    <el-tabs v-model="tab">
      <el-tab-pane label="用户" name="users">
        <div class="stats-grid" v-loading="statsLoading">
          <div class="stat-card"><span>用户总数</span><strong>{{ stats.totalUsers }}</strong></div>
          <div class="stat-card"><span>会员</span><strong>{{ stats.memberCount }}</strong></div>
          <div class="stat-card"><span>教练</span><strong>{{ stats.coachCount }}</strong></div>
          <div class="stat-card"><span>健身房管理员</span><strong>{{ stats.gymAdminCount }}</strong></div>
          <div v-if="authStore.isPlatformAdmin" class="stat-card"><span>平台管理员</span><strong>{{ stats.platformAdminCount }}</strong></div>
        </div>
        <div v-if="Object.keys(stats.membershipTypeCounts).length" class="membership-summary simple-card">
          <span class="summary-title">套餐类型</span>
          <el-tag v-for="(count, name) in stats.membershipTypeCounts" :key="name" effect="plain">{{ name }}：{{ count }}</el-tag>
        </div>
        <div class="user-panel simple-card">
          <div class="user-toolbar">
            <el-select v-if="authStore.isPlatformAdmin" v-model="filters.tenantId" clearable filterable placeholder="按门店筛选" style="width: 210px" @change="loadUsers">
              <el-option v-for="tenant in tenants" :key="tenant.id" :label="tenant.name" :value="tenant.id" />
            </el-select>
            <el-input v-model="filters.displayName" clearable placeholder="按姓名模糊查询" style="width: 220px" @keyup.enter="loadUsers" />
            <el-button @click="loadUsers">查询</el-button>
            <el-button type="primary" @click="openCreateUser">新增用户</el-button>
          </div>
          <el-table :data="users" v-loading="usersLoading" stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column v-if="authStore.isPlatformAdmin" prop="tenantName" label="关联门店" min-width="140" />
            <el-table-column prop="username" label="用户名" min-width="120" />
            <el-table-column prop="email" label="邮箱" min-width="180" />
            <el-table-column prop="status" label="状态" width="90" />
            <el-table-column label="角色" min-width="130"><template #default="{ row }">{{ formatRoles(row.roles) }}</template></el-table-column>
            <el-table-column prop="membershipType" label="会员类型" min-width="120" />
            <el-table-column prop="membershipDaysRemaining" label="剩余天数" width="100" />
          </el-table>
        </div>
      </el-tab-pane>
      <el-tab-pane label="角色" name="roles">
        <BizCrudPanel title="系统角色" :loader="loadRoles" />
      </el-tab-pane>
      <el-tab-pane label="操作审计" name="audit">
        <BizCrudPanel title="操作日志" :loader="loadAuditOps" />
      </el-tab-pane>
      <el-tab-pane label="AI 用量" name="ai-usage">
        <BizCrudPanel title="AI 调用记录" :loader="loadAiUsage" />
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="createUserVisible" title="新增用户" width="520px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item v-if="authStore.isPlatformAdmin" label="关联门店" required><el-select v-model="newUser.tenantId" filterable placeholder="选择门店" style="width: 100%"><el-option v-for="tenant in tenants" :key="tenant.id" :label="tenant.name" :value="tenant.id" /></el-select></el-form-item>
        <el-form-item label="用户名" required><el-input v-model="newUser.username" /></el-form-item>
        <el-form-item label="邮箱" required><el-input v-model="newUser.email" /></el-form-item>
        <el-form-item label="姓名" required><el-input v-model="newUser.displayName" /></el-form-item>
        <el-form-item label="密码" required><el-input v-model="newUser.password" type="password" show-password /></el-form-item>
        <el-form-item label="角色" required><el-select v-model="newUser.role" style="width: 100%"><el-option label="会员" value="MEMBER" /><el-option label="教练" value="COACH" /><el-option v-if="authStore.isPlatformAdmin" label="健身房管理员" value="GYM_ADMIN" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="createUserVisible = false">取消</el-button><el-button type="primary" :loading="creatingUser" @click="createUser">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import BizCrudPanel from '@/components/business/BizCrudPanel.vue'
import { usersApi } from '@/api/users'
import { auditApi } from '@/api/audit'
import { tenancyApi } from '@/api/tenancy'
// The shared utility is also consumed directly by the Node regression test.
// @ts-ignore declaration is unnecessary for this small ESM display helper.
import { toDisplayRole } from '@/utils/roleLabels.mjs'

const tab = ref('users')
const authStore = useAuthStore()
const statsLoading = ref(false)
const stats = ref({ totalUsers: 0, memberCount: 0, coachCount: 0, gymAdminCount: 0, platformAdminCount: 0, membershipTypeCounts: {} as Record<string, number> })

/** 租户内用户列表不展示 tenantId */
const tenants = ref<Array<{ id: number; name: string }>>([])
const users = ref<any[]>([])
const usersLoading = ref(false)
const createUserVisible = ref(false)
const creatingUser = ref(false)
const filters = reactive<{ tenantId?: number; displayName: string }>({ displayName: '' })
const newUser = reactive({ tenantId: undefined as number | undefined, username: '', email: '', displayName: '', password: '', role: 'MEMBER' })

function formatRoles(roles: string[] = []) { return roles.map(role => ({ MEMBER: '会员', COACH: '教练', GYM_ADMIN: '健身房管理员', PLATFORM_ADMIN: '平台管理员' } as Record<string, string>)[role] || role).join('、') || '—' }

async function loadUsers() {
  usersLoading.value = true
  try {
    const response = await usersApi.list(0, 1000, { tenantId: filters.tenantId, displayName: filters.displayName.trim() || undefined })
    users.value = (response.data as any)?.content ?? []
  } finally { usersLoading.value = false }
}

async function loadTenants() {
  if (!authStore.isPlatformAdmin) return
  const response = await tenancyApi.listTenants(0, 200)
  const data = (response as any)?.data
  const rows = Array.isArray(data?.content) ? data.content : Array.isArray(data) ? data : []
  tenants.value = rows.map((tenant: any) => ({ id: Number(tenant.id), name: String(tenant.name || tenant.code || tenant.id) }))
}

function openCreateUser() { Object.assign(newUser, { tenantId: authStore.isPlatformAdmin ? undefined : authStore.tenantId ?? undefined, username: '', email: '', displayName: '', password: '', role: 'MEMBER' }); createUserVisible.value = true }
async function createUser() {
  if (!newUser.username || !newUser.email || !newUser.displayName || !newUser.password || (authStore.isPlatformAdmin && !newUser.tenantId)) { ElMessage.warning('请完整填写用户信息'); return }
  creatingUser.value = true
  try { await usersApi.create({ ...newUser, ...(authStore.isPlatformAdmin ? { tenantId: newUser.tenantId } : {}) }); ElMessage.success('用户创建成功'); createUserVisible.value = false; await loadUsers() } finally { creatingUser.value = false }
}

async function loadStats() {
  statsLoading.value = true
  try {
    const response = await usersApi.statistics()
    if (response.data) stats.value = { ...stats.value, ...response.data }
  } finally {
    statsLoading.value = false
  }
}

async function loadRoles() {
  const response = await usersApi.listRoles()
  if (Array.isArray(response?.data)) {
    response.data = response.data.map(toDisplayRole)
  }
  return response
}

async function loadAuditOps() {
  return auditApi.operations()
}

async function loadAiUsage() {
  return auditApi.aiUsage()
}

onMounted(async () => { await loadTenants(); await Promise.all([loadStats(), loadUsers()]) })
</script>

<style scoped>
.stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(130px, 1fr)); gap: 12px; margin-bottom: 16px; }
.stat-card { padding: 16px; background: var(--surface); border: 1px solid var(--border); border-radius: 8px; }
.stat-card span { display: block; color: var(--text-muted); font-size: 12px; }
.stat-card strong { display: block; margin-top: 8px; font-size: 28px; color: var(--primary); }
.membership-summary { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 16px; }
.summary-title { color: var(--text-muted); font-size: 13px; }
.user-panel { margin-bottom: 16px; }
.user-toolbar { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 14px; }
</style>

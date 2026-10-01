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
        <BizCrudPanel
          title="用户列表"
          :column-keys="userColumnKeys"
          :column-labels="userColumnLabels"
          :loader="loadUsers"
        />
      </el-tab-pane>
      <el-tab-pane label="角色" name="roles">
        <BizCrudPanel title="系统角色" :loader="loadRoles" />
      </el-tab-pane>
      <el-tab-pane label="邀请" name="invite">
        <BizCrudPanel title="邀请用户" :create-fields="inviteFields" :loader="emptyLoader" :creator="sendInvite" create-permission="user:write" />
      </el-tab-pane>
      <el-tab-pane label="操作审计" name="audit">
        <BizCrudPanel title="操作日志" :loader="loadAuditOps" />
      </el-tab-pane>
      <el-tab-pane label="AI 用量" name="ai-usage">
        <BizCrudPanel title="AI 调用记录" :loader="loadAiUsage" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import BizCrudPanel, { type FieldDef } from '@/components/business/BizCrudPanel.vue'
import { usersApi } from '@/api/users'
import { auditApi } from '@/api/audit'
// The shared utility is also consumed directly by the Node regression test.
// @ts-ignore declaration is unnecessary for this small ESM display helper.
import { toDisplayRole } from '@/utils/roleLabels.mjs'

const tab = ref('users')
const authStore = useAuthStore()
const statsLoading = ref(false)
const stats = ref({ totalUsers: 0, memberCount: 0, coachCount: 0, gymAdminCount: 0, platformAdminCount: 0, membershipTypeCounts: {} as Record<string, number> })

/** 租户内用户列表不展示 tenantId */
const userColumnKeys = computed(() => authStore.isPlatformAdmin
  ? ['id', 'tenantId', 'username', 'email', 'displayName', 'status', 'roles', 'membershipType', 'membershipDaysRemaining']
  : ['id', 'username', 'email', 'displayName', 'status', 'roles', 'membershipType', 'membershipDaysRemaining'])

const userColumnLabels = {
  membershipType: '会员类型',
  membershipDaysRemaining: '剩余天数'
}

const inviteFields: FieldDef[] = [
  { key: 'email', label: '邮箱' },
  {
    key: 'role',
    label: '角色',
    type: 'select',
    options: [
      { label: '教练', value: 'COACH' },
      { label: '会员', value: 'MEMBER' }
    ]
  }
]

async function loadUsers() {
  return usersApi.list()
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

async function sendInvite(p: Record<string, string>) {
  return usersApi.invite({ email: p.email, role: p.role })
}

async function loadAuditOps() {
  return auditApi.operations()
}

async function loadAiUsage() {
  return auditApi.aiUsage()
}

async function emptyLoader() {
  return { data: [] }
}

onMounted(loadStats)
</script>

<style scoped>
.stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(130px, 1fr)); gap: 12px; margin-bottom: 16px; }
.stat-card { padding: 16px; background: var(--surface); border: 1px solid var(--border); border-radius: 8px; }
.stat-card span { display: block; color: var(--text-muted); font-size: 12px; }
.stat-card strong { display: block; margin-top: 8px; font-size: 28px; color: var(--primary); }
.membership-summary { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 16px; }
.summary-title { color: var(--text-muted); font-size: 13px; }
</style>

<template>
  <div class="dashboard">
    <h2 class="simple-page-title">你好，{{ authStore.username }}</h2>
    <p class="simple-page-desc">快速进入常用功能</p>

    <div class="quick-grid">
      <router-link
        v-for="item in quickLinks"
        :key="item.path"
        :to="item.path"
        class="quick-card simple-card"
      >
        <span class="icon">{{ item.icon }}</span>
        <strong>{{ item.title }}</strong>
        <span class="hint">{{ item.desc }}</span>
      </router-link>
    </div>

    <section v-if="showTenantOpsOverview" class="ops-section">
      <div class="ops-head">
        <h3 class="ops-title">门店概览</h3>
        <router-link v-if="hasAnalyticsMenu" to="/analytics" class="ops-more">查看详情 →</router-link>
      </div>
      <div v-loading="opsLoading" class="stats ops-stats simple-card">
        <div class="stat">
          <span class="label">有效会员</span>
          <span class="value">{{ opsOverview?.activeMembers ?? '—' }}</span>
        </div>
        <div class="stat">
          <span class="label">上月签到人次</span>
          <span class="value">{{ opsOverview?.lastMonthCheckIns ?? '—' }}</span>
        </div>
        <div class="stat">
          <span class="label">本月至今签到人次</span>
          <span class="value">{{ opsOverview?.monthToDateCheckIns ?? '—' }}</span>
        </div>
      </div>
    </section>

    <div v-if="docCount !== null" class="stats simple-card">
      <div class="stat">
        <span class="label">知识库文档</span>
        <span class="value">{{ docCount }}</span>
      </div>
      <div class="stat">
        <span class="label">Agent 数量</span>
        <span class="value">{{ agentCount ?? '—' }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { knowledgeApi } from '@/api/knowledge'
import { agentApi } from '@/api/agent'
import {
  analyticsApi,
  currentMonthToDateRange,
  previousCalendarMonthRange
} from '@/api/analytics'
import { hasMenuPermission, MENU_CODES } from '@/utils/menuPermissions'

const authStore = useAuthStore()
const docCount = ref<number | null>(null)
const agentCount = ref<number | null>(null)
const opsLoading = ref(false)
const opsOverview = ref<{
  activeMembers: number
  lastMonthCheckIns: number
  monthToDateCheckIns: number
} | null>(null)

const showTenantOpsOverview = computed(() =>
  authStore.tenantId != null
  && authStore.roles.includes('GYM_ADMIN')
  && authStore.permissions.includes('analytics:read'))
const hasAnalyticsMenu = computed(() => hasMenuPermission(MENU_CODES.ANALYTICS))

const quickLinks = computed(() => [
  { path: '/assistant', permission: MENU_CODES.ASSISTANT, icon: '💬', title: 'AI 助手', desc: '健身问答与咨询' },
  { path: '/courses', permission: MENU_CODES.COURSES, icon: '📚', title: '课程中心', desc: '课程发布与报名' },
  { path: '/training', permission: MENU_CODES.TRAINING, icon: '🏋️', title: '训练营养', desc: '动作库与计划' },
  { path: '/knowledge', permission: MENU_CODES.KNOWLEDGE, icon: '📄', title: '知识库', desc: '上传门店文档' },
    { path: '/agents/manage', permission: MENU_CODES.AI_CONFIG, icon: '🤖', title: 'AI 配置', desc: '配置门店助手' },
  { path: '/admin', permission: MENU_CODES.ADMIN, icon: '⚙️', title: '用户审计', desc: '用户与操作日志' }
].filter(item => hasMenuPermission(item.permission)))

async function loadTenantOpsOverview() {
  if (!showTenantOpsOverview.value) return
  opsLoading.value = true
  try {
    const lastMonth = previousCalendarMonthRange()
    const monthToDate = currentMonthToDateRange()
    const [lastRes, mtdRes] = await Promise.all([
      analyticsApi.tenantOperationsSummary(lastMonth.from, lastMonth.to, false),
      analyticsApi.tenantOperationsSummary(monthToDate.from, monthToDate.to, false)
    ])
    opsOverview.value = {
      activeMembers: mtdRes.data?.activeMembers ?? lastRes.data?.activeMembers ?? 0,
      lastMonthCheckIns: lastRes.data?.checkInPersonTimes ?? 0,
      monthToDateCheckIns: mtdRes.data?.checkInPersonTimes ?? 0
    }
  } catch {
    opsOverview.value = null
  } finally {
    opsLoading.value = false
  }
}

onMounted(async () => {
  void loadTenantOpsOverview()
  try {
    const docs = await knowledgeApi.listDocuments()
    docCount.value = docs.data?.length ?? 0
  } catch {
    docCount.value = 0
  }
  try {
    const agents = await agentApi.list({ silent: true })
    agentCount.value = agents.data?.length ?? 0
  } catch {
    agentCount.value = null
  }
})
</script>

<style scoped>
.quick-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.quick-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  text-decoration: none;
  color: inherit;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.quick-card:hover {
  border-color: var(--primary);
  box-shadow: 0 4px 12px rgba(5, 150, 105, 0.12);
  text-decoration: none;
}

.icon {
  font-size: 24px;
}

.quick-card strong {
  font-size: 15px;
}

.hint {
  font-size: 12px;
  color: var(--text-muted);
}

.ops-section {
  margin-bottom: 16px;
}

.ops-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.ops-title {
  margin: 0;
  font-size: 16px;
}

.ops-more {
  font-size: 13px;
  color: var(--primary);
}

.ops-stats {
  min-height: 72px;
}

.stats {
  display: flex;
  gap: 32px;
  flex-wrap: wrap;
}

.stat {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat .label {
  font-size: 12px;
  color: var(--text-muted);
}

.stat .value {
  font-size: 22px;
  font-weight: 600;
}
</style>

<template>
  <div class="dashboard">
    <section class="hero simple-card">
      <div class="hero-copy">
        <p class="eyebrow">OPERATIONS OVERVIEW</p>
        <h2 class="hero-title">你好，{{ authStore.username }}</h2>
        <p class="hero-desc">统一入口管理 AI 助手、知识库与门店运营数据。</p>
      </div>
      <router-link
        v-if="hasAssistant"
        to="/assistant"
        class="hero-cta"
      >
        AI 决策助手
        <span class="cta-arrow">→</span>
      </router-link>
    </section>

    <div v-if="showMetricRow" class="gm-metric-row">
      <div v-if="hasKnowledgeMetric" class="gm-metric">
        <div class="gm-metric__label">知识库文档</div>
        <div class="gm-metric__value">{{ docCount ?? '—' }}</div>
        <div class="gm-metric__sub">已上传资料</div>
      </div>
      <div v-if="hasAiConfigMetric" class="gm-metric">
        <div class="gm-metric__label">Agent</div>
        <div class="gm-metric__value">{{ agentCount ?? '—' }}</div>
        <div class="gm-metric__sub">门店助手配置</div>
      </div>
      <template v-if="showTenantOpsOverview && opsOverview">
        <div class="gm-metric">
          <div class="gm-metric__label">有效会员</div>
          <div class="gm-metric__value">{{ opsOverview.activeMembers }}</div>
          <div class="gm-metric__sub">当前在籍</div>
        </div>
        <div class="gm-metric">
          <div class="gm-metric__label">上月签到</div>
          <div class="gm-metric__value">{{ opsOverview.lastMonthCheckIns }}</div>
          <div class="gm-metric__sub">人次</div>
        </div>
        <div class="gm-metric">
          <div class="gm-metric__label">本月签到</div>
          <div class="gm-metric__value">{{ opsOverview.monthToDateCheckIns }}</div>
          <div class="gm-metric__sub">至今累计</div>
        </div>
      </template>
    </div>

    <h3 class="section-title">核心业务分发</h3>
    <div class="dispatch-grid">
      <router-link
        v-for="item in quickLinks"
        :key="item.path"
        :to="item.path"
        class="dispatch-card simple-card"
      >
        <span class="dispatch-icon">{{ item.icon }}</span>
        <div class="dispatch-body">
          <strong>{{ item.title }}</strong>
          <span class="dispatch-desc">{{ item.desc }}</span>
        </div>
        <span class="dispatch-go">进入 →</span>
      </router-link>
    </div>

    <section v-if="showTenantOpsOverview" v-loading="opsLoading" class="ops-banner simple-card">
      <div>
        <h3 class="ops-title">门店运营概览</h3>
        <p class="ops-sub">会员活跃与签到趋势可在运营统计中进一步分析。</p>
      </div>
      <router-link v-if="hasAnalyticsMenu" to="/analytics" class="ops-more">查看详情 →</router-link>
    </section>
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

const hasAssistant = computed(() => hasMenuPermission(MENU_CODES.ASSISTANT))
const hasKnowledgeMetric = computed(() => hasMenuPermission(MENU_CODES.KNOWLEDGE))
const hasAiConfigMetric = computed(() => hasMenuPermission(MENU_CODES.AI_CONFIG))
const showTenantOpsOverview = computed(() =>
  authStore.tenantId != null
  && authStore.roles.includes('GYM_ADMIN')
  && authStore.permissions.includes('analytics:read'))
const showMetricRow = computed(() =>
  hasKnowledgeMetric.value || hasAiConfigMetric.value || showTenantOpsOverview.value)
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
  if (hasKnowledgeMetric.value) {
    try {
      const docs = await knowledgeApi.listDocuments()
      docCount.value = docs.data?.length ?? 0
    } catch {
      docCount.value = 0
    }
  }
  if (hasAiConfigMetric.value) {
    try {
      const agents = await agentApi.list({ silent: true })
      agentCount.value = agents.data?.length ?? 0
    } catch {
      agentCount.value = null
    }
  }
})
</script>

<style scoped>
.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;
  padding: 24px 28px;
  background: linear-gradient(135deg, var(--surface) 0%, rgba(34, 197, 94, 0.06) 100%);
  border-color: rgba(34, 197, 94, 0.2);
}

.eyebrow {
  margin: 0 0 8px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: var(--primary);
}

.hero-title {
  margin: 0 0 8px;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.hero-desc {
  margin: 0;
  font-size: 13px;
  color: var(--text-muted);
  max-width: 420px;
  line-height: 1.5;
}

.hero-cta {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 14px 22px;
  border-radius: 12px;
  background: var(--primary);
  color: #052e16;
  font-weight: 700;
  font-size: 14px;
  text-decoration: none;
  box-shadow: 0 8px 24px var(--primary-glow);
  transition: transform 0.12s, background 0.15s;
}

.hero-cta:hover {
  background: var(--primary-hover);
  color: #052e16;
  transform: translateY(-1px);
  text-decoration: none;
}

.cta-arrow {
  font-size: 18px;
}

.section-title {
  margin: 8px 0 14px;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--text-dim);
}

.dispatch-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
  margin-bottom: 24px;
}

.dispatch-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 18px;
  text-decoration: none;
  color: inherit;
  transition: border-color 0.15s, transform 0.12s, box-shadow 0.15s;
}

.dispatch-card:hover {
  border-color: rgba(34, 197, 94, 0.45);
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.35);
  text-decoration: none;
}

.dispatch-icon {
  font-size: 28px;
  line-height: 1;
}

.dispatch-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.dispatch-body strong {
  font-size: 15px;
}

.dispatch-desc {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.45;
}

.dispatch-go {
  font-size: 11px;
  font-weight: 600;
  color: var(--primary);
  white-space: nowrap;
  align-self: center;
}

.ops-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 22px;
}

.ops-title {
  margin: 0 0 4px;
  font-size: 16px;
}

.ops-sub {
  margin: 0;
  font-size: 12px;
  color: var(--text-muted);
}

.ops-more {
  font-size: 13px;
  font-weight: 600;
  color: var(--primary);
  white-space: nowrap;
}

@media (max-width: 640px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-cta {
    width: 100%;
    justify-content: center;
  }
}
</style>

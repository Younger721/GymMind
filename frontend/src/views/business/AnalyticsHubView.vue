<template>
  <div class="tenant-ops">
    <div class="page-head">
      <div>
        <h2 class="simple-page-title">门店运营统计</h2>
        <p class="simple-page-desc">基于门店签到（gym_check_in）汇总有效会员与签到人次；与 AI 门店运营数据口径一致。</p>
      </div>
      <router-link to="/member-risks" class="risk-link">会员流失预警 →</router-link>
    </div>

    <div class="simple-card filter-box">
      <el-form inline>
        <el-form-item label="统计区间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD"
            :disabled="loading"
          />
        </el-form-item>
        <el-form-item>
          <el-button @click="applyPreset('lastMonth')" :disabled="loading">上一自然月</el-button>
          <el-button @click="applyPreset('monthToDate')" :disabled="loading">本月至今</el-button>
          <el-button type="primary" :loading="loading" @click="load">查询</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div v-if="summary" class="kpi-grid">
      <div class="kpi simple-card">
        <span class="kpi-label">当前有效会员</span>
        <strong class="kpi-value">{{ summary.activeMembers }}</strong>
        <small class="kpi-hint">档案总数 {{ summary.totalMembers }}</small>
      </div>
      <div class="kpi simple-card">
        <span class="kpi-label">签到人次</span>
        <strong class="kpi-value">{{ summary.checkInPersonTimes }}</strong>
        <small class="kpi-hint">{{ summary.periodLabel }}</small>
      </div>
      <div class="kpi simple-card">
        <span class="kpi-label">到店会员数</span>
        <strong class="kpi-value">{{ summary.uniqueVisitors }}</strong>
        <small class="kpi-hint">周期内至少签到 1 次</small>
      </div>
      <div class="kpi simple-card">
        <span class="kpi-label">签到人次较上期</span>
        <strong class="kpi-value" :class="changeClass(summary.checkInPersonTimesChangeRate)">
          {{ formatChange(summary.checkInPersonTimesChangeRate) }}
        </strong>
        <small v-if="summary.previousCheckInPersonTimes != null" class="kpi-hint">
          上期 {{ summary.previousCheckInPersonTimes }} 人次
        </small>
      </div>
    </div>

    <div class="simple-card trend-card">
      <div class="trend-head">
        <h3>按日签到人次</h3>
        <span class="muted">{{ summary?.periodLabel || '' }}</span>
      </div>
      <el-table :data="trend" v-loading="loading" stripe empty-text="该区间暂无签到记录">
        <el-table-column prop="day" label="日期" width="140">
          <template #default="{ row }">{{ row.day || row.bucket }}</template>
        </el-table-column>
        <el-table-column prop="checkInPersonTimes" label="签到人次" width="120" />
        <el-table-column prop="uniqueVisitors" label="到店会员" width="120" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  analyticsApi,
  currentMonthToDateRange,
  previousCalendarMonthRange,
  type TenantOperationsSummary,
  type TenantOperationsTrendPoint
} from '@/api/analytics'

const loading = ref(false)
const dateRange = ref<[string, string] | null>(null)
const summary = ref<TenantOperationsSummary | null>(null)
const trend = ref<TenantOperationsTrendPoint[]>([])

function applyPreset(preset: 'lastMonth' | 'monthToDate') {
  const range = preset === 'lastMonth' ? previousCalendarMonthRange() : currentMonthToDateRange()
  dateRange.value = [range.from, range.to]
  void load()
}

function formatChange(value?: number | string | null) {
  if (value == null || value === '') return '—'
  const num = typeof value === 'number' ? value : Number(value)
  if (Number.isNaN(num)) return String(value)
  return `${num > 0 ? '+' : ''}${num}%`
}

function changeClass(value?: number | string | null) {
  const num = typeof value === 'number' ? value : Number(value)
  if (Number.isNaN(num) || num === 0) return ''
  return num > 0 ? 'up' : 'down'
}

async function load() {
  if (!dateRange.value || dateRange.value.length !== 2) {
    ElMessage.warning('请选择统计区间')
    return
  }
  const [from, to] = dateRange.value
  loading.value = true
  try {
    const [summaryRes, trendRes] = await Promise.all([
      analyticsApi.tenantOperationsSummary(from, to, true),
      analyticsApi.tenantOperationsTrend(from, to, 'day')
    ])
    summary.value = summaryRes.data || null
    trend.value = trendRes.data || []
  } catch {
    ElMessage.error('加载门店运营统计失败')
    summary.value = null
    trend.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  applyPreset('lastMonth')
})
</script>

<style scoped>
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.page-head .simple-page-desc {
  margin-bottom: 0;
}

.risk-link {
  color: var(--primary);
  font-size: 14px;
  white-space: nowrap;
}

.filter-box {
  margin-bottom: 16px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.kpi {
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.kpi-label {
  color: var(--text-muted);
  font-size: 13px;
}

.kpi-value {
  font-size: 32px;
  line-height: 1.1;
  color: var(--primary);
}

.kpi-value.up {
  color: #19b477;
}

.kpi-value.down {
  color: #e85d5d;
}

.kpi-hint {
  color: var(--text-muted);
  font-size: 12px;
}

.trend-card {
  padding: 16px 18px;
}

.trend-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 12px;
}

.trend-head h3 {
  margin: 0;
  font-size: 15px;
}

.muted {
  color: var(--text-muted);
  font-size: 13px;
}
</style>

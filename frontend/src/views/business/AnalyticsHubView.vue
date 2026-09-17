<template>
  <div>
    <h2 class="simple-page-title">数据分析</h2>
    <p class="simple-page-desc">运营看板、周报与流失预警</p>

    <div class="simple-card filter-box">
      <el-form inline>
        <el-form-item label="开始">
          <el-input v-model="from" placeholder="2026-09-01" />
        </el-form-item>
        <el-form-item label="结束">
          <el-input v-model="to" placeholder="2026-09-17" />
        </el-form-item>
        <el-form-item label="周起始">
          <el-input v-model="weekStart" placeholder="2026-09-15" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="loadDashboard">看板</el-button>
          <el-button :loading="loading" @click="loadWeekly">周指标</el-button>
          <el-button :loading="loading" @click="loadReport">周报</el-button>
          <el-button :loading="loading" @click="loadAiReport">AI 周报</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="simple-card filter-box">
      <h3 class="sub-title">流失风险评估</h3>
      <el-form label-width="140px">
        <el-form-item label="距上次训练(天)">
          <el-input-number v-model="churn.daysSinceLastWorkout" :min="0" />
        </el-form-item>
        <el-form-item label="近30天训练次数">
          <el-input-number v-model="churn.workoutsLast30Days" :min="0" />
        </el-form-item>
        <el-form-item label="前30天训练次数">
          <el-input-number v-model="churn.workoutsPrevious30Days" :min="0" />
        </el-form-item>
        <el-form-item label="会籍剩余天数">
          <el-input-number v-model="churn.membershipDaysRemaining" :min="0" />
        </el-form-item>
        <el-form-item label="会籍即将到期">
          <el-switch v-model="churn.membershipExpiring" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="runChurn">评估</el-button>
        </el-form-item>
      </el-form>
    </div>

    <JsonBlock v-if="result" :data="result" />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import JsonBlock from '@/components/business/JsonBlock.vue'
import { analyticsApi } from '@/api/analytics'

const from = ref('2026-09-01')
const to = ref('2026-09-17')
const weekStart = ref('2026-09-15')
const loading = ref(false)
const result = ref<unknown>(null)

const churn = reactive({
  daysSinceLastWorkout: 14,
  workoutsLast30Days: 2,
  workoutsPrevious30Days: 8,
  membershipDaysRemaining: 7,
  membershipExpiring: true
})

async function run(fn: () => Promise<unknown>) {
  loading.value = true
  try {
    result.value = await fn()
  } finally {
    loading.value = false
  }
}

function loadDashboard() {
  return run(() => analyticsApi.dashboard(from.value, to.value))
}

function loadWeekly() {
  return run(() => analyticsApi.weeklyMetrics(weekStart.value))
}

function loadReport() {
  return run(() => analyticsApi.weeklyReport(weekStart.value))
}

function loadAiReport() {
  return run(() => analyticsApi.aiWeeklyReport(weekStart.value))
}

function runChurn() {
  return run(() => analyticsApi.churnRisk({ ...churn }))
}
</script>

<style scoped>
.filter-box {
  margin-bottom: 16px;
}

.sub-title {
  margin: 0 0 12px;
  font-size: 15px;
}
</style>

<template>
  <div class="profile">
    <h2 class="simple-page-title">个人中心</h2>
    <p class="simple-page-desc">{{ overview?.member ? '查看账户、套餐和出勤' : '查看账户信息' }}</p>

    <el-alert v-if="error" type="error" :title="error" show-icon closable @close="error = ''" />

    <div class="overview-grid" :class="{ 'with-attendance': showAttendance }">
      <div class="simple-card account-card">
        <h3>账户信息</h3>
        <div class="row"><span class="label">用户名</span><span>{{ overview?.username || authStore.username || '—' }}</span></div>
        <div class="row"><span class="label">邮箱</span><span>{{ overview?.email || authStore.email || '—' }}</span></div>
        <div class="row"><span class="label">用户 ID</span><span>{{ overview?.userId || authStore.userId || '—' }}</span></div>
        <el-button type="danger" plain class="logout-btn" @click="logout">退出登录</el-button>
      </div>

      <div v-if="showAttendance" class="simple-card checkin-card">
        <div class="card-head">
          <div><h3>签到统计</h3><span class="muted">选择月份查看出勤记录</span></div>
          <div class="month-picker">
            <el-button text circle aria-label="上个月" @click="changeMonth(-1)"><ArrowLeft /></el-button>
            <el-date-picker v-model="selectedMonth" type="month" value-format="YYYY-MM" placeholder="选择月份" @change="loadOverview" />
            <el-button text circle aria-label="下个月" @click="changeMonth(1)"><ArrowRight /></el-button>
          </div>
        </div>
        <div class="checkin-summary">
          <div><strong>{{ checkInDays }}</strong><small>出勤天</small></div>
          <div class="summary-secondary"><strong>{{ overview?.checkInCount ?? 0 }}</strong><small>签到次</small></div>
        </div>
        <div class="month-comparison" :class="comparisonTone">{{ comparisonText }}</div>
        <div class="attendance-calendar">
          <div v-for="day in weekDays" :key="day" class="calendar-weekday">{{ day }}</div>
          <template v-for="(cell, index) in calendarCells" :key="index">
            <div v-if="cell === null" class="calendar-cell calendar-blank"></div>
            <div v-else class="calendar-cell" :class="{ attended: checkedDates.has(cell.date) }">
              <span>{{ cell.day }}</span>
              <i v-if="checkedDates.has(cell.date)" class="attendance-dot" aria-label="已签到"></i>
            </div>
          </template>
        </div>
      </div>
    </div>

    <div v-if="overview?.member" class="simple-card membership-card">
      <h3>当前套餐</h3>
      <div class="row"><span class="label">会员编号</span><span>{{ overview.member.memberNumber || '—' }}</span></div>
      <div class="membership-type">{{ overview.membership?.membershipType || '暂无有效套餐' }}</div>
      <div class="row"><span class="label">套餐名称</span><span>{{ overview.membership?.packageName || '—' }}</span></div>
      <div class="row"><span class="label">有效期</span><span>{{ formatDate(overview.membership?.startsAt) }} 至 {{ formatDate(overview.membership?.expiresAt) }}</span></div>
    </div>

    <div v-if="overview?.membership?.id" class="simple-card change-card">
      <h3>停卡申请</h3>
      <p class="muted">提交后需健身房管理员审批，最长单次 30 天。</p>
      <el-form inline @submit.prevent="submitFreeze">
        <el-form-item label="停卡区间">
          <el-date-picker v-model="freezeRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始" end-placeholder="结束" />
        </el-form-item>
        <el-form-item label="说明"><el-input v-model="freezeReason" placeholder="可选" clearable /></el-form-item>
        <el-button type="primary" :loading="freezeSubmitting" @click="submitFreeze">提交停卡</el-button>
      </el-form>
      <el-table v-if="myRequests.length" :data="myRequests" size="small" stripe class="request-table">
        <el-table-column label="类型" width="80"><template #default="{ row }">{{ row.type === 'FREEZE' ? '停卡' : '转卡' }}</template></el-table-column>
        <el-table-column label="状态" width="90"><template #default="{ row }">{{ requestStatusLabel(row.status) }}</template></el-table-column>
        <el-table-column label="详情" min-width="160">
          <template #default="{ row }">
            <span v-if="row.type === 'FREEZE'">{{ row.freezeStart }} ~ {{ row.freezeEnd }}</span>
            <span v-else>转至 #{{ row.targetMemberId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" link type="danger" @click="cancelRequest(row.id)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { membershipChangeRequestsApi, type MembershipChangeRequest } from '@/api/membershipChangeRequests'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { profileApi, type ProfileOverview } from '@/api/profile'

const authStore = useAuthStore()
const overview = ref<ProfileOverview | null>(null)
const previousOverview = ref<ProfileOverview | null>(null)
const selectedMonth = ref(monthKey(new Date()))
const error = ref('')
const freezeRange = ref<[string, string] | null>(null)
const freezeReason = ref('')
const freezeSubmitting = ref(false)
const myRequests = ref<MembershipChangeRequest[]>([])
const weekDays = ['一', '二', '三', '四', '五', '六', '日']
const showAttendance = computed(() => !!overview.value?.member
  && !authStore.isPlatformAdmin
  && !authStore.roles.includes('GYM_ADMIN')
  && !authStore.roles.includes('COACH'))

interface CalendarCell { date: string; day: number }

function monthKey(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
}

function monthRange(month: string): [string, string] {
  const [year, value] = month.split('-').map(Number)
  const lastDay = new Date(year, value, 0).getDate()
  return [`${year}-${String(value).padStart(2, '0')}-01`, `${year}-${String(value).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`]
}

function previousMonth(month: string): string {
  const [year, value] = month.split('-').map(Number)
  return monthKey(new Date(year, value - 2, 1))
}

function changeMonth(offset: number) {
  const [year, month] = selectedMonth.value.split('-').map(Number)
  selectedMonth.value = monthKey(new Date(year, month - 1 + offset, 1))
  void loadOverview()
}

async function loadOverview() {
  const range = monthRange(selectedMonth.value)
  const previousRange = monthRange(previousMonth(selectedMonth.value))
  error.value = ''
  try {
    const [response, previousResponse] = await Promise.all([
      profileApi.getOverview(range[0], range[1]),
      profileApi.getOverview(previousRange[0], previousRange[1])
    ])
    overview.value = response.data
    previousOverview.value = previousResponse.data
    if (response.data?.membership?.id) {
      await loadMyRequests()
    } else {
      myRequests.value = []
    }
  } catch {
    error.value = '个人信息加载失败，请稍后重试'
  }
}

async function loadMyRequests() {
  try {
    const res = await membershipChangeRequestsApi.list()
    myRequests.value = res.data ?? []
  } catch {
    myRequests.value = []
  }
}

function requestStatusLabel(status: string) {
  const map: Record<string, string> = { PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回', CANCELLED: '已取消' }
  return map[status] ?? status
}

async function submitFreeze() {
  const membershipId = overview.value?.membership?.id
  if (!membershipId || !freezeRange.value?.[0] || !freezeRange.value[1]) {
    ElMessage.warning('请选择停卡日期')
    return
  }
  freezeSubmitting.value = true
  try {
    await membershipChangeRequestsApi.create({
      type: 'FREEZE',
      membershipId,
      freezeStart: freezeRange.value[0],
      freezeEnd: freezeRange.value[1],
      reason: freezeReason.value.trim() || undefined
    })
    ElMessage.success('停卡申请已提交')
    freezeRange.value = null
    freezeReason.value = ''
    await loadMyRequests()
  } finally {
    freezeSubmitting.value = false
  }
}

async function cancelRequest(id: number) {
  try {
    await membershipChangeRequestsApi.cancel(id)
    ElMessage.success('已取消')
    await loadMyRequests()
  } catch {
    /* interceptor */
  }
}

const checkedDates = computed(() => new Set(overview.value?.checkInDates || []))
const checkInDays = computed(() => checkedDates.value.size)
const previousCheckInDays = computed(() => new Set(previousOverview.value?.checkInDates || []).size)
const comparisonTone = computed(() => checkInDays.value >= previousCheckInDays.value ? 'positive' : 'negative')
const comparisonText = computed(() => {
  const difference = checkInDays.value - previousCheckInDays.value
  if (difference === 0) return '与上月持平'
  if (previousCheckInDays.value === 0) return `比上月多 ${difference} 天`
  const percentage = Math.round(Math.abs(difference) / previousCheckInDays.value * 100)
  return difference > 0
    ? `比上月多 ${difference} 天，提升 ${percentage}%`
    : `比上月少 ${Math.abs(difference)} 天，下降 ${percentage}%`
})
const calendarCells = computed<Array<CalendarCell | null>>(() => {
  const [year, month] = selectedMonth.value.split('-').map(Number)
  const daysInMonth = new Date(year, month, 0).getDate()
  const firstDay = (new Date(year, month - 1, 1).getDay() + 6) % 7
  const cells: Array<CalendarCell | null> = Array.from({ length: firstDay }, () => null)
  for (let day = 1; day <= daysInMonth; day++) {
    cells.push({ date: `${selectedMonth.value}-${String(day).padStart(2, '0')}`, day })
  }
  return cells
})

function formatDate(value?: string) {
  return value ? value.slice(0, 10) : '—'
}

const logout = async () => {
  try {
    await ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning' })
    await authStore.logout()
  } catch {
    /* 取消 */
  }
}

onMounted(loadOverview)
</script>

<style scoped>
.row {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid var(--border);
  font-size: 14px;
}

.row:last-of-type {
  border-bottom: none;
}

.overview-grid { display: grid; grid-template-columns: 1fr; gap: 16px; margin-bottom: 16px; }
.overview-grid.with-attendance { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.simple-card h3 { margin: 0 0 14px; font-size: 16px; }
.card-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; flex-wrap: wrap; }
.month-picker { display: flex; align-items: center; gap: 2px; }
.month-picker .el-date-editor { width: 138px; }
.muted { color: var(--text-muted); font-size: 12px; }
.checkin-summary { display: flex; align-items: baseline; gap: 24px; margin: 22px 0 4px; }
.checkin-summary strong { color: var(--primary); font-size: 38px; line-height: 1; }
.checkin-summary small { margin-left: 5px; color: var(--text-muted); font-size: 13px; }
.summary-secondary strong { color: var(--text); font-size: 22px; }
.month-comparison { min-height: 20px; margin-bottom: 14px; font-size: 12px; font-weight: 600; }
.month-comparison.positive { color: #16805b; }
.month-comparison.negative { color: #b65f4f; }
.attendance-calendar { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); gap: 5px; }
.calendar-weekday { padding: 5px 0; color: var(--text-muted); font-size: 11px; text-align: center; }
.calendar-cell { min-height: 38px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 3px; border-radius: 6px; color: var(--text); font-size: 12px; }
.calendar-cell.attended { background: #effaf4; color: #0e805d; font-weight: 700; }
.calendar-blank { background: transparent; }
.attendance-dot { width: 5px; height: 5px; border-radius: 50%; background: #19b477; }
.membership-card { margin-bottom: 16px; }
.membership-type { font-size: 30px; font-weight: 700; color: var(--primary); margin: 18px 0 8px; }

@media (max-width: 720px) { .overview-grid, .overview-grid.with-attendance { grid-template-columns: 1fr; } .card-head { align-items: stretch; } .month-picker { justify-content: space-between; } }

.label {
  color: var(--text-muted);
}

.logout-btn {
  margin-top: 20px;
  width: 100%;
}
</style>

<template>
  <div class="profile">
    <h2 class="simple-page-title">个人中心</h2>
    <p class="simple-page-desc">查看账户、套餐和签到信息</p>

    <el-alert v-if="error" type="error" :title="error" show-icon closable @close="error = ''" />

    <div class="overview-grid">
      <div class="simple-card account-card">
        <h3>账户信息</h3>
        <div class="row"><span class="label">用户名</span><span>{{ overview?.username || authStore.username || '—' }}</span></div>
        <div class="row"><span class="label">邮箱</span><span>{{ overview?.email || authStore.email || '—' }}</span></div>
        <div class="row"><span class="label">用户 ID</span><span>{{ overview?.userId || authStore.userId || '—' }}</span></div>
        <el-button type="danger" plain class="logout-btn" @click="logout">退出登录</el-button>
      </div>

      <div class="simple-card checkin-card">
        <div class="card-head">
          <div><h3>签到统计</h3><span class="muted">选择日期范围查看签到次数</span></div>
          <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" @change="loadOverview" />
        </div>
        <div class="checkin-count">{{ overview?.checkInCount ?? 0 }} <small>次</small></div>
        <div class="muted">{{ overview?.from || '—' }} 至 {{ overview?.to || '—' }}</div>
      </div>
    </div>

    <div v-if="overview?.member" class="overview-grid member-grid">
      <div class="simple-card qr-card">
        <h3>会员签到二维码</h3>
        <div v-if="qrDataUrl" class="qr-wrap"><img :src="qrDataUrl" alt="会员签到二维码" /></div>
        <div class="muted member-number">会员编号：{{ overview.member.memberNumber || '—' }}</div>
      </div>
      <div class="simple-card membership-card">
        <h3>当前套餐</h3>
        <div class="membership-type">{{ overview.membership?.membershipType || '暂无有效套餐' }}</div>
        <div class="row"><span class="label">套餐名称</span><span>{{ overview.membership?.packageName || '—' }}</span></div>
        <div class="row"><span class="label">有效期</span><span>{{ formatDate(overview.membership?.startsAt) }} 至 {{ formatDate(overview.membership?.expiresAt) }}</span></div>
      </div>
    </div>

    <div v-else class="simple-card empty-card">当前账号不是会员，暂无签到二维码和套餐信息。</div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import QRCode from 'qrcode'
import { useAuthStore } from '@/stores/auth'
import { profileApi, type ProfileOverview } from '@/api/profile'

const authStore = useAuthStore()
const overview = ref<ProfileOverview | null>(null)
const dateRange = ref<[string, string] | null>(null)
const qrDataUrl = ref('')
const error = ref('')

function monthRange(): [string, string] {
  const today = new Date()
  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const lastDay = new Date(year, today.getMonth() + 1, 0).getDate()
  return [`${year}-${month}-01`, `${year}-${month}-${String(lastDay).padStart(2, '0')}`]
}

async function loadOverview() {
  const range = dateRange.value || monthRange()
  dateRange.value = range
  error.value = ''
  try {
    const response = await profileApi.getOverview(range[0], range[1])
    overview.value = response.data
    qrDataUrl.value = response.data?.member?.checkInToken
      ? await QRCode.toDataURL(response.data.member.checkInToken, { width: 220, margin: 2 })
      : ''
  } catch {
    error.value = '个人信息加载失败，请稍后重试'
  }
}

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

.overview-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; margin-bottom: 16px; }
.simple-card h3 { margin: 0 0 14px; font-size: 16px; }
.card-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; flex-wrap: wrap; }
.muted { color: var(--text-muted); font-size: 12px; }
.checkin-count { margin-top: 34px; font-size: 42px; font-weight: 700; color: var(--primary); }
.checkin-count small { font-size: 16px; font-weight: 500; }
.member-grid { align-items: stretch; }
.qr-card { text-align: center; }
.qr-wrap { display: flex; justify-content: center; margin: 12px 0; }
.qr-wrap img { width: 220px; height: 220px; image-rendering: pixelated; }
.member-number { text-align: center; }
.membership-type { font-size: 30px; font-weight: 700; color: var(--primary); margin: 18px 0 24px; }
.empty-card { color: var(--text-muted); }

@media (max-width: 720px) { .overview-grid { grid-template-columns: 1fr; } }

.label {
  color: var(--text-muted);
}

.logout-btn {
  margin-top: 20px;
  width: 100%;
}
</style>

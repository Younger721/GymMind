<template>
  <div>
    <h2 class="simple-page-title">租户设置</h2>
    <p class="simple-page-desc">门店设置、AI 服务配置与当前资源使用情况</p>

    <el-tabs v-model="tab">
      <el-tab-pane label="门店设置" name="settings">
        <div class="simple-card form-box">
          <el-form label-width="120px">
            <el-form-item label="预约功能">
              <el-switch v-model="settingsForm.reservationEnabled" />
            </el-form-item>
            <el-form-item label="签到功能">
              <el-switch v-model="settingsForm.checkInEnabled" />
            </el-form-item>
            <el-form-item label="时区">
              <el-input v-model="settingsForm.timezone" placeholder="Asia/Shanghai" />
            </el-form-item>
            <el-button v-if="canWrite" type="primary" :loading="loading" @click="saveSettings">保存设置</el-button>
          </el-form>
        </div>
      </el-tab-pane>
      <el-tab-pane v-if="authStore.isTenantUser && authStore.roles.includes('GYM_ADMIN')" label="AI 服务" name="ai">
        <HarnessConfigPanel />
      </el-tab-pane>
      <el-tab-pane label="额度与使用量" name="quota">
        <div class="simple-card">
          <p class="panel-desc">额度由平台管理员设置，租户管理员只能查看当前使用范围。</p>
          <el-button v-if="canReadQuota" type="primary" :loading="loading" @click="loadQuota">刷新额度</el-button>
          <JsonBlock v-if="quota" :data="quota" />
        </div>
      </el-tab-pane>
    </el-tabs>
    <JsonBlock v-if="saveResult" :data="saveResult" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import JsonBlock from '@/components/business/JsonBlock.vue'
import HarnessConfigPanel from '@/components/business/HarnessConfigPanel.vue'
import { tenancyApi } from '@/api/tenancy'
import { useAuthStore } from '@/stores/auth'
import { hasPermission } from '@/utils/permissions'

const authStore = useAuthStore()
const canWrite = computed(() => hasPermission('tenant:settings:write'))
const canReadQuota = computed(() => hasPermission('tenant:quota:read'))

const tab = ref('settings')
const loading = ref(false)
const quota = ref<unknown>(null)
const saveResult = ref<unknown>(null)
const settingsForm = reactive({
  reservationEnabled: true,
  checkInEnabled: true,
  timezone: 'Asia/Shanghai'
})

async function loadQuota() {
  loading.value = true
  try {
    quota.value = await tenancyApi.getMyQuota()
  } finally {
    loading.value = false
  }
}

async function loadSettings() {
  loading.value = true
  try {
    const res = await tenancyApi.getSettings()
    const data = (res as { data?: typeof settingsForm }).data
    if (data) {
      settingsForm.reservationEnabled = data.reservationEnabled ?? true
      settingsForm.checkInEnabled = data.checkInEnabled ?? true
      settingsForm.timezone = data.timezone ?? 'Asia/Shanghai'
    }
    saveResult.value = res
  } finally {
    loading.value = false
  }
}

async function saveSettings() {
  loading.value = true
  try {
    saveResult.value = await tenancyApi.updateSettings({ ...settingsForm })
  } finally {
    loading.value = false
  }
}

onMounted(loadSettings)
</script>

<style scoped>
.form-box {
  margin-top: 16px;
}
</style>

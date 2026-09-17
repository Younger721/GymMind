<template>
  <div>
    <h2 class="simple-page-title">租户设置</h2>
    <p class="simple-page-desc">门店功能开关、时区与配额</p>

    <HarnessConfigPanel v-if="authStore.isTenantUser && authStore.roles.includes('GYM_ADMIN')" />

    <div class="simple-card" style="margin-top: 16px">
      <el-button type="primary" :loading="loading" @click="loadQuota">我的配额</el-button>
      <el-button :loading="loading" @click="loadSettings">加载设置</el-button>
    </div>

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
        <el-button type="primary" :loading="loading" @click="saveSettings">保存设置</el-button>
      </el-form>
    </div>

    <JsonBlock v-if="quota" :data="quota" />
    <JsonBlock v-if="saveResult" :data="saveResult" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import JsonBlock from '@/components/business/JsonBlock.vue'
import HarnessConfigPanel from '@/components/business/HarnessConfigPanel.vue'
import { tenancyApi } from '@/api/tenancy'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

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

onMounted(loadQuota)
</script>

<style scoped>
.form-box {
  margin-top: 16px;
}
</style>

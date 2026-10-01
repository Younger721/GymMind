<template>
  <div>
    <h2 class="simple-page-title">平台控制台</h2>
    <p class="simple-page-desc">管理租户生命周期、共享资源配额和全局运行状态</p>

    <el-tabs v-model="tab">
      <el-tab-pane label="全局概览" name="stats">
        <div class="simple-card">
          <h3 class="sub-title">平台运行概览</h3>
          <p class="panel-desc">仅展示聚合指标，不进入任何租户业务上下文。</p>
          <el-button type="primary" :loading="loading" @click="loadStats">刷新统计</el-button>
          <JsonBlock v-if="stats" :data="stats" />
        </div>
      </el-tab-pane>
      <el-tab-pane label="租户生命周期" name="tenants">
        <BizCrudPanel
          title="租户列表"
          :create-fields="tenantFields"
          :row-actions="tenantActions"
          :loader="loadTenants"
          :creator="createTenant"
          create-permission="platform:tenant:write"
        />
      </el-tab-pane>
      <el-tab-pane label="门店设置" name="settings">
        <div class="simple-card">
          <el-form inline>
            <el-form-item label="选择门店">
              <el-select v-model="selectedTenantId" placeholder="请选择门店" style="width: 280px">
                <el-option v-for="tenant in tenants" :key="tenant.id" :label="`${tenant.name}（ID: ${tenant.id}）`" :value="Number(tenant.id)" />
              </el-select>
            </el-form-item>
          </el-form>
          <el-form v-if="selectedTenantId && settingsReady" label-width="120px" style="max-width: 560px">
            <el-form-item label="预约功能"><el-switch v-model="settingsForm.reservationEnabled" /></el-form-item>
            <el-form-item label="签到功能"><el-switch v-model="settingsForm.checkInEnabled" /></el-form-item>
            <el-form-item label="时区"><el-input v-model="settingsForm.timezone" /></el-form-item>
            <el-button type="primary" :loading="settingsLoading" :disabled="!settingsReady" @click="saveTenantSettings">保存设置</el-button>
          </el-form>
          <el-empty v-if="!selectedTenantId" description="请选择门店后管理设置" />
          <el-empty v-else-if="settingsLoading" description="正在加载门店设置" />
          <el-empty v-else-if="!settingsReady" description="门店设置加载失败，请切换门店后重试" />
        </div>
      </el-tab-pane>
      <el-tab-pane label="平台 AI 服务" name="harness">
        <HarnessConfigPanel platform />
      </el-tab-pane>
      <el-tab-pane label="配额与模块" name="quota">
        <div class="simple-card">
          <el-form inline>
            <el-form-item label="租户ID">
              <el-input v-model="quotaTenantId" style="width: 120px" />
            </el-form-item>
            <el-button :loading="loading" @click="loadQuota">查询</el-button>
            <el-button type="primary" :loading="loading" @click="saveQuota">更新配额</el-button>
          </el-form>
          <el-form label-width="140px" style="margin-top: 12px">
            <el-form-item label="最大 Agent"><el-input v-model="quotaForm.maxAgents" /></el-form-item>
            <el-form-item label="月 AI 调用上限"><el-input v-model="quotaForm.maxAiCallsPerMonth" /></el-form-item>
            <el-form-item label="知识库文档上限"><el-input v-model="quotaForm.maxKnowledgeDocuments" /></el-form-item>
            <el-form-item label="启用 AI 模块"><el-switch v-model="quotaForm.aiModuleEnabled" /></el-form-item>
            <el-form-item label="启用知识库"><el-switch v-model="quotaForm.knowledgeModuleEnabled" /></el-form-item>
          </el-form>
          <JsonBlock v-if="quotaResult" :data="quotaResult" />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import BizCrudPanel, { type FieldDef, type RowAction } from '@/components/business/BizCrudPanel.vue'
import HarnessConfigPanel from '@/components/business/HarnessConfigPanel.vue'
import JsonBlock from '@/components/business/JsonBlock.vue'
import { tenancyApi } from '@/api/tenancy'
import { useAuthStore } from '@/stores/auth'
import { isTenantRequestCurrent } from '@/utils/tenantContext'

const tab = ref('stats')
const loading = ref(false)
const authStore = useAuthStore()
const stats = ref<unknown>(null)
const quotaTenantId = ref('')
const quotaResult = ref<unknown>(null)
const quotaForm = reactive({
  maxAgents: '5',
  maxAiCallsPerMonth: '1000',
  maxKnowledgeDocuments: '100',
  aiModuleEnabled: true,
  knowledgeModuleEnabled: true
})
const tenants = ref<Record<string, any>[]>([])
const selectedTenantId = computed<number | null>({
  get: () => authStore.platformContextTenantId,
  set: value => authStore.setPlatformContextTenantId(value)
})
const settingsForm = reactive({ reservationEnabled: true, checkInEnabled: true, timezone: 'Asia/Shanghai' })
const settingsLoading = ref(false)
const settingsLoadedForTenantId = ref<number | null>(null)
let settingsRequestVersion = 0
const settingsReady = computed(() => selectedTenantId.value != null
  && settingsLoadedForTenantId.value === selectedTenantId.value
  && !settingsLoading.value)

const tenantFields: FieldDef[] = [
  { key: 'tenantCode', label: '租户编码', placeholder: 'gym-east' },
  { key: 'tenantName', label: '租户名称' },
  { key: 'adminEmail', label: '管理员邮箱' },
  { key: 'adminPassword', label: '管理员密码' },
  { key: 'adminDisplayName', label: '管理员姓名' }
]

const tenantActions: RowAction[] = [
  { label: '启用', handler: row => tenancyApi.activateTenant(Number(row.id)) },
  { label: '禁用', handler: row => tenancyApi.disableTenant(Number(row.id)) }
]

async function loadStats() {
  loading.value = true
  try {
    stats.value = await tenancyApi.platformStats()
  } finally {
    loading.value = false
  }
}

async function loadTenants() {
  const response = await tenancyApi.listTenants()
  const data = (response as any)?.data
  tenants.value = Array.isArray(data?.content) ? data.content : Array.isArray(data) ? data : []
  return response
}

async function loadTenantSettings(tenantId = selectedTenantId.value) {
  const requestVersion = ++settingsRequestVersion
  settingsLoadedForTenantId.value = null
  Object.assign(settingsForm, { reservationEnabled: true, checkInEnabled: true, timezone: 'Asia/Shanghai' })
  if (!tenantId) {
    settingsLoading.value = false
    return
  }
  settingsLoading.value = true
  try {
    const response = await tenancyApi.getTenantSettings(tenantId)
    const data = (response as any)?.data
    if (isTenantRequestCurrent(tenantId, selectedTenantId.value, requestVersion, settingsRequestVersion) && data) {
      Object.assign(settingsForm, data)
      settingsLoadedForTenantId.value = tenantId
    }
  } catch {
    // The form stays disabled until a successful load for the current tenant.
  } finally {
    if (requestVersion === settingsRequestVersion) settingsLoading.value = false
  }
}

async function saveTenantSettings() {
  const targetTenantId = selectedTenantId.value
  if (!targetTenantId || settingsLoadedForTenantId.value !== targetTenantId || settingsLoading.value) return
  settingsLoading.value = true
  try {
    await tenancyApi.updateTenantSettings(targetTenantId, { ...settingsForm })
    ElMessage.success('门店设置已保存')
  } finally {
    if (selectedTenantId.value === targetTenantId) settingsLoading.value = false
  }
}

watch(() => selectedTenantId.value, tenantId => {
  void loadTenantSettings(tenantId)
}, { immediate: true })

async function createTenant(p: Record<string, string>) {
  return tenancyApi.createTenant(p)
}

async function loadQuota() {
  if (!quotaTenantId.value) return ElMessage.warning('请输入租户 ID')
  loading.value = true
  try {
    const response = await tenancyApi.getTenantQuota(Number(quotaTenantId.value))
    quotaResult.value = response
    const data = (response as { data?: Record<string, unknown> }).data
    if (data) {
      quotaForm.maxAgents = String(data.maxAgents ?? quotaForm.maxAgents)
      quotaForm.maxAiCallsPerMonth = String(data.maxAiCallsPerMonth ?? quotaForm.maxAiCallsPerMonth)
      quotaForm.maxKnowledgeDocuments = String(data.maxKnowledgeDocuments ?? quotaForm.maxKnowledgeDocuments)
      quotaForm.aiModuleEnabled = Boolean(data.aiModuleEnabled ?? quotaForm.aiModuleEnabled)
      quotaForm.knowledgeModuleEnabled = Boolean(data.knowledgeModuleEnabled ?? quotaForm.knowledgeModuleEnabled)
    }
  } finally {
    loading.value = false
  }
}

async function saveQuota() {
  if (!quotaTenantId.value) return ElMessage.warning('请输入租户 ID')
  loading.value = true
  try {
    quotaResult.value = await tenancyApi.updateTenantQuota(Number(quotaTenantId.value), {
      maxAgents: Number(quotaForm.maxAgents),
      maxAiCallsPerMonth: Number(quotaForm.maxAiCallsPerMonth),
      maxKnowledgeDocuments: Number(quotaForm.maxKnowledgeDocuments),
      aiModuleEnabled: quotaForm.aiModuleEnabled,
      knowledgeModuleEnabled: quotaForm.knowledgeModuleEnabled
    })
    ElMessage.success('已更新')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.sub-title {
  margin: 0 0 4px;
  font-size: 16px;
}

.panel-desc {
  margin: 0 0 12px;
  color: var(--text-muted);
  font-size: 13px;
}
</style>

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
      <el-tab-pane label="管理员账号" name="admins">
        <div class="simple-card">
          <el-button :disabled="!selectedTenantId" :loading="adminsLoading" @click="loadAdministrators">刷新</el-button>
          <el-table v-if="selectedTenantId" :data="administrators" v-loading="adminsLoading" row-key="id">
            <el-table-column prop="displayName" label="姓名" />
            <el-table-column prop="username" label="用户名" />
            <el-table-column prop="email" label="邮箱" />
            <el-table-column label="状态"><template #default="{ row }">{{ row.status === 'ACTIVE' ? '启用' : '停用' }}</template></el-table-column>
            <el-table-column label="操作" width="210">
              <template #default="{ row }">
                <el-button link type="primary" @click="resetAdministratorPassword(row)">重置密码</el-button>
                <el-button link :type="row.status === 'ACTIVE' ? 'danger' : 'success'" @click="toggleAdministrator(row)">{{ row.status === 'ACTIVE' ? '停用' : '启用' }}</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!selectedTenantId" description="请先在顶部选择租户" />
        </div>
      </el-tab-pane>
      <el-tab-pane label="平台 AI 服务" name="harness">
        <HarnessConfigPanel platform />
      </el-tab-pane>
      <el-tab-pane label="配额与模块" name="quota">
        <div class="simple-card">
          <el-button :disabled="!selectedTenantId" :loading="loading" @click="loadQuota">刷新</el-button>
          <el-empty v-if="!selectedTenantId" description="请先在顶部选择租户" />
          <div v-if="selectedTenantId && quotaUsage" class="quota-usage">
            <span>本月 AI 调用 {{ formatNumber(quotaUsage.usedAiCallsThisMonth) }} / {{ formatNumber(quotaUsage.maxAiCallsPerMonth) }}</span>
            <span>本月 Token {{ formatNumber(quotaUsage.usedAiTokensThisMonth) }} / {{ formatNumber(quotaUsage.maxAiTokensPerMonth) }}</span>
          </div>
          <el-form v-if="selectedTenantId" label-width="140px" class="quota-form">
            <el-form-item label="最大 Agent"><el-input :model-value="'1'" disabled /></el-form-item>
            <el-form-item label="月 AI 调用上限"><el-input v-model="quotaForm.maxAiCallsPerMonth" /></el-form-item>
            <el-form-item label="月 Token 配额"><el-input v-model="quotaForm.maxAiTokensPerMonth" /></el-form-item>
            <el-form-item label="知识库文档上限"><el-input v-model="quotaForm.maxKnowledgeDocuments" /></el-form-item>
            <el-form-item label="启用 AI 模块"><el-switch v-model="quotaForm.aiModuleEnabled" /></el-form-item>
            <el-form-item label="启用知识库"><el-switch v-model="quotaForm.knowledgeModuleEnabled" /></el-form-item>
            <el-button type="primary" :disabled="!selectedTenantId" :loading="loading" @click="saveQuota">保存额度</el-button>
          </el-form>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import BizCrudPanel, { type FieldDef, type RowAction } from '@/components/business/BizCrudPanel.vue'
import HarnessConfigPanel from '@/components/business/HarnessConfigPanel.vue'
import JsonBlock from '@/components/business/JsonBlock.vue'
import { tenancyApi } from '@/api/tenancy'
import { useAuthStore } from '@/stores/auth'
import { buildTenantCreatePayload, isTenantCreateFormValid } from '@/utils/tenantCreate.mjs'

const tab = ref('stats')
const loading = ref(false)
const authStore = useAuthStore()
const stats = ref<unknown>(null)
const quotaUsage = ref<{ usedAiCallsThisMonth: number; maxAiCallsPerMonth: number; usedAiTokensThisMonth: number; maxAiTokensPerMonth: number } | null>(null)

function formatNumber(value: number | null | undefined) {
  return Number(value ?? 0).toLocaleString('zh-CN')
}

const quotaForm = reactive({
  maxAgents: '1',
  maxAiCallsPerMonth: '1000',
  maxAiTokensPerMonth: '1000000',
  maxKnowledgeDocuments: '100',
  aiModuleEnabled: true,
  knowledgeModuleEnabled: true
})
const selectedTenantId = computed(() => authStore.platformContextTenantId)
const administrators = ref<Record<string, any>[]>([])
const adminsLoading = ref(false)

const tenantFields: FieldDef[] = [
  { key: 'tenantName', label: '租户名称', required: true },
  { key: 'adminEmail', label: '管理员邮箱', required: true },
  { key: 'adminPassword', label: '管理员密码', required: true },
  { key: 'adminDisplayName', label: '管理员姓名', required: true }
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

function loadTenants() {
  return tenancyApi.listTenants()
}

watch(() => selectedTenantId.value, tenantId => {
  quotaUsage.value = null
  if (!tenantId) return
  void loadAdministrators()
  void loadQuota()
}, { immediate: true })

async function loadAdministrators() {
  const tenantId = selectedTenantId.value
  if (!tenantId) return
  adminsLoading.value = true
  try {
    const response = await tenancyApi.listTenantAdministrators(tenantId)
    administrators.value = (response as any)?.data?.content ?? []
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '管理员列表加载失败')
  } finally {
    adminsLoading.value = false
  }
}

async function resetAdministratorPassword(row: Record<string, any>) {
  const tenantId = selectedTenantId.value
  if (!tenantId) return
  try {
    const { value } = await ElMessageBox.prompt('请输入新密码（至少 6 位）', {
      title: '重置管理员密码',
      inputType: 'password', inputPattern: /^.{6,72}$/, inputErrorMessage: '密码长度须为 6 至 72 位'
    })
    await tenancyApi.resetTenantAdministratorPassword(tenantId, Number(row.id), value)
    ElMessage.success('密码已重置，原有会话已失效')
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.response?.data?.message || '密码重置失败')
  }
}

async function toggleAdministrator(row: Record<string, any>) {
  const tenantId = selectedTenantId.value
  if (!tenantId) return
  const status = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  try {
    await tenancyApi.changeTenantAdministratorStatus(tenantId, Number(row.id), status)
    row.status = status
    ElMessage.success(status === 'ACTIVE' ? '账号已启用' : '账号已停用')
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '账号状态更新失败')
  }
}

async function createTenant(p: Record<string, string>) {
  if (!isTenantCreateFormValid(p)) {
    ElMessage.warning('请填写租户和管理员必填信息')
    throw new Error('tenant create form is incomplete')
  }
  return tenancyApi.createTenant(buildTenantCreatePayload(p))
}

async function loadQuota() {
  const tenantId = selectedTenantId.value
  if (!tenantId) return
  loading.value = true
  try {
    const response = await tenancyApi.getTenantQuota(tenantId)
    const data = (response as { data?: Record<string, unknown> }).data
    if (!data) return
    quotaForm.maxAgents = '1'
    quotaForm.maxAiCallsPerMonth = String(data.maxAiCallsPerMonth ?? quotaForm.maxAiCallsPerMonth)
    quotaForm.maxAiTokensPerMonth = String(data.maxAiTokensPerMonth ?? quotaForm.maxAiTokensPerMonth)
    quotaForm.maxKnowledgeDocuments = String(data.maxKnowledgeDocuments ?? quotaForm.maxKnowledgeDocuments)
    quotaForm.aiModuleEnabled = Boolean(data.aiModuleEnabled ?? quotaForm.aiModuleEnabled)
    quotaForm.knowledgeModuleEnabled = Boolean(data.knowledgeModuleEnabled ?? quotaForm.knowledgeModuleEnabled)
    quotaUsage.value = {
      usedAiCallsThisMonth: Number(data.usedAiCallsThisMonth ?? 0),
      maxAiCallsPerMonth: Number(data.maxAiCallsPerMonth ?? 0),
      usedAiTokensThisMonth: Number(data.usedAiTokensThisMonth ?? 0),
      maxAiTokensPerMonth: Number(data.maxAiTokensPerMonth ?? 0)
    }
  } finally {
    loading.value = false
  }
}

async function saveQuota() {
  const tenantId = selectedTenantId.value
  if (!tenantId) return ElMessage.warning('请先在顶部选择租户')
  const payload = {
    maxAgents: 1,
    maxAiCallsPerMonth: Number(quotaForm.maxAiCallsPerMonth),
    maxAiTokensPerMonth: Number(quotaForm.maxAiTokensPerMonth),
    maxKnowledgeDocuments: Number(quotaForm.maxKnowledgeDocuments),
    aiModuleEnabled: quotaForm.aiModuleEnabled,
    knowledgeModuleEnabled: quotaForm.knowledgeModuleEnabled
  }
  if ([payload.maxAgents, payload.maxAiCallsPerMonth, payload.maxAiTokensPerMonth, payload.maxKnowledgeDocuments].some(value => !Number.isInteger(value) || value < 1)) {
    return ElMessage.warning('额度必须是大于 0 的整数')
  }
  loading.value = true
  try {
    await tenancyApi.updateTenantQuota(tenantId, payload)
    ElMessage.success('额度已保存')
    await loadQuota()
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

.quota-usage {
  display: flex;
  gap: 24px;
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 13px;
}

.quota-form {
  max-width: 520px;
  margin-top: 12px;
}
</style>

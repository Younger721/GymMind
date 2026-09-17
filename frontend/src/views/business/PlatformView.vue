<template>
  <div>
    <h2 class="simple-page-title">平台管理</h2>
    <p class="simple-page-desc">租户、配额与平台统计（平台管理员）</p>

    <el-tabs v-model="tab">
      <el-tab-pane label="平台统计" name="stats">
        <div class="simple-card">
          <el-button type="primary" :loading="loading" @click="loadStats">刷新统计</el-button>
          <JsonBlock v-if="stats" :data="stats" />
        </div>
      </el-tab-pane>
      <el-tab-pane label="租户" name="tenants">
        <BizCrudPanel
          title="租户列表"
          :create-fields="tenantFields"
          :row-actions="tenantActions"
          :loader="loadTenants"
          :creator="createTenant"
        />
      </el-tab-pane>
      <el-tab-pane label="AI Harness" name="harness">
        <HarnessConfigPanel platform />
      </el-tab-pane>
      <el-tab-pane label="租户配额" name="quota">
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
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import BizCrudPanel, { type FieldDef, type RowAction } from '@/components/business/BizCrudPanel.vue'
import HarnessConfigPanel from '@/components/business/HarnessConfigPanel.vue'
import JsonBlock from '@/components/business/JsonBlock.vue'
import { tenancyApi } from '@/api/tenancy'

const tab = ref('stats')
const loading = ref(false)
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
  return tenancyApi.listTenants()
}

async function createTenant(p: Record<string, string>) {
  return tenancyApi.createTenant(p)
}

async function loadQuota() {
  if (!quotaTenantId.value) return ElMessage.warning('请输入租户 ID')
  loading.value = true
  try {
    quotaResult.value = await tenancyApi.getTenantQuota(Number(quotaTenantId.value))
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

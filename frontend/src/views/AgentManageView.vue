<template>
  <div class="agent-manage">
    <div class="page-header">
      <div>
        <h2 class="simple-page-title">门店 AI 提示词</h2>
        <p class="simple-page-desc">自定义本店 AI 助手的角色、口吻和业务规则</p>
      </div>
      <el-tag v-if="authStore.isPlatformAdmin" type="info">平台管理员 · 全租户只读</el-tag>
    </div>

    <el-card v-if="quota && !authStore.isPlatformAdmin" class="quota-card" shadow="never">
      <div class="quota-grid">
        <div class="quota-item">
          <span class="label">Agent 数量</span>
          <strong>{{ quota.usedAgents }} / {{ quota.maxAgents }}</strong>
        </div>
        <div class="quota-item">
          <span class="label">本月 AI 调用</span>
          <strong>{{ quota.usedAiCallsThisMonth }} / {{ quota.maxAiCallsPerMonth }}</strong>
        </div>
        <div class="quota-item">
          <span class="label">知识文档</span>
          <strong>{{ quota.usedKnowledgeDocuments }} / {{ quota.maxKnowledgeDocuments }}</strong>
        </div>
      </div>
    </el-card>

    <el-table :data="agents" v-loading="loading" stripe>
      <el-table-column v-if="authStore.isPlatformAdmin" prop="tenantId" label="租户ID" width="90" />
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
      <el-table-column prop="systemPrompt" label="系统提示词" min-width="300" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
            {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="canWrite" label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.id === editableAgentId" link type="primary" @click="openPromptEdit(row)">编辑提示词</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="编辑门店 AI 提示词" width="640px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="系统提示词" required>
          <el-input v-model="form.systemPrompt" type="textarea" :rows="10" maxlength="4000" show-word-limit
            placeholder="例如：你是本店的健身助手，请使用专业、友好的语气回答会员问题。" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button v-if="canWrite" type="primary" :loading="saving" @click="savePrompt">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { agentApi, type AgentView, type TenantQuotaView } from '@/api/agent'
import { useAuthStore } from '@/stores/auth'
import { hasPermission } from '@/utils/permissions'
import { MENU_CODES } from '@/utils/menuPermissions'
import { ElMessage } from 'element-plus'

const authStore = useAuthStore()
const agents = ref<AgentView[]>([])
const quota = ref<TenantQuotaView | null>(null)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  systemPrompt: ''
})
// The page itself is the permission boundary. Once visible, all page actions
// are available; the backend still enforces tenant scope for writes.
const canWrite = computed(() =>
  !authStore.isPlatformAdmin
  && authStore.roles.includes('GYM_ADMIN')
  && hasPermission(MENU_CODES.AI_CONFIG))
const editableAgentId = computed(() => agents.value
  .filter(agent => agent.status === 'ACTIVE')
  .slice()
  .sort((left, right) => left.id - right.id)[0]?.id ?? null)

const load = async () => {
  loading.value = true
  try {
    const agentRes = await agentApi.list({ silent: authStore.isPlatformAdmin })
    agents.value = agentRes.data || []
    if (!authStore.isPlatformAdmin) {
      const quotaRes = await agentApi.quota()
      quota.value = quotaRes.data
    } else {
      quota.value = null
    }
  } catch {
    ElMessage.error('加载 Agent 列表失败，请确认您有管理员权限')
  } finally {
    loading.value = false
  }
}

const openPromptEdit = (row: AgentView) => {
  editingId.value = row.id
  form.systemPrompt = row.systemPrompt
  dialogVisible.value = true
}

const savePrompt = async () => {
  if (!editingId.value || !form.systemPrompt.trim()) {
    ElMessage.warning('请填写系统提示词')
    return
  }
  saving.value = true
  try {
    await agentApi.updatePrompt(editingId.value, { systemPrompt: form.systemPrompt.trim() })
    ElMessage.success('提示词已保存')
    dialogVisible.value = false
    await load()
  } catch {
    ElMessage.error('保存失败，请确认提示词内容和当前权限')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.agent-manage {
  max-width: 1100px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0 0 6px;
  font-size: 1.5rem;
}

.subtitle {
  margin: 0;
  color: var(--text-secondary, #666);
  font-size: 0.9rem;
}

.quota-card {
  margin-bottom: 20px;
}

.quota-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.quota-item .label {
  display: block;
  color: var(--text-secondary, #666);
  font-size: 0.85rem;
  margin-bottom: 4px;
}
</style>

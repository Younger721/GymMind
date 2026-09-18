<template>
  <div class="agent-manage">
    <div class="page-header">
      <div>
        <h2 class="simple-page-title">Agent 管理</h2>
        <p class="simple-page-desc">配置门店 AI 助手（会员仅可使用）</p>
      </div>
      <el-button v-if="!authStore.isPlatformAdmin" type="primary" @click="openCreate">新建</el-button>
      <el-tag v-else type="info">平台管理员 · 全租户只读</el-tag>
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
      <el-table-column label="知识库" width="90">
        <template #default="{ row }">
          <el-tag :type="row.knowledgeEnabled ? 'success' : 'info'" size="small">
            {{ row.knowledgeEnabled ? '启用' : '关闭' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="工具" min-width="160">
        <template #default="{ row }">
          <el-tag v-for="tool in row.enabledTools" :key="tool" size="small" style="margin-right: 4px">
            {{ tool }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
            {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="!authStore.isPlatformAdmin" label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button
            link
            :type="row.status === 'ACTIVE' ? 'danger' : 'success'"
            @click="toggleStatus(row)"
          >
            {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑 Agent' : '新建 Agent'" width="640px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="如：门店私教助手" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="系统提示词" required>
          <el-input v-model="form.systemPrompt" type="textarea" :rows="5" />
        </el-form-item>
        <el-form-item label="启用知识库">
          <el-switch v-model="form.knowledgeEnabled" />
        </el-form-item>
        <el-form-item label="MCP 工具">
          <el-checkbox-group v-model="form.enabledTools">
            <el-checkbox label="knowledge_search">知识库检索</el-checkbox>
            <el-checkbox label="search_articles">文章搜索</el-checkbox>
            <el-checkbox label="import_article">文章导入</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { agentApi, type AgentView, type TenantQuotaView } from '@/api/agent'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const authStore = useAuthStore()
const agents = ref<AgentView[]>([])
const quota = ref<TenantQuotaView | null>(null)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editing = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  name: '',
  description: '',
  systemPrompt: '',
  knowledgeEnabled: true,
  enabledTools: ['knowledge_search'] as string[]
})

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

const resetForm = () => {
  form.name = ''
  form.description = ''
  form.systemPrompt = '你是本健身房的智能助手，请基于本店知识库给出专业、安全的健身建议。'
  form.knowledgeEnabled = true
  form.enabledTools = ['knowledge_search']
}

const openCreate = () => {
  editing.value = false
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

const openEdit = (row: AgentView) => {
  editing.value = true
  editingId.value = row.id
  form.name = row.name
  form.description = row.description
  form.systemPrompt = row.systemPrompt
  form.knowledgeEnabled = row.knowledgeEnabled
  form.enabledTools = [...row.enabledTools]
  dialogVisible.value = true
}

const save = async () => {
  if (!form.name.trim() || !form.systemPrompt.trim()) {
    ElMessage.warning('请填写名称和系统提示词')
    return
  }
  saving.value = true
  try {
    const payload = { ...form, enabledTools: form.enabledTools.length ? form.enabledTools : ['knowledge_search'] }
    if (editing.value && editingId.value) {
      await agentApi.update(editingId.value, payload)
      ElMessage.success('Agent 已更新')
    } else {
      await agentApi.create(payload)
      ElMessage.success('Agent 已创建')
    }
    dialogVisible.value = false
    await load()
  } catch {
    ElMessage.error('保存失败，可能已超出配额')
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (row: AgentView) => {
  try {
    if (row.status === 'ACTIVE') {
      await agentApi.disable(row.id)
      ElMessage.success('已停用')
    } else {
      await agentApi.activate(row.id)
      ElMessage.success('已启用')
    }
    await load()
  } catch {
    ElMessage.error('操作失败')
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

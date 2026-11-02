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
      <el-table-column label="技能" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          {{ (row.enabledTools || []).join('、') || '默认（knowledge_search）' }}
        </template>
      </el-table-column>
      <el-table-column v-if="canWrite" label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.id === editableAgentId" link type="primary" @click="openPromptEdit(row)">编辑提示词</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="编辑门店 AI 助手" width="640px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="系统提示词" required>
          <el-input v-model="form.systemPrompt" type="textarea" :rows="10" maxlength="4000" show-word-limit
            placeholder="例如：你是本店的健身助手，请使用专业、友好的语气回答会员问题。" />
        </el-form-item>
        <el-form-item label="启用技能">
          <div class="skill-groups">
            <div class="skill-group">
              <div class="skill-group-title">内置工具</div>
              <el-checkbox-group v-model="form.selectedTools">
                <div v-for="tool in builtinTools" :key="tool.name" class="skill-item">
                  <el-checkbox :label="tool.name">
                    <strong>{{ tool.name }}</strong>
                    <span class="skill-desc">{{ tool.description }}</span>
                  </el-checkbox>
                </div>
              </el-checkbox-group>
              <div v-if="!builtinTools.length" class="skill-empty">未获取到内置工具清单</div>
            </div>
            <div class="skill-group">
              <div class="skill-group-title">远程 MCP 技能</div>
              <el-checkbox-group v-model="form.selectedTools">
                <div v-for="tool in remoteTools" :key="tool.qualifiedName" class="skill-item">
                  <el-checkbox :label="tool.qualifiedName">
                    <strong>{{ tool.qualifiedName }}</strong>
                    <span class="skill-desc">{{ tool.description }}</span>
                  </el-checkbox>
                </div>
              </el-checkbox-group>
              <div v-if="!remoteTools.length" class="skill-empty">
                未配置远程 MCP server，或全部不可达（仅展示已配置且在线的技能）
              </div>
            </div>
          </div>
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
import { mcpApi, type McpToolView, type RemoteMcpToolView } from '@/api/mcp'
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
// 内置工具与远程 MCP 技能目录（对话框打开时懒加载并缓存）
const builtinTools = ref<McpToolView[]>([])
const remoteTools = ref<RemoteMcpToolView[]>([])
let toolCatalogLoaded = false

const form = reactive({
  systemPrompt: '',
  selectedTools: [] as string[]
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
  form.selectedTools = [...(row.enabledTools || [])]
  void loadToolCatalogs()
  dialogVisible.value = true
}

// 拉取内置工具与远程 MCP 技能目录（静默失败：列表为空时界面给出提示）
const loadToolCatalogs = async () => {
  if (toolCatalogLoaded) {
    return
  }
  try {
    const [toolRes, remoteRes] = await Promise.all([
      mcpApi.tools(),
      mcpApi.remoteTools()
    ])
    builtinTools.value = toolRes.data || []
    remoteTools.value = remoteRes.data || []
    toolCatalogLoaded = true
  } catch {
    // 静默：勾选区显示空态提示，保存仍可进行（后端会校验）
  }
}

const savePrompt = async () => {
  if (!editingId.value || !form.systemPrompt.trim()) {
    ElMessage.warning('请填写系统提示词')
    return
  }
  saving.value = true
  try {
    await agentApi.updatePrompt(editingId.value, { systemPrompt: form.systemPrompt.trim() })
    // 技能清单一并保存；后端校验内置工具名与远程技能格式
    await agentApi.updateTools(editingId.value, form.selectedTools)
    ElMessage.success('助手配置已保存')
    dialogVisible.value = false
    await load()
  } catch {
    ElMessage.error('保存失败，请确认提示词内容、技能配置和当前权限')
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

/* 技能勾选区：内置工具与远程 MCP 技能两组 */
.skill-groups {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.skill-group-title {
  font-weight: 600;
  font-size: 0.9rem;
  margin-bottom: 6px;
}

.skill-item {
  padding: 2px 0;
}

.skill-desc {
  margin-left: 8px;
  color: var(--text-secondary, #888);
  font-size: 0.85rem;
}

.skill-empty {
  color: var(--text-secondary, #999);
  font-size: 0.85rem;
  padding: 4px 0;
}
</style>

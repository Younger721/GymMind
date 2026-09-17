<template>
  <div>
    <h2 class="simple-page-title">AI 工具箱</h2>
    <p class="simple-page-desc">会员分析、运营分析、推荐、教练助手与记忆</p>

    <el-tabs v-model="tab">
      <el-tab-pane label="会员分析" name="member">
        <div class="simple-card">
          <el-form label-width="140px">
            <el-form-item label="距上次训练(天)">
              <el-input-number v-model="memberStats.daysSinceLastWorkout" :min="0" />
            </el-form-item>
            <el-form-item label="近30天训练">
              <el-input-number v-model="memberStats.workoutsLast30Days" :min="0" />
            </el-form-item>
            <el-form-item label="前30天训练">
              <el-input-number v-model="memberStats.workoutsPrevious30Days" :min="0" />
            </el-form-item>
            <el-form-item label="会籍剩余天">
              <el-input-number v-model="memberStats.membershipDaysRemaining" :min="0" />
            </el-form-item>
            <el-form-item label="即将到期">
              <el-switch v-model="memberStats.membershipExpiring" />
            </el-form-item>
            <el-button type="primary" :loading="loading" @click="runMemberAnalysis">分析</el-button>
          </el-form>
        </div>
      </el-tab-pane>
      <el-tab-pane label="运营分析" name="ops">
        <div class="simple-card">
          <el-form inline>
            <el-form-item label="从"><el-input v-model="from" /></el-form-item>
            <el-form-item label="到"><el-input v-model="to" /></el-form-item>
            <el-button type="primary" :loading="loading" @click="runOpsAnalysis">分析</el-button>
          </el-form>
        </div>
      </el-tab-pane>
      <el-tab-pane label="动作推荐" name="rec">
        <div class="simple-card">
          <el-form label-width="80px">
            <el-form-item label="目标">
              <el-input v-model="goal" placeholder="增肌 / 减脂" />
            </el-form-item>
            <el-button type="primary" :loading="loading" @click="runRecommend">推荐</el-button>
          </el-form>
        </div>
      </el-tab-pane>
      <el-tab-pane label="教练助手" name="coach">
        <div class="simple-card">
          <el-form label-width="80px">
            <el-form-item label="会员ID">
              <el-input v-model="coachMemberId" placeholder="可选" />
            </el-form-item>
            <el-form-item label="问题">
              <el-input v-model="coachQuestion" type="textarea" rows="3" placeholder="向教练助手提问" />
            </el-form-item>
            <el-button type="primary" :loading="loading" @click="runCoach">提问</el-button>
          </el-form>
        </div>
      </el-tab-pane>
      <el-tab-pane label="AI 记忆" name="memory">
        <BizCrudPanel
          title="记忆列表"
          :create-fields="memoryFields"
          :loader="loadMemories"
          :creator="writeMemory"
          :row-actions="memoryActions"
        />
      </el-tab-pane>
      <el-tab-pane label="MCP 工具" name="mcp">
        <BizCrudPanel title="可用工具" :loader="loadMcpTools" />
        <div class="simple-card">
          <el-form label-width="80px">
            <el-form-item label="工具名"><el-input v-model="mcpName" /></el-form-item>
            <el-form-item label="输入"><el-input v-model="mcpInput" type="textarea" rows="2" /></el-form-item>
            <el-button type="primary" :loading="loading" @click="invokeMcp">调用</el-button>
          </el-form>
          <JsonBlock v-if="mcpResult" :data="mcpResult" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <JsonBlock v-if="result && tab !== 'memory' && tab !== 'mcp'" :data="result" />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import BizCrudPanel, { type FieldDef, type RowAction } from '@/components/business/BizCrudPanel.vue'
import JsonBlock from '@/components/business/JsonBlock.vue'
import { aiExtendedApi } from '@/api/aiExtended'
import { mcpApi } from '@/api/mcp'

const tab = ref('member')
const loading = ref(false)
const result = ref<unknown>(null)
const memberStats = reactive({
  daysSinceLastWorkout: 14,
  workoutsLast30Days: 2,
  workoutsPrevious30Days: 8,
  membershipDaysRemaining: 7,
  membershipExpiring: true
})
const from = ref('2026-09-01')
const to = ref('2026-09-17')
const goal = ref('增肌')
const coachMemberId = ref('')
const coachQuestion = ref('')
const mcpName = ref('')
const mcpInput = ref('')
const mcpResult = ref<unknown>(null)

const memoryFields: FieldDef[] = [
  { key: 'type', label: '类型', placeholder: 'GOAL' },
  { key: 'key', label: '键' },
  { key: 'value', label: '值' }
]

const memoryActions: RowAction[] = [
  {
    label: '删除',
    handler: row =>
      aiExtendedApi.deleteMemory(String(row.type ?? ''), String(row.key ?? ''))
  }
]

async function run(fn: () => Promise<unknown>) {
  loading.value = true
  try {
    result.value = await fn()
  } finally {
    loading.value = false
  }
}

function runMemberAnalysis() {
  return run(() => aiExtendedApi.memberAnalysis({ ...memberStats }))
}

function runOpsAnalysis() {
  return run(() => aiExtendedApi.operationAnalysis(from.value, to.value))
}

function runRecommend() {
  return run(() => aiExtendedApi.recommend({ goal: goal.value }))
}

function runCoach() {
  return run(() =>
    aiExtendedApi.coachAssistant({
      memberId: coachMemberId.value ? Number(coachMemberId.value) : null,
      request: coachQuestion.value
    })
  )
}

function loadMemories() {
  return aiExtendedApi.listMemories()
}

function writeMemory(p: Record<string, string>) {
  return aiExtendedApi.writeMemory(p)
}

function loadMcpTools() {
  return mcpApi.tools()
}

async function invokeMcp() {
  loading.value = true
  try {
    mcpResult.value = await mcpApi.invoke({ name: mcpName.value, input: mcpInput.value })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.mt {
  margin-top: 12px;
}
</style>

<template>
  <div>
    <h2 class="simple-page-title">教练 AI 工作台</h2>
    <p class="simple-page-desc">围绕会员训练工作，快速制定计划、分析记录和生成沟通内容。</p>

    <el-tabs v-model="tab" type="card">
      <el-tab-pane label="调整训练计划" name="adjust">
        <div class="simple-card coach-card">
          <p class="hint">描述会员近期的训练表现或恢复情况，AI 会给出调整建议。</p>
          <el-form label-width="120px">
            <el-form-item label="会员">
              <el-select v-model="coachMemberId" filterable placeholder="选择你负责的会员" style="width: 100%">
                <el-option v-for="target in targets" :key="target.memberId" :label="`${target.name || '会员'}（${target.memberNumber || target.memberId}）`" :value="String(target.memberId)" />
              </el-select>
            </el-form-item>
            <el-form-item label="调整原因"><el-input v-model="adjustRequest" type="textarea" rows="3" placeholder="例如：卧推两周没有进步，训练后恢复较慢" /></el-form-item>
            <el-button type="primary" :loading="loading" @click="runAdjust">生成调整建议</el-button>
          </el-form>
        </div>
      </el-tab-pane>
      <el-tab-pane label="分析训练记录" name="member">
        <div class="simple-card">
          <el-form label-width="140px">
            <el-form-item label="会员">
              <el-select v-model="coachMemberId" filterable placeholder="选择你负责的会员" style="width: 100%">
                <el-option v-for="target in targets" :key="target.memberId" :label="`${target.name || '会员'}（${target.memberNumber || target.memberId}）`" :value="String(target.memberId)" />
              </el-select>
            </el-form-item>
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
      <el-tab-pane label="生成会员沟通内容" name="coach">
        <div class="simple-card">
          <el-form label-width="80px">
            <el-form-item label="会员">
              <el-select v-model="coachMemberId" filterable placeholder="选择你负责的会员" style="width: 100%">
                <el-option v-for="target in targets" :key="target.memberId" :label="`${target.name || '会员'}（${target.memberNumber || target.memberId}）`" :value="String(target.memberId)" />
              </el-select>
            </el-form-item>
            <el-form-item label="需求">
              <el-input v-model="coachQuestion" type="textarea" rows="3" placeholder="例如：帮我把本周训练计划解释成会员容易理解的版本" />
            </el-form-item>
            <el-button type="primary" :loading="loading" @click="runCoach">提问</el-button>
          </el-form>
        </div>
      </el-tab-pane>
      <el-tab-pane v-if="false" label="AI 记忆" name="memory" />
    </el-tabs>

    <JsonBlock v-if="result && tab !== 'memory' && tab !== 'mcp'" :data="result" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import JsonBlock from '@/components/business/JsonBlock.vue'
import { aiExtendedApi } from '@/api/aiExtended'
import { workoutPlanDraftApi, type WorkoutPlanTarget } from '@/api/workoutPlanDraft'

const tab = ref('adjust')
const loading = ref(false)
const result = ref<unknown>(null)
const memberStats = reactive({
  daysSinceLastWorkout: 14,
  workoutsLast30Days: 2,
  workoutsPrevious30Days: 8,
  membershipDaysRemaining: 7,
  membershipExpiring: true
})
const adjustRequest = ref('')
const goal = ref('增肌')
const coachMemberId = ref('')
const coachQuestion = ref('')
const targets = ref<WorkoutPlanTarget[]>([])

async function run(fn: () => Promise<unknown>) {
  loading.value = true
  try {
    result.value = await fn()
  } finally {
    loading.value = false
  }
}

function runMemberAnalysis() {
  if (!coachMemberId.value) return
  const request = `请分析会员训练活跃度：距上次训练 ${memberStats.daysSinceLastWorkout} 天，近30天训练 ${memberStats.workoutsLast30Days} 次，前30天训练 ${memberStats.workoutsPrevious30Days} 次，会籍剩余 ${memberStats.membershipDaysRemaining} 天，即将到期 ${memberStats.membershipExpiring ? '是' : '否'}。`
  return run(() => aiExtendedApi.coachAssistant({ memberId: Number(coachMemberId.value), request }))
}

function runAdjust() {
  if (!coachMemberId.value || !adjustRequest.value.trim()) return
  return run(() => aiExtendedApi.coachAssistant({ memberId: Number(coachMemberId.value), request: `请调整训练计划：${adjustRequest.value}` }))
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

onMounted(async () => {
  try {
    const response = await workoutPlanDraftApi.getTargets()
    const data = response.data as WorkoutPlanTarget[] | { targets?: WorkoutPlanTarget[] }
    targets.value = Array.isArray(data) ? data : data?.targets || []
    if (targets.value.length === 1) coachMemberId.value = String(targets.value[0].memberId)
  } catch {
    targets.value = []
  }
})

</script>

<style scoped>
.mt {
  margin-top: 12px;
}
.coach-card { max-width: 760px; }
.hint { color: var(--text-muted); margin-top: 0; }
</style>

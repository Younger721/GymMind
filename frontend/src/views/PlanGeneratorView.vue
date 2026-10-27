<template>
  <div class="plan">
    <h2 class="simple-page-title">AI 训练计划</h2>
    <p class="simple-page-desc">填写目标与训练约束，AI 将结合你的历史训练记录生成可保存的草案。</p>

    <div class="simple-card form-area">
      <el-form label-width="120px">
        <el-form-item label="健身目标">
          <el-select v-model="goal" placeholder="选择目标" style="width: 100%">
            <el-option label="增肌" value="增肌" />
            <el-option label="减脂" value="减脂" />
            <el-option label="力量提升" value="力量提升" />
            <el-option label="体能与耐力" value="体能与耐力" />
          </el-select>
        </el-form-item>
        <el-form-item label="每周训练次数">
          <el-input-number v-model="sessionsPerWeek" :min="1" :max="7" />
        </el-form-item>
        <el-form-item label="训练经验">
          <el-select v-model="experience" placeholder="选择经验" style="width: 100%">
            <el-option label="新手（0～6 个月）" value="新手" />
            <el-option label="中级（6 个月～2 年）" value="中级" />
            <el-option label="高级（2 年以上）" value="高级" />
          </el-select>
        </el-form-item>
        <el-form-item label="参考训练记录">
          <el-switch v-model="includeHistory" />
        </el-form-item>
        <el-form-item label="补充说明">
          <el-input
            v-model="extraNotes"
            type="textarea"
            :rows="4"
            placeholder="例如：每次约 45 分钟，健身房有哑铃和杠铃，左肩旧伤需避免过头推举。"
          />
        </el-form-item>
      </el-form>
      <el-button type="primary" :loading="loading" class="gen-btn" @click="generate">
        生成计划草案
      </el-button>
    </div>

    <div v-if="historySummary" class="simple-card history">
      <h3>训练记录参考</h3>
      <p>{{ historySummary }}</p>
    </div>

    <div v-if="draftPlan" class="simple-card result">
      <div class="result-head">
        <h3>{{ draftPlan.name }}</h3>
        <el-button type="success" :loading="saving" @click="saveDraft">保存到训练计划</el-button>
      </div>
      <p class="meta">目标：{{ draftPlan.goal }} · {{ draftPlan.startDate }} 至 {{ draftPlan.endDate }}</p>
      <p v-if="draftPlan.description" class="desc">{{ draftPlan.description }}</p>
      <el-table
        :data="draftPlan.items"
        stripe
        class="plan-table"
        :span-method="planSpanMethod"
      >
        <el-table-column prop="dayOfWeek" label="日期" width="108" align="center">
          <template #default="{ row }">
            <span class="day-label">{{ formatDayOfWeek(row.dayOfWeek) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="exerciseName" label="动作" min-width="160" />
        <el-table-column prop="sets" label="组" width="72" align="center" />
        <el-table-column prop="reps" label="次" width="72" align="center" />
        <el-table-column prop="restSeconds" label="休息(秒)" width="100" align="center" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { workoutPlanDraftApi, type WorkoutPlanDraft } from '@/api/workoutPlanDraft'
import { dayOfWeekRowSpans, formatDayOfWeek } from '@/utils/dayOfWeek'

type TableSpanArgs = {
  rowIndex: number
  columnIndex: number
}

const goal = ref('增肌')
const sessionsPerWeek = ref(3)
const experience = ref('新手')
const includeHistory = ref(true)
const extraNotes = ref('')
const loading = ref(false)
const saving = ref(false)
const draftPlan = ref<WorkoutPlanDraft | null>(null)
const historySummary = ref('')

const planDayRowSpans = computed(() => dayOfWeekRowSpans(draftPlan.value?.items ?? []))

function planSpanMethod({ rowIndex, columnIndex }: TableSpanArgs) {
  if (columnIndex !== 0) {
    return { rowspan: 1, colspan: 1 }
  }
  const rowspan = planDayRowSpans.value[rowIndex] ?? 1
  if (rowspan === 0) {
    return { rowspan: 0, colspan: 0 }
  }
  return { rowspan, colspan: 1 }
}

const generate = async () => {
  if (!goal.value) {
    ElMessage.warning('请选择健身目标')
    return
  }
  loading.value = true
  draftPlan.value = null
  historySummary.value = ''
  try {
    const res = await workoutPlanDraftApi.generate({
      memberId: null,
      request: extraNotes.value.trim(),
      includeHistory: includeHistory.value,
      goal: goal.value,
      sessionsPerWeek: sessionsPerWeek.value,
      experience: experience.value
    })
    const data = res.data as { plan?: WorkoutPlanDraft; history?: { summary?: string; recordCount?: number; included?: boolean } }
    draftPlan.value = (data as WorkoutPlanDraft).items ? (data as WorkoutPlanDraft) : data.plan ?? null
    if (data.history?.summary) {
      historySummary.value = data.history.summary
    } else if (data.history?.included === false) {
      historySummary.value = '未参考训练记录。'
    } else if (data.history?.recordCount === 0) {
      historySummary.value = '近期暂无可参考训练记录，本次仅依据表单信息生成。'
    }
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '生成失败')
  } finally {
    loading.value = false
  }
}

const saveDraft = async () => {
  if (!draftPlan.value) return
  saving.value = true
  try {
    await workoutPlanDraftApi.save(draftPlan.value)
    ElMessage.success('训练计划已保存为草案')
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.form-area {
  margin-bottom: 16px;
}

.gen-btn {
  margin-top: 4px;
}

.history h3,
.result h3 {
  margin: 0 0 12px;
  font-size: 18px;
  font-weight: 600;
}

.result-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.meta,
.desc {
  color: var(--text-secondary, #666);
  font-size: 15px;
  line-height: 1.65;
}

.desc {
  margin-bottom: 16px;
}

.result {
  margin-top: 4px;
}

.plan-table :deep(.el-table__cell) {
  font-size: 15px;
  padding: 12px 0;
}

.plan-table :deep(th.el-table__cell) {
  font-size: 14px;
  font-weight: 600;
  text-transform: none;
  letter-spacing: normal;
}

.day-label {
  font-weight: 600;
  color: var(--text-primary, inherit);
  white-space: nowrap;
}

.history {
  margin-bottom: 16px;
}
</style>

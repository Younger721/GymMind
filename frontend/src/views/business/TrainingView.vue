<template>
  <div>
    <h2 class="simple-page-title">训练与营养</h2>
    <p class="simple-page-desc">动作库、训练计划、训练记录与营养</p>

    <el-tabs v-model="tab">
      <el-tab-pane v-if="canReadExercises" label="动作库" name="exercises">
        <BizCrudPanel
          title="动作"
          :create-fields="exerciseFields"
          :loader="loadExercises"
          :creator="createExercise"
          create-permission="exercise:write"
        />
      </el-tab-pane>
      <el-tab-pane v-if="canReadPlans" label="训练计划" name="plans">
        <BizCrudPanel title="训练计划" :row-actions="planActions" :loader="loadPlans" />
      </el-tab-pane>
      <el-tab-pane v-if="canGenerateAiPlans" label="AI 计划" name="ai-plan">
        <div class="simple-card ai-box">
          <el-form label-width="90px">
            <el-form-item :label="requiresMemberTarget ? '会员ID *' : '会员ID'">
              <el-input v-model="aiMemberId" :placeholder="requiresMemberTarget ? '管理员或教练请填写会员ID' : '会员留空则使用本人'" />
            </el-form-item>
            <el-form-item label="需求">
              <el-input v-model="aiRequest" type="textarea" rows="3" placeholder="例如：增肌，每周3练" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="aiLoading" :disabled="!canRunAiPlan" @click="runAiDraft">生成草稿</el-button>
              <el-button :loading="aiLoading" :disabled="!canRunAiPlan" @click="runAiSave">生成并保存</el-button>
            </el-form-item>
          </el-form>
          <JsonBlock v-if="aiResult" :data="aiResult" />
        </div>
      </el-tab-pane>
      <el-tab-pane v-if="canCreateWorkoutRecords" label="训练记录" name="records">
        <BizCrudPanel title="记录训练" :create-fields="recordFields" :loader="emptyLoader" :creator="createRecord" />
      </el-tab-pane>
      <el-tab-pane v-if="canCreateNutritionPlans" label="营养计划" name="nutrition-plan">
        <BizCrudPanel title="营养计划" :create-fields="planNutritionFields" :loader="emptyLoader" :creator="createNutritionPlan" />
      </el-tab-pane>
      <el-tab-pane v-if="canCreateNutritionRecords" label="营养记录" name="nutrition-record">
        <BizCrudPanel title="饮食记录" :create-fields="recordNutritionFields" :loader="emptyLoader" :creator="createNutritionRecord" />
      </el-tab-pane>
      <el-tab-pane v-if="canCreateNutritionFoods" label="食物库" name="foods">
        <BizCrudPanel title="食物" :create-fields="foodFields" :loader="emptyLoader" :creator="createFood" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import BizCrudPanel, { type FieldDef, type RowAction } from '@/components/business/BizCrudPanel.vue'
import JsonBlock from '@/components/business/JsonBlock.vue'
import { exercisesApi } from '@/api/exercises'
import { workoutPlansApi, workoutRecordsApi } from '@/api/workouts'
import { nutritionApi } from '@/api/nutrition'
import { useAuthStore } from '@/stores/auth'
import { canUseTenantOperation } from '@/utils/operationAccess'

const authStore = useAuthStore()
const roleOperation = (operation: string, allowedRoles: string[]) => computed(() => {
  return canUseTenantOperation(authStore.isPlatformAdmin, authStore.permissions, authStore.roles, operation, allowedRoles)
})
const canReadExercises = roleOperation('exercise:read', ['GYM_ADMIN', 'COACH', 'MEMBER'])
const canReadPlans = roleOperation('workout:read', ['GYM_ADMIN', 'COACH', 'MEMBER'])
const canGenerateAiPlans = roleOperation('ai:plan', ['GYM_ADMIN', 'COACH', 'MEMBER'])
const canCreateWorkoutRecords = roleOperation('workout-record:write', ['MEMBER'])
const canCreateNutritionPlans = roleOperation('nutrition:write', ['GYM_ADMIN', 'COACH', 'MEMBER'])
const canCreateNutritionRecords = roleOperation('nutrition:write', ['MEMBER'])
const canCreateNutritionFoods = roleOperation('nutrition:write', ['GYM_ADMIN'])
const canPublishPlans = roleOperation('workout:write', ['GYM_ADMIN', 'COACH'])
const tab = ref(canReadExercises.value ? 'exercises' : canReadPlans.value ? 'plans' : canGenerateAiPlans.value ? 'ai-plan' : '')
const aiMemberId = ref('')
const aiRequest = ref('')
const aiLoading = ref(false)
const aiResult = ref<unknown>(null)
const requiresMemberTarget = computed(() => authStore.roles.some(role => role === 'GYM_ADMIN' || role === 'COACH'))
const canRunAiPlan = computed(() => canGenerateAiPlans.value && (!requiresMemberTarget.value || !!aiMemberId.value.trim()))

const exerciseFields: FieldDef[] = [
  { key: 'name', label: '名称' },
  { key: 'category', label: '分类', placeholder: 'STRENGTH' },
  { key: 'targetMuscle', label: '目标肌群', placeholder: 'CHEST' },
  { key: 'equipment', label: '器械', placeholder: 'DUMBBELL' }
]

const recordFields: FieldDef[] = [
  { key: 'planId', label: '计划ID', type: 'number' },
  { key: 'workoutDate', label: '日期', placeholder: '2026-09-17' },
  { key: 'duration', label: '时长(分)', type: 'number' },
  { key: 'exerciseId', label: '动作ID', type: 'number' },
  { key: 'notes', label: '备注' }
]

const planNutritionFields: FieldDef[] = [
  { key: 'memberId', label: '会员ID', type: 'number' },
  { key: 'calories', label: '热量', type: 'number' },
  { key: 'protein', label: '蛋白质', type: 'number' },
  { key: 'description', label: '说明' }
]

const recordNutritionFields: FieldDef[] = [
  { key: 'recordDate', label: '日期', placeholder: '2026-09-17' },
  { key: 'calories', label: '热量', type: 'number' },
  { key: 'notes', label: '备注' }
]

const foodFields: FieldDef[] = [
  { key: 'name', label: '名称' },
  { key: 'calories', label: '热量', type: 'number' },
  { key: 'servingSize', label: '份量', placeholder: '100g' }
]

const planActions = computed<RowAction[]>(() => canPublishPlans.value
  ? [{ label: '发布', handler: row => workoutPlansApi.publish(Number(row.id)) }]
  : [])

const PLACEHOLDER = '待补充'

async function loadExercises() {
  return exercisesApi.list()
}

async function createExercise(p: Record<string, string>) {
  return exercisesApi.create({
    name: p.name,
    category: p.category || 'STRENGTH',
    targetMuscle: p.targetMuscle || 'CHEST',
    difficulty: 'BEGINNER',
    equipment: p.equipment || 'BODYWEIGHT',
    description: PLACEHOLDER,
    steps: PLACEHOLDER,
    commonMistakes: PLACEHOLDER,
    safetyNotes: PLACEHOLDER,
    tags: ''
  })
}

async function loadPlans() {
  return workoutPlansApi.list()
}

async function emptyLoader() {
  return { data: [] }
}

async function createRecord(p: Record<string, string>) {
  return workoutRecordsApi.create({
    planId: Number(p.planId),
    workoutDate: p.workoutDate || '2026-09-17',
    duration: Number(p.duration || 45),
    calories: 200,
    notes: p.notes || '',
    sets: [{
      exerciseId: Number(p.exerciseId || 1),
      setNumber: 1,
      weight: 0,
      reps: 10,
      duration: 0,
      completed: true
    }]
  })
}

async function createNutritionPlan(p: Record<string, string>) {
  return nutritionApi.createPlan({
    memberId: Number(p.memberId),
    calories: Number(p.calories),
    protein: Number(p.protein || 0),
    carbohydrate: 0,
    fat: 0,
    description: p.description || PLACEHOLDER
  })
}

async function createNutritionRecord(p: Record<string, string>) {
  return nutritionApi.createRecord({
    recordDate: p.recordDate || '2026-09-17',
    calories: Number(p.calories),
    protein: 0,
    carbohydrate: 0,
    fat: 0,
    notes: p.notes || ''
  })
}

async function createFood(p: Record<string, string>) {
  return nutritionApi.createFood({
    name: p.name,
    calories: Number(p.calories),
    protein: 0,
    carbohydrate: 0,
    fat: 0,
    servingSize: p.servingSize || '100g'
  })
}

async function runAiDraft() {
  if (!canRunAiPlan.value) return
  aiLoading.value = true
  try {
    aiResult.value = await workoutPlansApi.aiDraft({
      memberId: aiMemberId.value ? Number(aiMemberId.value) : null,
      request: aiRequest.value
    })
  } finally {
    aiLoading.value = false
  }
}

async function runAiSave() {
  if (!canRunAiPlan.value) return
  aiLoading.value = true
  try {
    aiResult.value = await workoutPlansApi.aiSave({
      memberId: aiMemberId.value ? Number(aiMemberId.value) : null,
      request: aiRequest.value
    })
  } finally {
    aiLoading.value = false
  }
}
</script>

<style scoped>
.ai-box {
  margin-bottom: 16px;
}
</style>

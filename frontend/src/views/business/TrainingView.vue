<template>
  <div class="training-page">
    <div class="page-heading">
      <div><div class="eyebrow">GYMMIND TRAINING</div><h2>{{ profile.title }}</h2></div>
      <el-button v-if="isMember" type="primary" @click="showRecord = true">开始训练</el-button>
    </div>

    <div v-if="isMember" class="toolbar simple-card">
      <el-radio-group v-model="range" @change="load"><el-radio-button value="7">本周</el-radio-button><el-radio-button value="30">近30天</el-radio-button><el-radio-button value="90">近90天</el-radio-button></el-radio-group>
      <el-button @click="load">刷新概览</el-button>
    </div>
    <div v-if="isMember" class="stats-grid">
      <div v-for="stat in stats" :key="stat.label" class="stat-card"><span>{{ stat.label }}</span><strong>{{ stat.value }}</strong></div>
    </div>

    <section class="simple-card panel">
      <div class="heading"><h3>健身房训练计划库</h3><el-select v-model="category" clearable placeholder="全部分类" class="category-select"><el-option v-for="item in categories" :key="item" :label="item" :value="item" /></el-select></div>
      <div class="plan-grid">
        <article v-for="plan in filteredTemplates" :key="plan.id" class="plan-card" @click="viewPlan(plan.id, 'template')">
          <small>{{ plan.category }}</small><h4>{{ plan.name }}</h4><p>{{ plan.description }}</p>
          <el-button type="primary" link @click.stop="viewPlan(plan.id, 'template')">查看计划详情</el-button>
          <el-button v-if="isMember" type="primary" link @click.stop="choosePlan(plan.id)">加入我的计划</el-button>
          <el-button v-else-if="canManage" type="success" link @click.stop="publishPlan(plan)">发布</el-button>
        </article>
      </div>
      <el-empty v-if="!filteredTemplates.length" description="暂无已发布训练计划" />
    </section>

    <section v-if="isMember" class="simple-card panel">
      <div class="heading"><h3>我的训练计划</h3><el-button @click="loadMemberPlans">刷新</el-button></div>
      <el-table :data="memberPlans" stripe>
        <el-table-column prop="name" label="计划名称" min-width="170" />
        <el-table-column prop="goal" label="训练目标" min-width="140" />
        <el-table-column label="有效期" min-width="190"><template #default="scope">{{ scope.row.startDate }} 至 {{ scope.row.endDate }}</template></el-table-column>
        <el-table-column prop="status" label="状态" width="110" />
        <el-table-column label="操作" width="120"><template #default="scope"><el-button link type="primary" @click="viewPlan(scope.row.id, 'member')">查看详情</el-button></template></el-table-column>
      </el-table>
      <el-empty v-if="!memberPlans.length" description="暂无个人训练计划" />
    </section>

    <section v-if="isMember" class="simple-card panel">
      <div class="heading"><h3>我的训练日历</h3><el-date-picker v-model="calendarDate" type="date" value-format="YYYY-MM-DD" @change="loadCalendar" /></div>
      <el-table :data="calendarRows" stripe><el-table-column prop="scheduledDate" label="日期"/><el-table-column prop="planName" label="训练计划"/><el-table-column label="操作"><template #default="scope"><el-button link type="primary" @click="startScheduled(scope.row)">开始训练</el-button></template></el-table-column></el-table>
      <el-empty v-if="!calendarRows.length" description="暂无排期" />
    </section>

    <section v-if="isMember" class="simple-card panel">
      <div class="heading"><h3>我的训练记录</h3></div>
      <el-table :data="data.records" stripe><el-table-column prop="workoutDate" label="日期"/><el-table-column prop="duration" label="时长（分钟）"/><el-table-column prop="calories" label="消耗热量"/><el-table-column prop="notes" label="备注"/></el-table>
      <el-empty v-if="!data.records.length" description="暂无训练记录" />
    </section>

    <el-button v-if="canManage || isMember" class="create-button" type="primary" @click="openCreate">{{ isMember ? '创建我的计划' : '新建训练计划' }}</el-button>

    <el-dialog v-model="showRecord" title="记录一次训练" width="520px" class="responsive-dialog">
      <el-form label-position="top"><el-form-item label="训练日期"><el-date-picker v-model="form.workoutDate" type="date" value-format="YYYY-MM-DD" /></el-form-item><el-form-item label="训练时长（分钟）"><el-input-number v-model="form.duration" :min="1" /></el-form-item><el-form-item label="动作 ID（历史记录兼容）"><el-input-number v-model="form.exerciseId" :min="1" /></el-form-item><el-form-item label="备注"><el-input v-model="form.notes" type="textarea" /></el-form-item></el-form>
      <template #footer><el-button @click="showRecord = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveRecord">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="showPlan" :title="selectedPlan?.name || '训练计划详情'" width="760px" class="responsive-dialog">
      <div v-if="selectedPlan" class="plan-detail">
        <p class="plan-description">{{ selectedPlan.goal || selectedPlan.description }}</p>
        <p v-if="selectedPlan.startDate" class="plan-period">有效期：{{ selectedPlan.startDate }} 至 {{ selectedPlan.endDate }}</p>
        <div v-for="phase in phases" :key="phase" class="phase-block"><h4>{{ phaseLabels[phase] }}</h4>
          <el-table v-if="itemsForPhase(phase).length" :data="itemsForPhase(phase)" stripe>
            <el-table-column prop="exerciseName" label="动作" min-width="140"/><el-table-column label="次数 / 组数" min-width="125"><template #default="scope">{{ scope.row.reps }} 次 × {{ scope.row.sets }} 组</template></el-table-column><el-table-column label="休息" width="100"><template #default="scope">{{ scope.row.restSeconds }} 秒</template></el-table-column><el-table-column label="动作说明" min-width="180"><template #default="scope">{{ displayNote(scope.row.notes) || '—' }}</template></el-table-column>
          </el-table>
          <el-empty v-else description="暂无动作" :image-size="48" />
        </div>
      </div>
      <template #footer><el-button v-if="isMember && selectedPlanKind === 'template' && selectedPlan" type="primary" @click="choosePlan(selectedPlan.id); showPlan = false">加入我的计划</el-button><el-button @click="showPlan = false">关闭</el-button></template>
    </el-dialog>

    <el-dialog v-model="showCreate" :title="isMember ? '创建我的训练计划' : '新建训练计划'" width="900px" class="responsive-dialog" @open="resetCreateForm">
      <el-form label-position="top" class="create-form">
        <div class="form-grid"><el-form-item label="计划名称"><el-input v-model="createForm.name" placeholder="例如：全身力量训练" /></el-form-item><el-form-item v-if="!isMember" label="分类"><el-select v-model="createForm.category" placeholder="选择分类"><el-option v-for="item in categories" :key="item" :label="item" :value="item" /></el-select></el-form-item><el-form-item label="训练目标"><el-input v-model="createForm.goal" placeholder="例如：提升基础力量" /></el-form-item><el-form-item label="计划说明"><el-input v-model="createForm.description" type="textarea" :rows="2" /></el-form-item></div>
        <div v-for="phase in phases" :key="phase" class="editor-phase">
          <div class="phase-heading"><h3>{{ phaseLabels[phase] }}</h3><el-button type="primary" link @click="addCreateItem(phase)">添加动作</el-button></div>
          <div v-for="(item, index) in createForm.phaseItems[phase]" :key="item.rowId" class="create-item">
            <div class="action-field exercise-field"><label>动作名称</label><el-select v-model="item.exerciseId" filterable clearable placeholder="搜索并选择动作"><el-option v-for="exercise in exercises" :key="exercise.id" :label="exercise.name" :value="exercise.id" /></el-select></div>
            <div class="action-field"><label>次数</label><el-input-number v-model="item.reps" :min="1" :step="1" controls-position="right" /></div>
            <div class="action-field"><label>组数</label><el-input-number v-model="item.sets" :min="1" :step="1" controls-position="right" /></div>
            <div class="action-field"><label>休息（秒）</label><el-input-number v-model="item.restSeconds" :min="0" :step="5" controls-position="right" /></div>
            <div class="action-field notes-field"><label>动作说明</label><el-input v-model="item.notes" placeholder="选填" /></div>
            <el-button class="delete-action" text type="danger" :icon="Delete" :aria-label="`删除${phaseLabels[phase]}第${index + 1}个动作`" @click="removeCreateItem(phase, item.rowId)" />
          </div>
          <el-empty v-if="!createForm.phaseItems[phase].length" description="暂无动作" :image-size="48" />
        </div>
      </el-form>
      <template #footer><el-button @click="showCreate = false">取消</el-button><el-button type="primary" :loading="creating" @click="createPlan">{{ isMember ? '保存我的计划' : '保存并发布' }}</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { trainingApi, type TrainingOverview, type TrainingTemplate } from '@/api/training'
import { workoutPlansApi, workoutRecordsApi, type WorkoutPlanView } from '@/api/workouts'
import { exercisesApi, type Exercise } from '@/api/exercises'

type Phase = 'WARMUP' | 'MAIN' | 'STRETCH'
type EditableItem = { rowId: number; exerciseId: number | null; sets: number; reps: number; restSeconds: number; weight: number; notes: string }
type PlanDetail = { id: number; name: string; goal?: string; description: string; startDate?: string; endDate?: string; items?: Array<{ id: number; exerciseId?: number; exerciseName: string; dayOfWeek?: number; phase: string; sortOrder: number; sets: number; reps: number; restSeconds: number; weight: number; notes?: string }> }

const auth = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const creating = ref(false)
const range = ref('30')
const showRecord = ref(false)
const showPlan = ref(false)
const showCreate = ref(false)
const selectedPlan = ref<PlanDetail | null>(null)
const selectedPlanKind = ref<'template' | 'member'>('template')
const category = ref('')
const templates = ref<TrainingTemplate[]>([])
const memberPlans = ref<WorkoutPlanView[]>([])
const exercises = ref<Exercise[]>([])
const calendarDate = ref(new Date().toISOString().slice(0, 10))
const calendarRows = ref<any[]>([])
const nextRowId = ref(1)
const data = ref<TrainingOverview>({ role: '', since: '', until: '', summary: { workoutCount: 0, activeMembers: 0, totalMinutes: 0, averageMinutes: 0, streakDays: 0 }, records: [], members: [] })
const form = reactive({ workoutDate: calendarDate.value, duration: 45, exerciseId: 1, notes: '完成今日训练' })
const phases: Phase[] = ['WARMUP', 'MAIN', 'STRETCH']
const phaseLabels: Record<Phase, string> = { WARMUP: '热身', MAIN: '正式组', STRETCH: '拉伸' }
const categories = ['肩', '背', '胸', '手臂', '臀腿', '推日', '拉日']
const noteTranslations: Record<string, string> = { 'Dynamic warmup': '动态热身', 'Main press': '主要推举动作', 'Incline press': '上斜推举动作', 'Cool down stretch': '放松拉伸' }
const isMember = computed(() => auth.roles.includes('MEMBER') && !auth.isPlatformAdmin)
const canManage = computed(() => auth.roles.includes('COACH') || auth.roles.includes('GYM_ADMIN'))
const profile = computed(() => isMember.value ? { title: '我的训练' } : { title: '训练计划管理' })
const stats = computed(() => [{ label: '训练次数', value: data.value.summary.workoutCount }, { label: '训练人数', value: data.value.summary.activeMembers }, { label: '总时长', value: `${data.value.summary.totalMinutes} 分钟` }, { label: '平均每次', value: `${Math.round(data.value.summary.averageMinutes)} 分钟` }, { label: '连续训练', value: `${data.value.summary.streakDays} 天` }])
const filteredTemplates = computed(() => templates.value.filter(plan => !category.value || plan.category === category.value))
const createForm = reactive({ name: '', category: '', goal: '', description: '', phaseItems: { WARMUP: [] as EditableItem[], MAIN: [] as EditableItem[], STRETCH: [] as EditableItem[] } })

function displayNote(note?: string) { const value = note?.trim() || ''; return noteTranslations[value] || value }
function resetCreateForm() {
  createForm.name = ''; createForm.category = ''; createForm.goal = ''; createForm.description = ''
  createForm.phaseItems.WARMUP = []; createForm.phaseItems.MAIN = [newItem()]; createForm.phaseItems.STRETCH = []
}
function newItem(): EditableItem { return { rowId: nextRowId.value++, exerciseId: null, sets: 3, reps: 10, restSeconds: 90, weight: 0, notes: '' } }
function openCreate() { resetCreateForm(); showCreate.value = true }
function addCreateItem(phase: Phase) { createForm.phaseItems[phase].push(newItem()) }
function removeCreateItem(phase: Phase, rowId: number) { createForm.phaseItems[phase] = createForm.phaseItems[phase].filter(item => item.rowId !== rowId) }
function allCreateItems() { return phases.flatMap(phase => createForm.phaseItems[phase].map((item, index) => ({ ...item, phase, sortOrder: index + 1 }))) }
function itemsForPhase(phase: Phase) { return (selectedPlan.value?.items || []).filter(item => item.phase === phase).sort((a, b) => a.sortOrder - b.sortOrder) }

async function load() {
  loading.value = true
  try {
    exercises.value = (await exercisesApi.list()).data?.filter((item: Exercise) => item.active) || []
    templates.value = (await trainingApi.templates()).data || []
    if (isMember.value) {
      const end = new Date(); const start = new Date(); start.setDate(end.getDate() - Number(range.value) + 1)
      data.value = (await trainingApi.overview(start.toISOString().slice(0, 10), end.toISOString().slice(0, 10))).data
      await Promise.all([loadCalendar(), loadMemberPlans()])
    }
  } catch (error: any) {
    if (error?.response?.data?.code !== 'TENANT_CONTEXT_REQUIRED') ElMessage.error(error?.response?.data?.message || '训练数据加载失败')
  } finally { loading.value = false }
}
async function loadMemberPlans() { memberPlans.value = (await workoutPlansApi.list()).data || [] }
async function choosePlan(id: number) {
  try { const plan = (await trainingApi.select(id)).data; await trainingApi.schedule(plan.id, calendarDate.value); ElMessage.success('已加入今天的训练日历'); await Promise.all([loadCalendar(), loadMemberPlans()]) }
  catch (error: any) { ElMessage.error(error?.response?.data?.message || '加入失败') }
}
async function viewPlan(id: number, kind: 'template' | 'member') {
  try {
    selectedPlanKind.value = kind
    selectedPlan.value = kind === 'member' ? (await workoutPlansApi.find(id)).data : (await trainingApi.template(id)).data
    showPlan.value = true
  } catch (error: any) { ElMessage.error(error?.response?.data?.message || '计划详情加载失败') }
}
async function createPlan() {
  const items = allCreateItems()
  if (!createForm.name.trim() || !createForm.goal.trim() || (!isMember.value && !createForm.category)) { ElMessage.warning('请填写计划名称、目标和分类'); return }
  if (!items.length) { ElMessage.warning('请至少添加一个训练动作'); return }
  for (const [index, item] of items.entries()) {
    const label = `${phaseLabels[item.phase]}第${createForm.phaseItems[item.phase].findIndex(row => row.rowId === item.rowId) + 1}个动作`
    if (!item.exerciseId) { ElMessage.warning(`${label}：请选择动作`); return }
    if (!Number.isInteger(item.reps) || item.reps < 1) { ElMessage.warning(`${label}：次数必须至少为1`); return }
    if (!Number.isInteger(item.sets) || item.sets < 1) { ElMessage.warning(`${label}：组数必须至少为1`); return }
    if (!Number.isInteger(item.restSeconds) || item.restSeconds < 0) { ElMessage.warning(`${label}：休息秒数不能小于0`); return }
    item.sortOrder = index + 1
  }
  creating.value = true
  try {
    if (isMember.value) {
      const startDate = new Date().toISOString().slice(0, 10); const endDate = new Date(); endDate.setMonth(endDate.getMonth() + 1)
      await workoutPlansApi.createMember({ name: createForm.name, goal: createForm.goal, startDate, endDate: endDate.toISOString().slice(0, 10), description: createForm.description || createForm.goal, items: items.map(item => ({ exerciseId: item.exerciseId, dayOfWeek: 1, phase: item.phase, sortOrder: item.sortOrder, sets: item.sets, reps: item.reps, restSeconds: item.restSeconds, weight: item.weight, notes: item.notes })) })
      ElMessage.success('个人训练计划已保存')
      await loadMemberPlans()
    } else {
      const created = (await trainingApi.createTemplate({ name: createForm.name, category: createForm.category, goal: createForm.goal, description: createForm.description || createForm.goal, items: items.map(item => ({ exerciseId: item.exerciseId, phase: item.phase, sortOrder: item.sortOrder, sets: item.sets, reps: item.reps, restSeconds: item.restSeconds, weight: item.weight, notes: item.notes })) })).data
      await trainingApi.publish(created.id); ElMessage.success('训练计划已创建并发布'); await load()
    }
    showCreate.value = false
  } catch (error: any) { ElMessage.error(error?.response?.data?.message || '创建失败') }
  finally { creating.value = false }
}
async function loadCalendar() { const from = new Date(calendarDate.value); from.setDate(1); const to = new Date(from); to.setMonth(to.getMonth() + 1); const rows = (await trainingApi.calendar(from.toISOString().slice(0, 10), to.toISOString().slice(0, 10))).data; calendarRows.value = rows.map((row: any) => ({ ...row, planName: `我的训练计划 #${row.memberPlanId}` })) }
async function publishPlan(plan: TrainingTemplate) { try { await trainingApi.publish(plan.id); ElMessage.success('计划已发布'); await load() } catch (error: any) { ElMessage.error(error?.response?.data?.message || '发布失败') } }
function startScheduled(row: any) { form.workoutDate = row.scheduledDate; showRecord.value = true }
async function saveRecord() {
  saving.value = true
  try { await workoutRecordsApi.create({ workoutDate: form.workoutDate, duration: form.duration, calories: 200, notes: form.notes, sets: [{ exerciseId: form.exerciseId, setNumber: 1, weight: 0, reps: 10, duration: 0, completed: true }] }); showRecord.value = false; ElMessage.success('训练记录已保存'); await load() }
  catch (error: any) { ElMessage.error(error?.response?.data?.message || '保存失败') }
  finally { saving.value = false }
}
onMounted(() => { resetCreateForm(); load() })
</script>

<style scoped>
.training-page{max-width:1100px;margin:0 auto}.page-heading,.toolbar,.heading,.phase-heading{display:flex;align-items:center;justify-content:space-between;gap:14px}.toolbar{margin:18px 0;padding:14px}.stats-grid{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:12px;margin-bottom:16px}.stat-card,.panel{padding:18px}.stat-card{border:1px solid var(--border);border-radius:8px;background:#fff}.stat-card span{display:block;color:var(--text-muted);font-size:12px}.stat-card strong{display:block;color:var(--primary);font-size:24px;margin-top:8px}.panel{margin-bottom:16px}.heading{margin-bottom:14px}.heading h3,.phase-heading h3{margin:0}.category-select{width:170px}.plan-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px}.plan-card{min-width:0;border:1px solid var(--border);padding:14px;border-radius:8px;cursor:pointer}.plan-card h4{margin:8px 0}.plan-card p{color:var(--text-muted);min-height:40px}.create-button{margin:0 0 18px}.plan-description{white-space:pre-wrap}.plan-period{color:var(--text-muted)}.phase-block{margin-top:22px}.phase-block h4{margin:0 0 10px}.create-form{max-height:calc(100vh - 240px);overflow-y:auto;padding-right:8px}.form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:0 16px}.editor-phase{border-top:1px solid var(--border);padding:18px 0 8px}.create-item{display:grid;grid-template-columns:minmax(180px,2fr) repeat(3,minmax(95px,.8fr)) minmax(160px,1.6fr) 36px;gap:12px;align-items:end;padding:12px 0;border-bottom:1px solid var(--border)}.action-field{min-width:0}.action-field label{display:block;margin-bottom:6px;font-size:12px;color:var(--text-muted)}.action-field :deep(.el-select),.action-field :deep(.el-input),.action-field :deep(.el-input-number){width:100%}.delete-action{width:36px;height:32px}.phase-block :deep(.el-empty){padding:8px 0}.responsive-dialog :deep(.el-dialog__body){max-height:calc(100vh - 180px);overflow-y:auto}@media(max-width:900px){.stats-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.plan-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.create-item{grid-template-columns:repeat(2,minmax(0,1fr))}.exercise-field,.notes-field{grid-column:span 2}.delete-action{justify-self:end}}@media(max-width:600px){.page-heading,.toolbar,.heading{align-items:flex-start;flex-wrap:wrap}.stats-grid,.plan-grid,.form-grid{grid-template-columns:1fr}.category-select{width:100%}.create-item{grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}.action-field{min-width:0}.responsive-dialog{width:calc(100vw - 24px)!important}.create-form{max-height:calc(100vh - 210px)}}
</style>

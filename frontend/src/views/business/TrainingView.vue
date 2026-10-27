<template>
  <div class="training-page">
    <div class="page-heading">
      <div><div class="eyebrow">GYMMIND TRAINING</div><h2>{{ profile.title }}</h2></div>
    </div>

    <div v-if="isMember" class="toolbar simple-card">
      <el-radio-group v-model="range" @change="load"><el-radio-button value="7">本周</el-radio-button><el-radio-button value="30">近30天</el-radio-button><el-radio-button value="90">近90天</el-radio-button></el-radio-group>
      <el-button @click="load">刷新</el-button>
    </div>

    <section class="simple-card panel">
      <div class="heading"><h3>教练训练计划</h3><el-select v-model="category" clearable placeholder="全部分类" class="category-select"><el-option v-for="item in planCategories" :key="item" :label="item" :value="item" /></el-select></div>
      <div class="plan-grid">
        <article v-for="plan in filteredTemplates" :key="plan.id" class="plan-card" @click="viewPlan(plan.id, 'template')">
          <small>{{ plan.category }}</small><h4>{{ plan.name }}</h4><p>{{ plan.description }}</p>
          <el-button type="primary" link @click.stop="viewPlan(plan.id, 'template')">查看计划详情</el-button>
          <el-button v-if="isMember" type="primary" link @click.stop="choosePlan(plan.id)">保存到我的计划</el-button>
        </article>
      </div>
      <el-empty v-if="!filteredTemplates.length" description="暂无已发布训练计划" />
    </section>

    <section v-if="isMember" class="simple-card panel calendar-panel">
      <div class="heading">
        <div>
          <h3>我的训练日历</h3>
          <p class="calendar-hint">按日查看已安排的训练计划，点击「开始训练」查看当日动作与组数</p>
        </div>
        <div class="heading-actions">
          <el-button @click="showPlanManage = true">管理我的计划</el-button>
          <el-button @click="load">刷新</el-button>
        </div>
      </div>
      <div v-if="dayGroups.length" class="day-groups">
        <article v-for="group in dayGroups" :key="group.date" class="day-group">
          <header class="day-header">
            <div class="day-title">
              <strong>{{ group.date }}</strong>
              <span class="weekday">{{ weekdayLabel(group.date) }}</span>
            </div>
            <span class="day-count">{{ dayItemCount(group) }} 项</span>
          </header>
          <ul class="day-items">
            <li
              v-for="(item, index) in group.scheduled"
              :key="`s-${item.id ?? index}-${item.workoutPlanId ?? item.planId}`"
              class="day-item"
            >
              <div class="item-body">
                <span class="item-title">{{ displayScheduleName(item) }}</span>
                <span v-if="displayScheduleGoal(item)" class="item-meta">{{ displayScheduleGoal(item) }}</span>
              </div>
              <div class="item-actions">
                <el-button link type="primary" @click="startScheduled(item)">开始训练</el-button>
                <el-button link type="danger" @click="removeSchedule(item)">删除</el-button>
              </div>
            </li>
          </ul>
        </article>
      </div>
      <el-empty v-else description="所选时间段内暂无训练计划排期" />
    </section>

    <el-dialog v-model="showPlanManage" title="管理我的训练计划" width="min(860px, calc(100vw - 24px))" class="responsive-dialog">
      <div class="plan-manage-toolbar">
        <el-date-picker v-model="calendarDate" type="date" value-format="YYYY-MM-DD" placeholder="安排到该日期" />
        <router-link to="/training/ai-plan"><el-button type="primary" plain>AI 生成计划</el-button></router-link>
        <el-button @click="loadMemberPlans">刷新列表</el-button>
      </div>
      <el-table :data="memberPlans" stripe max-height="360">
        <el-table-column prop="name" label="计划名称" min-width="150" />
        <el-table-column prop="goal" label="训练目标" min-width="120" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="viewPlan(scope.row.id, 'member')">详情</el-button>
            <el-button v-if="scope.row.status === 'DRAFT'" link type="primary" @click="openEdit(scope.row.id)">编辑</el-button>
            <el-button link type="primary" @click="schedulePlan(scope.row.id)">安排到所选日期</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!memberPlans.length" description="暂无个人训练计划" />
      <template #footer><el-button @click="showPlanManage = false">关闭</el-button></template>
    </el-dialog>

    <el-button v-if="isCoach || isMember" class="create-button" type="primary" @click="openCreate">{{ isMember ? '创建我的计划' : '新建教练训练计划' }}</el-button>

    <el-dialog v-model="showPlan" :title="planDialogTitle" width="760px" class="responsive-dialog" @closed="clearSessionContext">
      <div v-if="selectedPlan" class="plan-detail">
        <p class="plan-description">{{ selectedPlan.goal || selectedPlan.description }}</p>
        <p v-if="sessionScheduleDate" class="plan-period">
          本次训练日：{{ sessionScheduleDate }}（{{ formatDayOfWeek(sessionDayFilter || dateToDayOfWeek(sessionScheduleDate)) }}）
        </p>
        <p v-else-if="selectedPlan.startDate" class="plan-period">有效期：{{ selectedPlan.startDate }} 至 {{ selectedPlan.endDate }}</p>
        <div v-for="phase in phases" :key="phase" class="phase-block"><h4>{{ phaseLabels[phase] }}</h4>
          <el-table v-if="itemsForPhase(phase).length" :data="itemsForPhase(phase)" stripe>
            <el-table-column prop="exerciseName" label="动作" min-width="140"/><el-table-column label="次数 / 组数" min-width="125"><template #default="scope">{{ scope.row.reps }} 次 × {{ scope.row.sets }} 组</template></el-table-column><el-table-column label="休息" width="100"><template #default="scope">{{ scope.row.restSeconds }} 秒</template></el-table-column><el-table-column label="动作说明" min-width="180"><template #default="scope">{{ displayNote(scope.row.notes) || '—' }}</template></el-table-column>
          </el-table>
          <el-empty v-else description="暂无动作" :image-size="48" />
        </div>
      </div>
      <template #footer><el-button v-if="isMember && selectedPlanKind === 'template' && selectedPlan" type="primary" @click="choosePlan(selectedPlan.id); showPlan = false">保存到我的计划</el-button><el-button @click="showPlan = false">关闭</el-button></template>
    </el-dialog>

    <el-dialog v-model="showCreate" :title="editingPlanId ? '编辑我的训练计划' : (isMember ? '创建我的训练计划' : '新建教练训练计划')" width="min(900px, calc(100vw - 24px))" class="responsive-dialog">
      <el-form label-position="top" class="create-form">
        <div class="form-grid"><el-form-item label="计划名称"><el-input v-model="createForm.name" placeholder="例如：全身力量训练" /></el-form-item><el-form-item v-if="!isMember" label="计划分类"><el-input v-model="createForm.category" placeholder="例如：臀腿、减脂" /></el-form-item><el-form-item label="训练目标"><el-input v-model="createForm.goal" placeholder="例如：提升基础力量" /></el-form-item><el-form-item label="开始日期"><el-date-picker v-model="createForm.startDate" type="date" value-format="YYYY-MM-DD" /></el-form-item><el-form-item label="结束日期"><el-date-picker v-model="createForm.endDate" type="date" value-format="YYYY-MM-DD" /></el-form-item><el-form-item label="计划说明"><el-input v-model="createForm.description" type="textarea" :rows="2" /></el-form-item></div>
        <div v-for="phase in phases" :key="phase" class="editor-phase">
          <div class="phase-heading"><h3>{{ phaseLabels[phase] }}</h3><el-button type="primary" link @click="addCreateItem(phase)">添加动作</el-button></div>
          <div v-for="(item, index) in createForm.phaseItems[phase]" :key="item.rowId" class="create-item">
            <div class="action-field exercise-field"><label>动作名称</label><el-input v-model="item.exerciseName" placeholder="例如：卧推、开合跳、拉伸" /></div>
            <div class="action-field"><label>每组次数</label><el-input-number v-model="item.reps" :min="1" :step="1" controls-position="right" /></div>
            <div class="action-field"><label>组数</label><el-input-number v-model="item.sets" :min="1" :step="1" controls-position="right" /></div>
            <div class="action-field"><label>休息（秒）</label><el-input-number v-model="item.restSeconds" :min="0" :step="5" controls-position="right" /></div>
            <div class="action-field notes-field"><label>动作说明</label><el-input v-model="item.notes" placeholder="选填" /></div>
            <el-button class="delete-action" text type="danger" :icon="Delete" :aria-label="`删除${phaseLabels[phase]}第${index + 1}个动作`" @click="removeCreateItem(phase, item.rowId)" />
          </div>
          <el-empty v-if="!createForm.phaseItems[phase].length" description="暂无动作" :image-size="48" />
        </div>
      </el-form>
      <template #footer><el-button @click="showCreate = false">取消</el-button><el-button type="primary" :loading="creating" @click="savePlan">{{ editingPlanId ? '保存修改' : (isMember ? '保存我的计划' : '保存并发布') }}</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { trainingApi, type TrainingSchedule, type TrainingTemplate } from '@/api/training'
import { workoutPlansApi, type WorkoutPlanView } from '@/api/workouts'
import { flattenPlanItems, planToEditor, validatePlanEditor, PHASES } from '@/utils/workoutPlanEditor.mjs'
import { formatDayOfWeek } from '@/utils/dayOfWeek'

type Phase = 'WARMUP' | 'MAIN' | 'STRETCH'
type EditableItem = { rowId: number | string; exerciseId?: number | null; exerciseName: string; dayOfWeek: number; sets: number; reps: number; restSeconds: number; weight: number; notes: string }
type PlanDetail = { id: number; name: string; category?: string; goal?: string; description: string; startDate?: string; endDate?: string; items?: Array<{ id: number; exerciseId?: number; exerciseName: string; dayOfWeek?: number; phase: string; sortOrder: number; sets: number; reps: number; restSeconds: number; weight: number; notes?: string }> }

const auth = useAuthStore()
const loading = ref(false)
const creating = ref(false)
const range = ref('30')
const showPlan = ref(false)
const showCreate = ref(false)
const selectedPlan = ref<PlanDetail | null>(null)
const selectedPlanKind = ref<'template' | 'member'>('template')
const editingPlanId = ref<number | null>(null)
const category = ref('')
const templates = ref<TrainingTemplate[]>([])
const memberPlans = ref<WorkoutPlanView[]>([])
const calendarDate = ref(new Date().toISOString().slice(0, 10))
const calendarRows = ref<any[]>([])
const showPlanManage = ref(false)

type DayGroup = { date: string; scheduled: any[] }
const nextRowId = ref(1)
const sessionScheduleDate = ref<string | null>(null)
const sessionDayFilter = ref<number | null>(null)
const phases = [...PHASES] as Phase[]
const phaseLabels: Record<Phase, string> = { WARMUP: '热身', MAIN: '正式组', STRETCH: '拉伸' }
const planCategories = ['肩', '背', '胸', '手臂', '臀腿', '推日', '拉日']
const noteTranslations: Record<string, string> = { 'Dynamic warmup': '动态热身', 'Main press': '主要推举动作', 'Incline press': '上斜推举动作', 'Cool down stretch': '放松拉伸' }
const isMember = computed(() => auth.roles.includes('MEMBER') && !auth.isPlatformAdmin)
const isCoach = computed(() => auth.roles.includes('COACH') && !auth.isPlatformAdmin)
const profile = computed(() => isMember.value ? { title: '我的训练' } : { title: '训练计划管理' })
const filteredTemplates = computed(() => templates.value.filter(plan => !category.value || plan.category === category.value))

const planDialogTitle = computed(() => {
  if (!selectedPlan.value) return '训练计划详情'
  if (sessionScheduleDate.value) {
    return `${selectedPlan.value.name} · ${sessionScheduleDate.value}`
  }
  return selectedPlan.value.name
})

const dayGroups = computed((): DayGroup[] => {
  const map = new Map<string, DayGroup>()
  for (const row of calendarRows.value) {
    const date = row.scheduledDate
    if (!date) continue
    let group = map.get(date)
    if (!group) {
      group = { date, scheduled: [] }
      map.set(date, group)
    }
    group.scheduled.push(row)
  }
  return [...map.values()].sort((a, b) => b.date.localeCompare(a.date))
})

function weekdayLabel(dateStr: string) {
  const labels = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const day = new Date(`${dateStr}T12:00:00`).getDay()
  return labels[day] ?? ''
}

function dayItemCount(group: DayGroup) {
  return group.scheduled.length
}

function schedulePlanId(row: { workoutPlanId?: number; planId?: number }) {
  return row.workoutPlanId ?? row.planId ?? null
}

function displayScheduleName(row: { planName?: string; workoutPlanId?: number; planId?: number; memberPlanId?: number }) {
  if (row.planName?.trim()) {
    return row.planName.trim()
  }
  const planId = schedulePlanId(row)
  const fromList = planId ? memberPlans.value.find(p => p.id === planId)?.name : undefined
  if (fromList?.trim()) {
    return fromList.trim()
  }
  return planId ? `训练计划 #${planId}` : '训练计划'
}

function displayScheduleGoal(row: { goal?: string; workoutPlanId?: number; planId?: number }) {
  if (row.goal?.trim()) {
    return row.goal.trim()
  }
  const planId = schedulePlanId(row)
  return memberPlans.value.find(p => p.id === planId)?.goal?.trim() || ''
}

/** 排期日期转为计划 dayOfWeek（1=周一 … 7=周日） */
function dateToDayOfWeek(dateStr: string) {
  const day = new Date(`${dateStr}T12:00:00`).getDay()
  return day === 0 ? 7 : day
}

function clearSessionContext() {
  sessionScheduleDate.value = null
  sessionDayFilter.value = null
}
const createForm = reactive({ id: null as number | null, name: '', category: '', goal: '', startDate: '', endDate: '', description: '', phaseItems: { WARMUP: [] as EditableItem[], MAIN: [] as EditableItem[], STRETCH: [] as EditableItem[] } })

function displayNote(note?: string) { const value = note?.trim() || ''; return noteTranslations[value] || value }
function today() { return new Date().toISOString().slice(0, 10) }
function addMonths(date: string, months: number) { const result = new Date(date); result.setMonth(result.getMonth() + months); return result.toISOString().slice(0, 10) }
function resetCreateForm() {
  editingPlanId.value = null
  createForm.id = null
  createForm.name = ''; createForm.category = ''; createForm.goal = ''; createForm.description = ''
  createForm.startDate = today(); createForm.endDate = addMonths(createForm.startDate, 1)
  createForm.phaseItems.WARMUP = []; createForm.phaseItems.MAIN = [newItem()]; createForm.phaseItems.STRETCH = []
}
function newItem(): EditableItem { return { rowId: nextRowId.value++, exerciseId: null, exerciseName: '', dayOfWeek: 1, sets: 3, reps: 10, restSeconds: 90, weight: 0, notes: '' } }
function applyEditorPlan(plan: WorkoutPlanView | TrainingTemplate) { const edited = planToEditor(plan); editingPlanId.value = edited.id; createForm.id = edited.id; createForm.name = edited.name; createForm.category = edited.category; createForm.goal = edited.goal; createForm.startDate = edited.startDate || today(); createForm.endDate = edited.endDate || addMonths(createForm.startDate, 1); createForm.description = edited.description; createForm.phaseItems = edited.phaseItems as typeof createForm.phaseItems }
function openCreate() { resetCreateForm(); showCreate.value = true }
async function openEdit(id: number) { try { const plan = (await workoutPlansApi.find(id)).data; applyEditorPlan(plan); showCreate.value = true } catch (error: any) { ElMessage.error(error?.response?.data?.message || '计划加载失败') } }
function addCreateItem(phase: Phase) { createForm.phaseItems[phase].push(newItem()) }
function removeCreateItem(phase: Phase, rowId: number | string) { createForm.phaseItems[phase] = createForm.phaseItems[phase].filter(item => item.rowId !== rowId) }
function allCreateItems() { return flattenPlanItems(createForm.phaseItems) }
function itemsForPhase(phase: Phase) {
  let items = (selectedPlan.value?.items || []).filter(item => item.phase === phase)
  if (sessionDayFilter.value != null) {
    const dayItems = items.filter(item => (item.dayOfWeek ?? 1) === sessionDayFilter.value)
    if (dayItems.length) {
      items = dayItems
    }
  }
  return items.sort((a, b) => a.sortOrder - b.sortOrder)
}

async function load() {
  loading.value = true
  try {
    templates.value = (await trainingApi.templates()).data || []
    if (isMember.value) {
      await loadMemberPlans()
      await loadCalendar()
    }
  } catch (error: any) {
    if (error?.response?.data?.code !== 'TENANT_CONTEXT_REQUIRED') ElMessage.error(error?.response?.data?.message || '训练数据加载失败')
  } finally { loading.value = false }
}
async function loadMemberPlans() { memberPlans.value = (await workoutPlansApi.list()).data || [] }
async function choosePlan(id: number) {
  try { await trainingApi.select(id); ElMessage.success('已保存到我的训练计划'); await loadMemberPlans() }
  catch (error: any) { ElMessage.error(error?.response?.data?.message || '加入失败') }
}
async function schedulePlan(id: number) {
  if (!calendarDate.value) {
    ElMessage.warning('请先选择安排日期')
    return
  }
  try {
    await trainingApi.schedule(id, calendarDate.value)
    ElMessage.success(`已安排到 ${calendarDate.value}`)
    await loadCalendar()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '安排失败')
  }
}
async function viewPlan(id: number, kind: 'template' | 'member', keepSession = false) {
  try {
    if (!keepSession) {
      clearSessionContext()
    }
    selectedPlanKind.value = kind
    selectedPlan.value = kind === 'member' ? (await workoutPlansApi.find(id)).data : (await trainingApi.template(id)).data
    showPlan.value = true
  } catch (error: any) { ElMessage.error(error?.response?.data?.message || '计划详情加载失败') }
}
async function savePlan() {
  const message = validatePlanEditor(createForm, isMember.value)
  if (message) { ElMessage.warning(message); return }
  const items = allCreateItems()
  creating.value = true
  try {
    if (isMember.value) {
      const payload = { name: createForm.name.trim(), goal: createForm.goal.trim(), startDate: createForm.startDate, endDate: createForm.endDate, description: createForm.description.trim() || createForm.goal.trim(), items }
      if (editingPlanId.value) await workoutPlansApi.update(editingPlanId.value, payload)
      else await workoutPlansApi.createMember(payload)
      ElMessage.success(editingPlanId.value ? '个人训练计划已更新' : '个人训练计划已保存')
      await loadMemberPlans()
    } else {
      const created = (await trainingApi.createTemplate({ name: createForm.name, category: createForm.category, goal: createForm.goal, description: createForm.description || createForm.goal, items })).data
      await trainingApi.publish(created.id); ElMessage.success('训练计划已创建并发布'); await load()
    }
    showCreate.value = false
  } catch (error: any) { ElMessage.error(error?.response?.data?.message || '创建失败') }
  finally { creating.value = false }
}
async function loadCalendar() {
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - Number(range.value) + 1)
  const from = start.toISOString().slice(0, 10)
  const to = end.toISOString().slice(0, 10)
  const rows = (await trainingApi.calendar(from, to)).data || []
  const nameById = new Map(memberPlans.value.map(plan => [plan.id, plan.name]))
  const goalById = new Map(memberPlans.value.map(plan => [plan.id, plan.goal]))
  calendarRows.value = rows.map((row: TrainingSchedule) => {
    const planId = schedulePlanId(row)
    return {
      ...row,
      planName: row.planName?.trim() || (planId ? nameById.get(planId) : undefined) || row.planName,
      goal: row.goal?.trim() || (planId ? goalById.get(planId) : undefined) || row.goal,
    }
  })
}
async function removeSchedule(row: TrainingSchedule) {
  if (!row.id) {
    ElMessage.warning('无法删除该排期')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定从日历移除「${displayScheduleName(row)}」（${row.scheduledDate}）？`,
      '删除排期',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
    await trainingApi.deleteSchedule(row.id)
    ElMessage.success('已删除该排期')
    await loadCalendar()
  } catch (error: any) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error?.response?.data?.message || '删除失败')
  }
}

async function startScheduled(row: any) {
  const planId = schedulePlanId(row)
  if (!planId) {
    ElMessage.warning('无法识别该排期对应的训练计划')
    return
  }
  sessionScheduleDate.value = row.scheduledDate
  sessionDayFilter.value = dateToDayOfWeek(row.scheduledDate)
  await viewPlan(planId, 'member', true)
}
onMounted(() => { resetCreateForm(); load() })
</script>

<style scoped>
.training-page{max-width:1100px;margin:0 auto}.page-heading,.toolbar,.heading,.phase-heading{display:flex;align-items:center;justify-content:space-between;gap:14px}.toolbar{margin:18px 0;padding:14px}.stats-grid{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:12px;margin-bottom:16px}.stat-card,.panel{padding:18px}.stat-card{border:1px solid var(--border);border-radius:8px;background:var(--surface)}.stat-card span{display:block;color:var(--text-muted);font-size:12px}.stat-card strong{display:block;color:var(--primary);font-size:24px;margin-top:8px}.panel{margin-bottom:16px}.heading{margin-bottom:14px}.heading-actions{display:flex;gap:8px;align-items:center;flex-shrink:0}.heading h3,.phase-heading h3{margin:0}.calendar-hint{margin:6px 0 0;font-size:13px;color:var(--text-muted)}.day-groups{display:flex;flex-direction:column;gap:12px}.day-group{border:1px solid var(--border);border-radius:10px;overflow:hidden;background:var(--surface)}.day-header{display:flex;align-items:center;justify-content:space-between;padding:12px 16px;border-bottom:1px solid var(--border);background:rgba(255,255,255,.03)}.day-title{display:flex;align-items:baseline;gap:10px}.day-title strong{font-size:15px}.weekday{font-size:13px;color:var(--text-muted)}.day-count{font-size:12px;color:var(--text-muted)}.day-items{list-style:none;margin:0;padding:8px 0}.day-item{display:flex;align-items:center;gap:12px;padding:12px 16px;border-bottom:1px solid var(--border)}.day-item:last-child{border-bottom:none}.item-body{flex:1;min-width:0;display:flex;flex-direction:column;gap:4px}.item-title{font-size:15px;font-weight:500;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.item-meta{font-size:13px;color:var(--text-muted)}.item-actions{display:flex;align-items:center;gap:4px;flex-shrink:0}.plan-manage-toolbar{display:flex;flex-wrap:wrap;gap:10px;align-items:center;margin-bottom:14px}.category-select{width:170px}.plan-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px}.plan-card{min-width:0;border:1px solid var(--border);padding:14px;border-radius:8px;cursor:pointer}.plan-card h4{margin:8px 0}.plan-card p{color:var(--text-muted);min-height:40px}.create-button{margin:0 0 18px}.plan-description{white-space:pre-wrap}.plan-period{color:var(--text-muted)}.phase-block{margin-top:22px}.phase-block h4{margin:0 0 10px}.create-form{max-height:calc(100vh - 240px);overflow-y:auto;padding-right:8px}.form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:0 16px}.editor-phase{border-top:1px solid var(--border);padding:18px 0 8px}.create-item{display:grid;grid-template-columns:minmax(180px,2fr) repeat(3,minmax(95px,.8fr)) minmax(160px,1.6fr) 36px;gap:12px;align-items:end;padding:12px 0;border-bottom:1px solid var(--border)}.action-field{min-width:0}.action-field label{display:block;margin-bottom:6px;font-size:12px;color:var(--text-muted)}.action-field :deep(.el-select),.action-field :deep(.el-input),.action-field :deep(.el-input-number){width:100%}.delete-action{width:36px;height:32px}.phase-block :deep(.el-empty){padding:8px 0}.responsive-dialog :deep(.el-dialog__body){max-height:calc(100vh - 180px);overflow-y:auto}@media(max-width:900px){.stats-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.plan-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.create-item{grid-template-columns:repeat(2,minmax(0,1fr))}.exercise-field,.notes-field{grid-column:span 2}.delete-action{justify-self:end}}@media(max-width:600px){.page-heading,.toolbar,.heading{align-items:flex-start;flex-wrap:wrap}.stats-grid,.plan-grid,.form-grid{grid-template-columns:1fr}.category-select{width:100%}.create-item{grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}.action-field{min-width:0}.responsive-dialog{width:calc(100vw - 24px)!important}.create-form{max-height:calc(100vh - 210px)}}
</style>

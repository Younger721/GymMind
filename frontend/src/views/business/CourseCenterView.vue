<template>
  <div class="course-center">
    <div class="page-head">
      <div>
        <h2 class="simple-page-title">课程中心</h2>
        <p class="simple-page-desc">课程发布、查找与报名</p>
      </div>
      <el-button v-if="canWriteCourses" type="primary" @click="openCreate">发布课程</el-button>
    </div>

    <el-tabs v-if="!isPlatformAdmin" v-model="activeTab" @tab-change="loadCourses">
      <el-tab-pane label="全部课程" name="all" />
      <el-tab-pane label="我参与的课" name="mine" />
      <el-tab-pane v-if="isCoach" label="我负责的课" name="managed" />
    </el-tabs>

    <div v-if="isPlatformAdmin" class="tenant-grid">
      <section v-for="group in tenantGroups" :key="group.tenantId" class="tenant-card">
        <div class="tenant-heading"><strong>健身房 #{{ group.tenantId }}</strong><span>{{ group.courses.length }} 节课程</span></div>
        <div class="course-grid"><article v-for="course in group.courses" :key="course.id" class="course-card"><strong>{{ course.title }}</strong><span>{{ course.type }} · {{ formatDate(course.startsAt) }}</span><span>{{ course.location }} · {{ course.reservedCount ?? 0 }}/{{ course.capacity }} 人</span></article></div>
        <el-empty v-if="!group.courses.length" description="暂无课程" :image-size="48" />
      </section>
    </div>

    <div class="simple-card filters">
      <el-form inline @submit.prevent="loadCourses">
        <el-form-item label="日期范围">
          <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" clearable />
        </el-form-item>
        <el-form-item label="类型">
          <el-input v-model="typeFilter" placeholder="如瑜伽、团课" clearable />
        </el-form-item>
        <el-form-item v-if="canManage" label="教练ID">
          <el-input v-model="coachFilter" placeholder="可选" clearable />
        </el-form-item>
        <el-button :loading="loading" @click="loadCourses">查询</el-button>
      </el-form>
    </div>

    <div class="simple-card table-card">
      <el-table v-if="!isPlatformAdmin" v-loading="loading" :data="visibleCourses" stripe>
        <el-table-column prop="title" label="课程" min-width="180" />
        <el-table-column prop="type" label="类型" width="110" />
        <el-table-column prop="startsAt" label="开始时间" min-width="170">
          <template #default="{ row }">{{ formatDate(row.startsAt) }}</template>
        </el-table-column>
        <el-table-column prop="location" label="地点" min-width="120" />
        <el-table-column v-if="isPlatformAdmin" prop="tenantId" label="健身房ID" width="100" />
        <el-table-column label="名额" width="120">
          <template #default="{ row }">{{ row.reservedCount ?? 0 }} / {{ row.capacity }}</template>
        </el-table-column>
        <el-table-column label="状态" width="130">
          <template #default="{ row }">{{ statusLabel(row) }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" min-width="220">
          <template #default="{ row }">
            <el-button v-if="isMember && activeTab === 'all'" type="primary" size="small" :disabled="isBooked(row) || !isAvailable(row)" @click="book(row)">
              {{ isBooked(row) ? '已报名' : isAvailable(row) ? '立即报名' : '名额已满' }}
            </el-button>
            <el-button v-if="isMember && activeTab === 'mine' && bookingFor(row)" type="danger" text size="small" :disabled="!beforeStart(row)" @click="cancelBooking(row)">取消报名</el-button>
            <el-button v-if="canWriteCourse(row) && activeTab === 'managed'" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="canWriteCourse(row) && activeTab === 'managed'" type="danger" text size="small" @click="cancelCourse(row)">取消课程</el-button>
            <el-button v-if="(isCoach || canManage) && activeTab === 'managed'" type="primary" text size="small" @click="openRoster(row)">报名名单</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && visibleCourses.length === 0" description="暂无课程" />
    </div>

    <el-dialog v-model="courseDialogVisible" :title="editingCourse ? '编辑课程' : '发布课程'" width="520px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="课程名称"><el-input v-model="courseForm.title" /></el-form-item>
        <el-form-item label="课程类型"><el-input v-model="courseForm.type" /></el-form-item>
        <el-form-item v-if="canManage" label="教练ID"><el-input v-model.number="courseForm.coachId" type="number" /></el-form-item>
        <el-form-item label="容量"><el-input v-model.number="courseForm.capacity" type="number" min="1" /></el-form-item>
        <el-form-item label="地点"><el-input v-model="courseForm.location" /></el-form-item>
        <el-form-item label="开始时间"><el-date-picker v-model="courseForm.startsAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
        <el-form-item label="结束时间"><el-date-picker v-model="courseForm.endsAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="courseDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCourse">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rosterVisible" title="报名名单" width="560px">
      <el-table :data="roster" stripe>
        <el-table-column prop="memberName" label="会员" min-width="150">
          <template #default="{ row }">{{ row.memberName || `会员 #${row.memberId}` }}</template>
        </el-table-column>
        <el-table-column prop="memberPhone" label="手机" width="140" />
        <el-table-column prop="status" label="状态" />
        <el-table-column prop="checkedInAt" label="签到时间" />
      </el-table>
      <el-empty v-if="roster.length === 0" description="暂无报名" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { coursesApi } from '@/api/courses'
import { bookingsApi } from '@/api/bookings'
import { useAuthStore } from '@/stores/auth'
import { toLocalDateTime, toUtcIso } from '@/utils/courseDateTime.mjs'

type Course = { id: number; tenantId: number; title: string; type: string; coachId: number; startsAt: string; endsAt: string; capacity: number; reservedCount: number; location: string; status: string }
type Booking = { id: number; memberId: number; memberName?: string; memberPhone?: string; courseId: number; status: string; checkedInAt?: string }

const auth = useAuthStore()
const isMember = computed(() => auth.roles.includes('MEMBER'))
const isCoach = computed(() => auth.roles.includes('COACH'))
const canManage = computed(() => auth.roles.includes('GYM_ADMIN') || auth.roles.includes('PLATFORM_ADMIN'))
const isPlatformAdmin = computed(() => auth.isPlatformAdmin)
const canWriteCourses = computed(() => isCoach.value || auth.roles.includes('GYM_ADMIN') || (isPlatformAdmin.value && auth.hasPlatformTenantContext))
const activeTab = ref(isMember.value ? 'all' : isCoach.value ? 'managed' : 'all')
const dateRange = ref<[string, string] | null>(null)
const typeFilter = ref('')
const coachFilter = ref('')
const loading = ref(false)
const courses = ref<Course[]>([])
const bookings = ref<Booking[]>([])
const roster = ref<Booking[]>([])
const courseDialogVisible = ref(false)
const rosterVisible = ref(false)
const saving = ref(false)
const editingCourse = ref<Course | null>(null)
const courseForm = reactive<Record<string, any>>({ title: '', type: '', coachId: undefined, capacity: 10, location: '', startsAt: '', endsAt: '' })

const visibleCourses = computed(() => activeTab.value === 'mine'
  ? courses.value.filter(course => bookings.value.some(booking => booking.courseId === course.id && booking.status !== 'CANCELLED'))
  : courses.value)
const tenantGroups = computed(() => {
  const groups = new Map<number, Course[]>()
  for (const course of courses.value) { const list = groups.get(course.tenantId) || []; list.push(course); groups.set(course.tenantId, list) }
  return [...groups.entries()].map(([tenantId, courses]) => ({ tenantId, courses }))
})

function formatDate(value?: string) { return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '—' }
function beforeStart(course: Course) { return new Date(course.startsAt).getTime() > Date.now() }
function isAvailable(course: Course) { return course.status === 'SCHEDULED' && beforeStart(course) && (course.reservedCount ?? 0) < course.capacity }
function statusLabel(course: Course) { if (course.status === 'CANCELLED') return '已取消'; if (!beforeStart(course)) return '已开始'; if (!isAvailable(course)) return '已满'; return '可报名' }
function bookingFor(course: Course) { return bookings.value.find(booking => booking.courseId === course.id && booking.status !== 'CANCELLED') }
function isBooked(course: Course) { return !!bookingFor(course) }
function canWriteCourse(course: Course) {
  if (isCoach.value) return true
  if (auth.roles.includes('GYM_ADMIN')) return true
  return isPlatformAdmin.value && auth.platformContextTenantId === course.tenantId
}

async function loadCourses() {
  loading.value = true
  try {
    const response = await coursesApi.list({ scope: isPlatformAdmin.value ? 'all' : activeTab.value as 'all' | 'mine' | 'managed', from: dateRange.value?.[0], to: dateRange.value?.[1], type: typeFilter.value || undefined, coachId: coachFilter.value ? Number(coachFilter.value) : undefined, availableOnly: isMember.value && activeTab.value === 'all' })
    courses.value = (response.data ?? []) as Course[]
    if (isMember.value || activeTab.value === 'managed') await loadBookings()
  } catch { ElMessage.error('课程加载失败') } finally { loading.value = false }
}

async function loadBookings() {
  const response = await bookingsApi.list()
  bookings.value = (response.data ?? []) as Booking[]
}

async function book(course: Course) {
  try { await bookingsApi.book({ courseId: course.id }); ElMessage.success('报名成功'); await loadCourses() } catch { /* client interceptor提示 */ }
}

async function cancelBooking(course: Course) {
  const booking = bookingFor(course)
  if (!booking) return
  try { await ElMessageBox.confirm('确定取消报名？', '提示', { type: 'warning' }); await bookingsApi.cancel(booking.id); ElMessage.success('已取消报名'); await loadCourses() } catch { /* cancelled */ }
}

function openCreate() { editingCourse.value = null; Object.assign(courseForm, { title: '', type: '', coachId: undefined, capacity: 10, location: '', startsAt: '', endsAt: '' }); courseDialogVisible.value = true }
function openEdit(course: Course) {
  editingCourse.value = course
  Object.assign(courseForm, { ...course, startsAt: toLocalDateTime(course.startsAt), endsAt: toLocalDateTime(course.endsAt) })
  courseDialogVisible.value = true
}
async function saveCourse() {
  saving.value = true
  try {
    const payload = { title: courseForm.title, type: courseForm.type, coachId: courseForm.coachId ? Number(courseForm.coachId) : undefined, capacity: Number(courseForm.capacity), location: courseForm.location, startsAt: toUtcIso(courseForm.startsAt), endsAt: toUtcIso(courseForm.endsAt) }
    if (editingCourse.value) await coursesApi.update(editingCourse.value.id, payload)
    else await coursesApi.create(payload)
    ElMessage.success(editingCourse.value ? '课程已更新' : '课程已发布'); courseDialogVisible.value = false; await loadCourses()
  } catch { /* client interceptor提示 */ } finally { saving.value = false }
}
async function cancelCourse(course: Course) { try { await ElMessageBox.confirm('确定取消这节课程？', '提示', { type: 'warning' }); await coursesApi.cancel(course.id); ElMessage.success('课程已取消'); await loadCourses() } catch { /* cancelled */ } }
async function openRoster(course: Course) { const response = await bookingsApi.list(course.id); roster.value = (response.data ?? []) as Booking[]; rosterVisible.value = true }

onMounted(loadCourses)
</script>

<style scoped>
.page-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 12px; }
.filters { margin-bottom: 16px; }
.table-card { min-height: 220px; }
.tenant-grid { display:grid; gap:16px; margin:16px 0; }
.tenant-card { padding:18px; border:1px solid var(--border); border-radius:8px; background:var(--surface); }
.tenant-heading { display:flex; justify-content:space-between; align-items:center; margin-bottom:14px; }
.tenant-heading span, .course-card span { color:var(--text-muted); font-size:12px; }
.course-grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(210px,1fr)); gap:12px; }
.course-card { display:grid; gap:7px; padding:14px; border:1px solid var(--border); border-radius:7px; background:var(--bg); }
@media (max-width: 720px) { .page-head { flex-direction: column; } }
</style>

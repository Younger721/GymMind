<template>
  <div class="course-center">
    <div class="page-head">
      <div>
        <h2 class="simple-page-title">课程中心</h2>
        <p class="simple-page-desc">按健身房查看课程、报名和课程名单</p>
      </div>
      <el-button v-if="canCreateCourse" type="primary" @click="openCreate">发布课程</el-button>
    </div>

    <el-tabs v-if="showPersonalTabs" v-model="activeTab" @tab-change="loadCourses">
      <el-tab-pane label="全部课程" name="all" />
      <el-tab-pane label="与我相关" name="related" />
    </el-tabs>

    <div class="simple-card filters">
      <el-form inline @submit.prevent="loadCourses">
        <el-form-item label="日期范围">
          <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" clearable />
        </el-form-item>
        <el-form-item label="分类"><el-input v-model="typeFilter" placeholder="如瑜伽、团课" clearable /></el-form-item>
        <el-form-item v-if="canManageAnyCourse" label="教练姓名"><el-input v-model="coachFilter" placeholder="请输入教练姓名" clearable /></el-form-item>
        <el-button :loading="loading" @click="loadCourses">查询</el-button>
      </el-form>
    </div>

    <div v-loading="loading" class="tenant-grid">
      <section v-for="group in tenantGroups" :key="group.tenantId" class="tenant-card">
        <div class="tenant-heading">
          <div><h3>{{ group.name }}</h3><span>{{ group.courses.length }} 节课程</span></div>
        </div>
        <div v-if="group.courses.length" class="course-grid">
          <article v-for="course in group.courses" :key="course.id" class="course-card" @click="openDetail(course)">
            <div class="course-card-head"><strong>{{ course.title }}</strong><el-tag size="small" :type="statusTagType(course)">{{ displayStatus(course).label }}</el-tag></div>
            <div class="course-meta">{{ course.type }} · {{ course.coachName || `教练 #${course.coachId}` }}</div>
            <div class="course-meta">{{ formatDate(course.startsAt) }} 至 {{ formatDate(course.endsAt) }}</div>
            <div class="course-meta">{{ course.location }} · {{ course.reservedCount ?? 0 }}/{{ course.capacity }} 人</div>
            <div class="course-card-actions" @click.stop>
              <el-button v-if="isMember && activeTab === 'all'" size="small" type="primary" :disabled="!canBook(course)" @click="book(course)">{{ displayStatus(course).code === 'BOOKED' ? '已报名' : canBook(course) ? '立即报名' : displayStatus(course).label }}</el-button>
              <el-button v-if="isMember && activeTab === 'related' && bookingFor(course)" size="small" text type="danger" :disabled="!beforeStart(course)" @click="cancelBooking(course)">取消报名</el-button>
              <el-button v-if="canManageCourse(course)" size="small" @click="openEdit(course)">编辑</el-button>
              <el-button v-if="canManageCourse(course)" size="small" text type="danger" @click="cancelCourse(course)">取消课程</el-button>
              <el-button v-if="canViewRoster" size="small" text type="primary" @click="openDetail(course)">查看详情</el-button>
            </div>
          </article>
        </div>
        <el-empty v-else description="暂无课程" :image-size="52" />
      </section>
      <el-empty v-if="!loading && tenantGroups.length === 0" description="暂无课程" />
    </div>

    <el-drawer v-model="detailVisible" title="课程详情" size="620px" destroy-on-close>
      <template v-if="detailCourse">
        <div v-loading="detailLoading" class="detail-panel">
          <div class="detail-heading"><div><h3>{{ detailCourse.title }}</h3><p>{{ detailCourse.tenantName || `健身房 #${detailCourse.tenantId}` }}</p></div><el-tag :type="statusTagType(detailCourse)">{{ displayStatus(detailCourse).label }}</el-tag></div>
          <dl class="detail-grid">
            <div><dt>分类</dt><dd>{{ detailCourse.type }}</dd></div>
            <div><dt>教练</dt><dd>{{ detailCourse.coachName || `教练 #${detailCourse.coachId}` }}</dd></div>
            <div><dt>开课时间</dt><dd>{{ formatDate(detailCourse.startsAt) }}</dd></div>
            <div><dt>结束时间</dt><dd>{{ formatDate(detailCourse.endsAt) }}</dd></div>
            <div><dt>地点</dt><dd>{{ detailCourse.location }}</dd></div>
            <div><dt>容量</dt><dd>{{ detailCourse.reservedCount ?? 0 }} / {{ detailCourse.capacity }} 人</dd></div>
          </dl>
          <div class="detail-description"><h4>课程说明</h4><p>{{ detailCourse.description || '暂无课程说明' }}</p></div>
          <div class="detail-actions">
            <el-button v-if="isMember && displayStatus(detailCourse).code !== 'BOOKED'" type="primary" :disabled="!canBook(detailCourse)" @click="book(detailCourse)">立即报名</el-button>
            <el-button v-if="isMember && bookingFor(detailCourse)" type="danger" plain :disabled="!beforeStart(detailCourse)" @click="cancelBooking(detailCourse)">取消报名</el-button>
            <el-button v-if="canManageCourse(detailCourse)" @click="openEdit(detailCourse)">编辑课程</el-button>
            <el-button v-if="canManageCourse(detailCourse)" type="danger" plain @click="cancelCourse(detailCourse)">取消课程</el-button>
          </div>
          <div v-if="canViewRoster" class="roster-section">
            <div class="section-heading"><h4>报名名单</h4><span>{{ detailBookings.length }} 人</span></div>
            <div v-for="group in rosterGroups" :key="group.status" class="roster-group">
              <div class="roster-group-heading"><strong>{{ group.label }}</strong><el-tag size="small" type="info">{{ group.count }}</el-tag></div>
              <el-table :data="group.bookings" stripe size="small"><el-table-column label="会员" min-width="150"><template #default="{ row }">{{ row.memberName || `会员 #${row.memberId}` }}</template></el-table-column><el-table-column prop="memberPhone" label="手机" width="140" /><el-table-column prop="checkedInAt" label="签到时间" min-width="150" /></el-table>
            </div>
            <el-empty v-if="detailBookings.length === 0" description="暂无报名" :image-size="48" />
          </div>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="courseDialogVisible" :title="editingCourse ? '编辑课程' : '发布课程'" width="560px" destroy-on-close>
      <el-form label-width="92px">
        <el-form-item v-if="isPlatformAdmin" label="健身房" required><el-select v-model="courseForm.tenantId" filterable placeholder="选择健身房" style="width: 100%" @change="loadCoachOptions"><el-option v-for="tenant in tenants" :key="tenant.id" :label="`${tenant.name}（ID: ${tenant.id}）`" :value="tenant.id" /></el-select></el-form-item>
        <el-form-item label="课程名称" required><el-input v-model="courseForm.title" /></el-form-item>
        <el-form-item label="分类" required><el-input v-model="courseForm.type" placeholder="如瑜伽、力量训练" /></el-form-item>
        <el-form-item label="课程说明"><el-input v-model="courseForm.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item v-if="!isCoach" label="教练" required><el-select v-model="courseForm.coachId" filterable placeholder="选择教练" style="width: 100%"><el-option v-for="coach in coachOptions" :key="coach.id" :label="`${coach.fullName}（${coach.coachNumber}）`" :value="coach.id" /></el-select><span v-if="coachOptions.length === 0" class="field-hint">暂无可选教练，也可以直接填写教练 ID</span></el-form-item>
        <el-form-item v-if="!isCoach && coachOptions.length === 0" label="教练ID" required><el-input v-model.number="courseForm.coachId" type="number" /></el-form-item>
        <el-form-item label="容量" required><el-input-number v-model="courseForm.capacity" :min="1" :max="10000" /></el-form-item>
        <el-form-item label="地点" required><el-input v-model="courseForm.location" /></el-form-item>
        <el-form-item label="开始时间" required><el-date-picker v-model="courseForm.startsAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
        <el-form-item label="结束时间" required><el-date-picker v-model="courseForm.endsAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="courseDialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveCourse">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { coursesApi, type CoursePayload, type CourseView } from '@/api/courses'
import { bookingsApi, type BookingView } from '@/api/bookings'
import { coachesApi, type CoachView } from '@/api/coaches'
import { tenancyApi } from '@/api/tenancy'
import { useAuthStore } from '@/stores/auth'
import { toLocalDateTime, toUtcIso } from '@/utils/courseDateTime.mjs'
import { courseStatus, groupBookingsByStatus, groupCoursesByTenant } from '@/utils/coursePresentation.mjs'

type TenantOption = { id: number; name: string; status?: string }
type CourseTab = 'all' | 'related'
const auth = useAuthStore()
const isMember = computed(() => auth.roles.includes('MEMBER'))
const isCoach = computed(() => auth.roles.includes('COACH'))
const isPlatformAdmin = computed(() => auth.isPlatformAdmin)
const isGymAdmin = computed(() => auth.roles.includes('GYM_ADMIN'))
const canManageAnyCourse = computed(() => isPlatformAdmin.value || isGymAdmin.value)
const canCreateCourse = computed(() => isCoach.value)
const canViewRoster = computed(() => isPlatformAdmin.value || isGymAdmin.value || isCoach.value)
const showPersonalTabs = computed(() => !isPlatformAdmin.value && (isCoach.value || isMember.value))
const activeTab = ref<CourseTab>('all')
const dateRange = ref<[string, string] | null>(null)
const typeFilter = ref('')
const coachFilter = ref('')
const loading = ref(false)
const detailLoading = ref(false)
const saving = ref(false)
const courses = ref<CourseView[]>([])
const bookings = ref<BookingView[]>([])
const detailBookings = ref<BookingView[]>([])
const tenants = ref<TenantOption[]>([])
const coachOptions = ref<CoachView[]>([])
const detailCourse = ref<CourseView | null>(null)
const detailVisible = ref(false)
const courseDialogVisible = ref(false)
const editingCourse = ref<CourseView | null>(null)
const courseForm = reactive({ tenantId: undefined as number | undefined, coachId: undefined as number | undefined, title: '', type: '', description: '', capacity: 10, location: '', startsAt: '', endsAt: '' })
const currentCoachId = computed(() => coachOptions.value.find(coach => coach.userId === auth.userId)?.id)
const tenantGroups = computed(() => {
  const selectedTenantId = isPlatformAdmin.value ? auth.platformContextTenantId : auth.tenantId
  if (selectedTenantId == null) return []
  const visibleCourses = courses.value.filter(course => Number(course.tenantId) === Number(selectedTenantId))
  const grouped = groupCoursesByTenant(visibleCourses)
  if (grouped.length) return grouped
  const tenant = tenants.value.find(item => item.id === selectedTenantId)
  return [{ tenantId: selectedTenantId, name: tenant?.name || '当前健身房', courses: [] }]
})
const rosterGroups = computed(() => groupBookingsByStatus(detailBookings.value))
function formatDate(value?: string) { return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '—' }
function beforeStart(course: CourseView) { return new Date(course.startsAt).getTime() > Date.now() }
function bookingFor(course: CourseView) { return bookings.value.find(booking => booking.courseId === course.id && booking.status !== 'CANCELLED') }
function displayStatus(course: CourseView) { return courseStatus(course, bookings.value, new Date(), isMember.value ? 'MEMBER' : 'STAFF') }
function statusTagType(course: CourseView) { const code = displayStatus(course).code; if (code === 'BOOKABLE') return 'success'; if (code === 'BOOKED') return 'primary'; if (code === 'IN_PROGRESS') return 'warning'; if (code === 'CANCELLED') return 'danger'; return 'info' }
function canBook(course: CourseView) { const status = displayStatus(course); return isMember.value && status.code === 'BOOKABLE' }
function canManageCourse(course: CourseView) { if (isPlatformAdmin.value || isGymAdmin.value) return true; return isCoach.value && currentCoachId.value != null && currentCoachId.value === course.coachId }
async function loadTenants() {
  if (!isPlatformAdmin.value) return
  try { const response = await tenancyApi.listTenants(0, 200); const data = (response as any)?.data; const rows = Array.isArray(data?.content) ? data.content : Array.isArray(data) ? data : []; tenants.value = rows.map((tenant: Record<string, unknown>) => ({ id: Number(tenant.id), name: String(tenant.name || tenant.code || `健身房 #${tenant.id}`), status: String(tenant.status || '') })).filter((tenant: TenantOption) => Number.isSafeInteger(tenant.id) && tenant.id > 0 && tenant.status === 'ACTIVE') } catch { tenants.value = [] }
}
async function loadCoachOptions() {
  const tenantId = courseForm.tenantId ?? auth.tenantId ?? undefined
  if (!tenantId || isMember.value) { coachOptions.value = []; return }
  try { const response = await coachesApi.list(tenantId); coachOptions.value = response.data ?? [] } catch { coachOptions.value = [] }
}
async function loadCourses() {
  loading.value = true
  try {
    const scope = isPlatformAdmin.value ? 'all' : activeTab.value === 'related' ? (isCoach.value ? 'managed' : 'mine') : 'all'
    const response = await coursesApi.list({ scope, from: dateRange.value?.[0], to: dateRange.value?.[1], type: typeFilter.value || undefined })
    const keyword = coachFilter.value.trim().toLocaleLowerCase()
    const list = response.data ?? []
    courses.value = keyword
      ? list.filter(course => (course.coachName || '').toLocaleLowerCase().includes(keyword))
      : list
    if (isMember.value) await loadBookings()
  } catch { ElMessage.error('课程加载失败') } finally { loading.value = false }
}
async function loadBookings() { try { const response = await bookingsApi.list(); bookings.value = response.data ?? [] } catch { bookings.value = [] } }
async function book(course: CourseView) { if (!canBook(course)) return; try { await bookingsApi.book({ courseId: course.id }); ElMessage.success('报名成功'); await loadCourses(); if (detailCourse.value?.id === course.id) await openDetail(course) } catch { /* client interceptor提示 */ } }
async function cancelBooking(course: CourseView) { const booking = bookingFor(course); if (!booking) return; try { await ElMessageBox.confirm('确定取消报名？', '提示', { type: 'warning' }); await bookingsApi.cancel(booking.id); ElMessage.success('已取消报名'); await loadCourses(); if (detailCourse.value?.id === course.id) await openDetail(course) } catch { /* 用户取消 */ } }
function resetCourseForm() { Object.assign(courseForm, { tenantId: isPlatformAdmin.value ? undefined : auth.tenantId ?? undefined, coachId: undefined, title: '', type: '', description: '', capacity: 10, location: '', startsAt: '', endsAt: '' }) }
async function openCreate() { resetCourseForm(); editingCourse.value = null; courseDialogVisible.value = true; await loadCoachOptions() }
async function openEdit(course: CourseView) { editingCourse.value = course; Object.assign(courseForm, { tenantId: course.tenantId, coachId: course.coachId, title: course.title, type: course.type, description: course.description || '', capacity: course.capacity, location: course.location, startsAt: toLocalDateTime(course.startsAt), endsAt: toLocalDateTime(course.endsAt) }); courseDialogVisible.value = true; await loadCoachOptions() }
async function saveCourse() {
  if (!courseForm.title.trim() || !courseForm.type.trim() || !courseForm.location.trim() || !courseForm.startsAt || !courseForm.endsAt) { ElMessage.warning('请完整填写课程基本信息'); return }
  if (!isCoach.value && !courseForm.coachId) { ElMessage.warning('请选择教练'); return }
  let startsAt: string; let endsAt: string
  try { startsAt = toUtcIso(courseForm.startsAt); endsAt = toUtcIso(courseForm.endsAt) } catch { ElMessage.warning('请输入有效的课程时间'); return }
  if (new Date(endsAt).getTime() <= new Date(startsAt).getTime()) { ElMessage.warning('结束时间必须晚于开始时间'); return }
  saving.value = true
  try { const payload: CoursePayload = { ...(isPlatformAdmin.value && courseForm.tenantId ? { tenantId: Number(courseForm.tenantId) } : {}), ...(isCoach.value ? {} : { coachId: Number(courseForm.coachId) }), title: courseForm.title.trim(), type: courseForm.type.trim(), description: courseForm.description.trim() || undefined, capacity: Number(courseForm.capacity), location: courseForm.location.trim(), startsAt, endsAt }; if (!editingCourse.value && isPlatformAdmin.value && !payload.tenantId) { ElMessage.warning('请选择目标健身房'); return } if (editingCourse.value) await coursesApi.update(editingCourse.value.id, payload); else await coursesApi.create(payload); ElMessage.success(editingCourse.value ? '课程已更新' : '课程已发布'); courseDialogVisible.value = false; await loadCourses() } catch { /* client interceptor提示 */ } finally { saving.value = false }
}
async function cancelCourse(course: CourseView) { try { await ElMessageBox.confirm('确定取消这节课程？', '提示', { type: 'warning' }); await coursesApi.cancel(course.id); ElMessage.success('课程已取消'); detailVisible.value = false; await loadCourses() } catch { /* 用户取消 */ } }
async function openDetail(course: CourseView) {
  detailCourse.value = course; detailVisible.value = true; detailLoading.value = true
  try { const requests: Promise<unknown>[] = [coursesApi.find(course.id)]; if (canViewRoster.value) requests.push(bookingsApi.list(course.id)); else if (isMember.value) requests.push(bookingsApi.list()); const results = await Promise.all(requests); const courseResponse = results[0] as { data?: CourseView }; const bookingResponse = results[1] as { data?: BookingView[] } | undefined; detailCourse.value = courseResponse.data ?? course; detailBookings.value = bookingResponse?.data ?? []; if (isMember.value && !canViewRoster.value && bookingResponse?.data) bookings.value = bookingResponse.data } catch { ElMessage.error('课程详情加载失败') } finally { detailLoading.value = false }
}
onMounted(async () => { await Promise.all([loadTenants(), loadCoachOptions()]); await loadCourses() })
</script>

<style scoped>
.page-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 12px; }
.filters { margin: 16px 0; }
.tenant-grid { display: grid; gap: 16px; min-height: 220px; }
.tenant-card { padding: 18px; border: 1px solid var(--border); border-radius: 8px; background: var(--surface); }
.tenant-heading, .course-card-head, .section-heading, .roster-group-heading { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.tenant-heading { margin-bottom: 14px; }
.tenant-heading h3, .detail-heading h3 { margin: 0; font-size: 18px; }
.tenant-heading span, .detail-heading p, .course-meta, .field-hint { color: var(--text-muted); font-size: 12px; }
.course-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 12px; }
.course-card { display: grid; gap: 9px; padding: 15px; border: 1px solid var(--border); border-radius: 7px; background: var(--bg); cursor: pointer; transition: border-color .15s, box-shadow .15s; }
.course-card:hover { border-color: var(--primary); box-shadow: 0 3px 10px rgba(5, 150, 105, .12); }
.course-card-head strong { font-size: 15px; }
.course-card-actions { display: flex; flex-wrap: wrap; gap: 6px; padding-top: 4px; }
.detail-panel { padding-bottom: 24px; }
.detail-heading { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; padding-bottom: 18px; border-bottom: 1px solid var(--border); }
.detail-heading p { margin: 6px 0 0; }
.detail-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; margin: 20px 0; }
.detail-grid div { padding: 11px 12px; background: var(--bg); border-radius: 6px; }
.detail-grid dt { color: var(--text-muted); font-size: 12px; margin-bottom: 4px; }
.detail-grid dd { margin: 0; font-size: 14px; }
.detail-description { margin: 18px 0; }
.detail-description h4, .roster-section h4 { margin: 0 0 8px; }
.detail-description p { margin: 0; color: var(--text-muted); white-space: pre-wrap; line-height: 1.6; }
.detail-actions { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 24px; }
.section-heading { margin-bottom: 10px; }
.section-heading span { color: var(--text-muted); font-size: 12px; }
.roster-group { margin-top: 14px; }
.roster-group-heading { margin-bottom: 6px; }
@media (max-width: 720px) { .page-head { flex-direction: column; } .detail-grid { grid-template-columns: 1fr; } }
</style>

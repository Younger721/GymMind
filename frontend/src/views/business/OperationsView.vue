<template>
  <div>
    <h2 class="simple-page-title">门店运营</h2>
    <p class="simple-page-desc">会员、教练、课程、预约与签到</p>

    <el-tabs v-model="tab">
      <el-tab-pane label="会员" name="members">
        <BizCrudPanel
          title="会员列表"
          searchable
          :create-fields="memberFields"
          :row-actions="memberActions"
          :loader="loadMembers"
          :creator="createMember"
        />
      </el-tab-pane>
      <el-tab-pane label="教练" name="coaches">
        <div class="simple-card lookup-box">
          <el-form inline>
            <el-form-item label="教练 ID">
              <el-input v-model="coachLookupId" style="width: 120px" />
            </el-form-item>
            <el-button :loading="coachLoading" @click="findCoach">查询</el-button>
          </el-form>
          <JsonBlock v-if="coachLookupResult" :data="coachLookupResult" />
        </div>
        <BizCrudPanel title="新建教练" :create-fields="coachFields" :loader="emptyLoader" :creator="createCoach" />
      </el-tab-pane>
      <el-tab-pane label="课程" name="courses">
        <BizCrudPanel
          title="课程排期"
          :create-fields="courseFields"
          :row-actions="courseActions"
          :loader="loadCourses"
          :creator="createCourse"
        />
      </el-tab-pane>
      <el-tab-pane label="预约" name="bookings">
        <BizCrudPanel
          title="预约记录"
          :create-fields="bookingFields"
          :row-actions="bookingActions"
          :loader="loadBookings"
          :creator="createBooking"
        />
      </el-tab-pane>
      <el-tab-pane label="签到" name="checkins">
        <BizCrudPanel title="课程签到" :create-fields="checkInFields" :creator="doCheckIn" :loader="emptyLoader" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import BizCrudPanel, { type FieldDef, type RowAction } from '@/components/business/BizCrudPanel.vue'
import JsonBlock from '@/components/business/JsonBlock.vue'
import { membersApi } from '@/api/members'
import { coachesApi } from '@/api/coaches'
import { coursesApi } from '@/api/courses'
import { bookingsApi, checkInsApi } from '@/api/bookings'

const tab = ref('members')
const coachLookupId = ref('')
const coachLookupResult = ref<unknown>(null)
const coachLoading = ref(false)

const memberFields: FieldDef[] = [
  { key: 'memberNumber', label: '会员号', placeholder: 'M001' },
  { key: 'fullName', label: '姓名' },
  { key: 'phone', label: '手机' }
]

const coachFields: FieldDef[] = [
  { key: 'coachNumber', label: '教练编号', placeholder: 'C001' },
  { key: 'fullName', label: '姓名' },
  { key: 'phone', label: '手机' }
]

const courseFields: FieldDef[] = [
  { key: 'title', label: '课程名' },
  { key: 'type', label: '类型', placeholder: 'YOGA' },
  { key: 'coachId', label: '教练ID', type: 'number' },
  { key: 'capacity', label: '容量', type: 'number' },
  { key: 'location', label: '地点', placeholder: 'A区' },
  { key: 'startsAt', label: '开始时间', placeholder: '2026-09-17T10:00:00Z' },
  { key: 'endsAt', label: '结束时间', placeholder: '2026-09-17T11:00:00Z' }
]

const bookingFields: FieldDef[] = [
  { key: 'memberId', label: '会员ID', type: 'number' },
  { key: 'courseId', label: '课程ID', type: 'number' }
]

const checkInFields: FieldDef[] = [
  { key: 'memberId', label: '会员ID', type: 'number' },
  { key: 'courseId', label: '课程ID', type: 'number' }
]

const memberActions: RowAction[] = [
  { label: '暂停', handler: row => membersApi.suspend(Number(row.id)) }
]

const courseActions: RowAction[] = [
  { label: '取消', handler: row => coursesApi.cancel(Number(row.id)) }
]

const bookingActions: RowAction[] = [
  { label: '确认', handler: row => bookingsApi.confirm(Number(row.id)) },
  { label: '取消', handler: row => bookingsApi.cancel(Number(row.id)) }
]

async function loadMembers(q?: string) {
  return membersApi.list(q)
}

async function createMember(p: Record<string, string>) {
  return membersApi.create({
    memberNumber: p.memberNumber,
    fullName: p.fullName,
    phone: p.phone
  })
}

async function findCoach() {
  if (!coachLookupId.value) return ElMessage.warning('请输入教练 ID')
  coachLoading.value = true
  try {
    coachLookupResult.value = await coachesApi.find(Number(coachLookupId.value))
  } finally {
    coachLoading.value = false
  }
}

async function createCoach(p: Record<string, string>) {
  return coachesApi.create({
    coachNumber: p.coachNumber,
    fullName: p.fullName,
    phone: p.phone
  })
}

async function loadCourses() {
  return coursesApi.list()
}

async function createCourse(p: Record<string, string>) {
  return coursesApi.create({
    title: p.title,
    type: p.type,
    coachId: Number(p.coachId),
    capacity: Number(p.capacity),
    location: p.location,
    startsAt: p.startsAt,
    endsAt: p.endsAt
  })
}

async function loadBookings() {
  return bookingsApi.list()
}

async function createBooking(p: Record<string, string>) {
  return bookingsApi.book({
    memberId: Number(p.memberId),
    courseId: Number(p.courseId)
  })
}

async function doCheckIn(p: Record<string, string>) {
  return checkInsApi.checkIn({
    memberId: Number(p.memberId),
    courseId: Number(p.courseId)
  })
}

async function emptyLoader() {
  return { data: [] }
}
</script>

<style scoped>
.lookup-box {
  margin-bottom: 16px;
}
</style>

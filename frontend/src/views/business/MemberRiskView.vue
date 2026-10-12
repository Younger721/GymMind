<template>
  <div>
    <div class="page-head">
      <div><h2 class="simple-page-title">会员活跃度与流失预警</h2><p class="simple-page-desc">根据到店、预约、课程参与和会籍剩余时间识别需要跟进的会员。</p></div>
      <div class="head-actions">
        <el-select v-model="levelFilter" placeholder="风险等级" clearable style="width: 140px">
          <el-option label="高风险" value="HIGH" />
          <el-option label="中风险" value="MEDIUM" />
          <el-option label="低风险" value="LOW" />
        </el-select>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>
    <el-alert title="风险结果仅作为运营提醒，最终请结合教练与会员实际情况判断。" type="info" show-icon class="notice" />
    <el-table :data="filteredRows" v-loading="loading" stripe>
      <el-table-column prop="memberName" label="会员" min-width="130" />
      <el-table-column label="风险等级" width="110">
        <template #default="{ row }"><el-tag :type="levelType(row.level)">{{ levelText(row.level) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="score" label="风险分" width="90" />
      <el-table-column label="最近到店" width="170"><template #default="{ row }">{{ formatDate(row.lastCheckInAt) }}</template></el-table-column>
      <el-table-column prop="checkInsLast30Days" label="30天到店" width="100" />
      <el-table-column prop="bookingsLast30Days" label="30天预约" width="100" />
      <el-table-column prop="courseParticipationsLast30Days" label="课程参与" width="100" />
      <el-table-column prop="membershipDaysRemaining" label="会籍剩余(天)" width="125" />
      <el-table-column label="风险原因" min-width="240"><template #default="{ row }"><span>{{ row.reasons?.join('；') || '暂无明显风险' }}</span></template></el-table-column>
    </el-table>
    <el-empty v-if="!loading && !filteredRows.length" description="暂无可查看的会员预警" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { memberRisksApi, type MemberRiskView } from '@/api/memberRisks'
const rows = ref<MemberRiskView[]>([])
const loading = ref(false)
const levelFilter = ref<string | undefined>()
const filteredRows = computed(() =>
  levelFilter.value ? rows.value.filter(r => r.level === levelFilter.value) : rows.value
)
async function load(){loading.value=true;try{rows.value=(await memberRisksApi.list()).data||[]}catch{ElMessage.error('加载会员预警失败')}finally{loading.value=false}}
const levelText=(level:string)=>level==='HIGH'?'高风险':level==='MEDIUM'?'中风险':'低风险'
const levelType=(level:string)=>level==='HIGH'?'danger':level==='MEDIUM'?'warning':'success'
const formatDate=(value?:string)=>value?new Date(value).toLocaleString('zh-CN',{hour12:false}):'暂无记录'
onMounted(load)
</script>

<style scoped>
.page-head{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:16px}.page-head h2{margin-bottom:6px}.head-actions{display:flex;gap:8px;align-items:center}.notice{margin-bottom:16px}
</style>

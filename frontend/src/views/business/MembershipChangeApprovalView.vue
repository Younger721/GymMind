<template>
  <div class="membership-changes">
    <div class="page-head">
      <div>
        <h2 class="simple-page-title">会籍停卡 / 转卡审批</h2>
        <p class="simple-page-desc">健身房管理员审批会员停卡与转卡申请</p>
        <p v-if="!canApprove" class="muted">平台管理员不参与审批，仅支持查看</p>
      </div>
      <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 140px" @change="load">
        <el-option label="待审批" value="PENDING" />
        <el-option label="已通过" value="APPROVED" />
        <el-option label="已驳回" value="REJECTED" />
        <el-option label="已取消" value="CANCELLED" />
      </el-select>
    </div>

    <div class="simple-card table-card" v-loading="loading">
      <el-table :data="rows" stripe>
        <el-table-column prop="id" label="单号" width="80" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">{{ row.type === 'FREEZE' ? '停卡' : '转卡' }}</template>
        </el-table-column>
        <el-table-column prop="memberId" label="会员 ID" width="90" />
        <el-table-column label="详情" min-width="200">
          <template #default="{ row }">
            <span v-if="row.type === 'FREEZE'">{{ row.freezeStart }} 至 {{ row.freezeEnd }}</span>
            <span v-else>转至会员 #{{ row.targetMemberId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="说明" min-width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="canApprove && row.status === 'PENDING'">
              <el-button link type="success" @click="decide(row, 'approve')">通过</el-button>
              <el-button link type="danger" @click="decide(row, 'reject')">驳回</el-button>
            </template>
            <span v-else class="muted">{{ canApprove ? '—' : '仅查看' }}</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !rows.length" description="暂无申请" :image-size="56" />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'approve' ? '审批通过' : '驳回申请'" width="420px">
      <el-input v-model="comment" type="textarea" :rows="3" placeholder="审批意见（可选）" />
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitDecision">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import {
  membershipChangeRequestsApi,
  type MembershipChangeRequest,
  type MembershipChangeStatus
} from '@/api/membershipChangeRequests'

const auth = useAuthStore()
/** 仅健身房管理员可审批；平台管理员只读（后端同样拦截，见 ActorAccess.requireGymAdminApproval） */
const canApprove = computed(() => auth.roles.includes('GYM_ADMIN'))

const loading = ref(false)
const saving = ref(false)
const rows = ref<MembershipChangeRequest[]>([])
const statusFilter = ref<MembershipChangeStatus | ''>('')
const dialogVisible = ref(false)
const dialogMode = ref<'approve' | 'reject'>('approve')
const comment = ref('')
const selected = ref<MembershipChangeRequest | null>(null)

function formatDate(value?: string | null) {
  if (!value) return '—'
  return new Date(value).toLocaleString('zh-CN')
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING: '待审批',
    APPROVED: '已通过',
    REJECTED: '已驳回',
    CANCELLED: '已取消'
  }
  return map[status] ?? status
}

function statusType(status: string) {
  if (status === 'PENDING') return 'warning'
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  return 'info'
}

async function load() {
  loading.value = true
  try {
    const res = await membershipChangeRequestsApi.list(statusFilter.value || undefined)
    rows.value = res.data ?? []
  } catch {
    ElMessage.error('加载申请列表失败')
  } finally {
    loading.value = false
  }
}

function decide(row: MembershipChangeRequest, mode: 'approve' | 'reject') {
  selected.value = row
  dialogMode.value = mode
  comment.value = ''
  dialogVisible.value = true
}

async function submitDecision() {
  if (!selected.value) return
  saving.value = true
  try {
    if (dialogMode.value === 'approve') {
      await membershipChangeRequestsApi.approve(selected.value.id, comment.value.trim() || undefined)
      ElMessage.success('已通过')
    } else {
      await membershipChangeRequestsApi.reject(selected.value.id, comment.value.trim() || undefined)
      ElMessage.success('已驳回')
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}
.muted {
  color: var(--el-text-color-secondary);
}
</style>

<template>
  <div>
    <template v-if="authStore.isPlatformAdmin">
      <h2 class="simple-page-title">租户设置</h2>
      <p class="simple-page-desc">管理租户生命周期。禁用租户会停止该租户功能并停用所有关联账号。</p>

      <div class="summary-grid">
        <div class="summary-card"><span>全部租户</span><strong>{{ tenants.length }}</strong></div>
        <div class="summary-card"><span>正常运行</span><strong>{{ activeCount }}</strong></div>
        <div class="summary-card"><span>已禁用</span><strong>{{ disabledCount }}</strong></div>
      </div>

      <div class="simple-card tenant-panel">
        <div class="panel-head">
          <div>
            <h3 class="panel-title">租户列表</h3>
            <p class="panel-desc">删除操作会以禁用方式保留租户及其历史数据。</p>
          </div>
          <div class="panel-actions">
            <el-button :loading="loading" @click="loadTenants">刷新</el-button>
            <el-button type="primary" @click="openCreate">新建租户</el-button>
          </div>
        </div>

        <el-table v-loading="loading" :data="tenants" stripe>
          <el-table-column prop="name" label="租户名称" min-width="180" show-overflow-tooltip />
          <el-table-column prop="code" label="租户编码" min-width="150" show-overflow-tooltip />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
                {{ row.status === 'ACTIVE' ? '正常' : '已禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="userCount" label="关联账号" width="110" />
          <el-table-column label="创建时间" min-width="180">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" width="220">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button v-if="row.status === 'ACTIVE'" link type="danger" @click="disableTenant(row)">禁用</el-button>
              <el-button v-else link type="success" @click="activateTenant(row)">重新启用</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && tenants.length === 0" description="暂无租户" />
      </div>

      <el-dialog v-model="createVisible" title="新建租户" width="480px" destroy-on-close>
        <el-form label-width="110px" autocomplete="off">
          <el-form-item label="租户名称" required><el-input v-model="createForm.tenantName" autocomplete="off" /></el-form-item>
          <el-form-item label="管理员邮箱" required>
            <el-input v-model="createForm.adminEmail" placeholder="请输入管理员邮箱" autocomplete="off" />
          </el-form-item>
          <el-form-item label="管理员密码" required><el-input v-model="createForm.adminPassword" type="password" placeholder="请输入管理员密码" show-password autocomplete="new-password" /></el-form-item>
          <el-form-item label="管理员姓名" required><el-input v-model="createForm.adminDisplayName" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="createVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitCreate">创建</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="editVisible" title="编辑租户" width="440px" destroy-on-close>
        <el-form label-width="90px">
          <el-form-item label="租户编码"><el-input :model-value="editForm.code" disabled /></el-form-item>
          <el-form-item label="租户名称" required><el-input v-model="editForm.name" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="editVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitEdit">保存</el-button>
        </template>
      </el-dialog>
    </template>

    <template v-else>
      <h2 class="simple-page-title">额度使用</h2>
      <p class="simple-page-desc">查看本月已用和总额度</p>

      <div class="simple-card quota-panel">
        <div class="panel-head">
          <p class="panel-desc">额度由平台管理员分配。</p>
          <el-button v-if="canReadQuota" :loading="loading" @click="loadQuota">刷新</el-button>
        </div>
        <div v-if="quota" class="summary-grid quota-grid">
          <div class="summary-card">
            <span>AI 调用</span>
            <strong>{{ formatNumber(quota.usedAiCallsThisMonth) }} / {{ formatNumber(quota.maxAiCallsPerMonth) }}</strong>
            <small>本月已用 / 总额度</small>
          </div>
          <div class="summary-card">
            <span>Token</span>
            <strong>{{ formatNumber(quota.usedAiTokensThisMonth) }} / {{ formatNumber(quota.maxAiTokensPerMonth) }}</strong>
            <small>本月已用 / 总额度</small>
          </div>
        </div>
        <el-empty v-else-if="!loading" description="暂无额度数据" />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TenantQuotaView } from '@/api/agent'
import { tenancyApi } from '@/api/tenancy'
import { useAuthStore } from '@/stores/auth'
import { hasPermission } from '@/utils/permissions'
import { buildTenantCreatePayload, isTenantCreateFormValid } from '@/utils/tenantCreate.mjs'

interface TenantRow {
  id: number
  code: string
  name: string
  status: string
  createdAt?: string | null
  userCount?: number
}

const authStore = useAuthStore()
const loading = ref(false)
const submitting = ref(false)
const tenants = ref<TenantRow[]>([])
const createVisible = ref(false)
const editVisible = ref(false)
const editTenantId = ref<number | null>(null)
const createForm = reactive({ tenantName: '', adminEmail: '', adminPassword: '', adminDisplayName: '' })
const editForm = reactive({ code: '', name: '' })
const activeCount = computed(() => tenants.value.filter(item => item.status === 'ACTIVE').length)
const disabledCount = computed(() => tenants.value.filter(item => item.status === 'DISABLED').length)

const canReadQuota = computed(() => hasPermission('tenant:quota:read'))
const quota = ref<TenantQuotaView | null>(null)

function formatNumber(value: number | null | undefined) {
  return Number(value ?? 0).toLocaleString('zh-CN')
}

function formatDate(value: string | null | undefined) {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false })
}

async function loadTenants() {
  loading.value = true
  try {
    const response = await tenancyApi.listTenants(0, 200)
    const data = (response as any)?.data
    const rows = Array.isArray(data?.content) ? data.content : Array.isArray(data) ? data : []
    tenants.value = rows.map((row: Record<string, unknown>) => ({
      id: Number(row.id), code: String(row.code ?? ''), name: String(row.name ?? ''),
      status: String(row.status ?? ''), createdAt: row.createdAt as string | null | undefined,
      userCount: Number(row.userCount ?? 0)
    })).filter((row: TenantRow) => Number.isSafeInteger(row.id) && row.id > 0)
    if (authStore.platformContextTenantId != null && !tenants.value.some(row => row.id === authStore.platformContextTenantId && row.status === 'ACTIVE')) {
      authStore.setPlatformContextTenantId(null)
    }
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(createForm, { tenantName: '', adminEmail: '', adminPassword: '', adminDisplayName: '' })
  createVisible.value = true
}

function openEdit(row: TenantRow) {
  editTenantId.value = row.id
  Object.assign(editForm, { code: row.code, name: row.name })
  editVisible.value = true
}

async function submitCreate() {
  if (!isTenantCreateFormValid(createForm)) return ElMessage.warning('请填写租户和管理员必填信息')
  submitting.value = true
  try {
    await tenancyApi.createTenant(buildTenantCreatePayload(createForm))
    ElMessage.success('租户创建成功')
    createVisible.value = false
    await loadTenants()
  } finally {
    submitting.value = false
  }
}

async function submitEdit() {
  if (!editTenantId.value || !editForm.name.trim()) return ElMessage.warning('租户名称不能为空')
  submitting.value = true
  try {
    await tenancyApi.updateTenant(editTenantId.value, { tenantName: editForm.name.trim() })
    ElMessage.success('租户已更新')
    editVisible.value = false
    await loadTenants()
  } finally {
    submitting.value = false
  }
}

async function disableTenant(row: TenantRow) {
  try {
    await ElMessageBox.confirm('禁用后该租户所有功能将停止，关联的管理员、教练和会员账号都将无法登录。', '确认禁用租户', { type: 'warning', confirmButtonText: '确认禁用', cancelButtonText: '取消' })
    await tenancyApi.disableTenant(row.id)
    ElMessage.success('租户已禁用，关联账号已停用')
    await loadTenants()
  } catch {
    /* 用户取消或请求错误由客户端拦截器提示 */
  }
}

async function activateTenant(row: TenantRow) {
  try {
    await ElMessageBox.confirm('重新启用租户不会自动恢复已禁用的用户账号。', '确认重新启用', { type: 'info', confirmButtonText: '重新启用', cancelButtonText: '取消' })
    await tenancyApi.activateTenant(row.id)
    ElMessage.success('租户已重新启用')
    await loadTenants()
  } catch {
    /* 用户取消或请求错误由客户端拦截器提示 */
  }
}

async function loadQuota() {
  if (!canReadQuota.value) return
  loading.value = true
  try {
    const res = await tenancyApi.getMyQuota()
    quota.value = (res as { data?: TenantQuotaView }).data ?? null
  } finally { loading.value = false }
}

onMounted(async () => {
  if (authStore.isPlatformAdmin) {
    await loadTenants()
    return
  }
  await loadQuota()
})
</script>

<style scoped>
.quota-panel { margin-top: 16px; }
.tenant-panel { margin-top: 16px; }
.panel-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
.panel-title { margin: 0; font-size: 16px; }
.panel-desc { margin: 4px 0 0; color: var(--text-muted); font-size: 13px; }
.panel-actions { display: flex; gap: 8px; }
.summary-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-top: 16px; }
.summary-card { padding: 16px; border: 1px solid var(--border); border-radius: 8px; background: var(--surface); }
.quota-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.summary-card span { display: block; color: var(--text-muted); font-size: 13px; }
.summary-card strong { display: block; margin-top: 8px; font-size: 24px; font-variant-numeric: tabular-nums; }
.summary-card small { display: block; margin-top: 6px; color: var(--text-muted); font-size: 12px; }
.field-hint { margin-top: 4px; color: var(--text-muted); font-size: 12px; line-height: 1.4; }
@media (max-width: 640px) { .summary-grid { grid-template-columns: 1fr; } }
</style>

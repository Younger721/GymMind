<template>
  <div class="knowledge">
    <div class="head">
      <div>
        <h2 class="simple-page-title">知识库</h2>
        <p class="simple-page-desc">
          {{ authStore.isPlatformAdmin ? '先上传平台基础文档，所有租户的 AI 助手都可使用' : '上传文档后自动解析，数据仅在本健身房内可见' }}
        </p>
      </div>
      <el-upload
        :show-file-list="false"
        :http-request="handleUpload"
        :disabled="uploading || !canManage"
      >
        <el-select v-model="uploadCategory" placeholder="资料分类" style="width: 150px; margin-right: 8px" @click.stop>
          <el-option label="通用资料" value="GENERAL" /><el-option label="动作规范" value="ACTION_STANDARD" /><el-option label="训练基础" value="TRAINING_BASICS" /><el-option label="器械说明" value="EQUIPMENT" /><el-option label="课程资料" value="COURSE" /><el-option label="饮食知识" value="NUTRITION" /><el-option label="门店制度" value="GYM_RULES" /><el-option label="常见问题" value="FAQ" />
        </el-select>
        <el-select v-model="uploadAudienceRoles" multiple collapse-tags collapse-tags-tooltip placeholder="面向角色" style="width: 220px; margin-right: 8px" @click.stop>
          <el-option v-for="option in audienceRoleOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
        <el-select v-model="uploadKnowledgeType" placeholder="知识类型" style="width: 150px; margin-right: 8px" @click.stop>
          <el-option label="普通公告" value="GENERAL_NOTICE" /><el-option label="基础价格" value="BASE_PRICE" /><el-option label="促销规则" value="PROMOTION_RULE" />
        </el-select>
        <el-input v-model="uploadProduct" placeholder="适用卡种" style="width: 130px; margin-right: 8px" @click.stop />
        <el-select v-if="uploadKnowledgeType !== 'GENERAL_NOTICE'" v-model="replaceDocumentId" clearable filterable placeholder="替换旧文档（可选）" style="width: 220px; margin-right: 8px" @click.stop>
          <el-option v-for="row in replacementCandidates" :key="row.id" :label="`${row.fileName} v${row.knowledgeVersion || 1}`" :value="row.id" />
        </el-select>
        <el-input-number v-if="uploadKnowledgeType === 'BASE_PRICE'" v-model="uploadPrice" :min="0" :precision="2" placeholder="价格" style="width: 130px; margin-right: 8px" @click.stop />
        <template v-if="uploadKnowledgeType === 'PROMOTION_RULE'">
          <el-select v-model="uploadDiscountType" style="width: 145px; margin-right: 8px" @click.stop>
            <el-option label="折扣比例" value="PERCENT" /><el-option label="减免金额" value="FIXED_AMOUNT" /><el-option label="优惠价" value="FIXED_PRICE" />
          </el-select>
          <el-input-number v-model="uploadDiscount" :min="0" :max="uploadDiscountType === 'PERCENT' ? 1 : undefined" :step="uploadDiscountType === 'PERCENT' ? 0.05 : 1" :precision="uploadDiscountType === 'PERCENT' ? 2 : 2" :placeholder="uploadDiscountType === 'PERCENT' ? '折扣系数' : '金额'" style="width: 130px; margin-right: 8px" @click.stop />
          <el-input v-model="uploadConditions" placeholder="适用条件（有条件时保守确认）" style="width: 220px; margin-right: 8px" @click.stop />
          <el-input-number v-model="uploadPriority" :min="0" :step="1" placeholder="优先级" style="width: 120px; margin-right: 8px" @click.stop />
        </template>
        <el-date-picker v-if="uploadKnowledgeType === 'PROMOTION_RULE'" v-model="uploadFrom" type="datetime" placeholder="生效时间" style="width: 180px; margin-right: 8px" @click.stop />
        <el-date-picker v-if="uploadKnowledgeType === 'PROMOTION_RULE'" v-model="uploadTo" type="datetime" placeholder="失效时间" style="width: 180px; margin-right: 8px" @click.stop />
        <el-button type="primary" :loading="uploading" :disabled="!canManage">上传文档</el-button>
      </el-upload>
    </div>

    <el-table :data="documents" v-loading="loading" class="simple-card" style="padding: 0; overflow: hidden">
      <el-table-column v-if="authStore.isPlatformAdmin" prop="tenantId" label="租户ID" width="90" />
      <el-table-column prop="fileName" label="文件名" min-width="200" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag size="small" :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布状态" width="110"><template #default="{ row }"><el-tag size="small" :type="publicationType(row)">{{ publicationText(row) }}</el-tag></template></el-table-column>
      <el-table-column label="资料来源" width="120">
        <template #default="{ row }">
          <span>{{ row.uploaderName || '未知上传者' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="面向角色" min-width="180">
        <template #default="{ row }">
          <el-tag v-for="role in row.audienceRoles || []" :key="role" size="small" type="info" class="role-tag">{{ audienceRoleLabel(role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="切分策略" width="130">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ chunkStrategyText(row.chunkStrategy) }}</el-tag>
          <span v-if="row.chunkStrategyVersion" class="strategy-version">v{{ row.chunkStrategyVersion }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="visibility" label="可见范围" width="120">
        <template #default="{ row }">
          {{ visibilityText(row.visibility) }}
        </template>
      </el-table-column>
      <el-table-column v-if="canManage" label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 'READY'" link type="success" @click="togglePublication(row)">{{ row.published ? '撤回' : '发布' }}</el-button>
          <el-button link type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="editVisible" title="编辑文档" width="420px">
      <el-form label-width="90px">
        <el-form-item label="文件名">
          <el-input :model-value="editForm.fileName" disabled />
        </el-form-item>
        <el-form-item label="可见范围">
          <el-select v-model="editForm.visibility" style="width: 100%">
            <el-option label="全店可见" value="TENANT" />
            <el-option label="仅本人可见" value="PRIVATE_USER" />
          </el-select>
        </el-form-item>
        <el-form-item label="面向角色" required>
          <el-select v-model="editForm.audienceRoles" multiple collapse-tags collapse-tags-tooltip style="width: 100%">
            <el-option v-for="option in audienceRoleOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'
import { knowledgeApi, type KnowledgeDocumentView } from '@/api/knowledge'
import { useAuthStore } from '@/stores/auth'
import { hasMenuPermission, MENU_CODES } from '@/utils/menuPermissions'

const authStore = useAuthStore()
const documents = ref<KnowledgeDocumentView[]>([])
const loading = ref(false)
let pollTimer: ReturnType<typeof setInterval> | undefined
const uploading = ref(false)
const uploadCategory = ref('GENERAL')
const allAudienceRoles = ['PLATFORM_ADMIN', 'GYM_ADMIN', 'COACH', 'MEMBER']
const tenantAudienceRoles = ['GYM_ADMIN', 'COACH', 'MEMBER']
const audienceRoleLabels: Record<string, string> = { PLATFORM_ADMIN: '平台管理员', GYM_ADMIN: '健身房管理员', COACH: '教练', MEMBER: '会员' }
const audienceRoleOptions = computed(() => (authStore.isPlatformAdmin ? allAudienceRoles : tenantAudienceRoles).map(value => ({ value, label: audienceRoleLabels[value] })))
const uploadAudienceRoles = ref<string[]>(authStore.isPlatformAdmin ? [...allAudienceRoles] : [...tenantAudienceRoles])
const uploadKnowledgeType = ref('GENERAL_NOTICE')
const uploadProduct = ref('')
const uploadPrice = ref<number | undefined>()
const uploadDiscount = ref<number | undefined>()
const uploadDiscountType = ref('PERCENT')
const uploadFrom = ref<Date | undefined>()
const uploadTo = ref<Date | undefined>()
const uploadConditions = ref('')
const uploadPriority = ref(0)
const replaceDocumentId = ref<number | undefined>()
const replacementCandidates = computed(() => documents.value.filter(row =>
  row.status !== 'DELETED'
  && row.knowledgeType === uploadKnowledgeType.value
  && (!uploadProduct.value || row.applicableProduct?.trim().toLowerCase() === uploadProduct.value.trim().toLowerCase())
))
const saving = ref(false)
const editVisible = ref(false)
const editForm = reactive({
  id: 0,
  fileName: '',
  visibility: 'TENANT' as 'TENANT' | 'PRIVATE_USER',
  audienceRoles: [] as string[]
})

const canManage = computed(() => authStore.isPlatformAdmin
  || (authStore.roles.includes('GYM_ADMIN')
    && hasMenuPermission(MENU_CODES.KNOWLEDGE)))

/** 入库流水线未结束的状态，需轮询列表直至就绪或失败 */
const ingestInProgressStatuses = ['UPLOADING', 'PARSING', 'CHUNKED', 'INDEXING'] as const
const isIngestInProgress = (status: string) =>
  (ingestInProgressStatuses as readonly string[]).includes(status)

const load = async () => {
  loading.value = true
  try {
    const res = await knowledgeApi.listDocuments()
    documents.value = res.data || []
  } catch {
    ElMessage.error('加载文档列表失败')
  } finally {
    loading.value = false
  }
}

const handleUpload = async (options: UploadRequestOptions) => {
  if (!uploadAudienceRoles.value.length) { ElMessage.warning('至少选择一个面向角色'); return }
  uploading.value = true
  try {
    await knowledgeApi.uploadDocument(options.file as File, 'TENANT', uploadCategory.value, uploadAudienceRoles.value, {
      knowledgeType: uploadKnowledgeType.value, applicableProduct: uploadProduct.value || undefined,
      replaceDocumentId: replaceDocumentId.value,
      priceAmount: uploadPrice.value,
      discountType: uploadKnowledgeType.value === 'PROMOTION_RULE' ? uploadDiscountType.value : undefined,
      discountValue: uploadDiscount.value, effectiveFrom: localDateTime(uploadFrom.value), effectiveTo: localDateTime(uploadTo.value),
      conditions: uploadConditions.value || undefined, promotionPriority: uploadPriority.value
    })
    ElMessage.success('上传成功，正在解析')
    await load()
    startPolling()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

const localDateTime = (value?: Date) => value
  ? `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}T${String(value.getHours()).padStart(2, '0')}:${String(value.getMinutes()).padStart(2, '0')}:${String(value.getSeconds()).padStart(2, '0')}`
  : undefined

const openEdit = (row: KnowledgeDocumentView) => {
  editForm.id = row.id
  editForm.fileName = row.fileName
  editForm.visibility = row.visibility as 'TENANT' | 'PRIVATE_USER'
  editForm.audienceRoles = [...(row.audienceRoles || [])]
  editVisible.value = true
}

const saveEdit = async () => {
  if (!editForm.audienceRoles.length) { ElMessage.warning('至少选择一个面向角色'); return }
  saving.value = true
  try {
    await knowledgeApi.updateDocument(editForm.id, editForm.visibility, editForm.audienceRoles)
    ElMessage.success('已更新')
    editVisible.value = false
    await load()
  } catch {
    ElMessage.error('更新失败')
  } finally {
    saving.value = false
  }
}

const remove = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定删除该文档？', '提示', { type: 'warning' })
    await knowledgeApi.deleteDocument(id)
    ElMessage.success('已删除')
    await load()
  } catch {
    /* 取消或失败 */
  }
}

const togglePublication = async (row: KnowledgeDocumentView) => {
  try { await knowledgeApi.setPublication(row.id, !row.published); ElMessage.success(row.published ? '已撤回' : '已发布'); await load() }
  catch { ElMessage.error('更新发布状态失败') }
}

const statusText = (s: string) => {
  const map: Record<string, string> = {
    UPLOADING: '上传中',
    PARSING: '解析中',
    CHUNKED: '已切片',
    INDEXING: '索引中',
    READY: '就绪',
    FAILED: '失败',
    DELETED: '已删除'
  }
  return map[s] || s
}

const visibilityText = (v: string) => {
  return v === 'PRIVATE_USER' ? '仅本人' : '全店'
}
const audienceRoleLabel = (role: string) => audienceRoleLabels[role] || role

const chunkStrategyText = (strategy?: string) => {
  const map: Record<string, string> = {
    PROCEDURE: '动作步骤',
    KNOWLEDGE: '知识说明',
    FAQ_RULE: '问答规则',
    RECORD: '记录表格',
    GENERAL: '通用切分'
  }
  return map[strategy || 'GENERAL'] || strategy || '通用切分'
}

const statusType = (s: string) => {
  if (s === 'READY') return 'success'
  if (s === 'FAILED') return 'danger'
  if (isIngestInProgress(s)) return 'warning'
  return 'info'
}

const publicationText = (row: KnowledgeDocumentView) => {
  if (isIngestInProgress(row.status)) return '处理中'
  if (row.status === 'FAILED') return '不可用'
  return row.published ? '已发布' : '待发布'
}

const publicationType = (row: KnowledgeDocumentView) => {
  if (row.status === 'FAILED') return 'danger'
  if (row.status !== 'READY') return 'warning'
  return row.published ? 'success' : 'warning'
}

const startPolling = () => {
  if (pollTimer) clearInterval(pollTimer)
  pollTimer = setInterval(async () => {
    await load()
    if (documents.value.every(row => !isIngestInProgress(row.status))) {
      if (pollTimer) clearInterval(pollTimer)
      pollTimer = undefined
    }
  }, 3000)
}

onMounted(async () => {
  await load()
  if (documents.value.some(row => isIngestInProgress(row.status))) startPolling()
})
onBeforeUnmount(() => { if (pollTimer) clearInterval(pollTimer) })
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.head .simple-page-desc {
  margin-bottom: 0;
}

.tenant-hint {
  margin-bottom: 16px;
}

.strategy-version {
  margin-left: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>

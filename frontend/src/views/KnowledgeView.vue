<template>
  <div class="knowledge">
    <div class="head">
      <div>
        <h2 class="simple-page-title">知识库</h2>
        <p class="simple-page-desc">
          {{ authStore.isPlatformAdmin ? '按门店租户隔离管理文档，请在顶部选择门店' : '上传文档后自动解析，数据仅在本健身房内可见' }}
        </p>
      </div>
      <el-upload
        :show-file-list="false"
        :http-request="handleUpload"
        :disabled="uploading || !canManage"
      >
        <el-button type="primary" :loading="uploading" :disabled="!canManage">上传文档</el-button>
      </el-upload>
    </div>

    <el-alert
      v-if="authStore.isPlatformAdmin && !authStore.platformContextTenantId"
      type="warning"
      :closable="false"
      show-icon
      title="请先在顶部选择要管理的门店租户"
      class="tenant-hint"
    />

    <el-table :data="documents" v-loading="loading" class="simple-card" style="padding: 0; overflow: hidden">
      <el-table-column v-if="authStore.isPlatformAdmin" prop="tenantId" label="租户ID" width="90" />
      <el-table-column prop="fileName" label="文件名" min-width="200" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag size="small" :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
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
          <el-button link type="warning" @click="reindex(row.id)">重新解析</el-button>
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
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'
import { knowledgeApi, type KnowledgeDocumentView } from '@/api/knowledge'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const documents = ref<KnowledgeDocumentView[]>([])
const loading = ref(false)
const uploading = ref(false)
const saving = ref(false)
const editVisible = ref(false)
const editForm = reactive({
  id: 0,
  fileName: '',
  visibility: 'TENANT' as 'TENANT' | 'PRIVATE_USER'
})

const canManage = computed(() => {
  if (authStore.isPlatformAdmin) {
    return authStore.platformContextTenantId != null
  }
  return true
})

const load = async () => {
  if (authStore.isPlatformAdmin && !authStore.platformContextTenantId) {
    documents.value = []
    return
  }
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
  uploading.value = true
  try {
    await knowledgeApi.uploadDocument(options.file as File)
    ElMessage.success('上传成功，正在解析')
    await load()
  } catch {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

const openEdit = (row: KnowledgeDocumentView) => {
  editForm.id = row.id
  editForm.fileName = row.fileName
  editForm.visibility = row.visibility as 'TENANT' | 'PRIVATE_USER'
  editVisible.value = true
}

const saveEdit = async () => {
  saving.value = true
  try {
    await knowledgeApi.updateDocument(editForm.id, editForm.visibility)
    ElMessage.success('已更新')
    editVisible.value = false
    await load()
  } catch {
    ElMessage.error('更新失败')
  } finally {
    saving.value = false
  }
}

const reindex = async (id: number) => {
  try {
    await knowledgeApi.reindexDocument(id)
    ElMessage.success('已提交重新解析')
    await load()
  } catch {
    ElMessage.error('重新解析失败')
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

const statusText = (s: string) => {
  const map: Record<string, string> = {
    UPLOADING: '上传中',
    PARSING: '解析中',
    READY: '就绪',
    FAILED: '失败',
    DELETED: '已删除'
  }
  return map[s] || s
}

const visibilityText = (v: string) => {
  return v === 'PRIVATE_USER' ? '仅本人' : '全店'
}

const statusType = (s: string) => {
  if (s === 'READY') return 'success'
  if (s === 'FAILED') return 'danger'
  if (s === 'PARSING' || s === 'UPLOADING') return 'warning'
  return 'info'
}

watch(
  () => authStore.platformContextTenantId,
  () => load()
)

onMounted(load)
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
</style>

<template>
  <div class="knowledge">
    <div class="head">
      <div>
        <h2 class="simple-page-title">知识库</h2>
        <p class="simple-page-desc">上传文档后自动解析，无需手动分类</p>
      </div>
      <el-upload
        :show-file-list="false"
        :http-request="handleUpload"
        :disabled="uploading"
      >
        <el-button type="primary" :loading="uploading">上传文档</el-button>
      </el-upload>
    </div>

    <el-table :data="documents" v-loading="loading" class="simple-card" style="padding: 0; overflow: hidden">
      <el-table-column prop="fileName" label="文件名" min-width="200" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag size="small" :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="visibility" label="可见范围" width="120" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'
import { knowledgeApi, type KnowledgeDocumentView } from '@/api/knowledge'

const documents = ref<KnowledgeDocumentView[]>([])
const loading = ref(false)
const uploading = ref(false)

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

const statusType = (s: string) => {
  if (s === 'READY') return 'success'
  if (s === 'FAILED') return 'danger'
  if (s === 'PARSING' || s === 'UPLOADING') return 'warning'
  return 'info'
}

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
</style>

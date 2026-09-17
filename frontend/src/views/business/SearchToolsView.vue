<template>
  <div>
    <h2 class="simple-page-title">搜索与导入</h2>
    <p class="simple-page-desc">视频、文章与知识库检索</p>

    <div class="simple-card">
      <el-input v-model="keyword" placeholder="输入关键词" clearable @keyup.enter="searchAll">
        <template #append>
          <el-button :loading="loading" @click="searchAll">搜索</el-button>
        </template>
      </el-input>
      <div class="btn-row">
        <el-button size="small" @click="searchVideos">仅视频</el-button>
        <el-button size="small" @click="searchArticles">仅文章</el-button>
        <el-button size="small" @click="searchKnowledge">知识库</el-button>
      </div>
    </div>

    <BizTable :rows="rows" :loading="loading" />

    <div class="simple-card" style="margin-top: 16px">
      <h3 class="sub-title">创建文章 / 导入</h3>
      <el-form label-width="80px">
        <el-form-item label="URL">
          <el-input v-model="article.url" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="article.title" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="article.summary" type="textarea" rows="2" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="saveArticle">保存文章</el-button>
          <el-button :loading="submitting" @click="importArticle">导入</el-button>
        </el-form-item>
      </el-form>
      <JsonBlock v-if="actionResult" :data="actionResult" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import BizTable from '@/components/business/BizTable.vue'
import JsonBlock from '@/components/business/JsonBlock.vue'
import { searchApi } from '@/api/searchApi'
import { useBizList } from '@/composables/useBizList'

const keyword = ref('')
const { rows, loading, runLoader } = useBizList()
const submitting = ref(false)
const actionResult = ref<unknown>(null)
const article = reactive({ url: '', title: '', summary: '' })

async function searchVideos() {
  if (!keyword.value.trim()) return ElMessage.warning('请输入关键词')
  await runLoader(() => searchApi.searchVideos(keyword.value.trim()))
}

async function searchArticles() {
  if (!keyword.value.trim()) return ElMessage.warning('请输入关键词')
  await runLoader(() => searchApi.searchArticles(keyword.value.trim()))
}

async function searchKnowledge() {
  if (!keyword.value.trim()) return ElMessage.warning('请输入关键词')
  await runLoader(() => searchApi.searchKnowledge(keyword.value.trim()))
}

async function searchAll() {
  await searchArticles()
}

async function saveArticle() {
  if (!article.url.trim() || !article.title.trim()) return ElMessage.warning('请填写 URL 与标题')
  submitting.value = true
  try {
    actionResult.value = await searchApi.saveArticle({ ...article })
    ElMessage.success('已保存')
  } finally {
    submitting.value = false
  }
}

async function importArticle() {
  if (!article.url.trim() || !article.title.trim()) return ElMessage.warning('请填写 URL 与标题')
  submitting.value = true
  try {
    actionResult.value = await searchApi.importArticle({ ...article })
    ElMessage.success('已导入')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.btn-row {
  margin-top: 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.sub-title {
  margin: 0 0 12px;
  font-size: 15px;
}
</style>

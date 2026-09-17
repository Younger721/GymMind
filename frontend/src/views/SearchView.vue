<template>
  <div class="search-view">
    <el-card>
      <template #header>
        <h2>Web Search & Import</h2>
      </template>

      <!-- Search Input -->
      <el-input
        v-model="searchQuery"
        placeholder="Search for fitness articles, research papers..."
        size="large"
        @keyup.enter="handleSearch"
      >
        <template #append>
          <el-button :loading="searching" @click="handleSearch">
            <el-icon><Search /></el-icon>
            Search
          </el-button>
        </template>
      </el-input>

      <!-- Search Results -->
      <div v-if="searchResults.length > 0" class="results-container">
        <el-divider content-position="left">Search Results ({{ searchResults.length }})</el-divider>

        <div v-for="result in searchResults" :key="result.url" class="result-card">
          <el-card shadow="hover">
            <div class="result-header">
              <h3>{{ result.title }}</h3>
              <el-tag size="small" type="info">{{ result.source }}</el-tag>
            </div>
            <p class="result-snippet">{{ result.snippet }}</p>
            <div class="result-footer">
              <div class="result-meta">
                <el-tag size="small">{{ result.publishedDate }}</el-tag>
                <el-tag size="small" type="success">Score: {{ (result.relevanceScore * 100).toFixed(0) }}%</el-tag>
              </div>
              <div class="result-actions">
                <el-button size="small" @click="openUrl(result.url)">
                  <el-icon><Link /></el-icon>
                  View
                </el-button>
                <el-button type="primary" size="small" @click="importContent(result)">
                  <el-icon><Download /></el-icon>
                  Import
                </el-button>
              </div>
            </div>
          </el-card>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else-if="!searching && searchQuery" class="empty-state">
        <el-empty description="No results found. Try a different query." />
      </div>
    </el-card>

    <!-- Import Dialog -->
    <el-dialog v-model="importDialogVisible" title="Import to Knowledge Base" width="500px">
      <el-form :model="importForm" label-width="100px">
        <el-form-item label="Title">
          <el-input v-model="importForm.title" disabled />
        </el-form-item>
        <el-form-item label="URL">
          <el-input v-model="importForm.url" disabled />
        </el-form-item>
        <el-form-item label="Category">
          <el-select v-model="importForm.category">
            <el-option label="Fitness" value="FITNESS" />
            <el-option label="Nutrition" value="NUTRITION" />
            <el-option label="Research" value="RESEARCH" />
            <el-option label="Other" value="OTHER" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importDialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="importing" @click="confirmImport">
          Import
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { searchApi, type SearchResult } from '@/api/search'
import { ElMessage } from 'element-plus'

const searchQuery = ref('')
const searching = ref(false)
const searchResults = ref<SearchResult[]>([])
const importDialogVisible = ref(false)
const importing = ref(false)

const importForm = ref({
  title: '',
  url: '',
  category: 'FITNESS'
})

const handleSearch = async () => {
  if (!searchQuery.value.trim()) {
    ElMessage.warning('Please enter a search query')
    return
  }

  searching.value = true
  try {
    const response = await searchApi.search(searchQuery.value)
    searchResults.value = response.data.results
  } catch (error) {
    console.error('Search failed:', error)
  } finally {
    searching.value = false
  }
}

const openUrl = (url: string) => {
  window.open(url, '_blank')
}

const importContent = (result: SearchResult) => {
  importForm.value = {
    title: result.title,
    url: result.url,
    category: 'FITNESS'
  }
  importDialogVisible.value = true
}

const confirmImport = async () => {
  importing.value = true
  try {
    await searchApi.importFromWeb(importForm.value)
    ElMessage.success('Content imported successfully')
    importDialogVisible.value = false
  } catch (error) {
    console.error('Import failed:', error)
  } finally {
    importing.value = false
  }
}
</script>

<style scoped>
.search-view h2 {
  margin: 0;
  color: #303133;
}

.results-container {
  margin-top: 20px;
}

.result-card {
  margin-bottom: 16px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.result-header h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
  flex: 1;
  margin-right: 12px;
}

.result-snippet {
  color: #606266;
  line-height: 1.6;
  margin-bottom: 12px;
}

.result-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-meta {
  display: flex;
  gap: 8px;
}

.result-actions {
  display: flex;
  gap: 8px;
}

.empty-state {
  margin-top: 40px;
}
</style>

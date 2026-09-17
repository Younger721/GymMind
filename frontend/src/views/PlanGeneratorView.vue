<template>
  <div class="plan">
    <h2 class="simple-page-title">训练计划</h2>
    <p class="simple-page-desc">描述你的目标，AI 将生成结构化训练计划</p>

    <div class="simple-card form-area">
      <el-input
        v-model="request"
        type="textarea"
        :rows="5"
        placeholder="例如：我是健身新手，目标增肌，每周能练 3 次，每次 45 分钟，健身房有哑铃和杠铃。"
      />
      <el-button type="primary" :loading="loading" class="gen-btn" @click="generate">
        生成计划
      </el-button>
    </div>

    <div v-if="result" class="simple-card result">
      <h3>生成结果</h3>
      <pre>{{ result }}</pre>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import apiClient from '@/api/client'

const request = ref('')
const loading = ref(false)
const result = ref('')

const generate = async () => {
  if (!request.value.trim()) {
    ElMessage.warning('请先描述你的训练需求')
    return
  }
  loading.value = true
  result.value = ''
  try {
    const res = await apiClient.post<any, { data: unknown }>('/v1/ai/workout-plan-drafts', {
      memberId: null,
      request: request.value.trim()
    })
    result.value = JSON.stringify(res.data, null, 2)
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '生成失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.form-area {
  margin-bottom: 16px;
}

.gen-btn {
  margin-top: 12px;
}

.result h3 {
  margin: 0 0 12px;
  font-size: 15px;
}

.result pre {
  margin: 0;
  padding: 12px;
  background: var(--bg);
  border-radius: 8px;
  font-size: 12px;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>

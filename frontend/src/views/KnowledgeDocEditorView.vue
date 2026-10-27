<template>
  <div class="doc-editor-page">
    <div class="toolbar">
      <el-button @click="goBack">返回资料库</el-button>
      <span class="title">{{ fileName || '在线编辑' }}</span>
      <el-tag v-if="savingHint" type="warning" size="small">{{ savingHint }}</el-tag>
    </div>
    <div v-if="loading" v-loading="true" class="editor-host" />
    <div v-else-if="error" class="error-box">
      <p>{{ error }}</p>
      <el-button type="primary" @click="goBack">返回</el-button>
    </div>
    <div v-else id="onlyoffice-editor" class="editor-host" />
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { knowledgeApi } from '@/api/knowledge'

declare global {
  interface Window {
    DocsAPI?: { DocEditor: new (id: string, config: { token: string }) => void }
  }
}

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const error = ref('')
const fileName = ref('')
const savingHint = ref('')
let docEditor: { destroyEditor?: () => void } | undefined
let scriptEl: HTMLScriptElement | undefined

const documentId = () => Number(route.params.id)

const goBack = () => router.push('/knowledge')

const loadScript = (src: string) =>
  new Promise<void>((resolve, reject) => {
    if (window.DocsAPI) {
      resolve()
      return
    }
    const tryLoad = (attempt: number) => {
      scriptEl = document.createElement('script')
      scriptEl.src = src
      scriptEl.async = true
      scriptEl.onload = () => resolve()
      scriptEl.onerror = () => {
        scriptEl?.remove()
        scriptEl = undefined
        if (attempt < 12) {
          window.setTimeout(() => tryLoad(attempt + 1), 5000)
          return
        }
        reject(new Error(
          'OnlyOffice 未就绪：请执行 docker compose up -d onlyoffice，等待 1～3 分钟后刷新（http://localhost:8088/welcome/ 可访问后再试）'
        ))
      }
      document.body.appendChild(scriptEl)
    }
    tryLoad(1)
  })

onMounted(async () => {
  const id = documentId()
  if (!id || Number.isNaN(id)) {
    error.value = '无效的文档 ID'
    loading.value = false
    return
  }
  try {
    const res = await knowledgeApi.getEditorConfig(id)
    const cfg = res.data
    if (!cfg?.token || !cfg.documentServerUrl || !cfg.config) {
      throw new Error('编辑器配置不完整')
    }
    fileName.value = cfg.fileName
    savingHint.value = '保存后将自动重新解析，解析完成后请在资料库发布'
    await loadScript(`${cfg.documentServerUrl.replace(/\/$/, '')}/web-apps/apps/api/documents/api.js`)
    loading.value = false
    await new Promise<void>(r => requestAnimationFrame(() => r()))
    if (!window.DocsAPI) {
      throw new Error('OnlyOffice API 未就绪')
    }
    // JWT 开启时 config 字段须与 token payload 完全一致（OnlyOffice 会逐项比对）
    docEditor = new window.DocsAPI.DocEditor('onlyoffice-editor', {
      ...cfg.config,
      token: cfg.token,
      width: '100%',
      height: '100%'
    })
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '加载编辑器失败'
    error.value = msg
    ElMessage.error(msg)
    loading.value = false
  }
})

onBeforeUnmount(() => {
  docEditor?.destroyEditor?.()
  if (scriptEl) {
    scriptEl.remove()
    scriptEl = undefined
  }
})
</script>

<style scoped>
.doc-editor-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-height: 480px;
  padding: 12px 16px;
  box-sizing: border-box;
  background: var(--el-bg-color);
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.title {
  font-weight: 600;
  flex: 1;
}

.editor-host {
  flex: 1;
  min-height: 400px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  overflow: hidden;
}

.error-box {
  padding: 24px;
}
</style>

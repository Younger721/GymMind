<template>
  <div class="harness-config simple-card">
    <h3 class="panel-title">{{ platform ? '平台 AI Harness 默认配置' : '租户 AI Harness 配置' }}</h3>
    <p class="panel-desc">一切皆插件：勾选默认启用的能力，新会话将按此组装上下文</p>

    <el-form label-width="100px" v-loading="loading">
      <el-form-item label="系统提示词">
        <el-input v-model="systemPrompt" type="textarea" rows="3" />
      </el-form-item>
      <el-form-item label="启用插件">
        <el-checkbox-group v-model="selectedPlugins">
          <div v-for="p in availablePlugins" :key="p.id" class="plugin-item">
            <el-checkbox :label="p.id">
              <strong>{{ p.name }}</strong>
              <span class="plugin-desc">{{ p.description }}</span>
              <el-tag size="small" type="info">{{ p.category }}</el-tag>
            </el-checkbox>
          </div>
        </el-checkbox-group>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="save">保存配置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { harnessApi, type HarnessPluginDescriptor } from '@/api/harness'

const props = defineProps<{ platform?: boolean }>()

const loading = ref(false)
const saving = ref(false)
const systemPrompt = ref('')
const selectedPlugins = ref<string[]>([])
const availablePlugins = ref<HarnessPluginDescriptor[]>([])

async function load() {
  loading.value = true
  try {
    const res = props.platform
      ? await harnessApi.getPlatformConfig()
      : await harnessApi.getConfig()
    const data = res.data
    systemPrompt.value = data.systemPrompt
    selectedPlugins.value = [...data.enabledPlugins]
    availablePlugins.value = data.availablePlugins
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!selectedPlugins.value.length) {
    return ElMessage.warning('至少启用一个插件')
  }
  saving.value = true
  try {
    const body = { enabledPlugins: selectedPlugins.value, systemPrompt: systemPrompt.value }
    if (props.platform) {
      await harnessApi.updatePlatformConfig(body)
    } else {
      await harnessApi.updateConfig(body)
    }
    ElMessage.success('已保存')
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.panel-title {
  margin: 0 0 4px;
  font-size: 16px;
}

.panel-desc {
  margin: 0 0 16px;
  font-size: 12px;
  color: var(--text-muted);
}

.plugin-item {
  margin-bottom: 8px;
  width: 100%;
}

.plugin-desc {
  margin: 0 8px;
  font-size: 12px;
  color: var(--text-muted);
}
</style>

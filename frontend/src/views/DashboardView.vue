<template>
  <div class="dashboard">
    <h2 class="simple-page-title">你好，{{ authStore.username }}</h2>
    <p class="simple-page-desc">快速进入常用功能</p>

    <div class="quick-grid">
      <router-link
        v-for="item in quickLinks"
        :key="item.path"
        :to="item.path"
        class="quick-card simple-card"
      >
        <span class="icon">{{ item.icon }}</span>
        <strong>{{ item.title }}</strong>
        <span class="hint">{{ item.desc }}</span>
      </router-link>
    </div>

    <div v-if="docCount !== null" class="stats simple-card">
      <div class="stat">
        <span class="label">知识库文档</span>
        <span class="value">{{ docCount }}</span>
      </div>
      <div class="stat">
        <span class="label">Agent 数量</span>
        <span class="value">{{ agentCount ?? '—' }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { knowledgeApi } from '@/api/knowledge'
import { agentApi } from '@/api/agent'

const authStore = useAuthStore()
const docCount = ref<number | null>(null)
const agentCount = ref<number | null>(null)

const quickLinks = [
  { path: '/assistant', icon: '💬', title: 'AI 助手', desc: '健身问答与咨询' },
  { path: '/operations', icon: '👥', title: '门店运营', desc: '会员·课程·预约' },
  { path: '/training', icon: '🏋️', title: '训练营养', desc: '动作库与计划' },
  { path: '/analytics', icon: '📊', title: '数据分析', desc: '看板与周报' },
  { path: '/knowledge', icon: '📄', title: '知识库', desc: '上传门店文档' },
  { path: '/plan', icon: '📋', title: '训练计划', desc: 'AI 生成计划' },
  { path: '/agents/manage', icon: '🤖', title: 'Agent', desc: '配置门店助手' },
  { path: '/admin', icon: '⚙️', title: '用户审计', desc: '用户与操作日志' }
]

onMounted(async () => {
  if (authStore.isPlatformAdmin) {
    docCount.value = null
    agentCount.value = null
    return
  }
  try {
    const docs = await knowledgeApi.listDocuments()
    docCount.value = docs.data?.length ?? 0
  } catch {
    docCount.value = 0
  }
  try {
    const agents = await agentApi.list({ silent: true })
    agentCount.value = agents.data?.length ?? 0
  } catch {
    agentCount.value = null
  }
})
</script>

<style scoped>
.quick-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.quick-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  text-decoration: none;
  color: inherit;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.quick-card:hover {
  border-color: var(--primary);
  box-shadow: 0 4px 12px rgba(5, 150, 105, 0.12);
  text-decoration: none;
}

.icon {
  font-size: 24px;
}

.quick-card strong {
  font-size: 15px;
}

.hint {
  font-size: 12px;
  color: var(--text-muted);
}

.stats {
  display: flex;
  gap: 32px;
}

.stat {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat .label {
  font-size: 12px;
  color: var(--text-muted);
}

.stat .value {
  font-size: 22px;
  font-weight: 600;
}
</style>

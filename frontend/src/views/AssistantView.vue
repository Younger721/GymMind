<template>
  <div class="assistant">
    <h2 class="simple-page-title">AI 助手</h2>
    <p class="simple-page-desc">Harness 插件化对话 — 按需勾选能力，类似 DeepSeek 工具链</p>

    <div class="chat-box simple-card">
      <div class="plugin-bar">
        <span class="label">本次会话插件：</span>
        <el-checkbox-group v-model="sessionPlugins" size="small">
          <el-checkbox
            v-for="p in availablePlugins"
            :key="p.id"
            :label="p.id"
            :disabled="!enabledPlugins.includes(p.id)"
          >
            {{ p.name }}
          </el-checkbox>
        </el-checkbox-group>
        <router-link v-if="canConfigure" :to="configLink" class="config-link">配置默认插件</router-link>
      </div>

      <div ref="boxRef" class="messages">
        <div v-if="messages.length === 0" class="empty">
          <p>试试这些问题：</p>
          <button
            v-for="q in samples"
            :key="q"
            type="button"
            class="sample"
            @click="ask(q)"
          >
            {{ q }}
          </button>
        </div>

        <div
          v-for="(msg, i) in messages"
          :key="i"
          class="msg"
          :class="msg.role"
        >
          <div class="bubble">{{ msg.content }}</div>
          <ul v-if="msg.sources?.length" class="sources">
            <li v-for="(s, j) in msg.sources" :key="j">{{ sourceLabel(s) }}</li>
          </ul>
        </div>

        <div v-if="loading" class="msg assistant">
          <div class="bubble typing">思考中…</div>
        </div>
      </div>

      <form class="input-row" @submit.prevent="send">
        <el-input
          v-model="input"
          placeholder="输入问题，Enter 发送"
          :disabled="loading"
          @keydown.enter.exact.prevent="send"
        />
        <el-button type="primary" :loading="loading" @click="send">发送</el-button>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { harnessApi, type HarnessPluginDescriptor } from '@/api/harness'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

interface UiMessage {
  role: 'user' | 'assistant'
  content: string
  sources?: Array<{ documentName?: string; excerpt?: string; text?: string } | string>
}

const authStore = useAuthStore()
const availablePlugins = ref<HarnessPluginDescriptor[]>([])
const enabledPlugins = ref<string[]>(['basic_chat'])
const sessionPlugins = ref<string[]>(['basic_chat'])
const messages = ref<UiMessage[]>([])
const input = ref('')
const loading = ref(false)
const boxRef = ref<HTMLElement>()
const sessionId = `web-${Date.now()}`

const canConfigure = computed(() =>
  authStore.isPlatformAdmin || authStore.roles.includes('GYM_ADMIN')
)
const configLink = computed(() =>
  authStore.isPlatformAdmin ? '/platform' : '/tenant-settings'
)

const samples = [
  '新手如何开始力量训练？',
  '减脂期饮食要注意什么？',
  '深蹲标准动作要点'
]

const scrollBottom = () => {
  nextTick(() => {
    if (boxRef.value) boxRef.value.scrollTop = boxRef.value.scrollHeight
  })
}

const sourceLabel = (s: { documentName?: string; excerpt?: string; text?: string } | string) => {
  if (typeof s === 'string') return s
  return s.documentName || s.excerpt || s.text || '知识库片段'
}

const ask = (text: string) => {
  input.value = text
  send()
}

const send = async () => {
  const text = input.value.trim()
  if (!text || loading.value) return
  if (!sessionPlugins.value.length) {
    return ElMessage.warning('请至少选择一个插件')
  }

  messages.value.push({ role: 'user', content: text })
  input.value = ''
  loading.value = true
  scrollBottom()

  try {
    const res = await harnessApi.chat({
      sessionId,
      question: text,
      plugins: sessionPlugins.value
    })
    const view = res.data
    const sources: UiMessage['sources'] =
      view.citations?.map(c => c.text || c.documentId || '') ??
      view.sources ??
      []
    messages.value.push({
      role: 'assistant',
      content: view.answer,
      sources
    })
  } catch {
    ElMessage.error('发送失败，请稍后重试')
  } finally {
    loading.value = false
    scrollBottom()
  }
}

onMounted(async () => {
  try {
    const res = await harnessApi.getConfig()
    const data = res.data
    availablePlugins.value = data.availablePlugins
    enabledPlugins.value = data.enabledPlugins.length ? data.enabledPlugins : ['basic_chat']
    sessionPlugins.value = [...enabledPlugins.value]
  } catch {
    availablePlugins.value = [
      {
        id: 'basic_chat',
        name: '基础对话',
        description: '直接调用大模型',
        category: 'CORE',
        permission: 'ai:chat',
        requiresTenant: false,
        defaultEnabled: true
      }
    ]
    sessionPlugins.value = ['basic_chat']
  }
})
</script>

<style scoped>
.chat-box {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 180px);
  min-height: 420px;
  padding: 0;
  overflow: hidden;
}

.plugin-bar {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
  font-size: 13px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.plugin-bar .label {
  color: var(--text-muted);
  flex-shrink: 0;
}

.config-link {
  margin-left: auto;
  font-size: 12px;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.empty {
  text-align: center;
  color: var(--text-muted);
  padding-top: 40px;
}

.sample {
  display: block;
  width: 100%;
  max-width: 320px;
  margin: 8px auto 0;
  padding: 10px 14px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg);
  cursor: pointer;
  font-size: 13px;
  text-align: left;
}

.sample:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.msg {
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
}

.msg.user {
  align-items: flex-end;
}

.msg.user .bubble {
  background: var(--primary);
  color: #fff;
}

.msg.assistant .bubble {
  background: var(--bg);
  border: 1px solid var(--border);
}

.bubble {
  max-width: 85%;
  padding: 10px 14px;
  border-radius: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  font-size: 14px;
}

.typing {
  color: var(--text-muted);
}

.sources {
  margin: 6px 0 0;
  padding-left: 18px;
  font-size: 12px;
  color: var(--text-muted);
}

.input-row {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid var(--border);
}
</style>

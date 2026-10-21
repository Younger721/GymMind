<template>
  <div class="assistant">
    <div class="chat-layout">
      <aside class="history-panel simple-card">
        <div class="history-heading"><strong>最近对话</strong><el-button size="small" type="primary" plain @click="newConversation">新建对话</el-button></div>
        <div class="history-list">
          <div v-for="item in sessions" :key="item.sessionKey" class="history-item" :class="{active:item.sessionKey===sessionId}" @click="selectSession(item.sessionKey)">
            <span class="history-title">{{ item.title }}</span>
            <el-tooltip content="删除对话" placement="top">
              <el-button class="history-delete" link type="danger" aria-label="删除对话" @click.stop="deleteSession(item.sessionKey)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
        </div>
      </aside>
      <div class="chat-box simple-card">
      <div class="plugin-bar">
        <div class="assistant-avatar">AI</div>
        <div class="assistant-meta"><strong>{{ assistantName }}</strong><span>{{ capabilityHint }}</span></div>
      </div>

      <div ref="boxRef" class="messages">
        <div v-if="messages.length === 0" class="empty">
          <div class="welcome-mark">✦</div>
          <h3>你好，我是{{ assistantName }}</h3>
          <p>{{ welcomeHint }}</p>
          <div class="sample-grid">
            <button v-for="q in samples" :key="q" type="button" class="sample" @click="ask(q)"><span>↗</span>{{ q }}</button>
          </div>
        </div>
        <div v-for="(msg, i) in messages" :key="i" class="msg" :class="msg.role">
          <div v-if="msg.role === 'assistant'" class="assistant-content">
            <div v-if="msg.processSteps?.length" class="process-card" :class="{ collapsed: msg.processExpanded === false }">
              <button type="button" class="process-header" @click="toggleProcessGraph(msg)">
                <span class="process-title"><span class="process-title-mark">⚙</span>执行过程</span>
                <span class="process-header-state">系统状态 · {{ processSummary(msg.processSteps, msg.processActive === true) }}</span>
                <span class="process-toggle">{{ msg.processExpanded === false ? '展开' : '收起' }}</span>
              </button>
              <div v-show="msg.processExpanded !== false" class="process-graph-viewport" role="region" aria-label="执行流程图" tabindex="0">
                <div class="process-graph-surface">
                  <div class="process-graph-toolbar">
                    <span class="process-graph-caption"><i></i>单 Agent · 任务链路</span>
                    <span class="process-graph-step-count">{{ msg.processSteps.length }} 个任务步骤</span>
                  </div>
                  <div class="process-graph" role="list" aria-label="执行步骤">
                    <template v-for="graph in [projectExecutionGraph(msg.processSteps, msg.processActive === true, msg.failed)]" :key="i">
                      <template v-for="(node, nodeIndex) in graph.nodes" :key="node.id">
                        <div class="process-node" role="listitem" :class="`process-${node.status.toLowerCase()}`">
                          <button v-if="node.details?.length" type="button" class="process-node-main process-node-action" :aria-expanded="isProcessStepExpanded(msg, node)" :aria-label="`${node.message}，${processStatusLabel(node.status)}，${isProcessStepExpanded(msg, node) ? '收起详情' : '展开详情'}`" @click="toggleProcessStep(msg, node, $event)">
                            <span class="process-node-topline">
                              <span class="process-node-sequence">任务 {{ String(nodeIndex + 1).padStart(2, '0') }}</span>
                              <span class="process-step-icon">{{ processStatusIcon(node.status) }}</span>
                              <span class="process-step-status">{{ processStatusLabel(node.status) }}</span>
                            </span>
                            <span class="process-node-message">{{ node.message }}</span>
                            <span v-if="node.resultCount !== null && node.resultCount !== undefined" class="process-step-count">{{ node.resultCount }} 条结果</span>
                          </button>
                          <div v-else class="process-node-main">
                            <span class="process-node-topline">
                              <span class="process-node-sequence">任务 {{ String(nodeIndex + 1).padStart(2, '0') }}</span>
                              <span class="process-step-icon">{{ processStatusIcon(node.status) }}</span>
                              <span class="process-step-status">{{ processStatusLabel(node.status) }}</span>
                            </span>
                            <span class="process-node-message">{{ node.message }}</span>
                            <span v-if="node.resultCount !== null && node.resultCount !== undefined" class="process-step-count">{{ node.resultCount }} 条结果</span>
                          </div>
                          <div v-if="node.details?.length && isProcessStepExpanded(msg, node)" class="process-details">
                            <div v-for="(detail, detailIndex) in node.details" :key="`${detail.kind}-${detail.chunkId || detail.documentId || detailIndex}`" class="process-detail">
                              <div class="process-detail-heading">
                                <span class="process-detail-kind">{{ detail.title || detailKindLabel(detail.kind) }}</span>
                                <span v-if="detail.rank" class="process-detail-rank">#{{ detail.rank }}</span>
                              </div>
                              <div v-if="detail.kind === 'ACTOR' || detail.kind === 'SCOPE'" class="process-detail-excerpt">{{ detail.excerpt }}</div>
                              <template v-else>
                                <div class="process-detail-source">{{ detail.documentName || detail.documentId || '未命名文档' }}</div>
                                <div v-if="detail.chunkId" class="process-detail-chunk">chunk {{ detail.chunkId }}</div>
                                <div class="process-detail-excerpt">{{ detail.excerpt }}</div>
                              </template>
                            </div>
                          </div>
                        </div>
                        <svg v-if="graph.edges[nodeIndex]" class="process-graph-edge" :class="{ 'is-on': graph.edges[nodeIndex].reached, active: graph.edges[nodeIndex].active }" viewBox="0 0 32 48" preserveAspectRatio="none" aria-hidden="true">
                          <path d="M16 0 C16 14 5 14 5 24 C5 35 27 34 16 48" pathLength="100" class="process-edge-base"></path>
                          <path v-if="graph.edges[nodeIndex].active" d="M16 0 C16 14 5 14 5 24 C5 35 27 34 16 48" class="process-edge-flow"></path>
                        </svg>
                      </template>
                    </template>
                  </div>
                </div>
              </div>
            </div>
            <div class="bubble markdown-body" :class="{ 'bubble-error': msg.failed }" v-html="renderMarkdown(msg.content)"></div>
            <ul v-if="msg.sources?.length" class="sources">
              <li v-for="(s, j) in msg.sources" :key="j">{{ sourceLabel(s) }}</li>
            </ul>
          </div>
          <div v-else class="bubble">{{ msg.content }}</div>
        </div>
        <div v-if="loading" class="msg assistant"><div class="assistant-avatar small">AI</div><div class="bubble typing"><i></i><i></i><i></i></div></div>
      </div>

      <form class="input-row" @submit.prevent="send">
        <el-input
          v-model="input"
          placeholder="输入问题，Enter 发送，↑↓ 翻阅本对话历史"
          :disabled="loading"
          @keydown="onComposerKeydown"
          @input="onComposerInput"
        />
        <el-button type="primary" :loading="loading" @click="send">发送</el-button>
      </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { harnessApi, type HarnessConfigView, type HarnessPluginDescriptor, type HarnessProgressEvent } from '@/api/harness'
import { aiChatHistoryApi, type ChatSession } from '@/api/aiChatHistory'
import { marked } from 'marked'
import { useAuthStore } from '@/stores/auth'
import { executionStepId, mergeExecutionHistory, projectExecutionGraph, upsertExecutionStep } from '@/utils/aiExecutionGraph'
interface UiMessage { role: 'user' | 'assistant'; content: string; sources?: Array<{ documentName?: string; excerpt?: string; text?: string; documentVersion?: number; knowledgeType?: string; effectiveFrom?: string | null; effectiveTo?: string | null } | string>; processSteps?: HarnessProgressEvent[]; processActive?: boolean; processExpanded?: boolean; processStepExpanded?: Record<string, boolean>; failed?: boolean }

const authStore = useAuthStore()
const availablePlugins = ref<HarnessPluginDescriptor[]>([])
const availableCapabilities = ref<NonNullable<HarnessConfigView['availableCapabilities']>>([])
const assistantName = ref('门店智能助手')
const enabledPlugins = ref<string[]>(['basic_chat'])
const sessionPlugins = ref<string[]>(['basic_chat'])
const messages = ref<UiMessage[]>([])
const sessions = ref<ChatSession[]>([])
const input = ref('')
/** 本对话已发送的用户提问（按时间顺序），供 ↑↓ 回溯 */
const sessionUserPrompts = computed(() =>
  messages.value.filter(item => item.role === 'user').map(item => item.content))
/** -1 表示未在历史浏览模式；0 为最近一次提问，越大越早 */
const inputHistoryIndex = ref(-1)
const inputDraftBeforeHistory = ref('')
const loading = ref(false)
const boxRef = ref<HTMLElement>()
const sessionStorageKey = computed(() => authStore.isPlatformAdmin
  ? `gymmind-ai-chat:platform:${authStore.userId ?? 'anonymous'}`
  : `gymmind-ai-chat:${authStore.tenantId ?? 'none'}:${authStore.userId ?? 'anonymous'}`)
const sessionId = ref(localStorage.getItem(sessionStorageKey.value + ':session') || `web-${Date.now()}`)

const platformSamples = [
  '全平台本月 AI 调用了多少次？',
  '有多少租户启用了 AI？',
  '现在有多少活跃租户？',
  '平台一共有多少租户？'
]
const platformStoreSamples = [
  '当前门店本月 AI 配额和用量是多少？',
  '当前门店是否已启用 AI？',
  '当前门店智能体和知识库配额用了多少？'
]
const roleProfile = computed(() => {
  if (authStore.isPlatformAdmin) {
    const samples = authStore.platformContextTenantId != null
      ? [...platformSamples, ...platformStoreSamples]
      : platformSamples
    return { name: '平台 AI 管理助手', samples }
  }
  if (authStore.roles.includes('GYM_ADMIN')) return {
    name: '门店运营助手',
    samples: [
      '上个月来了多少会员？',
      '和上上个月相比变化多少？',
      '哪些会员流失风险高？',
      '本月有哪些会员会籍快到期？',
      '最近预约到店率怎么样？'
    ]
  }
  if (authStore.roles.includes('COACH')) return {
    name: '教练工作助手',
    samples: [
      '我负责的会员最近训练得怎么样？',
      '本周会员约了哪些训练课？',
      '帮我看会员的训练计划排期',
      '这个动作有哪些要点和常见错误？',
      '给会员写一份本周训练建议'
    ]
  }
  return {
    name: '我的训练助手',
    samples: [
      '我的训练计划是什么？',
      '我本周还有哪些课？',
      '深蹲动作有哪些常见错误？',
      '新手每周训练几次比较合适？',
      '减脂期饮食要注意什么？'
    ]
  }
})
const samples = computed(() => roleProfile.value.samples)
const enabledCapabilityIds = computed(() => {
  const ids = new Set<string>()
  availablePlugins.value.forEach(plugin => ids.add(plugin.id))
  availableCapabilities.value.forEach(cap => ids.add(cap.id))
  return ids
})
const hasPlatformAiTool = computed(() => enabledCapabilityIds.value.has('platform_ai_admin_metrics'))
const hasStoreOpsTools = computed(() =>
  enabledCapabilityIds.value.has('gym_operations_metrics')
  || enabledCapabilityIds.value.has('member_churn_risk_list'))
const storeOpsContext = computed(() => authStore.roles.includes('GYM_ADMIN') && !authStore.isPlatformAdmin)
const dataToolsConnected = computed(() =>
  (authStore.isPlatformAdmin && hasPlatformAiTool.value)
  || (storeOpsContext.value && hasStoreOpsTools.value))
const capabilityHint = computed(() => {
  if (authStore.isPlatformAdmin && dataToolsConnected.value) return '可查询全平台 AI 用量，以及所选门店的配额和启用状态'
  if (dataToolsConnected.value) return '知识库与业务数据 Tool 按角色与权限自动匹配'
  if (authStore.isPlatformAdmin && authStore.platformContextTenantId == null) {
    return '可问平台 AI 用量；选顶栏门店后可看该店 AI 配额'
  }
  return '知识库按权限自动检索 · 回答经过权限校验'
})
const welcomeHint = computed(() => {
  if (authStore.isPlatformAdmin) {
    return authStore.platformContextTenantId != null
      ? '可查看全平台 AI 用量，以及当前门店的 AI 配额和启用状态。'
      : '可查看全平台 AI 用量和租户启用情况；选顶栏门店后可看该店 AI 配额。'
  }
  if (authStore.roles.includes('GYM_ADMIN')) {
    return '示例问题会优先调用门店运营与流失预警 Tool，也可咨询知识库制度与动作规范。'
  }
  if (authStore.roles.includes('COACH')) {
    return '可以看负责会员的训练、约课和计划，也可以问动作要点。'
  }
  return '可以问自己的训练计划和课程，也可以咨询动作与饮食。'
})
const scrollBottom = () => nextTick(() => { if (boxRef.value) boxRef.value.scrollTop = boxRef.value.scrollHeight })
const sourceLabel = (s: NonNullable<UiMessage['sources']>[number]) => {
  if (typeof s === 'string') return s
  const version = s.documentVersion ? ' v' + s.documentVersion : ''
  const dates = s.effectiveFrom || s.effectiveTo ? '（有效期：' + (s.effectiveFrom || '未注明') + ' 至 ' + (s.effectiveTo || '未注明') + '）' : ''
  return (s.documentName || '知识库片段') + version + dates + (s.excerpt ? '：' + s.excerpt : s.text ? '：' + s.text : '')
}
const renderMarkdown = (content: string) => {
  const html = String(marked.parse(content, { breaks: true }))
  const doc = new DOMParser().parseFromString(html, 'text/html')
  doc.querySelectorAll('script,style,iframe,object,embed,form').forEach(node => node.remove())
  doc.querySelectorAll('*').forEach(node => {
    Array.from(node.attributes).forEach(attribute => {
      if (attribute.name.toLowerCase().startsWith('on')) node.removeAttribute(attribute.name)
      if ((attribute.name === 'href' || attribute.name === 'src') && /^\s*javascript:/i.test(attribute.value)) node.removeAttribute(attribute.name)
    })
  })
  return doc.body.innerHTML
}
const sessionMessagesKey = (key: string) => `${sessionStorageKey.value}:messages:${key}`
const persistConversation = () => {
  localStorage.setItem(sessionStorageKey.value + ':session', sessionId.value)
  const persisted = messages.value.slice(-40).map(({ role, content, sources, processSteps, processExpanded, processStepExpanded, failed }) =>
    ({ role, content, sources, processSteps, processExpanded, processStepExpanded, failed }))
  localStorage.setItem(sessionStorageKey.value + ':messages', JSON.stringify(persisted))
  localStorage.setItem(sessionMessagesKey(sessionId.value), JSON.stringify(persisted))
}
const restoreConversation = () => {
  try {
    const raw = localStorage.getItem(sessionMessagesKey(sessionId.value)) || localStorage.getItem(sessionStorageKey.value + ':messages')
    const saved = raw ? JSON.parse(raw) : []
    if (Array.isArray(saved)) {
      messages.value = saved.filter(item => item && (item.role === 'user' || item.role === 'assistant') && typeof item.content === 'string')
        .map(item => ({
          ...item,
          processSteps: Array.isArray(item.processSteps) ? item.processSteps : undefined,
          processExpanded: item.processExpanded !== false,
          processStepExpanded: item.processStepExpanded ?? {},
          failed: Boolean(item.failed)
        }))
    }
  } catch { messages.value = [] }
}
const toggleProcessGraph = (message: UiMessage) => {
  message.processExpanded = message.processExpanded === false
  persistConversation()
}
const resetInputHistory = () => {
  inputHistoryIndex.value = -1
  inputDraftBeforeHistory.value = ''
}
let skipComposerInputHistoryReset = false
const setInputFromHistoryNav = (value: string) => {
  skipComposerInputHistoryReset = true
  input.value = value
  nextTick(() => { skipComposerInputHistoryReset = false })
}
const onComposerInput = () => {
  if (skipComposerInputHistoryReset) return
  if (inputHistoryIndex.value !== -1) resetInputHistory()
}
const onComposerKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    send()
    return
  }
  if (loading.value) return
  const history = sessionUserPrompts.value
  if (history.length === 0) return
  if (event.key === 'ArrowUp') {
    event.preventDefault()
    if (inputHistoryIndex.value === -1) {
      inputDraftBeforeHistory.value = input.value
      inputHistoryIndex.value = 0
    } else if (inputHistoryIndex.value < history.length - 1) {
      inputHistoryIndex.value += 1
    }
    setInputFromHistoryNav(history[history.length - 1 - inputHistoryIndex.value] ?? '')
  } else if (event.key === 'ArrowDown' && inputHistoryIndex.value !== -1) {
    event.preventDefault()
    if (inputHistoryIndex.value === 0) {
      setInputFromHistoryNav(inputDraftBeforeHistory.value)
      resetInputHistory()
    } else {
      inputHistoryIndex.value -= 1
      setInputFromHistoryNav(history[history.length - 1 - inputHistoryIndex.value] ?? '')
    }
  }
}
const loadSessions = async () => { try { sessions.value = (await aiChatHistoryApi.list()).data || [] } catch { sessions.value = [] } }
const loadSessionMessages = async () => {
  try {
    const result = await aiChatHistoryApi.messages(sessionId.value)
    const raw = localStorage.getItem(sessionMessagesKey(sessionId.value))
    const saved = raw ? JSON.parse(raw) : []
    messages.value = mergeExecutionHistory(result.data || [], Array.isArray(saved) ? saved : []).map(item => ({
      ...item,
      processSteps: Array.isArray(item.processSteps) ? item.processSteps : undefined,
      processExpanded: item.processExpanded !== false,
      processStepExpanded: item.processStepExpanded ?? {},
      failed: Boolean(item.failed)
    }))
  } catch { restoreConversation() }
}
const newConversation = async () => { messages.value = []; input.value = ''; resetInputHistory(); sessionPlugins.value = [...enabledPlugins.value]; sessionId.value = `web-${Date.now()}`; persistConversation(); try { await aiChatHistoryApi.create(sessionId.value, '新建对话'); await loadSessions() } catch { /* server creates it on first message */ } }
const selectSession = async (key: string) => { if (loading.value || key === sessionId.value) return; sessionId.value = key; localStorage.setItem(sessionStorageKey.value + ':session', key); resetInputHistory(); await loadSessionMessages(); scrollBottom() }
const deleteSession = async (key: string) => {
  try {
    await ElMessageBox.confirm('确定删除这条对话吗？删除后无法恢复。', '删除对话', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
    await aiChatHistoryApi.archive(key)
    if (key === sessionId.value) {
      messages.value = []
      input.value = ''
      resetInputHistory()
      sessionPlugins.value = [...enabledPlugins.value]
      sessionId.value = `web-${Date.now()}`
      persistConversation()
    }
    await loadSessions()
    ElMessage.success('对话已删除')
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.response?.data?.message || error?.message || '删除对话失败')
  }
}
const loadHarnessConfig = async () => {
  try {
    const res = await harnessApi.getConfig()
    const data = res.data
    availablePlugins.value = data.availablePlugins
    availableCapabilities.value = data.availableCapabilities ?? []
    assistantName.value = roleProfile.value.name
    enabledPlugins.value = data.enabledPlugins.length ? data.enabledPlugins : ['basic_chat']
    sessionPlugins.value = [...enabledPlugins.value]
  } catch {
    availablePlugins.value = [{ id: 'basic_chat', name: '基础对话', description: '直接调用大模型', category: 'CORE', permission: 'ai:chat', requiresTenant: false, defaultEnabled: true }]
    sessionPlugins.value = ['basic_chat']
  }
}

const ask = (text: string) => { input.value = text; send() }

const processStatusLabel = (status: HarnessProgressEvent['status']) => ({
  STARTED: '已启动', RUNNING: '进行中', COMPLETED: '已完成', FAILED: '失败'
}[status] || status)
const processStatusIcon = (status: HarnessProgressEvent['status']) => ({
  STARTED: '○', RUNNING: '…', COMPLETED: '✓', FAILED: '!'
}[status] || '·')
const processStepKey = (step: HarnessProgressEvent) => executionStepId(step)
const isProcessStepExpanded = (message: UiMessage, step: HarnessProgressEvent) => Boolean(message.processStepExpanded?.[processStepKey(step)])
const toggleProcessStep = (message: UiMessage, step: HarnessProgressEvent, event?: Event) => {
  if (!message.processStepExpanded) message.processStepExpanded = {}
  const key = processStepKey(step)
  message.processStepExpanded[key] = !message.processStepExpanded[key]
  const button = event?.currentTarget
  if (button instanceof HTMLElement) button.setAttribute('aria-expanded', String(message.processStepExpanded[key]))
  persistConversation()
}
const detailKindLabel = (kind: string) => ({ ACTOR: '身份', SCOPE: '范围', CHUNK: '片段', DOCUMENT: '文档' }[kind] || '明细')
const processSummary = (steps: HarnessProgressEvent[], isProcessing = false) => {
  const current = isProcessing ? [...steps].reverse().find(step => step.status === 'RUNNING') : undefined
  if (current) return current.message
  if (steps.some(step => step.status === 'FAILED')) return '执行失败'
  if (isProcessing && steps[steps.length - 1]?.status === 'STARTED') return '步骤已启动'
  return '已完成'
}

const send = async () => {
  const text = input.value.trim()
  if (!text || loading.value) return
  resetInputHistory()
  messages.value.push({ role: 'user', content: text }); input.value = ''; loading.value = true; scrollBottom()
  persistConversation()
  try {
    const message = reactive<UiMessage>({ role: 'assistant', content: '', sources: [], processSteps: [], processActive: true, processExpanded: true, processStepExpanded: {} })
    messages.value.push(message)
    await harnessApi.stream(
      { sessionId: sessionId.value, question: text, plugins: sessionPlugins.value,
        history: messages.value.slice(0, -1).slice(-10).map(item => ({ role: item.role, content: item.content })) },
      token => { message.content += token; persistConversation(); scrollBottom() },
      result => {
        // Some browsers/proxies may deliver the final SSE event without the
        // preceding token events. Always use the authoritative final answer.
        if (result.answer) message.content = result.answer
        message.sources = result.citations ?? result.sources ?? []
        persistConversation()
        scrollBottom()
      },
      progress => {
        if (message.processSteps) {
          const isNewStep = !message.processSteps.some(step =>
            step.pluginId === progress.pluginId && step.stage === progress.stage)
          message.processSteps = upsertExecutionStep(message.processSteps, progress)
          if (progress.details?.length && message.processStepExpanded && isNewStep) {
            message.processStepExpanded[processStepKey(progress)] = false
          }
        }
        persistConversation()
        scrollBottom()
      }
    )
    message.processActive = false
    persistConversation()
  } catch (error: any) {
    const messageText = error?.message || '发送失败，请稍后重试'
    const last = messages.value[messages.value.length - 1]
    if (last?.role === 'assistant') {
      last.processActive = false
      if (!last.content) last.content = messageText
      else if (!last.content.includes(messageText)) last.content = `${last.content}\n\n**错误：** ${messageText}`
      last.failed = true
      last.processExpanded = true
      if (!last.processSteps) last.processSteps = []
      last.processSteps.push({
        pluginId: 'harness',
        stage: 'chat_error',
        status: 'FAILED',
        message: messageText
      })
      persistConversation()
    }
    ElMessage.error(messageText)
  } finally { loading.value = false; scrollBottom() }
}

onMounted(async () => {
  restoreConversation()
  await loadHarnessConfig()
  await loadSessions()
  if (sessions.value.some(item => item.sessionKey === sessionId.value)) await loadSessionMessages()
})
</script>

<style scoped>
.chat-layout { display:grid; grid-template-columns:220px minmax(0,1fr); gap:12px; align-items:stretch; }
.history-panel { display:flex; flex-direction:column; height:calc(100vh - 132px); min-height:470px; padding:12px; overflow:hidden; }
.history-heading { display:flex; align-items:center; justify-content:space-between; gap:8px; margin-bottom:10px; font-size:13px; flex:none; }
.history-list { flex:1; min-height:0; overflow-y:auto; }
.history-item { display:flex; align-items:center; gap:5px; width:100%; min-width:0; padding:5px 6px 5px 10px; border-radius:6px; background:transparent; color:var(--text); text-align:left; cursor:pointer; font-size:12px; }
.history-title { min-width:0; flex:1; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.history-delete { flex:none; width:24px; height:24px; padding:0; opacity:0; transition:opacity .15s ease; }
.history-item:hover .history-delete, .history-item.active .history-delete, .history-delete:focus-visible { opacity:1; }
.history-item:hover, .history-item.active { background:var(--primary-soft); color:var(--primary); border:1px solid rgba(34,197,94,.25); }
.chat-box { display: flex; flex-direction: column; height: calc(100vh - 132px); min-height: 470px; padding: 0; overflow: hidden; border:1px solid var(--border); box-shadow:var(--shadow-card); }
.plugin-bar { padding: 14px 18px; border-bottom: 1px solid var(--border); background:var(--surface-muted); display: flex; align-items: center; gap: 11px; }
.assistant-avatar { width:34px; height:34px; display:grid; place-items:center; border-radius:10px; background:linear-gradient(145deg,var(--primary),#16a34a); color:#052e16; font-size:12px; font-weight:800; letter-spacing:.5px; box-shadow:0 4px 14px var(--primary-glow); flex:none; }
.assistant-avatar.small { width:26px; height:26px; border-radius:8px; font-size:10px; margin:5px 8px 0 0; }
.assistant-meta { display:flex; flex-direction:column; gap:3px; font-size:13px; }
.assistant-meta span { color:var(--text-muted); font-size:11px; }
.messages { flex: 1; overflow-y: auto; padding: 28px clamp(18px,5vw,58px); background:linear-gradient(180deg,var(--bg-elevated) 0%,var(--surface-muted) 100%); }
.empty { text-align: center; color: var(--text-muted); padding-top: clamp(20px,7vh,70px); max-width:720px; margin:auto; }
.welcome-mark { width:52px; height:52px; display:grid; place-items:center; margin:0 auto 15px; border-radius:16px; color:#052e16; font-size:25px; background:linear-gradient(145deg,var(--primary),#16a34a); box-shadow:0 10px 28px var(--primary-glow); }
.empty h3 { margin:0 0 8px; color:var(--text); font-size:20px; }
.empty p { margin:0 auto 24px; font-size:13px; }
.sample-grid { display:grid; grid-template-columns:repeat(3,1fr); gap:10px; }
.sample { min-height:70px; padding:13px 14px; border:1px solid var(--border); border-radius:10px; background:var(--surface); color:var(--text); cursor:pointer; font-size:13px; text-align:left; line-height:1.45; box-shadow:0 4px 12px rgba(0,0,0,.2); transition:.18s ease; }
.sample span { display:block; color:var(--primary); font-size:16px; margin-bottom:5px; }
.sample:hover { border-color:rgba(34,197,94,.5); transform:translateY(-2px); box-shadow:0 8px 20px rgba(34,197,94,.15); }
.msg { margin-bottom: 12px; display: flex; flex-direction: column; }
.msg.user { align-items: flex-end; }
.msg.user .bubble { background: var(--primary); color: #052e16; font-weight: 500; }
.msg.assistant { flex-direction:row; align-items:flex-start; }
.assistant-content { max-width: 85%; min-width: 0; }
.msg.assistant .bubble { background: var(--surface); border: 1px solid var(--border); border-radius:4px 14px 14px 14px; box-shadow:0 4px 14px rgba(0,0,0,.25); }
.msg.assistant .bubble-error { border-color: rgba(248,113,113,.45); background: rgba(248,113,113,.1); color: #fecaca; }
.bubble { max-width: 85%; padding: 10px 14px; border-radius: 12px; line-height: 1.5; white-space: pre-wrap; font-size: 14px; }
.markdown-body { white-space: normal; overflow-wrap: anywhere; }
.markdown-body :deep(p) { margin: 0 0 10px; }
.markdown-body :deep(p:last-child) { margin-bottom: 0; }
.markdown-body :deep(h1), .markdown-body :deep(h2), .markdown-body :deep(h3) { margin: 14px 0 8px; line-height: 1.3; }
.markdown-body :deep(h1) { font-size: 20px; }
.markdown-body :deep(h2) { font-size: 17px; }
.markdown-body :deep(h3) { font-size: 15px; }
.markdown-body :deep(ul), .markdown-body :deep(ol) { margin: 8px 0; padding-left: 22px; }
.markdown-body :deep(li) { margin: 4px 0; }
.markdown-body :deep(blockquote) { margin: 10px 0; padding: 8px 12px; border-left: 3px solid var(--primary); color: var(--text-muted); background: var(--surface); }
.markdown-body :deep(code) { padding: 2px 5px; border-radius: 4px; background: var(--surface); font-family: ui-monospace, SFMono-Regular, Consolas, monospace; font-size: 12px; }
.markdown-body :deep(pre) { overflow-x: auto; margin: 10px 0; padding: 10px 12px; border-radius: 6px; background: #1f2937; color: #f8fafc; }
.markdown-body :deep(pre code) { padding: 0; background: transparent; color: inherit; }
.markdown-body :deep(table) { width: 100%; margin: 10px 0; border-collapse: collapse; font-size: 13px; }
.markdown-body :deep(th), .markdown-body :deep(td) { padding: 6px 8px; border: 1px solid var(--border); text-align: left; }
.typing { color: var(--text-muted); display:flex; gap:4px; align-items:center; min-width:42px; }
.typing i { width:5px; height:5px; border-radius:50%; background:var(--text-dim); animation:pulse 1.1s infinite ease-in-out; }
.typing i:nth-child(2){animation-delay:.15s}.typing i:nth-child(3){animation-delay:.3s}
@keyframes pulse { 0%,80%,100%{opacity:.3;transform:translateY(0)} 40%{opacity:1;transform:translateY(-3px)} }
.sources { margin: 6px 0 0; padding-left: 18px; font-size: 12px; color: var(--text-muted); }
.process-card { margin-bottom: 8px; border: 1px solid var(--border); border-left: 3px solid var(--text-dim); border-radius: 8px; background: var(--surface-muted); color: var(--text-muted); font-size: 11px; overflow: hidden; }
.process-header { width: 100%; display:flex; align-items:center; gap:8px; padding: 8px 10px; border:0; background: transparent; color: inherit; cursor:pointer; text-align:left; }
.process-title { color:var(--text); font-weight:700; white-space:nowrap; }
.process-title-mark { display:inline-grid; place-items:center; width:17px; height:17px; margin-right:4px; border-radius:50%; background:var(--surface-hover); color:var(--text-muted); font-size:10px; }
.process-header-state { flex:1; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; color:var(--text-dim); }
.process-toggle { color:var(--text-muted); white-space:nowrap; }
.process-graph-viewport { max-width:100%; max-height:clamp(360px, 62vh, 680px); overflow-x:hidden; overflow-y:auto; border-top:1px solid var(--border); scrollbar-color:var(--border-strong) transparent; }
.process-graph-surface { position:relative; width:100%; min-height:100%; overflow:hidden; padding:20px 16px 26px; border:1px solid #283242; border-radius:10px; background-color:#0b111b; background-image:radial-gradient(rgba(135,153,179,.14) 1px, transparent 1px), radial-gradient(ellipse at 50% 0%, rgba(52,91,145,.16), transparent 58%); background-size:18px 18px, 100% 100%; }
.process-graph-toolbar { display:flex; align-items:center; justify-content:space-between; gap:12px; width:min(100%, 720px); margin:0 auto 14px; color:#8b9ab0; font-size:10px; letter-spacing:.06em; }
.process-graph-caption { display:flex; align-items:center; gap:8px; color:#b9c7da; font-size:11px; font-weight:650; letter-spacing:0; }
.process-graph-caption i { width:7px; height:7px; border-radius:50%; background:#65a9ff; box-shadow:0 0 10px rgba(101,169,255,.7); }
.process-graph-step-count { color:#73839a; white-space:nowrap; }
.process-graph { display:flex; width:100%; flex-direction:column; align-items:center; gap:0; }
.process-node { width:min(82%, 640px); min-height:108px; overflow:hidden; border:1px solid #cbd5e1; border-radius:10px; background:#f8fafc; color:#1e293b; box-shadow:0 8px 22px rgba(0,0,0,.32); animation:process-node-enter .4s cubic-bezier(.22,1,.36,1) both; transition:border-color .38s ease, box-shadow .38s ease, transform .38s cubic-bezier(.22,1,.36,1), background-color .38s ease; }
@keyframes process-node-enter { from { opacity:0; transform:translateY(8px) scale(.97); } to { opacity:1; transform:translateY(0) scale(1); } }
.process-node-main { display:flex; width:100%; min-height:116px; flex-direction:column; align-items:flex-start; gap:10px; padding:12px; border:0; background:transparent; color:inherit; text-align:left; }
.process-node-action { cursor:pointer; }
.process-node-action:hover { background:var(--surface-hover); }
.process-node-topline { display:flex; width:100%; align-items:center; gap:8px; }
.process-node-sequence { margin-right:auto; color:#8290a4; font-size:10px; font-weight:700; letter-spacing:.08em; }
.process-node-message { display:-webkit-box; width:100%; overflow:hidden; color:#172235; font-size:12px; font-weight:650; line-height:1.45; overflow-wrap:anywhere; -webkit-box-orient:vertical; -webkit-line-clamp:3; }
.process-graph-edge { display:block; flex:none; width:32px; height:48px; overflow:visible; }
.process-edge-base, .process-edge-flow { fill:none; stroke-linecap:round; vector-effect:non-scaling-stroke; }
.process-edge-base { stroke:var(--border-strong); stroke-width:1.6px; stroke-dasharray:100; stroke-dashoffset:100; transition:stroke-dashoffset .7s cubic-bezier(.4,0,.2,1), stroke .4s; }
.process-graph-edge.is-on .process-edge-base { stroke-dashoffset:0; }
.process-graph-edge.active .process-edge-base { stroke:rgba(56,189,248,.42); }
.process-edge-flow { stroke:#7dd3fc; stroke-width:2.4px; stroke-dasharray:3 11; animation:process-edge-flow 1.1s linear infinite; }
@keyframes process-edge-flow { to { stroke-dashoffset:-28px; } }
.process-step-icon { display:inline-grid; place-items:center; width:16px; height:16px; border-radius:50%; background:var(--surface-hover); color:var(--text-muted); font-size:11px; font-weight:700; transition:background-color .18s ease, color .18s ease, box-shadow .18s ease; }
.process-step-count, .process-step-status { color:#64748b; white-space:nowrap; }
.process-node.process-started, .process-node.process-running { border-color:#65a9ff; background:#112744; color:#eaf3ff; box-shadow:0 0 0 1px rgba(101,169,255,.34), 0 12px 34px rgba(50,120,220,.2); }
.process-node.process-running .process-node-message, .process-node.process-started .process-node-message { color:#e0f2fe; }
.process-node.process-started .process-node-sequence, .process-node.process-running .process-node-sequence { color:#9ac7ff; }
.process-node.process-running .process-step-icon, .process-node.process-started .process-step-icon { background:rgba(101,169,255,.16); color:#8cc2ff; }
.process-node.process-running .process-step-status, .process-node.process-started .process-step-status { color:#9ac7ff; }
.process-node.process-completed { border-color:rgba(101,169,255,.52); background:#dbeafe; color:#1e293b; box-shadow:0 0 0 1px rgba(101,169,255,.12), 0 8px 24px rgba(0,0,0,.22); }
.process-node.process-completed .process-node-message { color:#1e293b; }
.process-node.process-completed .process-node-sequence { color:#4e6e98; }
.process-node.process-completed .process-step-icon { background:var(--primary-soft); color:var(--primary); }
.process-node.process-failed { border-color:var(--danger); }
.process-node.process-failed .process-step-icon { background:rgba(248,113,113,.15); color:var(--danger); }
.process-details { max-height:220px; overflow-y:auto; padding:0 10px 8px; border-top:1px solid var(--border); background:var(--surface); }
.process-detail { padding:5px 0; border-bottom:1px solid var(--border); line-height:1.4; }
.process-detail:last-child { border-bottom:0; }
.process-detail-heading { display:flex; justify-content:space-between; gap:8px; color:var(--text); font-weight:700; }
.process-detail-kind { min-width:0; }
.process-detail-rank { color:var(--text-dim); font-weight:600; }
.process-detail-source, .process-detail-chunk { color:var(--text-muted); overflow-wrap:anywhere; }
.process-detail-chunk { font-family:ui-monospace,SFMono-Regular,Consolas,monospace; font-size:10px; }
.process-detail-excerpt { color:var(--text-muted); white-space:pre-wrap; overflow-wrap:anywhere; }
.input-row { display: flex; gap: 8px; padding: 12px 16px; border-top: 1px solid var(--border); background:var(--surface-muted); }
@media (max-width: 850px) { .chat-layout { grid-template-columns:1fr; } .history-panel { height:auto; max-height:220px; min-height:0; } .history-list { display:flex; gap:6px; overflow:auto; } .history-heading { flex-direction:column; align-items:flex-start; } .history-item { min-width:150px; width:auto; flex:none; } }
@media (prefers-reduced-motion: reduce) { .process-node { animation:none; } .process-edge-base { transition:none; } .process-edge-flow { animation:none; } }
@media (max-width: 700px) { .chat-box { height: calc(100vh - 150px); } .sample-grid { grid-template-columns:1fr; } .process-graph-surface { padding:16px 10px 20px; } .process-node { width:96%; } .process-graph-toolbar { margin-bottom:12px; } }
</style>

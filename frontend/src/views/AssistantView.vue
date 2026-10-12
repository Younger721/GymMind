<template>
  <div class="assistant">
    <div class="page-heading">
      <div>
        <div class="eyebrow">GYMMIND INTELLIGENCE</div>
        <h2 class="simple-page-title">AI 中心</h2>
        <p class="simple-page-desc">让每一次训练咨询，都有可靠、清晰、可追溯的答案。</p>
      </div>
      <div class="status-pill"><span class="status-dot"></span>助手在线</div>
    </div>

    <div class="chat-layout">
      <aside class="history-panel simple-card">
        <div class="history-heading"><strong>最近对话</strong><el-button size="small" type="primary" plain @click="newConversation">新建对话</el-button></div>
        <div v-for="item in sessions" :key="item.sessionKey" class="history-item" :class="{active:item.sessionKey===sessionId}" @click="selectSession(item.sessionKey)">
          <span class="history-title">{{ item.title }}</span>
          <el-tooltip content="删除对话" placement="top">
            <el-button class="history-delete" link type="danger" aria-label="删除对话" @click.stop="deleteSession(item.sessionKey)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
      </aside>
      <div class="chat-box simple-card">
      <div class="plugin-bar">
        <div class="assistant-avatar">AI</div>
        <div class="assistant-meta"><strong>{{ assistantName }}</strong><span>{{ isGymAdmin && operationsAvailable ? '知识库与门店数据按权限自动匹配' : '知识库按权限自动检索 · 回答经过权限校验' }}</span></div>
        <el-tag v-if="operationsAvailable && isGymAdmin" type="success" effect="plain" class="operations-skill">
          门店数据已连接
        </el-tag>
        <router-link v-if="canConfigure" :to="configLink" class="config-link">AI 配置</router-link>
      </div>

      <div ref="boxRef" class="messages">
        <div v-if="messages.length === 0" class="empty">
          <div class="welcome-mark">✦</div>
          <h3>你好，我是你的健身助手</h3>
          <p>可以咨询训练、饮食和动作规范，也可以直接选择一个问题开始。</p>
          <div class="sample-grid">
            <button v-for="q in samples" :key="q" type="button" class="sample" @click="ask(q)"><span>↗</span>{{ q }}</button>
          </div>
        </div>
        <div v-for="(msg, i) in messages" :key="i" class="msg" :class="msg.role">
          <div v-if="msg.role === 'assistant'" class="assistant-content">
            <div v-if="msg.processSteps?.length" class="process-card" :class="{ collapsed: msg.processExpanded === false }">
              <button type="button" class="process-header" @click="msg.processExpanded = msg.processExpanded === false">
                <span class="process-title"><span class="process-title-mark">⚙</span>执行过程</span>
                <span class="process-header-state">系统状态 · {{ processSummary(msg.processSteps) }}</span>
                <span class="process-toggle">{{ msg.processExpanded === false ? '展开' : '收起' }}</span>
              </button>
              <div v-show="msg.processExpanded !== false" class="process-list">
                <div v-for="step in msg.processSteps" :key="`${step.pluginId}-${step.stage}`" class="process-step" :class="`process-${step.status.toLowerCase()}`">
                  <button v-if="step.details?.length" type="button" class="process-step-row process-step-expand" @click="toggleProcessStep(msg, step)">
                    <span class="process-step-chevron">{{ isProcessStepExpanded(msg, step) ? '⌄' : '›' }}</span>
                    <span class="process-step-icon">{{ processStatusIcon(step.status) }}</span>
                    <span class="process-step-message">{{ step.message }}</span>
                    <span v-if="step.resultCount !== null && step.resultCount !== undefined" class="process-step-count">{{ step.resultCount }} 条</span>
                    <span class="process-step-status">{{ processStatusLabel(step.status) }}</span>
                  </button>
                  <div v-else class="process-step-row">
                    <span class="process-step-icon">{{ processStatusIcon(step.status) }}</span>
                    <span class="process-step-message">{{ step.message }}</span>
                    <span v-if="step.resultCount !== null && step.resultCount !== undefined" class="process-step-count">{{ step.resultCount }} 条</span>
                    <span class="process-step-status">{{ processStatusLabel(step.status) }}</span>
                  </div>
                  <div v-if="step.details?.length && isProcessStepExpanded(msg, step)" class="process-details">
                    <div v-for="(detail, detailIndex) in step.details" :key="`${detail.kind}-${detail.chunkId || detail.documentId || detailIndex}`" class="process-detail">
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
        <el-input v-model="input" placeholder="输入问题，Enter 发送" :disabled="loading" @keydown.enter.exact.prevent="send" />
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
import { harnessApi, type HarnessPluginDescriptor, type HarnessProgressEvent } from '@/api/harness'
import { aiChatHistoryApi, type ChatSession } from '@/api/aiChatHistory'
import { marked } from 'marked'
import { useAuthStore } from '@/stores/auth'
import { hasPermission } from '@/utils/permissions'
import { MENU_CODES } from '@/utils/menuPermissions'

interface UiMessage { role: 'user' | 'assistant'; content: string; sources?: Array<{ documentName?: string; excerpt?: string; text?: string; documentVersion?: number; knowledgeType?: string; effectiveFrom?: string | null; effectiveTo?: string | null } | string>; processSteps?: HarnessProgressEvent[]; processExpanded?: boolean; processStepExpanded?: Record<string, boolean>; failed?: boolean }

const authStore = useAuthStore()
const availablePlugins = ref<HarnessPluginDescriptor[]>([])
const assistantName = ref('门店智能助手')
const enabledPlugins = ref<string[]>(['basic_chat'])
const sessionPlugins = ref<string[]>(['basic_chat'])
const messages = ref<UiMessage[]>([])
const sessions = ref<ChatSession[]>([])
const input = ref('')
const loading = ref(false)
const boxRef = ref<HTMLElement>()
const sessionStorageKey = computed(() => `gymmind-ai-chat:${authStore.tenantId ?? `platform-${authStore.platformContextTenantId ?? 'none'}`}:${authStore.userId ?? 'anonymous'}`)
const sessionId = ref(localStorage.getItem(sessionStorageKey.value + ':session') || `web-${Date.now()}`)

const canConfigure = computed(() => authStore.roles.includes('GYM_ADMIN')
  && !authStore.isPlatformAdmin
  && hasPermission(MENU_CODES.AI_CONFIG))
const configLink = '/agents/manage'
const roleProfile = computed(() => {
  if (authStore.isPlatformAdmin) return { name: '平台 AI 管理助手', samples: ['查看 AI 使用情况', '检查租户 AI 状态'] }
  if (authStore.roles.includes('GYM_ADMIN')) return { name: '门店运营助手', samples: ['上个月来了多少会员？', '和上上个月相比变化多少？', '最近哪些会员活跃度下降？', '本月有哪些会员会籍快到期？', '最近预约到店率怎么样？'] }
  if (authStore.roles.includes('COACH')) return { name: '教练工作助手', samples: ['给会员制定训练建议', '查看训练动作要点'] }
  return {
    name: '我的训练助手',
    samples: [
      '深蹲动作有哪些常见错误？',
      '新手每周训练几次比较合适？',
      '史密斯机怎么用才安全？',
      '减脂期饮食要注意什么？'
    ]
  }
})
const samples = computed(() => roleProfile.value.samples)
const isGymAdmin = computed(() => authStore.roles.includes('GYM_ADMIN') && !authStore.isPlatformAdmin)
const operationsAvailable = computed(() => availablePlugins.value.some(plugin => plugin.id === 'gym_operations_metrics'))
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
const persistConversation = () => {
  localStorage.setItem(sessionStorageKey.value + ':session', sessionId.value)
  const persisted = messages.value.slice(-40).map(({ role, content, sources, processSteps, processExpanded, failed }) =>
    ({ role, content, sources, processSteps, processExpanded, failed }))
  localStorage.setItem(sessionStorageKey.value + ':messages', JSON.stringify(persisted))
}
const restoreConversation = () => {
  try {
    const raw = localStorage.getItem(sessionStorageKey.value + ':messages')
    const saved = raw ? JSON.parse(raw) : []
    if (Array.isArray(saved)) {
      messages.value = saved.filter(item => item && (item.role === 'user' || item.role === 'assistant') && typeof item.content === 'string')
        .map(item => ({
          ...item,
          processSteps: Array.isArray(item.processSteps) ? item.processSteps : undefined,
          processExpanded: item.processExpanded !== false,
          failed: Boolean(item.failed)
        }))
    }
  } catch { messages.value = [] }
}
const loadSessions = async () => { try { sessions.value = (await aiChatHistoryApi.list()).data || [] } catch { sessions.value = [] } }
const loadSessionMessages = async () => { try { const result = await aiChatHistoryApi.messages(sessionId.value); messages.value = (result.data || []).map(item => ({ role: item.role, content: item.content })) } catch { restoreConversation() } }
const newConversation = async () => { messages.value = []; input.value = ''; sessionPlugins.value = ['basic_chat']; sessionId.value = `web-${Date.now()}`; persistConversation(); try { await aiChatHistoryApi.create(sessionId.value, '新建对话'); await loadSessions() } catch { /* server creates it on first message */ } }
const selectSession = async (key: string) => { if (loading.value || key === sessionId.value) return; sessionId.value = key; localStorage.setItem(sessionStorageKey.value + ':session', key); await loadSessionMessages(); scrollBottom() }
const deleteSession = async (key: string) => {
  try {
    await ElMessageBox.confirm('确定删除这条对话吗？删除后无法恢复。', '删除对话', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
    await aiChatHistoryApi.archive(key)
    if (key === sessionId.value) {
      messages.value = []
      input.value = ''
      sessionPlugins.value = ['basic_chat']
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
    assistantName.value = roleProfile.value.name
    enabledPlugins.value = data.enabledPlugins.length ? data.enabledPlugins : ['basic_chat']
    sessionPlugins.value = ['basic_chat']
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
const processStepKey = (step: HarnessProgressEvent) => `${step.pluginId}-${step.stage}`
const isProcessStepExpanded = (message: UiMessage, step: HarnessProgressEvent) => Boolean(message.processStepExpanded?.[processStepKey(step)])
const toggleProcessStep = (message: UiMessage, step: HarnessProgressEvent) => {
  if (!message.processStepExpanded) message.processStepExpanded = {}
  const key = processStepKey(step)
  message.processStepExpanded[key] = !message.processStepExpanded[key]
}
const detailKindLabel = (kind: string) => ({ ACTOR: '身份', SCOPE: '范围', CHUNK: '片段', DOCUMENT: '文档' }[kind] || '明细')
const processSummary = (steps: HarnessProgressEvent[]) => {
  const current = [...steps].reverse().find(step => step.status === 'RUNNING')
  if (current) return current.message
  if (steps.some(step => step.status === 'FAILED')) return '执行失败'
  return '已完成'
}

const send = async () => {
  const text = input.value.trim()
  if (!text || loading.value) return
  messages.value.push({ role: 'user', content: text }); input.value = ''; loading.value = true; scrollBottom()
  persistConversation()
  try {
    const message = reactive<UiMessage>({ role: 'assistant', content: '', sources: [], processSteps: [], processExpanded: true, processStepExpanded: {} })
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
        const index = message.processSteps?.findIndex(step => step.pluginId === progress.pluginId && step.stage === progress.stage) ?? -1
        if (index >= 0 && message.processSteps) message.processSteps[index] = progress
        else {
          message.processSteps?.push(progress)
          if (progress.details?.length && message.processStepExpanded) {
            message.processStepExpanded[processStepKey(progress)] = false
          }
        }
        scrollBottom()
      }
    )
  } catch (error: any) {
    const messageText = error?.message || '发送失败，请稍后重试'
    const last = messages.value[messages.value.length - 1]
    if (last?.role === 'assistant') {
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
.page-heading { display:flex; align-items:flex-start; justify-content:space-between; margin-bottom:18px; }
.eyebrow { color:var(--primary); font-size:11px; font-weight:700; letter-spacing:1.5px; margin-bottom:7px; }
.status-pill { display:flex; align-items:center; gap:7px; color:#277a5c; background:#ecfdf5; border:1px solid #c9f1df; border-radius:999px; padding:7px 12px; font-size:12px; font-weight:600; }
.status-dot { width:7px; height:7px; border-radius:50%; background:#21b77a; box-shadow:0 0 0 3px #c9f1df; }
.chat-layout { display:grid; grid-template-columns:220px minmax(0,1fr); gap:12px; align-items:stretch; }
.history-panel { padding:12px; min-height:470px; }
.history-heading { display:flex; align-items:center; justify-content:space-between; gap:8px; margin-bottom:10px; font-size:13px; }
.history-item { display:flex; align-items:center; gap:5px; width:100%; min-width:0; padding:5px 6px 5px 10px; border-radius:6px; background:transparent; color:var(--text); text-align:left; cursor:pointer; font-size:12px; }
.history-title { min-width:0; flex:1; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.history-delete { flex:none; width:24px; height:24px; padding:0; opacity:0; transition:opacity .15s ease; }
.history-item:hover .history-delete, .history-item.active .history-delete, .history-delete:focus-visible { opacity:1; }
.history-item:hover, .history-item.active { background:#ecfdf5; color:var(--primary); }
.chat-box { display: flex; flex-direction: column; height: calc(100vh - 240px); min-height: 470px; padding: 0; overflow: hidden; border:1px solid #dfe5eb; box-shadow:0 8px 28px rgba(27,43,61,.06); }
.plugin-bar { padding: 14px 18px; border-bottom: 1px solid #edf0f3; background:#fff; display: flex; align-items: center; gap: 11px; }
.assistant-avatar { width:34px; height:34px; display:grid; place-items:center; border-radius:10px; background:linear-gradient(145deg,#0d9f77,#087e63); color:#fff; font-size:12px; font-weight:800; letter-spacing:.5px; box-shadow:0 4px 10px rgba(8,126,99,.22); flex:none; }
.assistant-avatar.small { width:26px; height:26px; border-radius:8px; font-size:10px; margin:5px 8px 0 0; }
.assistant-meta { display:flex; flex-direction:column; gap:3px; font-size:13px; }
.assistant-meta span { color:var(--text-muted); font-size:11px; }
.config-link { margin-left: auto; font-size: 12px; }
.messages { flex: 1; overflow-y: auto; padding: 28px clamp(18px,5vw,58px); background:linear-gradient(180deg,#fbfcfd 0%,#fff 40%); }
.empty { text-align: center; color: var(--text-muted); padding-top: clamp(20px,7vh,70px); max-width:720px; margin:auto; }
.welcome-mark { width:52px; height:52px; display:grid; place-items:center; margin:0 auto 15px; border-radius:16px; color:#fff; font-size:25px; background:linear-gradient(145deg,#0d9f77,#075e50); box-shadow:0 10px 22px rgba(8,126,99,.2); }
.empty h3 { margin:0 0 8px; color:var(--text); font-size:20px; }
.empty p { margin:0 auto 24px; font-size:13px; }
.sample-grid { display:grid; grid-template-columns:repeat(3,1fr); gap:10px; }
.sample { min-height:70px; padding:13px 14px; border:1px solid #e1e7ec; border-radius:10px; background:#fff; color:var(--text); cursor:pointer; font-size:13px; text-align:left; line-height:1.45; box-shadow:0 3px 9px rgba(27,43,61,.04); transition:.18s ease; }
.sample span { display:block; color:var(--primary); font-size:16px; margin-bottom:5px; }
.sample:hover { border-color:var(--primary); transform:translateY(-2px); box-shadow:0 7px 16px rgba(8,126,99,.12); }
.msg { margin-bottom: 12px; display: flex; flex-direction: column; }
.msg.user { align-items: flex-end; }
.msg.user .bubble { background: var(--primary); color: #fff; }
.msg.assistant { flex-direction:row; align-items:flex-start; }
.assistant-content { max-width: 85%; min-width: 0; }
.msg.assistant .bubble { background: #fff; border: 1px solid #e1e7ec; border-radius:4px 14px 14px 14px; box-shadow:0 3px 10px rgba(27,43,61,.04); }
.msg.assistant .bubble-error { border-color: #f5c2c7; background: #fff5f5; color: #842029; }
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
.typing i { width:5px; height:5px; border-radius:50%; background:#8ba39d; animation:pulse 1.1s infinite ease-in-out; }
.typing i:nth-child(2){animation-delay:.15s}.typing i:nth-child(3){animation-delay:.3s}
@keyframes pulse { 0%,80%,100%{opacity:.3;transform:translateY(0)} 40%{opacity:1;transform:translateY(-3px)} }
.sources { margin: 6px 0 0; padding-left: 18px; font-size: 12px; color: var(--text-muted); }
.process-card { margin-bottom: 8px; border: 1px solid #dfe5eb; border-left: 3px solid #8fa2ad; border-radius: 8px; background: #f5f7f8; color: #586772; font-size: 11px; overflow: hidden; }
.process-header { width: 100%; display:flex; align-items:center; gap:8px; padding: 8px 10px; border:0; background: transparent; color: inherit; cursor:pointer; text-align:left; }
.process-title { color:#43535e; font-weight:700; white-space:nowrap; }
.process-title-mark { display:inline-grid; place-items:center; width:17px; height:17px; margin-right:4px; border-radius:50%; background:#dbe3e7; color:#637580; font-size:10px; }
.process-header-state { flex:1; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; color:#74838c; }
.process-toggle { color:#657782; white-space:nowrap; }
.process-list { padding: 0 10px 8px 12px; border-top:1px solid #e2e7ea; }
.process-step { display:block; min-height:26px; border-bottom:1px dashed #e2e7ea; }
.process-step:last-child { border-bottom:0; }
.process-step-row { width:100%; min-height:26px; display:flex; align-items:center; gap:7px; border:0; background:transparent; color:inherit; text-align:left; padding:0; }
.process-step-expand { cursor:pointer; }
.process-step-expand:hover { color:#277a5c; }
.process-step-chevron { width:10px; color:#73838c; font-size:15px; line-height:1; }
.process-step-icon { display:inline-grid; place-items:center; width:16px; height:16px; border-radius:50%; background:#dce4e8; color:#71818b; font-size:11px; font-weight:700; }
.process-step-message { flex:1; min-width:0; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.process-step-count, .process-step-status { color:#81909a; white-space:nowrap; }
.process-running .process-step-icon { background:#d7e9e2; color:#13855f; }
.process-completed .process-step-icon { background:#dff2e8; color:#13855f; }
.process-failed { color:#a34f4f; }
.process-failed .process-step-icon { background:#f5dede; color:#a34f4f; }
.process-details { margin:0 0 6px 27px; padding:6px 8px; border-left:2px solid #ccd8dc; background:#edf1f2; border-radius:4px; }
.process-detail { padding:5px 0; border-bottom:1px solid #dce4e7; line-height:1.4; }
.process-detail:last-child { border-bottom:0; }
.process-detail-heading { display:flex; justify-content:space-between; gap:8px; color:#536671; font-weight:700; }
.process-detail-kind { min-width:0; }
.process-detail-rank { color:#81909a; font-weight:600; }
.process-detail-source, .process-detail-chunk { color:#657782; overflow-wrap:anywhere; }
.process-detail-chunk { font-family:ui-monospace,SFMono-Regular,Consolas,monospace; font-size:10px; }
.process-detail-excerpt { color:#687983; white-space:pre-wrap; overflow-wrap:anywhere; }
.input-row { display: flex; gap: 8px; padding: 12px 16px; border-top: 1px solid var(--border); }
@media (max-width: 850px) { .chat-layout { grid-template-columns:1fr; } .history-panel { min-height:0; display:flex; gap:6px; overflow:auto; } .history-heading { flex:none; flex-direction:column; align-items:flex-start; } .history-item { min-width:150px; width:auto; } }
@media (max-width: 700px) { .chat-box { height: calc(100vh - 260px); } .page-heading { gap:10px; } .status-pill { padding:6px 9px; } .sample-grid { grid-template-columns:1fr; } }
</style>

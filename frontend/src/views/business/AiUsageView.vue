<template>
  <div class="usage-page">
    <div class="page-head">
      <div>
        <h2 class="simple-page-title">AI 用量</h2>
        <p class="simple-page-desc">查看真实模型 token、调用次数、耗时和估算费用。</p>
      </div>
    </div>

    <section class="filter-card simple-card">
      <el-date-picker v-model="filters.range" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" :clearable="false" />
      <el-select v-if="authStore.isPlatformAdmin" v-model="filters.tenantId" clearable filterable placeholder="全部门店" style="width: 190px">
        <el-option v-for="tenant in tenants" :key="tenant.id" :label="tenant.name" :value="tenant.id" />
      </el-select>
      <el-select v-model="filters.model" clearable filterable placeholder="全部模型" style="width: 180px">
        <el-option v-for="model in models" :key="model" :label="model" :value="model" />
      </el-select>
      <el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 140px">
        <el-option label="成功" value="OK" /><el-option label="失败" value="ERROR" /><el-option label="无 usage" value="UNAVAILABLE" />
      </el-select>
      <el-button type="primary" :loading="loading" @click="reload">查询</el-button>
      <el-button :loading="loading" @click="resetFilters">重置</el-button>
    </section>

    <section class="summary-grid" v-loading="loading">
      <div class="summary-card"><span>调用次数</span><strong>{{ summary.calls }}</strong><small>成功率 {{ summary.successRate.toFixed(1) }}%</small></div>
      <div class="summary-card"><span>输入 token</span><strong>{{ formatNumber(summary.inputTokens) }}</strong><small>发送给模型</small></div>
      <div class="summary-card"><span>输出 token</span><strong>{{ formatNumber(summary.outputTokens) }}</strong><small>模型返回内容</small></div>
      <div class="summary-card"><span>总 token</span><strong>{{ formatNumber(summary.totalTokens) }}</strong><small>月度额度 {{ formatNumber(quota?.maxAiTokensPerMonth) }}</small></div>
      <div class="summary-card"><span>估算费用</span><strong>{{ formatCost(summary.estimatedCost) }}</strong><small>平均耗时 {{ Math.round(summary.averageLatencyMs) }} ms</small></div>
    </section>

    <section v-if="quota" class="quota-card simple-card">
      <div class="quota-head"><span>本月 token 额度</span><strong>{{ formatNumber(quota.usedAiTokensThisMonth) }} / {{ formatNumber(quota.maxAiTokensPerMonth) }}</strong></div>
      <el-progress :percentage="quotaPercent" :status="quotaPercent >= 90 ? 'warning' : undefined" />
    </section>

    <section class="trend-card simple-card">
      <div class="section-head"><h3>用量趋势</h3><span>按日汇总</span></div>
      <div v-if="trend.length" class="trend-list">
        <div v-for="point in trend" :key="point.date" class="trend-row">
          <span class="trend-date">{{ point.date }}</span><div class="trend-track"><i :style="{ width: `${trendWidth(point.totalTokens)}%` }" /></div><span class="trend-value">{{ formatNumber(point.totalTokens) }}</span>
        </div>
      </div>
      <el-empty v-else description="暂无趋势数据" :image-size="60" />
    </section>

    <section class="table-card simple-card">
      <div class="section-head"><h3>调用明细</h3><span>共 {{ total }} 条</span></div>
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="createdAt" label="时间" min-width="170"><template #default="{ row }">{{ formatDate(row.createdAt) }}</template></el-table-column>
        <el-table-column v-if="authStore.isPlatformAdmin" prop="tenantId" label="门店" min-width="130"><template #default="{ row }">{{ tenantLabel(row.tenantId) }}</template></el-table-column>
        <el-table-column prop="userId" label="用户 ID" width="90" />
        <el-table-column prop="feature" label="功能" min-width="110"><template #default="{ row }">{{ featureLabel(row.feature) }}</template></el-table-column>
        <el-table-column prop="model" label="模型" min-width="140" />
        <el-table-column prop="inputTokens" label="输入 token" width="110"><template #default="{ row }">{{ formatTokens(row.inputTokens) }}</template></el-table-column>
        <el-table-column prop="outputTokens" label="输出 token" width="110"><template #default="{ row }">{{ formatTokens(row.outputTokens) }}</template></el-table-column>
        <el-table-column prop="totalTokens" label="总 token" width="100"><template #default="{ row }">{{ formatTokens(row.totalTokens) }}</template></el-table-column>
        <el-table-column prop="estimatedCost" label="费用" width="100"><template #default="{ row }">{{ formatCost(row.estimatedCost, row.currency) }}</template></el-table-column>
        <el-table-column prop="latencyMs" label="耗时" width="100"><template #default="{ row }">{{ row.latencyMs }} ms</template></el-table-column>
        <el-table-column prop="status" label="状态" width="110"><template #default="{ row }"><el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status, row.usageSource) }}</el-tag></template></el-table-column>
      </el-table>
      <div class="pager"><el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[20, 50, 100]" layout="total, sizes, prev, pager, next" @current-change="load" @size-change="load" /></div>
    </section>

    <el-dialog v-model="pricingVisible" title="模型价格配置" width="720px" destroy-on-close>
      <div class="pricing-toolbar"><el-button type="primary" @click="addPricing">新增模型</el-button></div>
      <el-table :data="pricingRows" border>
        <el-table-column label="供应商" min-width="110"><template #default="{ row }"><el-input v-model="row.provider" /></template></el-table-column>
        <el-table-column label="模型" min-width="150"><template #default="{ row }"><el-input v-model="row.model" /></template></el-table-column>
        <el-table-column label="输入/百万" width="130"><template #default="{ row }"><el-input v-model="row.inputPricePerMillion" /></template></el-table-column>
        <el-table-column label="输出/百万" width="130"><template #default="{ row }"><el-input v-model="row.outputPricePerMillion" /></template></el-table-column>
        <el-table-column label="币种" width="90"><template #default="{ row }"><el-input v-model="row.currency" /></template></el-table-column>
        <el-table-column label="启用" width="70"><template #default="{ row }"><el-switch v-model="row.enabled" /></template></el-table-column>
        <el-table-column label="操作" width="80"><template #default="{ row }"><el-button link type="primary" @click="savePricing(row)">保存</el-button></template></el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { auditApi, type AiUsageRecord, type AiUsageSummary, type AiUsageTrendPoint } from '@/api/audit'
import { aiPricingApi, type AiModelPricing } from '@/api/aiPricing'
import { tenancyApi } from '@/api/tenancy'
import { agentApi, type TenantQuotaView } from '@/api/agent'

const authStore = useAuthStore()
const today = new Date()
const prior = new Date(today.getTime() - 6 * 24 * 60 * 60 * 1000)
const localDate = (value: Date) => `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`
const filters = reactive<{ range: string[]; tenantId?: number; model?: string; status?: string }>({ range: [localDate(prior), localDate(today)] })
const rows = ref<AiUsageRecord[]>([])
const trend = ref<AiUsageTrendPoint[]>([])
const tenants = ref<Array<{ id: number; name: string }>>([])
const models = ref<string[]>([])
const quota = ref<TenantQuotaView | null>(null)
const summary = ref<AiUsageSummary>({ calls: 0, inputTokens: 0, outputTokens: 0, totalTokens: 0, estimatedCost: 0, averageLatencyMs: 0, successRate: 0 })
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const pricingVisible = ref(false)
const pricingRows = ref<AiModelPricing[]>([])

const quotaPercent = computed(() => {
  if (!quota.value?.maxAiTokensPerMonth) return 0
  return Math.min(100, Math.round(quota.value.usedAiTokensThisMonth / quota.value.maxAiTokensPerMonth * 100))
})
const maxTrend = computed(() => Math.max(1, ...trend.value.map(item => item.totalTokens)))

function params() {
  const result: Record<string, unknown> = { page: page.value - 1, size: size.value }
  if (filters.range?.[0]) result.from = `${filters.range[0]}T00:00:00+08:00`
  if (filters.range?.[1]) result.to = `${filters.range[1]}T23:59:59.999+08:00`
  if (filters.tenantId) result.tenantId = filters.tenantId
  if (filters.model) result.model = filters.model
  if (filters.status) result.status = filters.status
  return result
}

async function load() {
  loading.value = true
  try {
    const query = params()
    const [pageResponse, trendResponse] = await Promise.all([auditApi.aiUsage(query), auditApi.aiUsageTrend(query)])
    const data = pageResponse.data
    rows.value = data?.content ?? []
    total.value = data?.totalElements ?? 0
    summary.value = data?.summary ?? summary.value
    trend.value = trendResponse.data ?? []
    rows.value.forEach(row => { if (row.model && !models.value.includes(row.model)) models.value.push(row.model) })
  } catch {
    rows.value = []; trend.value = []; ElMessage.error('AI 用量加载失败')
  } finally { loading.value = false }
}
async function reload() { page.value = 1; await loadQuota(); await load() }
async function resetFilters() { filters.range = [localDate(prior), localDate(today)]; filters.tenantId = undefined; filters.model = undefined; filters.status = undefined; await reload() }
async function loadTenants() {
  if (!authStore.isPlatformAdmin) return
  const response = await tenancyApi.listTenants(0, 200); const data = response.data as any; const list = Array.isArray(data?.content) ? data.content : []
  tenants.value = list.map((tenant: any) => ({ id: Number(tenant.id), name: String(tenant.name || tenant.code || tenant.id) }))
}
async function loadQuota() {
  try {
    if (authStore.isPlatformAdmin) {
      quota.value = filters.tenantId ? (await tenancyApi.getTenantQuota(filters.tenantId)).data as TenantQuotaView : null
    } else {
      quota.value = (await agentApi.quota()).data
    }
  } catch { quota.value = null }
}
async function openPricing() { pricingVisible.value = true; try { pricingRows.value = (await aiPricingApi.list()).data ?? [] } catch { ElMessage.error('价格配置加载失败') } }
function addPricing() { pricingRows.value.push({ provider: 'dashscope', model: '', inputPricePerMillion: 0, outputPricePerMillion: 0, currency: 'CNY', enabled: true }) }
async function savePricing(row: AiModelPricing) { try { await aiPricingApi.upsert({ ...row, inputPricePerMillion: Number(row.inputPricePerMillion), outputPricePerMillion: Number(row.outputPricePerMillion) }); ElMessage.success('价格已保存') } catch { /* client toast */ } }
function formatNumber(value?: number | null) { return value == null ? '未获取' : new Intl.NumberFormat('zh-CN').format(value) }
function formatTokens(value?: number | null) { return value == null ? '未获取' : formatNumber(value) }
function formatCost(value?: number | null, currency?: string | null) { return value == null ? '未获取' : `${currency || 'CNY'} ${Number(value).toFixed(6)}` }
function formatDate(value?: string | null) { return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '—' }
function statusLabel(status: string, source?: string) { return source === 'UNAVAILABLE' ? '未获取 usage' : status === 'OK' ? '成功' : '失败' }
function statusType(status: string) { return status === 'OK' ? 'success' : status === 'ERROR' ? 'danger' : 'warning' }
function featureLabel(value?: string) { return ({ chat: 'AI 咨询', agent: 'Agent', harness: 'AI 中心', analysis: '数据分析', legacy: '历史记录' } as Record<string, string>)[value || ''] || value || 'AI 调用' }
function tenantLabel(id: number) { return tenants.value.find(item => item.id === id)?.name || `门店 ${id}` }
function trendWidth(value: number) { return Math.max(2, Math.round(value / maxTrend.value * 100)) }

onMounted(async () => { await Promise.all([loadTenants(), loadQuota()]); await load() })
</script>

<style scoped>
.usage-page { display: flex; flex-direction: column; gap: 16px; }
.page-head, .section-head, .quota-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.page-head .simple-page-desc { margin-bottom: 0; }
.filter-card { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }
.summary-grid { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 12px; }
.summary-card { background: var(--surface); border: 1px solid var(--border); border-radius: 10px; padding: 16px; min-width: 0; }
.summary-card span, .summary-card small, .section-head span { display: block; color: var(--text-muted); font-size: 12px; }
.summary-card strong { display: block; margin: 8px 0 4px; font-size: 22px; font-variant-numeric: tabular-nums; overflow: hidden; text-overflow: ellipsis; }
.quota-head { margin-bottom: 10px; font-size: 13px; }
.quota-head strong { font-variant-numeric: tabular-nums; }
.section-head { margin-bottom: 14px; }
.section-head h3 { margin: 0; font-size: 16px; }
.trend-list { display: flex; flex-direction: column; gap: 9px; }
.trend-row { display: grid; grid-template-columns: 100px 1fr 100px; gap: 12px; align-items: center; font-size: 12px; }
.trend-track { height: 8px; background: var(--bg); border-radius: 4px; overflow: hidden; }
.trend-track i { display: block; height: 100%; background: var(--primary); border-radius: 4px; }
.trend-value { text-align: right; font-variant-numeric: tabular-nums; }
.pager { display: flex; justify-content: flex-end; padding-top: 14px; }
.pricing-toolbar { margin-bottom: 12px; }
@media (max-width: 1000px) { .summary-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (max-width: 640px) { .summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .trend-row { grid-template-columns: 80px 1fr 76px; } }
</style>

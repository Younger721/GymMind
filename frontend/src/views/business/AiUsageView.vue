<template>
  <div>
    <h2 class="simple-page-title">AI 用量</h2>
    <p class="simple-page-desc">统计 AI 调用次数、耗时和 token，用于成本估算、性能排查和配额管理。</p>
    <BizCrudPanel title="AI 调用记录" desc="输入 token 和输出 token 分别代表发送给模型、模型返回的文本量；耗时用于定位慢调用。" :loader="loadUsage" :column-keys="['tenantId','userId','model','inputTokens','outputTokens','latencyMs','providerRequestId','status','createdAt']" :column-labels="labels" />
  </div>
</template>

<script setup lang="ts">
import BizCrudPanel from '@/components/business/BizCrudPanel.vue'
import { auditApi } from '@/api/audit'
const labels = { tenantId: '租户 ID', userId: '用户 ID', model: '模型', inputTokens: '输入 token', outputTokens: '输出 token', latencyMs: '耗时（毫秒）', providerRequestId: '服务商请求 ID', status: '状态', createdAt: '时间' }
const loadUsage = () => auditApi.aiUsage()
</script>

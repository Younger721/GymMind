<template>
  <div>
    <h2 class="simple-page-title">日志管理</h2>
    <p class="simple-page-desc">查看用户和系统关键操作记录，用于追踪变更与排查问题。</p>
    <BizCrudPanel title="操作日志" :loader="loadAudit" :column-keys="['tenantId','userId','action','resourceType','resourceId','auditResult','createdAt']" :column-labels="labels" />
  </div>
</template>

<script setup lang="ts">
import BizCrudPanel from '@/components/business/BizCrudPanel.vue'
import { auditApi } from '@/api/audit'
const labels = { tenantId: '租户 ID', userId: '用户 ID', action: '操作', resourceType: '资源类型', resourceId: '资源 ID', auditResult: '结果', createdAt: '时间' }
const loadAudit = () => auditApi.operations()
</script>

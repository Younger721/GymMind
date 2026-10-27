<template>
  <div>
    <h2 class="simple-page-title">日志管理</h2>
    <p class="simple-page-desc">查看用户和系统关键操作记录，用于追踪变更与排查问题。</p>
    <BizCrudPanel
      title="操作日志"
      :loader="loadAudit"
      :column-keys="auditColumns"
      :column-labels="auditLabels"
    />
  </div>
</template>

<script setup lang="ts">
import BizCrudPanel from '@/components/business/BizCrudPanel.vue'
import { auditApi } from '@/api/audit'
import {
  OPERATION_AUDIT_COLUMN_KEYS,
  OPERATION_AUDIT_COLUMN_LABELS
} from '@/utils/auditLogDisplay'

const auditColumns = [...OPERATION_AUDIT_COLUMN_KEYS]
const auditLabels = OPERATION_AUDIT_COLUMN_LABELS
const loadAudit = () => auditApi.operations()
</script>

<template>
  <el-table :data="rows" v-loading="loading" stripe style="width: 100%">
    <el-table-column
      v-for="col in columns"
      :key="col"
      :prop="col"
      :label="columnLabel(col)"
      :width="columnWidth(col)"
      :min-width="columnMinWidth(col)"
      show-overflow-tooltip
    >
      <template #default="{ row }">
        {{ formatCell(col, row[col]) }}
      </template>
    </el-table-column>
    <el-table-column v-if="$slots.actions" label="操作" fixed="right" width="156">
      <template #default="scope">
        <slot name="actions" v-bind="scope" />
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import {
  DEFAULT_COLUMN_LABELS,
  formatRoleList,
  formatUserStatus
} from '@/utils/displayLabels'
import {
  formatAuditAction,
  formatAuditResourceType,
  formatAuditResult
} from '@/utils/auditLogDisplay'

const props = defineProps<{
  rows: Record<string, unknown>[]
  loading?: boolean
  columnKeys?: string[]
  columnLabels?: Record<string, string>
}>()

function columnLabel(key: string) {
  return props.columnLabels?.[key] ?? DEFAULT_COLUMN_LABELS[key] ?? key
}

const compactColumnWidths: Record<string, number> = {
  id: 56,
  tenantId: 76,
  actorId: 88,
  resourceId: 88,
  result: 76,
  status: 76,
  role: 88,
  roles: 108
}

const flexibleColumnMinWidths: Record<string, number> = {
  username: 108,
  email: 138,
  displayName: 112
}

function columnWidth(key: string) {
  return compactColumnWidths[key]
}

function columnMinWidth(key: string) {
  return flexibleColumnMinWidths[key] ?? (compactColumnWidths[key] ? undefined : 96)
}

function formatCell(key: string, value: unknown) {
  if (value == null) return '—'
  if (key === 'result' || key === 'auditResult') return formatAuditResult(value)
  if (key === 'action') return formatAuditAction(value)
  if (key === 'resourceType') return formatAuditResourceType(value)
  if (key === 'status') return formatUserStatus(value)
  if (key === 'roles') return formatRoleList(value)
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

const columns = computed(() => {
  if (props.columnKeys?.length) return props.columnKeys
  const first = props.rows[0]
  return first ? Object.keys(first).slice(0, 8) : []
})
</script>

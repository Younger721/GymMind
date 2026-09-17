<template>
  <el-table :data="rows" v-loading="loading" stripe style="width: 100%">
    <el-table-column
      v-for="col in columns"
      :key="col"
      :prop="col"
      :label="col"
      min-width="120"
      show-overflow-tooltip
    >
      <template #default="{ row }">
        {{ formatCell(row[col]) }}
      </template>
    </el-table-column>
    <el-table-column v-if="$slots.actions" label="操作" fixed="right" width="220">
      <template #default="scope">
        <slot name="actions" v-bind="scope" />
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  rows: Record<string, unknown>[]
  loading?: boolean
  columnKeys?: string[]
}>()

function formatCell(value: unknown) {
  if (value == null) return '—'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

const columns = computed(() => {
  if (props.columnKeys?.length) return props.columnKeys
  const first = props.rows[0]
  return first ? Object.keys(first).slice(0, 8) : []
})
</script>

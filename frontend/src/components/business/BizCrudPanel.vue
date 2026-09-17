<template>
  <div class="biz-panel simple-card">
    <div class="panel-head">
      <div>
        <h3 class="panel-title">{{ title }}</h3>
        <p v-if="desc" class="panel-desc">{{ desc }}</p>
      </div>
      <div class="panel-actions">
        <el-input
          v-if="searchable"
          v-model="keyword"
          placeholder="搜索"
          clearable
          size="small"
          style="width: 160px"
          @keyup.enter="load"
        />
        <el-button size="small" :loading="loading" @click="load">刷新</el-button>
        <el-button v-if="createFields.length" size="small" type="primary" @click="openCreate">
          新建
        </el-button>
      </div>
    </div>

    <BizTable :rows="rows" :loading="loading" :column-keys="columnKeys">
      <template v-if="rowActions.length" #actions="{ row }">
        <el-button
          v-for="act in rowActions"
          :key="act.label"
          size="small"
          text
          type="primary"
          @click="runAction(act, row)"
        >
          {{ act.label }}
        </el-button>
      </template>
    </BizTable>

    <JsonBlock v-if="result" :data="result" />

    <el-dialog v-model="createVisible" :title="'新建' + title" width="480px" destroy-on-close>
      <el-form label-width="100px" @submit.prevent>
        <el-form-item v-for="f in createFields" :key="f.key" :label="f.label">
          <el-input
            v-if="f.type !== 'select'"
            v-model="form[f.key]"
            :placeholder="f.placeholder"
            :type="f.type === 'number' ? 'number' : 'text'"
          />
          <el-select v-else v-model="form[f.key]" style="width: 100%">
            <el-option v-for="o in f.options ?? []" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import BizTable from './BizTable.vue'
import JsonBlock from './JsonBlock.vue'
import { useBizList } from '@/composables/useBizList'

export interface FieldDef {
  key: string
  label: string
  placeholder?: string
  type?: 'text' | 'number' | 'select'
  options?: { label: string; value: string }[]
}

export interface RowAction {
  label: string
  handler: (row: Record<string, unknown>) => Promise<unknown>
}

const props = defineProps<{
  title: string
  desc?: string
  searchable?: boolean
  columnKeys?: string[]
  createFields?: FieldDef[]
  rowActions?: RowAction[]
  loader: (keyword?: string) => Promise<unknown>
  creator?: (payload: Record<string, string>) => Promise<unknown>
}>()

const { rows, loading, result, runLoader } = useBizList()
const keyword = ref('')
const createVisible = ref(false)
const submitting = ref(false)
const form = reactive<Record<string, string>>({})

const createFields = props.createFields ?? []
const rowActions = props.rowActions ?? []

function openCreate() {
  createFields.forEach(f => { form[f.key] = '' })
  createVisible.value = true
}

async function load() {
  await runLoader(() => props.loader(keyword.value || undefined) as Promise<{ data?: Record<string, unknown>[] }>)
}

async function submitCreate() {
  if (!props.creator) return
  submitting.value = true
  try {
    const payload: Record<string, string> = {}
    createFields.forEach(f => { payload[f.key] = form[f.key] ?? '' })
    const res = await props.creator(payload)
    result.value = res
    ElMessage.success('创建成功')
    createVisible.value = false
    await load()
  } catch {
    /* client 已提示 */
  } finally {
    submitting.value = false
  }
}

async function runAction(act: RowAction, row: Record<string, unknown>) {
  try {
    const res = await act.handler(row)
    if (res !== undefined) result.value = res
    ElMessage.success('操作成功')
    await load()
  } catch {
    /* client 已提示 */
  }
}

onMounted(load)

defineExpose({ reload: load })
</script>

<style scoped>
.biz-panel {
  margin-bottom: 16px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.panel-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.panel-desc {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--text-muted);
}

.panel-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>

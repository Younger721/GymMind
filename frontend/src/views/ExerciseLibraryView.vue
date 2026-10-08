<template>
  <div class="exercise-library">
    <div class="page-heading">
      <div><div class="eyebrow">GYMMIND TRAINING</div><h2>动作库</h2><p>选择标准动作，快速配置训练计划。</p></div>
      <el-button v-if="canCreate" type="primary" @click="openCreate">新增动作</el-button>
    </div>
    <div class="toolbar simple-card">
      <el-input v-model="keyword" clearable placeholder="搜索名称、分类、肌群、器械或标签" @keyup.enter="load" />
      <el-select v-model="category" clearable placeholder="分类"><el-option v-for="item in categories" :key="item" :label="categoryLabel(item)" :value="item" /></el-select>
      <el-select v-model="difficulty" clearable placeholder="难度" @change="load"><el-option label="初级" value="BEGINNER" /><el-option label="中级" value="INTERMEDIATE" /><el-option label="高级" value="ADVANCED" /></el-select>
      <el-button @click="load">查询</el-button>
    </div>
    <el-table :data="filteredExercises" v-loading="loading" stripe>
      <el-table-column prop="name" label="动作名称" min-width="160" />
      <el-table-column label="分类" width="110"><template #default="scope">{{ categoryLabel(scope.row.category) }}</template></el-table-column>
      <el-table-column prop="targetMuscle" label="目标肌群" min-width="130" />
      <el-table-column prop="equipment" label="器械" min-width="120" />
      <el-table-column prop="difficulty" label="难度" width="100" />
      <el-table-column prop="active" label="状态" width="90"><template #default="scope"><el-tag :type="scope.row.active ? 'success' : 'info'">{{ scope.row.active ? '启用' : '停用' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <el-button link type="primary" @click="showDetail(scope.row)">详情</el-button>
          <el-button v-if="canEdit" link type="primary" @click="openEdit(scope.row)">编辑</el-button>
          <el-button v-if="canEdit" link :type="scope.row.active ? 'warning' : 'success'" @click="toggle(scope.row)">{{ scope.row.active ? '停用' : '启用' }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !filteredExercises.length" description="暂无动作" />
    <el-dialog v-model="detailVisible" title="动作详情" width="620px">
      <div v-if="selected"><h3>{{ selected.name }}</h3><p>{{ selected.description }}</p><el-descriptions :column="2" border><el-descriptions-item label="分类">{{ categoryLabel(selected.category) }}</el-descriptions-item><el-descriptions-item label="目标肌群">{{ selected.targetMuscle }}</el-descriptions-item><el-descriptions-item label="器械">{{ selected.equipment }}</el-descriptions-item><el-descriptions-item label="难度">{{ selected.difficulty }}</el-descriptions-item></el-descriptions><h4>动作步骤</h4><p class="pre">{{ selected.steps }}</p><h4>常见错误</h4><p class="pre">{{ selected.commonMistakes }}</p><h4>安全提示</h4><p class="pre">{{ selected.safetyNotes }}</p></div>
    </el-dialog>
    <el-dialog v-model="formVisible" :title="editing ? '编辑动作' : '新增动作'" width="650px">
      <el-form :model="form" label-position="top">
        <el-form-item label="名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="分类" required><el-input v-model="form.category" placeholder="例如：胸、背、臀腿" /></el-form-item>
        <el-form-item label="目标肌群" required><el-input v-model="form.targetMuscle" /></el-form-item>
        <el-form-item label="难度" required><el-select v-model="form.difficulty" style="width:100%"><el-option label="初级" value="BEGINNER" /><el-option label="中级" value="INTERMEDIATE" /><el-option label="高级" value="ADVANCED" /></el-select></el-form-item>
        <el-form-item label="器械" required><el-input v-model="form.equipment" /></el-form-item>
        <el-form-item label="描述" required><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="步骤" required><el-input v-model="form.steps" type="textarea" /></el-form-item>
        <el-form-item label="常见错误" required><el-input v-model="form.commonMistakes" type="textarea" /></el-form-item>
        <el-form-item label="安全提示" required><el-input v-model="form.safetyNotes" type="textarea" /></el-form-item>
        <el-form-item label="标签" required><el-input v-model="form.tags" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="formVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { exercisesApi, type Exercise } from '@/api/exercises'
import { useAuthStore } from '@/stores/auth'
import { exerciseCategoryLabel as categoryLabel, exerciseCategories, filterExercises } from '@/utils/exercisePresentation.mjs'
const auth = useAuthStore()
const loading = ref(false); const saving = ref(false); const keyword = ref(''); const category = ref(''); const difficulty = ref('')
const exercises = ref<Exercise[]>([]); const selected = ref<Exercise | null>(null); const detailVisible = ref(false); const formVisible = ref(false); const editing = ref(false)
const canEdit = computed(() => auth.roles.includes('GYM_ADMIN') && auth.permissions.includes('exercise:write'))
const canCreate = computed(() => auth.permissions.includes('exercise:write') && (auth.roles.includes('GYM_ADMIN') || auth.roles.includes('COACH')))
const form = reactive<Record<string, string>>({ name:'', category:'', targetMuscle:'', difficulty:'BEGINNER', equipment:'', description:'', steps:'', commonMistakes:'', safetyNotes:'', tags:'' })
const categories = computed(() => exerciseCategories(exercises.value))
const filteredExercises = computed(() => filterExercises(exercises.value, { keyword: keyword.value, category: category.value }).filter(item => !difficulty.value || item.difficulty === difficulty.value))
async function load() { loading.value = true; try { exercises.value = (await exercisesApi.list()).data?.filter((item: Exercise) => canEdit.value || item.active) || [] } catch (error: any) { ElMessage.error(error.response?.data?.message || '动作加载失败') } finally { loading.value = false } }
function showDetail(item: Exercise) { selected.value = item; detailVisible.value = true }
function resetForm() { Object.assign(form, { name:'', category:'', targetMuscle:'', difficulty:'BEGINNER', equipment:'', description:'', steps:'', commonMistakes:'', safetyNotes:'', tags:'' }) }
function openCreate() { editing.value = false; resetForm(); formVisible.value = true }
function openEdit(item: Exercise) { editing.value = true; Object.assign(form, item); selected.value = item; formVisible.value = true }
async function save() { if (Object.values(form).some(value => !value.trim())) { ElMessage.warning('请完整填写动作信息'); return }; saving.value = true; try { if (editing.value && selected.value) await exercisesApi.update(selected.value.id, form); else await exercisesApi.create(form); ElMessage.success('保存成功'); formVisible.value = false; await load() } catch (error: any) { ElMessage.error(error.response?.data?.message || '保存失败') } finally { saving.value = false } }
async function toggle(item: Exercise) { try { await ElMessageBox.confirm(`确定要${item.active ? '停用' : '启用'}“${item.name}”吗？`, '确认操作'); await exercisesApi.setActive(item.id, !item.active); ElMessage.success('状态已更新'); await load() } catch (error: any) { if (error !== 'cancel') ElMessage.error(error.response?.data?.message || '状态更新失败') } }
onMounted(load)
</script>
<style scoped>
.exercise-library{max-width:1100px;margin:auto}.page-heading{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:18px}.page-heading h2{margin:4px 0}.page-heading p{color:var(--text-muted)}.toolbar{display:flex;gap:12px;margin-bottom:16px;padding:14px}.toolbar .el-input{max-width:380px}.pre{white-space:pre-wrap;line-height:1.6}
</style>

<template>
  <div>
    <h2 class="simple-page-title">角色权限</h2>
    <p class="simple-page-desc">配置平台默认菜单或当前门店的角色菜单覆盖</p>
    <el-alert v-if="!canManageRolePermissions" type="error" title="仅管理员可以配置角色权限" show-icon />
    <div v-else class="simple-card">
      <el-table :data="roles" v-loading="loading" stripe>
        <el-table-column prop="name" label="角色名称" />
        <el-table-column prop="code" label="角色编码" />
        <el-table-column prop="permissionCount" label="菜单数量" width="110" />
        <el-table-column label="操作" width="150"><template #default="{ row }">
          <el-button size="small" type="primary" text @click="openPermissions(row)">菜单权限</el-button>
          <el-tag v-if="row.code === 'PLATFORM_ADMIN'" type="info" size="small">固定全量</el-tag>
          <el-tag v-else-if="row.readOnly" type="info" size="small">固定管理权限</el-tag>
        </template></el-table-column>
      </el-table>
    </div>
    <el-dialog v-model="dialogVisible" :title="`${selectedRole?.name || ''} · 菜单权限`" width="620px" destroy-on-close>
      <el-tree ref="treeRef" :data="permissionTree" node-key="code" show-checkbox default-expand-all :props="{ label: 'label', children: 'children', disabled: 'disabled' }" />
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button v-if="!selectedRole?.readOnly" type="primary" :loading="saving" @click="save">保存菜单</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElMessage, type ElTree } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { rolePermissionsApi, type PermissionCatalogItem } from '@/api/rolePermissions'
import { FULL_MENU_CATALOG, menuLabel } from '@/utils/menuPermissions'

const authStore = useAuthStore()
const canManageRolePermissions = computed(() => authStore.isPlatformAdmin || authStore.roles.includes('GYM_ADMIN'))
const loading = ref(false)
const saving = ref(false)
const roles = ref<any[]>([])
const catalog = ref<PermissionCatalogItem[]>([])
const dialogVisible = ref(false)
const selectedRole = ref<any>(null)
const treeRef = ref<InstanceType<typeof ElTree>>()
const roleNames: Record<string, string> = {
  PLATFORM_ADMIN: '平台管理员',
  GYM_ADMIN: '健身房管理员',
  COACH: '教练',
  MEMBER: '会员'
}
const roleOrder = ['PLATFORM_ADMIN', 'GYM_ADMIN', 'COACH', 'MEMBER']
const roleMenuBoundaries: Record<string, Set<string>> = {
  GYM_ADMIN: new Set([
    'menu:dashboard', 'menu:assistant', 'menu:knowledge', 'menu:courses',
    'menu:ai-config', 'menu:training', 'menu:analytics', 'menu:admin', 'menu:tenant-settings', 'menu:role-permissions', 'menu:profile'
  ]),
  COACH: new Set(['menu:dashboard', 'menu:assistant', 'menu:knowledge', 'menu:courses', 'menu:training', 'menu:profile']),
  MEMBER: new Set(['menu:dashboard', 'menu:assistant', 'menu:courses', 'menu:training', 'menu:profile'])
}
const menuItems = computed(() => {
  const byCode = new Map(catalog.value.filter(item => item.code.startsWith('menu:')).map(item => [item.code, item]))
  return FULL_MENU_CATALOG.map(item => {
    const serverItem = byCode.get(item.code)
    return {
      code: item.code,
      name: menuLabel(item.code, serverItem?.name || item.label),
      roles: serverItem?.roles || []
    }
  })
})
const permissionTree = computed(() => {
  const readOnly = selectedRole.value?.readOnly === true
  const allowed = selectedRole.value?.code === 'PLATFORM_ADMIN'
    ? new Set(FULL_MENU_CATALOG.map(item => item.code))
    : roleMenuBoundaries[selectedRole.value?.code] || new Set<string>()
  const groups = new Map<string, { code: string; label: string; disabled: boolean; children: { code: string; label: string; disabled: boolean }[] }>()
  menuItems.value.forEach(item => {
    const menu = FULL_MENU_CATALOG.find(menuItem => menuItem.code === item.code)
    const root = menu?.group || '菜单'
    if (!groups.has(root)) groups.set(root, { code: `group:${root}`, label: root, disabled: readOnly, children: [] })
    groups.get(root)!.children.push({
      code: item.code,
      label: item.name,
      disabled: readOnly || !allowed.has(item.code)
    })
  })
  return [...groups.values()]
})
async function load() {
  loading.value = true
  try {
    const pp = await rolePermissionsApi.catalog()
    catalog.value = (pp as any).data || []
    const visibleRoleCodes = authStore.isPlatformAdmin
      ? roleOrder
      : ['GYM_ADMIN', 'COACH', 'MEMBER']
    roles.value = visibleRoleCodes.map(code => {
      const menuCount = menuItems.value.filter(item => item.roles.includes(code)).length
      return {
        code,
        name: roleNames[code] || code,
        permissionCount: code === 'PLATFORM_ADMIN' ? FULL_MENU_CATALOG.length : menuCount,
        readOnly: code === 'PLATFORM_ADMIN' || (!authStore.isPlatformAdmin && code === 'GYM_ADMIN')
      }
    })
  } catch {
    ElMessage.error('加载角色菜单失败')
  } finally {
    loading.value = false
  }
}
async function openPermissions(row: any) {
  selectedRole.value = row
  dialogVisible.value = true
  await nextTick()
  const selected = row.code === 'PLATFORM_ADMIN'
    ? FULL_MENU_CATALOG.map(item => item.code)
    : menuItems.value.filter(p => roleMenuBoundaries[row.code]?.has(p.code) && p.roles.includes(row.code)).map(p => p.code)
  treeRef.value?.setCheckedKeys(selected)
}
async function save() {
  if (!selectedRole.value || selectedRole.value.readOnly) return
  saving.value = true
  try {
    const keys = (treeRef.value?.getCheckedKeys() || []).filter(k => !String(k).startsWith('group:')) as string[]
    await rolePermissionsApi.update(selectedRole.value.code, keys)
    ElMessage.success('菜单权限已保存')
    dialogVisible.value = false
    await load()
  } catch { ElMessage.error('保存菜单权限失败') } finally { saving.value = false }
}
onMounted(load)
</script>

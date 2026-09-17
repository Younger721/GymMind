<template>
  <div>
    <h2 class="simple-page-title">用户与审计</h2>
    <p class="simple-page-desc">用户管理、角色与操作审计</p>

    <el-tabs v-model="tab">
      <el-tab-pane label="用户" name="users">
        <BizCrudPanel
          title="用户列表"
          :create-fields="userFields"
          :row-actions="userActions"
          :loader="loadUsers"
          :creator="createUser"
        />
      </el-tab-pane>
      <el-tab-pane label="角色" name="roles">
        <BizCrudPanel title="系统角色" :loader="loadRoles" />
      </el-tab-pane>
      <el-tab-pane label="邀请" name="invite">
        <BizCrudPanel title="邀请用户" :create-fields="inviteFields" :loader="emptyLoader" :creator="sendInvite" />
      </el-tab-pane>
      <el-tab-pane label="操作审计" name="audit">
        <BizCrudPanel title="操作日志" :loader="loadAuditOps" />
      </el-tab-pane>
      <el-tab-pane label="AI 用量" name="ai-usage">
        <BizCrudPanel title="AI 调用记录" :loader="loadAiUsage" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import BizCrudPanel, { type FieldDef, type RowAction } from '@/components/business/BizCrudPanel.vue'
import { usersApi } from '@/api/users'
import { auditApi } from '@/api/audit'

const tab = ref('users')

const userFields: FieldDef[] = [
  { key: 'email', label: '邮箱' },
  { key: 'password', label: '密码' },
  { key: 'displayName', label: '显示名' },
  {
    key: 'role',
    label: '角色',
    type: 'select',
    options: [
      { label: '门店管理员', value: 'GYM_ADMIN' },
      { label: '教练', value: 'COACH' },
      { label: '会员', value: 'MEMBER' }
    ]
  }
]

const inviteFields: FieldDef[] = [
  { key: 'email', label: '邮箱' },
  {
    key: 'role',
    label: '角色',
    type: 'select',
    options: [
      { label: '教练', value: 'COACH' },
      { label: '会员', value: 'MEMBER' }
    ]
  }
]

const userActions: RowAction[] = [
  { label: '禁用', handler: row => usersApi.changeStatus(Number(row.id), 'DISABLED') },
  { label: '启用', handler: row => usersApi.changeStatus(Number(row.id), 'ACTIVE') }
]

async function loadUsers() {
  return usersApi.list()
}

async function createUser(p: Record<string, string>) {
  return usersApi.create({
    email: p.email,
    password: p.password,
    displayName: p.displayName,
    role: p.role
  })
}

async function loadRoles() {
  return usersApi.listRoles()
}

async function sendInvite(p: Record<string, string>) {
  return usersApi.invite({ email: p.email, role: p.role })
}

async function loadAuditOps() {
  return auditApi.operations()
}

async function loadAiUsage() {
  return auditApi.aiUsage()
}

async function emptyLoader() {
  return { data: [] }
}
</script>

<template>
  <div class="profile">
    <h2 class="simple-page-title">设置</h2>
    <p class="simple-page-desc">账户基本信息</p>

    <div class="simple-card">
      <div class="row">
        <span class="label">用户名</span>
        <span>{{ authStore.username }}</span>
      </div>
      <div class="row">
        <span class="label">邮箱</span>
        <span>{{ authStore.email || '—' }}</span>
      </div>
      <div class="row">
        <span class="label">用户 ID</span>
        <span>{{ authStore.userId }}</span>
      </div>

      <el-button type="danger" plain class="logout-btn" @click="logout">退出登录</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { ElMessageBox } from 'element-plus'

const authStore = useAuthStore()

const logout = async () => {
  try {
    await ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning' })
    await authStore.logout()
  } catch {
    /* 取消 */
  }
}
</script>

<style scoped>
.row {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid var(--border);
  font-size: 14px;
}

.row:last-of-type {
  border-bottom: none;
}

.label {
  color: var(--text-muted);
}

.logout-btn {
  margin-top: 20px;
  width: 100%;
}
</style>

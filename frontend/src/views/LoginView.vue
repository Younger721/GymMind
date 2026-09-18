<template>
  <div class="auth-page">
    <div class="auth-card simple-card">
      <h1>GymMind</h1>
      <p class="desc">智能健身房管理平台</p>

      <el-alert
        v-if="isDev"
        class="dev-hint"
        type="info"
        :closable="false"
        show-icon
        title="开发测试账号"
        description="平台管理员：admin@qq.com / 123456"
      />

      <el-form @submit.prevent="handleLogin" label-position="top">
        <el-form-item label="邮箱">
          <el-input v-model="email" placeholder="admin@qq.com" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" type="password" placeholder="123456" show-password />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" class="submit-btn">
          登录
        </el-button>
      </el-form>

      <p class="footer">
        没有账号？
        <router-link to="/register">注册健身房</router-link>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const authStore = useAuthStore()
const isDev = import.meta.env.DEV
const email = ref('')
const password = ref('')

/** 进入登录页时清除失效的旧 token，避免残留会话触发 401 弹窗 */
onMounted(() => {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  authStore.$patch({
    token: null,
    refreshToken: null
  })
})
const loading = ref(false)

const handleLogin = async () => {
  if (!email.value || !password.value) {
    ElMessage.warning('请填写账号和密码')
    return
  }
  loading.value = true
  try {
    await authStore.login({ email: email.value.trim(), password: password.value })
  } catch {
    /* 错误已由拦截器或 store 提示 */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: var(--bg);
}

.auth-card {
  width: 100%;
  max-width: 380px;
}

.auth-card h1 {
  margin: 0;
  font-size: 24px;
  text-align: center;
}

.desc {
  margin: 8px 0 24px;
  text-align: center;
  color: var(--text-muted);
  font-size: 13px;
}

.dev-hint {
  margin-bottom: 16px;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
}

.footer {
  margin: 20px 0 0;
  text-align: center;
  font-size: 13px;
  color: var(--text-muted);
}
</style>

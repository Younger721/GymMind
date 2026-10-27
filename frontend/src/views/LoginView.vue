<template>
  <div class="auth-page">
    <div class="auth-card simple-card">
      <div class="auth-brand">
        <span class="auth-mark">G</span>
        <div>
          <h1>GymMind</h1>
          <p class="platform-tag">AI PLATFORM</p>
        </div>
      </div>
      <p class="desc">智能健身房管理平台</p>

      <el-alert
        v-if="isDev"
        class="dev-hint"
        type="info"
        :closable="false"
        show-icon
        title="开发测试账号"
        description="平台管理员：admin / 123456（用户名登录）"
      />

      <el-form @submit.prevent="handleLogin" label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="username" placeholder="admin" autocomplete="username" />
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
const username = ref('')
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
  if (!username.value || !password.value) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  loading.value = true
  try {
    await authStore.login({ username: username.value.trim(), password: password.value })
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
  max-width: 400px;
  padding: 28px;
}

.auth-brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 8px;
}

.auth-mark {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-weight: 800;
  font-size: 18px;
  color: #052e16;
  background: linear-gradient(145deg, var(--primary), #16a34a);
  box-shadow: 0 6px 20px var(--primary-glow);
}

.auth-brand h1 {
  margin: 0;
  font-size: 22px;
  text-align: left;
}

.platform-tag {
  margin: 4px 0 0;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.14em;
  color: var(--text-dim);
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

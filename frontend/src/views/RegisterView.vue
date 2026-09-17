<template>
  <div class="auth-page">
    <div class="auth-card simple-card">
      <h1>注册健身房</h1>
      <p class="desc">创建租户并开通管理员账号</p>

      <el-form @submit.prevent="handleRegister" label-position="top">
        <el-form-item label="门店编码">
          <el-input v-model="form.tenantCode" placeholder="如 demo-gym" />
        </el-form-item>
        <el-form-item label="门店名称">
          <el-input v-model="form.tenantName" placeholder="如 示例健身馆" />
        </el-form-item>
        <el-form-item label="管理员姓名">
          <el-input v-model="form.displayName" placeholder="您的姓名" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="登录邮箱" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="至少 8 位" show-password />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" class="submit-btn">
          注册
        </el-button>
      </el-form>

      <p class="footer">
        已有账号？
        <router-link to="/login">登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const authStore = useAuthStore()
const loading = ref(false)
const form = reactive({
  tenantCode: '',
  tenantName: '',
  displayName: '',
  email: '',
  password: ''
})

const handleRegister = async () => {
  if (!form.tenantCode || !form.tenantName || !form.displayName || !form.email || !form.password) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (form.password.length < 8) {
    ElMessage.warning('密码至少 8 位')
    return
  }
  loading.value = true
  try {
    await authStore.register({
      tenantCode: form.tenantCode.trim(),
      tenantName: form.tenantName.trim(),
      displayName: form.displayName.trim(),
      email: form.email.trim(),
      password: form.password
    })
  } catch {
    /* 错误已由拦截器提示 */
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
  max-width: 420px;
}

.auth-card h1 {
  margin: 0;
  font-size: 22px;
  text-align: center;
}

.desc {
  margin: 8px 0 24px;
  text-align: center;
  color: var(--text-muted);
  font-size: 13px;
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

<template>
  <div class="auth-page">
    <div class="auth-bg">
      <div class="bg-text">GYMPRO</div>
    </div>
    <div class="auth-panel fade-in">
      <div class="auth-logo">
        <span class="display accent">GYM</span><span class="display">PRO</span>
      </div>
      <p class="auth-sub">Hệ thống quản lý luyện tập thông minh</p>

      <el-form :model="form" @submit.prevent="handleLogin" label-position="top" class="auth-form">
        <el-form-item label="Email">
          <el-input v-model="form.email" placeholder="admin@gym.com" type="email" size="large" prefix-icon="Message" />
        </el-form-item>
        <el-form-item label="Mật khẩu">
          <el-input v-model="form.password" placeholder="••••••••" type="password" size="large" prefix-icon="Lock" show-password />
        </el-form-item>

        <el-button type="primary" size="large" style="width:100%;margin-top:8px" :loading="auth.loading" @click="handleLogin">
          ĐĂNG NHẬP
        </el-button>
      </el-form>

      <div class="auth-footer">
        Chưa có tài khoản? <router-link to="/register">Đăng ký ngay</router-link>
      </div>

      <div class="demo-hint">
        <span class="mono" style="font-size:0.75rem;color:var(--c-text3)">
          Demo: admin@gym.com / admin123
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth   = useAuthStore()
const router = useRouter()
const form   = reactive({ email: '', password: '' })

async function handleLogin() {
  if (!form.email || !form.password) return
  try {
    const data = await auth.login(form)
    router.push(data.role === 'ROLE_ADMIN' ? '/admin' : '/app')
  } catch {}
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh; display: flex; align-items: stretch;
}
.auth-bg {
  flex: 1; background: var(--c-bg2);
  border-right: 1px solid var(--c-border);
  display: flex; align-items: center; justify-content: center;
  overflow: hidden; position: relative;
}
.bg-text {
  font-family: var(--font-display);
  font-size: 18vw; line-height: 1;
  color: transparent; -webkit-text-stroke: 1px var(--c-border);
  user-select: none; white-space: nowrap;
  transform: rotate(-15deg);
}
@media(max-width:768px) { .auth-bg { display: none; } }

.auth-panel {
  width: 440px; min-height: 100vh;
  display: flex; flex-direction: column; justify-content: center;
  padding: 48px 40px; background: var(--c-bg);
}
.auth-logo {
  font-size: 3rem; line-height: 1; margin-bottom: 8px;
}
.auth-sub {
  color: var(--c-text2); font-size: 0.85rem; margin-bottom: 36px;
}
.auth-form { margin-bottom: 24px; }
.auth-footer {
  text-align: center; color: var(--c-text2); font-size: 0.875rem;
  margin-bottom: 12px;
}
.demo-hint { text-align: center; margin-top: 16px; }
</style>

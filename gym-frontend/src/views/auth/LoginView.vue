<template>
  <div class="auth-page">
    <div class="auth-bg">
      <div class="bg-overlay"/>
      <div class="bg-text">GYMPRO</div>
      <div class="bg-tagline">Hệ thống quản lý luyện tập thông minh</div>
    </div>
    <div class="auth-panel fade-in">
      <div class="auth-logo">
        <span class="display" style="font-size:2.8rem;color:var(--c-text)">GYM</span>
        <span class="display accent" style="font-size:2.8rem">PRO</span>
      </div>
      <p style="color:var(--c-text2);font-size:0.875rem;margin-bottom:32px">Đăng nhập vào tài khoản của bạn</p>

      <el-form :model="form" label-position="top">
        <el-form-item label="Email">
          <el-input v-model="form.email" placeholder="you@email.com" type="email" size="large" prefix-icon="Message"/>
        </el-form-item>
        <el-form-item label="Mật khẩu">
          <el-input v-model="form.password" placeholder="••••••••" type="password" size="large" prefix-icon="Lock" show-password @keyup.enter="handleLogin"/>
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%;margin-top:8px;font-size:1rem;height:46px" :loading="auth.loading" @click="handleLogin">
          ĐĂNG NHẬP
        </el-button>
      </el-form>

      <div style="text-align:center;color:var(--c-text2);font-size:0.875rem;margin-top:20px">
        Chưa có tài khoản? <router-link to="/register" style="color:var(--c-accent);font-weight:600">Đăng ký ngay</router-link>
      </div>

      <div class="demo-box">
        <div style="font-size:0.72rem;font-weight:700;color:var(--c-text3);letter-spacing:0.1em;margin-bottom:4px">DEMO</div>
        <div style="font-size:0.78rem;color:var(--c-text2)">Admin: <strong>admin@gym.com</strong> / <strong>admin123</strong></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const form = reactive({ email: '', password: '' })

async function handleLogin() {
  if (!form.email || !form.password) return
  try {
    const data = await auth.login(form)
    router.push(data.role === 'ROLE_ADMIN' ? '/admin' : '/app')
  } catch {}
}
</script>

<style scoped>
.auth-page { min-height:100vh; display:flex; }
.auth-bg {
  flex:1; position:relative; overflow:hidden;
  background: linear-gradient(135deg, #4A2810 0%, #6B4226 50%, #8C5C3E 100%);
  display:flex; flex-direction:column; align-items:center; justify-content:center;
}
.bg-overlay {
  position:absolute; inset:0;
  background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.03'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
}
.bg-text {
  font-family:var(--font-display); font-size:10vw; line-height:1;
  color:rgba(255,255,255,0.08); user-select:none; letter-spacing:0.05em;
}
.bg-tagline { color:rgba(255,255,255,0.4); font-size:0.9rem; margin-top:16px; letter-spacing:0.05em; }
@media(max-width:768px) { .auth-bg { display:none; } }

.auth-panel {
  width:440px; min-height:100vh; background:var(--c-card);
  display:flex; flex-direction:column; justify-content:center;
  padding:48px 40px; box-shadow:-8px 0 32px rgba(107,66,38,0.2);
}
.demo-box {
  margin-top:24px; padding:12px 16px;
  background:var(--c-card2); border:1px solid var(--c-border2);
  border-radius:var(--radius-lg); text-align:center;
}
</style>
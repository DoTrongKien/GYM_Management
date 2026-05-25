<template>
  <div class="auth-page">
    <div class="auth-bg">
      <div class="bg-text">JOIN</div>
      <div class="bg-tagline">Bắt đầu hành trình chinh phục</div>
    </div>
    <div class="auth-panel fade-in">
      <div class="auth-logo">
        <span class="display" style="font-size:2.8rem;color:var(--c-text)">GYM</span>
        <span class="display accent" style="font-size:2.8rem">PRO</span>
      </div>
      <p style="color:var(--c-text2);font-size:0.875rem;margin-bottom:28px">Tạo tài khoản miễn phí</p>

      <el-form :model="form" label-position="top">
        <el-form-item label="Họ và tên">
          <el-input v-model="form.fullName" placeholder="Nguyễn Văn A" size="large" prefix-icon="User"/>
        </el-form-item>
        <el-form-item label="Email">
          <el-input v-model="form.email" placeholder="you@email.com" type="email" size="large" prefix-icon="Message"/>
        </el-form-item>
        <el-form-item label="Số điện thoại">
          <el-input v-model="form.phone" placeholder="0901234567" size="large" prefix-icon="Phone"/>
        </el-form-item>
        <el-form-item label="Mật khẩu">
          <el-input v-model="form.password" placeholder="Tối thiểu 6 ký tự" type="password" size="large" prefix-icon="Lock" show-password @keyup.enter="handleRegister"/>
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%;margin-top:8px;font-size:1rem;height:46px" :loading="auth.loading" @click="handleRegister">
          TẠO TÀI KHOẢN
        </el-button>
      </el-form>

      <div style="text-align:center;color:var(--c-text2);font-size:0.875rem;margin-top:20px">
        Đã có tài khoản? <router-link to="/login" style="color:var(--c-accent);font-weight:600">Đăng nhập</router-link>
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
const form = reactive({ fullName:'', email:'', phone:'', password:'' })

async function handleRegister() {
  if (!form.fullName || !form.email || !form.password) return
  try { await auth.register(form); router.push('/app/profile') } catch {}
}
</script>

<style scoped>
.auth-page { min-height:100vh; display:flex; }
.auth-bg {
  flex:1; background:linear-gradient(135deg, #4A2810 0%, #6B4226 50%, #8C5C3E 100%);
  display:flex; flex-direction:column; align-items:center; justify-content:center; position:relative;
}
.bg-text { font-family:var(--font-display); font-size:12vw; line-height:1; color:rgba(255,255,255,0.08); }
.bg-tagline { color:rgba(255,255,255,0.4); font-size:0.9rem; margin-top:16px; }
@media(max-width:768px) { .auth-bg { display:none; } }
.auth-panel {
  width:440px; min-height:100vh; background:var(--c-card);
  display:flex; flex-direction:column; justify-content:center;
  padding:48px 40px; box-shadow:-8px 0 32px rgba(107,66,38,0.2);
}
</style>
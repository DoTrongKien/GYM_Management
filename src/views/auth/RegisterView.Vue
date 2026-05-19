<template>
  <div class="auth-page">
    <div class="auth-bg">
      <div class="bg-text">JOIN</div>
    </div>
    <div class="auth-panel fade-in">
      <div class="auth-logo">
        <span class="display accent">GYM</span><span class="display">PRO</span>
      </div>
      <p class="auth-sub">Tạo tài khoản — bắt đầu hành trình</p>

      <el-form :model="form" label-position="top" class="auth-form">
        <el-form-item label="Họ và tên">
          <el-input v-model="form.fullName" placeholder="Nguyễn Văn A" size="large" prefix-icon="User" />
        </el-form-item>
        <el-form-item label="Email">
          <el-input v-model="form.email" placeholder="you@email.com" type="email" size="large" prefix-icon="Message" />
        </el-form-item>
        <el-form-item label="Số điện thoại">
          <el-input v-model="form.phone" placeholder="0901234567" size="large" prefix-icon="Phone" />
        </el-form-item>
        <el-form-item label="Mật khẩu">
          <el-input v-model="form.password" placeholder="Tối thiểu 6 ký tự" type="password" size="large" prefix-icon="Lock" show-password />
        </el-form-item>

        <el-button type="primary" size="large" style="width:100%;margin-top:8px" :loading="auth.loading" @click="handleRegister">
          TẠO TÀI KHOẢN
        </el-button>
      </el-form>

      <div class="auth-footer">
        Đã có tài khoản? <router-link to="/login">Đăng nhập</router-link>
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
const form   = reactive({ fullName: '', email: '', phone: '', password: '' })

async function handleRegister() {
  try {
    await auth.register(form)
    router.push('/app/profile')
  } catch {}
}
</script>

<style scoped>
.auth-page { min-height:100vh; display:flex; align-items:stretch; }
.auth-bg {
  flex:1; background:var(--c-bg2); border-right:1px solid var(--c-border);
  display:flex; align-items:center; justify-content:center; overflow:hidden;
}
.bg-text {
  font-family:var(--font-display); font-size:20vw; line-height:1;
  color:transparent; -webkit-text-stroke:1px var(--c-border);
  user-select:none; transform:rotate(-15deg);
}
@media(max-width:768px){.auth-bg{display:none;}}
.auth-panel {
  width:440px; min-height:100vh; display:flex; flex-direction:column;
  justify-content:center; padding:48px 40px;
}
.auth-logo { font-size:3rem; line-height:1; margin-bottom:8px; }
.auth-sub  { color:var(--c-text2); font-size:0.85rem; margin-bottom:36px; }
.auth-form { margin-bottom:24px; }
.auth-footer { text-align:center; color:var(--c-text2); font-size:0.875rem; }
</style>
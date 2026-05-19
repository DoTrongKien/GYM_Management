import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authAPI } from '@/api'
import { ElMessage } from 'element-plus'

export const useAuthStore = defineStore('auth', () => {
  const token   = ref(localStorage.getItem('token') || '')
  const user    = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  const loading = ref(false)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin    = computed(() => user.value?.role === 'ROLE_ADMIN')
  const isUser     = computed(() => user.value?.role === 'ROLE_USER')

  async function login(credentials) {
    loading.value = true
    try {
      // Interceptor: res.data => ApiResponse { success, message, data: AuthResponse }
      // Nên authData = res.data = AuthResponse { token, role, userId, ... }
      const res      = await authAPI.login(credentials)
      const authData = res.data  // AuthResponse nằm trong ApiResponse.data

      token.value = authData.token
      user.value  = {
        userId:        authData.userId,
        fullName:      authData.fullName,
        email:         authData.email,
        role:          authData.role,
        emailVerified: authData.emailVerified
      }
      localStorage.setItem('token', authData.token)
      localStorage.setItem('user',  JSON.stringify(user.value))
      ElMessage.success('Đăng nhập thành công!')
      return authData  // LoginView dùng authData.role để redirect
    } finally {
      loading.value = false
    }
  }

  async function register(payload) {
    loading.value = true
    try {
      const res      = await authAPI.register(payload)
      const authData = res.data

      token.value = authData.token
      user.value  = {
        userId:        authData.userId,
        fullName:      authData.fullName,
        email:         authData.email,
        role:          authData.role,
        emailVerified: authData.emailVerified
      }
      localStorage.setItem('token', authData.token)
      localStorage.setItem('user',  JSON.stringify(user.value))
      ElMessage.success('Đăng ký thành công!')
      return authData
    } finally {
      loading.value = false
    }
  }

  function logout() {
    token.value = ''
    user.value  = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    ElMessage.info('Đã đăng xuất')
  }

  return { token, user, loading, isLoggedIn, isAdmin, isUser, login, register, logout }
})
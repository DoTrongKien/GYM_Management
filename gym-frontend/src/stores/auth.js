import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authAPI } from '@/api'
import { ElMessage } from 'element-plus'

// ─────────────────────────────────────────────
// Decode JWT
// ─────────────────────────────────────────────
function parseJwt(token) {
  try {
    const parts = token.split('.')

    // JWT phải có đúng 3 phần
    if (parts.length !== 3) {
      return null
    }

    const base64 = parts[1]
        .replace(/-/g, '+')
        .replace(/_/g, '/')

    return JSON.parse(atob(base64))
  } catch (err) {
    console.error('JWT Parse Error:', err)
    return null
  }
}

// ─────────────────────────────────────────────
// Kiểm tra token còn hạn
// ─────────────────────────────────────────────
function isTokenValid(token) {
  if (!token) return false

  const payload = parseJwt(token)

  if (!payload) return false
  if (!payload.exp) return false

  return payload.exp * 1000 > Date.now()
}

export const useAuthStore = defineStore('auth', () => {

  // ───────────────────────────────────────────
  // Lấy token
  // ───────────────────────────────────────────
  const token = ref(
      localStorage.getItem('token') || ''
  )

  // ───────────────────────────────────────────
  // Lấy user an toàn
  // ───────────────────────────────────────────
  let storedUser = null

  try {
    const rawUser = localStorage.getItem('user')

    storedUser =
        rawUser &&
        rawUser !== 'undefined'
            ? JSON.parse(rawUser)
            : null
  } catch (err) {
    console.error('USER PARSE ERROR:', err)
    storedUser = null
  }

  const user = ref(storedUser)

  const loading = ref(false)

  // ───────────────────────────────────────────
  // Nếu token lỗi → logout luôn
  // ───────────────────────────────────────────
  if (!isTokenValid(token.value)) {
    token.value = ''
    user.value = null

    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  // ───────────────────────────────────────────
  // Computed
  // ───────────────────────────────────────────
  const isLoggedIn = computed(() => {
    return !!token.value && isTokenValid(token.value)
  })

  const isAdmin = computed(() => {
    return user.value?.role === 'ROLE_ADMIN'
  })

  const isUser = computed(() => {
    return user.value?.role === 'ROLE_USER'
  })

  // ───────────────────────────────────────────
  // Check token
  // ───────────────────────────────────────────
  function checkToken() {
    const valid = isTokenValid(token.value)

    if (!valid) {
      logout()
      return false
    }

    return true
  }

  // ───────────────────────────────────────────
  // Login
  // ───────────────────────────────────────────
  async function login(credentials) {
    loading.value = true

    try {
      const res = await authAPI.login(credentials)

      const authData = res.data

      if (!authData?.token) {
        throw new Error('Token không tồn tại')
      }

      if (!isTokenValid(authData.token)) {
        throw new Error('Token không hợp lệ')
      }

      token.value = authData.token

      user.value = {
        userId: authData.userId,
        fullName: authData.fullName,
        email: authData.email,
        role: authData.role,
        emailVerified: authData.emailVerified
      }

      // Save localStorage
      localStorage.setItem('token', token.value)

      localStorage.setItem(
          'user',
          JSON.stringify(user.value)
      )

      ElMessage.success('Đăng nhập thành công!')

      return authData

    } catch (err) {
      console.error('LOGIN ERROR:', err)

      logout()

      ElMessage.error(
          err?.response?.data?.message ||
          err.message ||
          'Đăng nhập thất bại'
      )

      throw err

    } finally {
      loading.value = false
    }
  }

  // ───────────────────────────────────────────
  // Register
  // ───────────────────────────────────────────
  async function register(payload) {
    loading.value = true

    try {
      const res = await authAPI.register(payload)

      const authData = res.data

      if (!authData?.token) {
        throw new Error('Token không tồn tại')
      }

      if (!isTokenValid(authData.token)) {
        throw new Error('Token không hợp lệ')
      }

      token.value = authData.token

      user.value = {
        userId: authData.userId,
        fullName: authData.fullName,
        email: authData.email,
        role: authData.role,
        emailVerified: authData.emailVerified
      }

      localStorage.setItem('token', token.value)

      localStorage.setItem(
          'user',
          JSON.stringify(user.value)
      )

      ElMessage.success('Đăng ký thành công!')

      return authData

    } catch (err) {
      console.error('REGISTER ERROR:', err)

      logout()

      ElMessage.error(
          err?.response?.data?.message ||
          err.message ||
          'Đăng ký thất bại'
      )

      throw err

    } finally {
      loading.value = false
    }
  }

  // ───────────────────────────────────────────
  // Logout
  // ───────────────────────────────────────────
  function logout() {
    token.value = ''
    user.value = null

    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return {
    token,
    user,
    loading,

    isLoggedIn,
    isAdmin,
    isUser,

    checkToken,

    login,
    register,
    logout
  }
})
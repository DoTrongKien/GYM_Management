import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authAPI } from '@/api'
import { ElMessage } from 'element-plus'

export const useAuthStore = defineStore('auth', () => {
    const token    = ref(localStorage.getItem('token') || '')
    const user     = ref(JSON.parse(localStorage.getItem('user') || 'null'))
    const loading  = ref(false)

    const isLoggedIn  = computed(() => !!token.value)
    const isAdmin     = computed(() => user.value?.role === 'ROLE_ADMIN')
    const isUser      = computed(() => user.value?.role === 'ROLE_USER')

    function setAuth(data) {
        token.value = data.token
        user.value  = {
            userId:       data.data.userId,
            fullName:     data.data.fullName,
            email:        data.data.email,
            role:         data.data.role,
            emailVerified:data.data.emailVerified
        }
        localStorage.setItem('token', data.data.token)
        localStorage.setItem('user',  JSON.stringify(user.value))
    }

    async function login(credentials) {
        loading.value = true
        try {
            const res = await authAPI.login(credentials)
            token.value = res.data.token
            user.value  = {
                userId:       res.data.userId,
                fullName:     res.data.fullName,
                email:        res.data.email,
                role:         res.data.role,
                emailVerified:res.data.emailVerified
            }
            localStorage.setItem('token', res.data.token)
            localStorage.setItem('user',  JSON.stringify(user.value))
            ElMessage.success('Đăng nhập thành công!')
            return res.data
        } finally {
            loading.value = false
        }
    }

    async function register(payload) {
        loading.value = true
        try {
            const res = await authAPI.register(payload)
            token.value = res.data.token
            user.value  = {
                userId:       res.data.userId,
                fullName:     res.data.fullName,
                email:        res.data.email,
                role:         res.data.role,
                emailVerified:res.data.emailVerified
            }
            localStorage.setItem('token', res.data.token)
            localStorage.setItem('user',  JSON.stringify(user.value))
            ElMessage.success('Đăng ký thành công!')
            return res.data
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
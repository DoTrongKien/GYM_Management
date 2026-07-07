import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
    baseURL: '/api',
    timeout: 15000,
    headers: { 'Content-Type': 'application/json' }
})

// Request interceptor — attach JWT
api.interceptors.request.use(config => {
    const token = localStorage.getItem('token')
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
})

// Response interceptor — handle errors globally
api.interceptors.response.use(
    res => res.data,
    err => {
        const msg = err.response?.data?.message || 'Lỗi kết nối server'
        if (err.response?.status === 401) {
            localStorage.clear()
            window.location.href = '/login'
        } else if (err.response?.status !== 404) {
            ElMessage.error(msg)
        }
        return Promise.reject(err)
    }
)

export default api

// ── Auth ─────────────────────────────────────
export const authAPI = {
    register: (data) => api.post('/auth/register', data),
    login:    (data) => api.post('/auth/login', data),
    verify:   (token) => api.get(`/auth/verify-email?token=${token}`)
}

// ── Profile ───────────────────────────────────
export const profileAPI = {
    get:    ()     => api.get('/profile'),
    save:   (data) => api.post('/profile', data),
    getById:(id)   => api.get(`/profile/${id}`)
}

// ── Workout Plans ─────────────────────────────
export const planAPI = {
    generate:          ()     => api.post('/workout-plans/generate'),
    generateWithGoal: (data) => api.post('/workout-plans/generate-with-goal', data),
    getActive:    ()     => api.get('/workout-plans/active'),
    getAll:       ()     => api.get('/workout-plans'),
    createCustom: (data) => api.post('/workout-plans', data),
    adjustWeek:   (id, data) => api.post(`/workout-plans/${id}/adjust-week`, data),
    // === MỚI: User xem & chọn giáo án mẫu do admin tạo ===
    getTemplates:    ()    => api.get('/workout-plans/templates'),
    selectTemplate:  (id)  => api.post(`/workout-plans/templates/${id}/select`)
}

// ── Sessions ──────────────────────────────────
export const sessionAPI = {
    getAll:       ()           => api.get('/sessions'),
    getWeek:      ()           => api.get('/sessions/this-week'),
    getById:      (id)         => api.get(`/sessions/${id}`),
    checkIn:    (id)         => api.post(`/sessions/${id}/check-in`),
    enroll:       (data)       => api.post('/sessions/enroll', data),
    schedule:   (data)       => api.post('/sessions/schedule', data),
    complete:     (id, data)   => api.post(`/sessions/${id}/check-out`, data),
    skip:         (id, notes)  => api.post(`/sessions/${id}/skip`, { notes }),
    delete:       (id)         => api.delete(`/sessions/${id}`),
    // Thêm hàm bọc API lấy tiến độ tuần từ WorkoutSessionController
    getWeekProgress: (planId, weekNumber) => api.get(`/sessions/week-progress?planId=${planId}&weekNumber=${weekNumber}`)
}

export const petAPI = {
    get: () => api.get('/pet')
}

// ── Progress ──────────────────────────────────
export const progressAPI = {
    getAll:   ()       => api.get('/progress'),
    getLatest:()       => api.get('/progress/latest'),
    add:      (data)   => api.post('/progress', data),
    update:   (id, d)  => api.put(`/progress/${id}`, d)
}

// ── Memberships ───────────────────────────────
export const membershipAPI = {
    getAll:         ()     => api.get('/memberships'),
    getActive:      ()     => api.get('/memberships/active'),
    purchase:       (data) => api.post('/memberships', data),
    confirmPayment: (id, txId) => api.post(`/memberships/${id}/confirm-payment`, { transactionId: txId })
}

// ── Invoices (MoMo QR) ─────────────────────────
export const invoiceAPI = {
    create:        (membershipType) => api.post('/invoices', { membershipType }),
    getAll:        ()   => api.get('/invoices'),
    getOne:        (id) => api.get(`/invoices/${id}`),
    regenerateQr:  (id) => api.post(`/invoices/${id}/regenerate-qr`),
    cancel:        (id) => api.post(`/invoices/${id}/cancel`)
}

// ── Nutrition ─────────────────────────────────
export const nutritionAPI = {
    generate:   ()  => api.post('/nutrition/generate'),
    getLatest:  ()  => api.get('/nutrition'),
    getHistory: ()  => api.get('/nutrition/history')
}

// ── Exercises ─────────────────────────────────
export const exerciseAPI = {
    getAll:   (params) => api.get('/exercises', { params }),
    getById:  (id)     => api.get(`/exercises/${id}`),
    create:   (data)   => api.post('/exercises', data),
    update:   (id, d)  => api.put(`/exercises/${id}`, d),
    delete:   (id)     => api.delete(`/exercises/${id}`)
}

// ── Ratings ───────────────────────────────────
export const ratingAPI = {
    add:          (data)         => api.post('/ratings', data),
    getPublic:    ()             => api.get('/ratings/public'),
    getMy:        ()             => api.get('/ratings/my'),
    getAverages:  ()             => api.get('/ratings/averages'),
    // Admin
    getAll:       ()             => api.get('/ratings/admin/all'),
    adminReply:   (id, reply)    => api.post(`/ratings/admin/${id}/reply`, { reply })
}

// ── Notifications ─────────────────────────────
export const notifAPI = {
    getAll:       () => api.get('/notifications'),
    getUnread:    () => api.get('/notifications/unread-count'),
    markAllRead:  () => api.post('/notifications/mark-all-read')
}

// ── Dashboard ─────────────────────────────────
export const dashboardAPI = {
    get: () => api.get('/dashboard')
}

// ── Admin ─────────────────────────────────────
export const adminAPI = {
    dashboard:          ()           => api.get('/admin/dashboard'),
    getUsers:           ()           => api.get('/admin/users'),
    getUserById:        (id)         => api.get(`/admin/users/${id}`),
    getUserProfile:     (id)         => api.get(`/admin/users/${id}/profile`),
    toggleStatus:       (id, status) => api.put(`/admin/users/${id}/status`, { status }),
    resetPassword:      (id, pwd)    => api.put(`/admin/users/${id}/reset-password`, { newPassword: pwd }),
    deleteUser:         (id)         => api.delete(`/admin/users/${id}`),
    getMemberships:     ()           => api.get('/admin/memberships'),
    getPending:         ()           => api.get('/admin/memberships/pending'),
    confirmPayment:     (id)         => api.post(`/admin/memberships/${id}/confirm-payment`),
    refund:             (id)         => api.put(`/admin/memberships/${id}/refund`),
    getUserMemberships: (uid)        => api.get(`/admin/memberships/user/${uid}`),
    getRevenue:         ()           => api.get('/admin/stats/revenue'),
    getPlans:           ()           => api.get('/admin/workout-plans'),
    getUserPlans:       (uid)        => api.get(`/admin/workout-plans/user/${uid}`),
    deletePlan:         (id)         => api.delete(`/admin/workout-plans/${id}`),
    getTemplates:       ()           => api.get('/admin/workout-plans/templates'),
    createTemplate:     (data)       => api.post('/admin/workout-plans/templates', data),
    updateTemplate:     (id, data)   => api.put(`/admin/workout-plans/templates/${id}`, data),
    deleteTemplate:     (id)         => api.delete(`/admin/workout-plans/templates/${id}`),
    broadcast:          (data)       => api.post('/admin/notifications/broadcast', data),
    sendToUser:         (uid, data)  => api.post(`/admin/notifications/user/${uid}`, data)
}
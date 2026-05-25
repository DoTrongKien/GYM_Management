import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // ── Public ──────────────────────────────
    { path: '/',        redirect: '/login' },
    { path: '/login',   component: () => import('@/views/auth/LoginView.vue'),    meta: { guest: true } },
    { path: '/register',component: () => import('@/views/auth/RegisterView.vue'), meta: { guest: true } },

    // ── User Layout ──────────────────────────
    {
      path: '/app',
      component: () => import('@/components/common/UserLayout.vue'),
      meta: { requiresAuth: true, role: 'ROLE_USER' },
      children: [
        { path: '',            redirect: '/app/dashboard' },
        { path: 'dashboard',   component: () => import('@/views/user/DashboardView.vue'),  name: 'UserDashboard' },
        { path: 'profile',     component: () => import('@/views/user/ProfileView.vue'),    name: 'Profile' },
        { path: 'plan',        component: () => import('@/views/user/WorkoutPlanView.vue'),name: 'WorkoutPlan' },
        { path: 'sessions',    component: () => import('@/views/user/SessionsView.vue'),   name: 'Sessions' },
        { path: 'progress',    component: () => import('@/views/user/ProgressView.vue'),   name: 'Progress' },
        { path: 'nutrition',   component: () => import('@/views/user/NutritionView.vue'),  name: 'Nutrition' },
        { path: 'membership',  component: () => import('@/views/user/MembershipView.vue'), name: 'Membership' },
        { path: 'exercises',   component: () => import('@/views/user/ExercisesView.vue'),  name: 'Exercises' },
        { path: 'ratings',     component: () => import('@/views/user/RatingsView.vue'),    name: 'Ratings' },
      ]
    },

    // ── Admin Layout ─────────────────────────
    {
      path: '/admin',
      component: () => import('@/components/common/AdminLayout.vue'),
      meta: { requiresAuth: true, role: 'ROLE_ADMIN' },
      children: [
        { path: '',           redirect: '/admin/dashboard' },
        { path: 'dashboard',  component: () => import('@/views/admin/AdminDashboard.vue'),  name: 'AdminDashboard' },
        { path: 'users',      component: () => import('@/views/admin/UsersView.vue'),       name: 'AdminUsers' },
        { path: 'memberships',component: () => import('@/views/admin/MembershipsView.vue'), name: 'AdminMemberships' },
        { path: 'exercises',  component: () => import('@/views/admin/ExercisesAdmin.vue'),  name: 'AdminExercises' },
        { path: 'plans',      component: () => import('@/views/admin/PlansView.vue'),       name: 'AdminPlans' },
        { path: 'notify',     component: () => import('@/views/admin/NotifyView.vue'),      name: 'AdminNotify' },
      ]
    },

    { path: '/:pathMatch(.*)*', redirect: '/login' }
  ]
})

router.beforeEach((to, _from, next) => {
    const auth = useAuthStore()

    // Chưa đăng nhập
    if (!auth.isLoggedIn) {
        if (to.path !== '/login' && to.path !== '/register') {
            return next('/login')
        }
        return next()
    }

    // Đã đăng nhập mà vào login
    if (to.path === '/login' || to.path === '/register') {
        return next(auth.isAdmin ? '/admin/dashboard' : '/app/dashboard')
    }

    // Kiểm tra quyền
    if (to.meta.role && auth.user?.role !== to.meta.role) {
        return next(auth.isAdmin ? '/admin/dashboard' : '/app/dashboard')
    }

    next()
})

export default router

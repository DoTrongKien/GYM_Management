import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
    history: createWebHistory(),

    routes: [

        // ─────────────────────────────────────────
        // PUBLIC
        // ─────────────────────────────────────────
        {
            path: '/',
            redirect: '/login'
        },

        {
            path: '/login',
            component: () =>
                import('@/views/auth/LoginView.vue'),

            meta: {
                guest: true
            }
        },

        {
            path: '/register',
            component: () =>
                import('@/views/auth/RegisterView.vue'),

            meta: {
                guest: true
            }
        },

        // ─────────────────────────────────────────
        // USER
        // ─────────────────────────────────────────
        {
            path: '/app',

            component: () =>
                import('@/components/common/UserLayout.vue'),

            meta: {
                requiresAuth: true,
                role: 'ROLE_USER'
            },

            children: [
                {
                    path: '',
                    redirect: '/app/dashboard'
                },

                {
                    path: 'dashboard',
                    name: 'UserDashboard',
                    component: () =>
                        import('@/views/user/DashboardView.vue')
                },

                {
                    path: 'profile',
                    name: 'Profile',
                    component: () =>
                        import('@/views/user/ProfileView.vue')
                },

                {
                    path: 'plan',
                    name: 'WorkoutPlan',
                    component: () =>
                        import('@/views/user/WorkoutPlanView.vue')
                },

                {
                    path: 'sessions',
                    name: 'Sessions',
                    component: () =>
                        import('@/views/user/SessionsView.vue')
                },

                {
                    path: 'progress',
                    name: 'Progress',
                    component: () =>
                        import('@/views/user/ProgressView.vue')
                },

                {
                    path: 'nutrition',
                    name: 'Nutrition',
                    component: () =>
                        import('@/views/user/NutritionView.vue')
                },

                {
                    path: 'membership',
                    name: 'Membership',
                    component: () =>
                        import('@/views/user/MembershipView.vue')
                },

                {
                    path: 'payment/:invoiceId',
                    name: 'UserPaymentQR',
                    component: () =>
                        import('@/views/user/PaymentQR.vue')
                },

                {
                    path: 'exercises',
                    name: 'Exercises',
                    component: () =>
                        import('@/views/user/ExercisesView.vue')
                },

                {
                    path: 'ratings',
                    name: 'Ratings',
                    component: () =>
                        import('@/views/user/RatingsView.vue')
                },

                {
                    path: 'chat',
                    name: 'Chat',
                    component: () =>
                        import('@/views/user/ChatView.vue')
                }
            ]
        },

        // ─────────────────────────────────────────
        // ADMIN
        // ─────────────────────────────────────────
        {
            path: '/admin',

            component: () =>
                import('@/components/common/AdminLayout.vue'),

            meta: {
                requiresAuth: true,
                role: 'ROLE_ADMIN'
            },

            children: [
                {
                    path: '',
                    redirect: '/admin/dashboard'
                },

                {
                    path: 'dashboard',
                    name: 'AdminDashboard',
                    component: () =>
                        import('@/views/admin/AdminDashboard.vue')
                },

                {
                    path: 'users',
                    name: 'AdminUsers',
                    component: () =>
                        import('@/views/admin/UsersView.vue')
                },

                {
                    path: 'memberships',
                    name: 'AdminMemberships',
                    component: () =>
                        import('@/views/admin/MembershipsView.vue')
                },
                {
                    path: 'invoices',
                    name: 'AdminInvoices',
                    component: () =>
                        import('@/views/admin/InvoiceHistoryView.vue')
                },

                {
                    path: 'exercises',
                    name: 'AdminExercises',
                    component: () =>
                        import('@/views/admin/ExercisesAdmin.vue')
                },

                {
                    path: 'plans',
                    name: 'AdminPlans',
                    component: () =>
                        import('@/views/admin/PlansView.vue')
                },

                {
                    path: 'ratings',
                    name: 'AdminRatings',
                    component: () =>
                        import('@/views/admin/AdminRatingsView.vue')
                },

                {
                    path: 'support',
                    name: 'AdminSupport',
                    component: () =>
                        import('@/views/admin/SupportView.vue')
                },

                {
                    path: 'notify',
                    name: 'AdminNotify',
                    component: () =>
                        import('@/views/admin/NotifyView.vue')
                }
            ]
        },

        // ─────────────────────────────────────────
        // 404
        // ─────────────────────────────────────────
        {
            path: '/:pathMatch(.*)*',
            redirect: '/login'
        }
    ]
})

// ─────────────────────────────────────────────
// ROUTER GUARD
// ─────────────────────────────────────────────
router.beforeEach((to, from, next) => {

    const auth = useAuthStore()

    console.log('PATH:', to.path)
    console.log('TOKEN:', auth.token)
    console.log('USER:', auth.user)

    // ─────────────────────────────────────────
    // Route cần login
    // ─────────────────────────────────────────
    if (to.meta.requiresAuth) {

        const valid = auth.checkToken()

        console.log('TOKEN VALID:', valid)

        // Chưa login
        if (!valid) {
            return next('/login')
        }

        // Sai role
        if (
            to.meta.role &&
            auth.user?.role !== to.meta.role
        ) {
            return next(
                auth.isAdmin
                    ? '/admin/dashboard'
                    : '/app/dashboard'
            )
        }

        return next()
    }

    // ─────────────────────────────────────────
    // Guest route
    // ─────────────────────────────────────────
    if (to.meta.guest) {

        const valid = auth.checkToken()

        if (valid) {
            return next(
                auth.isAdmin
                    ? '/admin/dashboard'
                    : '/app/dashboard'
            )
        }

        return next()
    }

    next()
})

export default router
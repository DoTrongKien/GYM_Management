<template>
  <div class="layout">
    <aside class="sidebar" :class="{ collapsed }">
      <div class="sidebar-logo" @click="router.push('/app/dashboard')">
        <span class="display" style="color:var(--c-text-inv);font-size:1.8rem">GYM</span>
        <span class="display accent" style="font-size:1.8rem" v-show="!collapsed">PRO</span>
        <span v-if="isVip" class="vip-badge" v-show="!collapsed">👑 VIP</span>
      </div>

      <el-menu :default-active="route.path" router class="sidebar-menu" :collapse="collapsed">
        <el-menu-item index="/app/dashboard"><el-icon><DataAnalysis/></el-icon><template #title>Dashboard</template></el-menu-item>
        <el-menu-item index="/app/profile"><el-icon><User/></el-icon><template #title>Hồ sơ</template></el-menu-item>
        <el-menu-item index="/app/plan"><el-icon><Calendar/></el-icon><template #title>Giáo án</template></el-menu-item>
        <el-menu-item index="/app/sessions"><el-icon><Timer/></el-icon><template #title>Buổi tập</template></el-menu-item>
        <el-menu-item index="/app/progress"><el-icon><TrendCharts/></el-icon><template #title>Tiến độ</template></el-menu-item>
        <el-menu-item index="/app/nutrition"><el-icon><Apple/></el-icon><template #title>Dinh dưỡng</template></el-menu-item>
        <el-menu-item index="/app/membership"><el-icon><CreditCard/></el-icon><template #title>Gói tập</template></el-menu-item>
        <el-menu-item index="/app/exercises"><el-icon><Trophy/></el-icon><template #title>Bài tập</template></el-menu-item>
        <el-menu-item index="/app/ratings"><el-icon><Star/></el-icon><template #title>Đánh giá</template></el-menu-item>
        <el-menu-item index="/app/chat">
          <el-badge :value="unreadCount" :max="9" :hidden="!unreadCount" class="menu-badge">
            <el-icon><ChatDotRound/></el-icon>
          </el-badge>
          <template #title>Trợ lý</template>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-bottom">
        <div class="user-info" v-show="!collapsed">
          <div class="user-avatar">{{ initials }}</div>
          <div class="user-meta">
            <div class="user-name">{{ auth.user?.fullName }}</div>
            <div class="user-role">Member</div>
          </div>
        </div>
        <el-button text @click="auth.logout(); router.push('/login')" class="logout-btn">
          <el-icon><SwitchButton/></el-icon>
          <span v-show="!collapsed" style="margin-left:6px">Đăng xuất</span>
        </el-button>
      </div>
    </aside>

    <div class="main-area">
      <header class="topbar">
        <el-button text @click="collapsed=!collapsed" class="toggle-btn">
          <el-icon size="20"><Fold v-if="!collapsed"/><Expand v-else/></el-icon>
        </el-button>
        <div class="topbar-title display">{{ pageTitle }}</div>
        <div style="flex:1"/>
        <NotificationBell/>
      </header>
      <main class="page-content">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component"/>
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { membershipAPI, supportAPI } from '@/api'
import { ElNotification } from 'element-plus'
import { setOtherRole, setSessions, unreadCount } from '@/stores/supportUnread'
import NotificationBell from './NotificationBell.vue'

const auth      = useAuthStore()
const route     = useRoute()
const router    = useRouter()
const collapsed = ref(false)
const isVip     = ref(false)
const initials  = computed(() => (auth.user?.fullName || 'U').split(' ').map(w=>w[0]).join('').toUpperCase().slice(0,2))

// ── Thông báo tin nhắn mới từ admin (hoạt động ở mọi trang) ──
const lastMsgSeen = new Map()       // id phiên -> lastMessageAt đã thấy
let firstSupportLoad = true
let supportTimer = null

setOtherRole('ADMIN')               // phía bên kia của user là admin

async function pollSupport() {
  try {
    const res = await supportAPI.sessions()
    const list = res.data || []
    setSessions(list)               // cập nhật badge chưa đọc
    const openIds = new Set(list.map(s => s.id))
    list.forEach(s => {
      const prevAt = lastMsgSeen.get(s.id)
      if (s.lastMessageAt) {
        // Bỏ qua khi đang ở trang chat — ChatView tự báo, tránh thông báo trùng
        if (!firstSupportLoad && s.lastMessageRole === 'ADMIN' && prevAt
            && new Date(s.lastMessageAt).getTime() > new Date(prevAt).getTime()
            && route.path !== '/app/chat') {
          notifyNewAdminMessage(s)
        }
        lastMsgSeen.set(s.id, s.lastMessageAt)
      }
    })
    lastMsgSeen.forEach((_, id) => { if (!openIds.has(id)) lastMsgSeen.delete(id) })
    firstSupportLoad = false
  } catch {}
}

function notifyNewAdminMessage(s) {
  const inst = ElNotification({
    title: `💬 ${s.adminName || 'Admin'} vừa nhắn`,
    type: 'success',
    duration: 6000,
    message: h('div', {
      style: 'cursor:pointer',
      onClick: () => { router.push('/app/chat'); inst.close() }
    }, s.lastMessage || 'Đã gửi một tin nhắn')
  })
}

onMounted(async () => {
  try {
    const res = await membershipAPI.getActive()
    isVip.value = res.data?.membershipType === 'VIP'
  } catch { isVip.value = false }
  pollSupport()
  supportTimer = setInterval(pollSupport, 5000)
})

onUnmounted(() => { if (supportTimer) clearInterval(supportTimer) })

const titles = {
  '/app/dashboard':'Dashboard', '/app/profile':'Hồ sơ cá nhân',
  '/app/plan':'Giáo án tập', '/app/sessions':'Lịch sử buổi tập',
  '/app/progress':'Theo dõi tiến độ', '/app/nutrition':'Dinh dưỡng',
  '/app/membership':'Gói tập', '/app/exercises':'Thư viện bài tập', '/app/ratings':'Đánh giá',
  '/app/chat':'Trợ lý',
}
const pageTitle = computed(() => titles[route.path] || 'GymPro')
</script>

<style scoped>
.layout { display:flex; height:100vh; overflow:hidden; }

.sidebar {
  width:220px; height:100vh; flex-shrink:0;
  background: var(--c-bg2);
  border-right: 1px solid var(--c-bg3);
  display:flex; flex-direction:column;
  transition: width 0.25s ease;
  box-shadow: 2px 0 12px rgba(0,0,0,0.2);
}
.sidebar.collapsed { width:64px; }

.sidebar-logo {
  height:60px; padding:0 16px; cursor:pointer;
  display:flex; align-items:center; gap:6px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
  flex-shrink:0;
}
.vip-badge {
  font-size:0.65rem; font-weight:700; letter-spacing:0.04em;
  background:linear-gradient(135deg,#f5c518,#d4892a);
  color:#2b1b17; padding:2px 8px; border-radius:20px;
  white-space:nowrap;
}

.sidebar-menu { flex:1; overflow-y:auto; overflow-x:hidden; padding:10px 0; }

.sidebar-bottom {
  padding:12px; border-top:1px solid rgba(255,255,255,0.1); flex-shrink:0;
}
.user-info { display:flex; align-items:center; gap:10px; margin-bottom:10px; overflow:hidden; }
.user-avatar {
  width:34px; height:34px; border-radius:50%; flex-shrink:0;
  background: var(--c-accent); color:#fff;
  display:flex; align-items:center; justify-content:center;
  font-family:var(--font-display); font-size:0.95rem; font-weight:700;
}
.user-name { font-size:0.83rem; font-weight:600; color:var(--c-text-inv); white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.user-role { font-size:0.72rem; color:var(--c-text-inv2); }
.logout-btn { color:var(--c-text-inv2) !important; width:100%; justify-content:flex-start; }
.logout-btn:hover { color:var(--c-text-inv) !important; }

.main-area { flex:1; display:flex; flex-direction:column; overflow:hidden; background:var(--c-bg); }

.topbar {
  height:56px; padding:0 24px; flex-shrink:0;
  display:flex; align-items:center; gap:12px;
  background: var(--c-bg2);
  border-bottom: 1px solid var(--c-bg3);
}
.toggle-btn { color:var(--c-text-inv2) !important; }
.topbar-title { font-size:1.1rem; color:var(--c-text-inv); letter-spacing:0.08em; }

.page-content { flex:1; overflow-y:auto; padding:28px; background:var(--c-bg); }

.page-enter-active,.page-leave-active { transition:opacity 0.2s; }
.page-enter-from,.page-leave-to { opacity:0; }

/* Badge đỏ số tin nhắn hỗ trợ chưa đọc trên menu item */
.sidebar-menu .el-menu-item .menu-badge { display:inline-flex; align-items:center; }
.sidebar-menu .el-menu-item .menu-badge :deep(.el-badge__content) {
  top:4px; right:4px; border:none; font-family:var(--font-mono); font-size:0.62rem;
}
</style>
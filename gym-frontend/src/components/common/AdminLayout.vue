<template>
  <div class="layout">
    <aside class="sidebar" :class="{ collapsed }">
      <div class="sidebar-logo" @click="router.push('/admin/dashboard')">
        <span class="display" style="color:var(--c-text-inv);font-size:1.8rem">GYM</span>
        <span class="display accent" style="font-size:1.8rem" v-show="!collapsed">PRO</span>
        <span class="admin-badge" v-show="!collapsed">ADMIN</span>
      </div>

      <el-menu :default-active="route.path" router class="sidebar-menu" :collapse="collapsed">
        <el-menu-item index="/admin/dashboard"><el-icon><DataAnalysis/></el-icon><template #title>Dashboard</template></el-menu-item>
        <el-menu-item index="/admin/users"><el-icon><UserFilled/></el-icon><template #title>Người dùng</template></el-menu-item>
        <el-menu-item index="/admin/memberships"><el-icon><CreditCard/></el-icon><template #title>Hóa đơn</template></el-menu-item>
        <el-menu-item index="/admin/exercises"><el-icon><Trophy/></el-icon><template #title>Bài tập</template></el-menu-item>
        <el-menu-item index="/admin/plans"><el-icon><Calendar/></el-icon><template #title>Giáo án</template></el-menu-item>
        <el-menu-item index="/admin/ratings"><el-icon><Star/></el-icon><template #title>Đánh giá</template></el-menu-item>
        <el-menu-item index="/admin/support">
          <el-badge :value="supportBadge" :max="9" :hidden="!supportBadge" class="menu-badge">
            <el-icon><ChatDotRound/></el-icon>
          </el-badge>
          <template #title>Hỗ trợ chat</template>
        </el-menu-item>
        <el-menu-item index="/admin/notify"><el-icon><Bell/></el-icon><template #title>Thông báo</template></el-menu-item>
      </el-menu>

      <div class="sidebar-bottom">
        <div class="user-info" v-show="!collapsed">
          <div class="user-avatar">AD</div>
          <div class="user-meta">
            <div class="user-name">{{ auth.user?.fullName }}</div>
            <div class="user-role accent" style="font-size:0.7rem;letter-spacing:0.08em">ADMINISTRATOR</div>
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
        <div class="topbar-title display">ADMIN PANEL</div>
        <div style="flex:1"/>
        <el-tag type="warning" size="small" style="font-family:var(--font-mono);font-size:0.7rem">ADMIN</el-tag>
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
import { ref, computed, h, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { adminSupportAPI } from '@/api'
import { ElNotification, ElButton, ElMessage } from 'element-plus'
import { setOtherRole, setSessions, unreadCount } from '@/stores/supportUnread'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const collapsed = ref(false)

// ── Thông báo yêu cầu hỗ trợ mới ─────────────────
const pendingCount = ref(0)
const seenPending = new Set()      // id các yêu cầu đã hiển thị thông báo
const notifs = new Map()           // id -> instance thông báo đang mở
const lastMsgSeen = new Map()      // id -> lastMessageAt đã thấy (phát hiện tin nhắn mới từ user)
let firstLoad = true
let supportTimer = null

setOtherRole('USER')               // phía bên kia của admin là user

// Badge trên menu: yêu cầu đang chờ + cuộc hội thoại có tin nhắn chưa đọc
const supportBadge = computed(() => pendingCount.value + unreadCount.value)

async function pollSupport() {
  try {
    const res = await adminSupportAPI.sessions()
    const list = res.data || []
    setSessions(list)              // cập nhật badge chưa đọc
    const pend = list.filter(s => s.status === 'PENDING')
    pendingCount.value = pend.length
    const pendIds = new Set(pend.map(s => s.id))

    // Yêu cầu mới → bật popup (bỏ qua lần tải đầu để tránh spam)
    pend.forEach(s => {
      if (!seenPending.has(s.id)) {
        seenPending.add(s.id)
        if (!firstLoad) notifyNewRequest(s)
      }
    })
    // Dọn các yêu cầu không còn chờ (đã được xử lý ở nơi khác)
    seenPending.forEach(id => { if (!pendIds.has(id)) seenPending.delete(id) })
    notifs.forEach((inst, id) => { if (!pendIds.has(id)) { inst.close(); notifs.delete(id) } })

    // Tin nhắn mới từ user trong các phiên đang chat → thông báo
    const openIds = new Set(list.map(s => s.id))
    list.filter(s => s.status === 'ACTIVE').forEach(s => {
      const prevAt = lastMsgSeen.get(s.id)
      if (s.lastMessageAt) {
        if (!firstLoad && s.lastMessageRole === 'USER' && prevAt
            && new Date(s.lastMessageAt).getTime() > new Date(prevAt).getTime()) {
          notifyNewMessage(s)
        }
        lastMsgSeen.set(s.id, s.lastMessageAt)
      }
    })
    lastMsgSeen.forEach((_, id) => { if (!openIds.has(id)) lastMsgSeen.delete(id) })

    firstLoad = false
  } catch {}
}

// Tách nội dung thành đoạn chữ + link bấm được (cho popup thông báo)
const URL_RE = /(https?:\/\/[^\s<]+)/g
function contentVNodes(text) {
  const out = []
  let last = 0, m
  URL_RE.lastIndex = 0
  while ((m = URL_RE.exec(text)) !== null) {
    if (m.index > last) out.push(text.slice(last, m.index))
    out.push(h('a', {
      href: m[0], target: '_blank', rel: 'noopener noreferrer',
      style: 'color:var(--el-color-primary); text-decoration:underline; word-break:break-all'
    }, m[0]))
    last = m.index + m[0].length
  }
  if (last < text.length) out.push(text.slice(last))
  return out
}

// Thông báo khi user gửi tin nhắn mới trong một phiên đang chat
function notifyNewMessage(s) {
  const inst = ElNotification({
    title: '💬 Tin nhắn mới',
    type: 'info',
    duration: 6000,
    customClass: 'support-notify',
    message: h('div', {}, [
      h('div', { style: 'margin-bottom:8px; line-height:1.5' }, [
        h('b', {}, s.userName || s.userEmail || 'Người dùng'),
        ': ',
        ...contentVNodes(s.lastMessage || 'Đã gửi một tin nhắn')
      ]),
      h(ElButton, {
        type: 'primary', size: 'small',
        onClick: () => { router.push('/admin/support'); inst.close() }
      }, () => 'Mở hội thoại')
    ])
  })
}

function notifyNewRequest(s) {
  const inst = ElNotification({
    title: 'Yêu cầu hỗ trợ mới',
    type: 'warning',
    duration: 0,               // giữ lại đến khi admin xử lý
    customClass: 'support-notify',
    onClose: () => notifs.delete(s.id),
    message: h('div', {}, [
      h('div', { style: 'margin-bottom:10px; line-height:1.5' }, [
        h('b', {}, s.userName || s.userEmail || 'Người dùng'),
        ' cần hỗ trợ về: ',
        h('i', {}, `“${s.subject || 'Hỗ trợ'}”`)
      ]),
      // Nội dung user trình bày (nếu có)
      s.lastMessage
        ? h('div', {
            style: 'margin-bottom:10px; padding:6px 10px; background:rgba(0,0,0,0.05); border-radius:6px; '
              + 'font-size:0.85rem; color:var(--el-text-color-regular); white-space:pre-wrap; '
              + 'word-break:break-word; max-height:120px; overflow:auto'
          }, contentVNodes(s.lastMessage))
        : null,
      h('div', { style: 'display:flex; gap:8px' }, [
        h(ElButton, {
          type: 'success', size: 'small',
          onClick: async () => { await acceptReq(s.id); inst.close() }
        }, () => 'Chấp nhận'),
        h(ElButton, {
          type: 'danger', size: 'small', plain: true,
          onClick: async () => { await rejectReq(s.id); inst.close() }
        }, () => 'Từ chối'),
        h(ElButton, {
          size: 'small', text: true,
          onClick: () => { router.push('/admin/support'); inst.close() }
        }, () => 'Mở trang')
      ])
    ])
  })
  notifs.set(s.id, inst)
}

async function acceptReq(id) {
  try { await adminSupportAPI.accept(id); ElMessage.success('Đã chấp nhận yêu cầu'); pollSupport() } catch {}
}
async function rejectReq(id) {
  try { await adminSupportAPI.reject(id); ElMessage.info('Đã từ chối yêu cầu'); pollSupport() } catch {}
}

onMounted(() => { pollSupport(); supportTimer = setInterval(pollSupport, 5000) })
onUnmounted(() => { if (supportTimer) clearInterval(supportTimer) })
</script>

<style scoped>
.layout { display:flex; height:100vh; overflow:hidden; }
.sidebar {
  width:220px; height:100vh; flex-shrink:0;
  background:var(--c-bg2); border-right:1px solid var(--c-bg3);
  display:flex; flex-direction:column;
  transition:width 0.25s; box-shadow:2px 0 12px rgba(0,0,0,0.2);
}
.sidebar.collapsed { width:64px; }
.sidebar-logo {
  height:60px; padding:0 16px; cursor:pointer;
  display:flex; align-items:center; gap:4px;
  border-bottom:1px solid rgba(255,255,255,0.1); flex-shrink:0;
}
.admin-badge {
  font-family:var(--font-mono); font-size:0.48rem; letter-spacing:0.12em;
  color:var(--c-accent); border:1px solid var(--c-accent);
  padding:2px 5px; border-radius:2px; margin-left:auto; white-space:nowrap;
}
.sidebar-menu { flex:1; overflow-y:auto; overflow-x:hidden; padding:10px 0; }
.sidebar-bottom { padding:12px; border-top:1px solid rgba(255,255,255,0.1); flex-shrink:0; }
.user-info { display:flex; align-items:center; gap:10px; margin-bottom:10px; overflow:hidden; }
.user-avatar {
  width:34px; height:34px; border-radius:4px; flex-shrink:0;
  background:var(--c-accent2); color:#fff;
  display:flex; align-items:center; justify-content:center;
  font-family:var(--font-display); font-size:0.9rem; font-weight:700;
}
.user-name { font-size:0.83rem; font-weight:600; color:var(--c-text-inv); white-space:nowrap; }
.logout-btn { color:var(--c-text-inv2) !important; width:100%; justify-content:flex-start; }
.logout-btn:hover { color:var(--c-text-inv) !important; }
.main-area { flex:1; display:flex; flex-direction:column; overflow:hidden; background:var(--c-bg); }
.topbar {
  height:56px; padding:0 24px; flex-shrink:0;
  display:flex; align-items:center; gap:12px;
  background:var(--c-bg2); border-bottom:1px solid var(--c-bg3);
}
.toggle-btn { color:var(--c-text-inv2) !important; }
.topbar-title { font-size:1rem; color:var(--c-text-inv2); letter-spacing:0.1em; }
.page-content { flex:1; overflow-y:auto; padding:28px; background:var(--c-bg); }
.page-enter-active,.page-leave-active { transition:opacity 0.2s; }
.page-enter-from,.page-leave-to { opacity:0; }

/* Badge số yêu cầu hỗ trợ chờ trên menu item */
.sidebar-menu .el-menu-item .menu-badge { display:inline-flex; align-items:center; }
.sidebar-menu .el-menu-item .menu-badge :deep(.el-badge__content) {
  top:4px; right:4px; border:none; font-family:var(--font-mono); font-size:0.62rem;
}
</style>
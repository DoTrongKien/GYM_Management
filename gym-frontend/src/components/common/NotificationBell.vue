<template>
  <el-popover placement="bottom-end" :width="320" trigger="click" @show="loadNotifications"
              v-model:visible="panelOpen">
    <template #reference>
      <el-badge :value="unread || ''" :max="99" :hidden="!unread">
        <!-- Chuông luôn nằm trên topbar nền nâu tối → dùng màu chữ nghịch đảo -->
        <el-button text class="bell-btn">
          <el-icon size="20"><Bell /></el-icon>
        </el-button>
      </el-badge>
    </template>

    <div class="notif-panel">
      <div class="notif-header">
        <span class="display" style="font-size:1rem">THÔNG BÁO</span>
        <el-button text size="small" @click="markAll" v-if="unread">Đọc tất cả</el-button>
      </div>

      <div class="notif-list" v-if="notifications.length">
        <div
          v-for="n in notifications.slice(0,8)"
          :key="n.id"
          class="notif-item"
          :class="{ unread: !n.isRead, clickable: targetRoute(n) }"
          @click="openNotification(n)"
        >
          <div class="notif-dot" v-if="!n.isRead" />
          <div>
            <div class="notif-title">{{ n.title }}</div>
            <div class="notif-msg">{{ n.message }}</div>
          </div>
        </div>
      </div>
      <div v-else class="notif-empty">Không có thông báo nào</div>
    </div>
  </el-popover>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { notifAPI } from '@/api'

const POLL_MS = 20000   // nhịp kiểm tra thông báo mới

const router = useRouter()
const auth   = useAuthStore()

const notifications = ref([])
const unread = ref(0)
const panelOpen = ref(false)
let timer = null

/**
 * Đích đến của một thông báo, hoặc null nếu nó không trỏ tới đâu cả.
 * Cùng một đánh giá nhưng admin và user mở ở hai trang khác nhau.
 */
function targetRoute(n) {
  if (!n.refId) return null
  if (n.refType === 'RATING') {
    return {
      name: auth.isAdmin ? 'AdminRatings' : 'Ratings',
      query: { highlight: n.refId },
    }
  }
  if (n.refType === 'SUPPORT') {
    return {
      name: auth.isAdmin ? 'AdminSupport' : 'Chat',
      query: { session: n.refId },
    }
  }
  return null
}

function openNotification(n) {
  const route = targetRoute(n)
  if (!route) return
  panelOpen.value = false
  // Bấm lại đúng thông báo đang mở → router từ chối điều hướng trùng, bỏ qua lỗi đó
  router.push(route).catch(() => {})
}

async function loadNotifications() {
  try {
    const [all, cnt] = await Promise.all([notifAPI.getAll(), notifAPI.getUnread()])
    notifications.value = all.data || []
    unread.value = cnt.data || 0
  } catch {}
}

async function markAll() {
  await notifAPI.markAllRead()
  unread.value = 0
  notifications.value.forEach(n => n.isRead = true)
}

/** Chỉ lấy số chưa đọc — đủ để bật chấm đỏ mà không tải cả danh sách. */
function refreshUnread() {
  notifAPI.getUnread().then(r => { unread.value = r.data || 0 }).catch(() => {})
}

onMounted(() => {
  refreshUnread()
  // Hỏi lại định kỳ để thông báo mới hiện chấm đỏ mà không cần tải lại trang
  timer = setInterval(refreshUnread, POLL_MS)
})

onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
/* Nút chuông đứng trên topbar nâu tối, phải dùng màu sáng mới nhìn rõ */
.bell-btn {
  color:var(--c-text-inv2) !important;
  border-radius:50%; padding:8px;
  transition: color var(--transition), background var(--transition), transform var(--transition);
}
.bell-btn:hover, .bell-btn:focus {
  color:var(--c-text-inv) !important;
  background:rgba(255,255,255,0.12) !important;
}
/* Chuông rung nhẹ khi rê chuột vào */
.bell-btn:hover .el-icon { animation: bell-ring 0.6s ease-in-out; transform-origin: top center; }
@keyframes bell-ring {
  0%, 100% { transform: rotate(0); }
  20% { transform: rotate(14deg); }
  40% { transform: rotate(-10deg); }
  60% { transform: rotate(6deg); }
  80% { transform: rotate(-4deg); }
}

/* Popover nền trắng mặc định của Element Plus → giữ nguyên, dùng chữ tối */
.notif-panel { color:var(--c-text); }
.notif-header {
  display:flex; justify-content:space-between; align-items:center;
  padding-bottom:10px; margin-bottom:10px;
  border-bottom:1px solid var(--c-border2);
}
.notif-list { max-height: 320px; overflow-y: auto; }
.notif-item {
  display:flex; gap:10px; align-items:flex-start;
  padding:10px; border-bottom:1px solid var(--c-border2);
  position:relative; border-radius:var(--radius); cursor:default;
  transition: background var(--transition), border-color var(--transition);
}
/* Chỉ thông báo có đích đến mới hiện con trỏ bấm được */
.notif-item.clickable { cursor:pointer; }
.notif-item:last-child { border-bottom:none; }
.notif-item.unread { background:rgba(212,137,42,0.07); }   /* nhấn nhẹ bằng màu accent */
/* Rê chuột vào thông báo nào thì tô cam đậm; chữ phải đổi sang màu sáng mới đọc được */
.notif-item:hover { background:var(--c-accent2); }
.notif-item:hover .notif-title { color:var(--c-text-inv); }
.notif-item:hover .notif-msg   { color:var(--c-text-inv2); }
.notif-item:hover .notif-dot   { background:var(--c-text-inv); }
.notif-dot {
  width:6px; height:6px; background:var(--c-accent);
  border-radius:50%; margin-top:6px; flex-shrink:0;
}
.notif-title { font-size:0.85rem; font-weight:600; color:var(--c-text); }
.notif-msg { font-size:0.78rem; color:var(--c-text2); margin-top:2px; }
.notif-empty { text-align:center; color:var(--c-text3); padding:20px 0; font-size:0.85rem; }
</style>

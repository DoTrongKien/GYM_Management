<template>
  <el-popover placement="bottom-end" :width="320" trigger="click" @show="loadNotifications">
    <template #reference>
      <el-badge :value="unread || ''" :max="99" :hidden="!unread">
        <el-button text style="color:var(--c-text2)">
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
          :class="{ unread: !n.isRead }"
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
import { ref, onMounted } from 'vue'
import { notifAPI } from '@/api'

const notifications = ref([])
const unread = ref(0)

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

onMounted(() => {
  notifAPI.getUnread().then(r => { unread.value = r.data || 0 }).catch(() => {})
})
</script>

<style scoped>
.notif-panel { background: var(--c-bg2); }
.notif-header {
  display:flex; justify-content:space-between; align-items:center;
  padding-bottom:10px; margin-bottom:10px;
  border-bottom:1px solid var(--c-border);
}
.notif-list { max-height: 320px; overflow-y: auto; }
.notif-item {
  display:flex; gap:10px; align-items:flex-start;
  padding:10px 0; border-bottom:1px solid var(--c-border);
  position:relative;
}
.notif-item:last-child { border-bottom:none; }
.notif-item.unread { background:rgba(232,255,0,0.03); }
.notif-dot {
  width:6px; height:6px; background:var(--c-accent);
  border-radius:50%; margin-top:6px; flex-shrink:0;
}
.notif-title { font-size:0.85rem; font-weight:600; color:var(--c-text); }
.notif-msg { font-size:0.78rem; color:var(--c-text2); margin-top:2px; }
.notif-empty { text-align:center; color:var(--c-text3); padding:20px 0; font-size:0.85rem; }
</style>

<template>
  <div class="layout">
    <!-- Sidebar -->
    <aside class="sidebar" :class="{ collapsed }">
      <div class="sidebar-logo" @click="router.push('/app/dashboard')">
        <span class="display accent">G</span>
        <span class="display sidebar-name" v-show="!collapsed">YMPRO</span>
      </div>

      <el-menu :default-active="route.path" router class="sidebar-menu" :collapse="collapsed">
        <el-menu-item index="/app/dashboard">
          <el-icon><DataAnalysis /></el-icon><template #title>Dashboard</template>
        </el-menu-item>
        <el-menu-item index="/app/profile">
          <el-icon><User /></el-icon><template #title>Hồ sơ</template>
        </el-menu-item>
        <el-menu-item index="/app/plan">
          <el-icon><Calendar /></el-icon><template #title>Giáo án</template>
        </el-menu-item>
        <el-menu-item index="/app/sessions">
          <el-icon><Timer /></el-icon><template #title>Buổi tập</template>
        </el-menu-item>
        <el-menu-item index="/app/progress">
          <el-icon><TrendCharts /></el-icon><template #title>Tiến độ</template>
        </el-menu-item>
        <el-menu-item index="/app/nutrition">
          <el-icon><Apple /></el-icon><template #title>Dinh dưỡng</template>
        </el-menu-item>
        <el-menu-item index="/app/membership">
          <el-icon><CreditCard /></el-icon><template #title>Gói tập</template>
        </el-menu-item>
        <el-menu-item index="/app/exercises">
          <el-icon><Trophy /></el-icon><template #title>Bài tập</template>
        </el-menu-item>
        <el-menu-item index="/app/ratings">
          <el-icon><Star /></el-icon><template #title>Đánh giá</template>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-bottom">
        <div class="user-info" v-show="!collapsed">
          <div class="user-avatar">{{ initials }}</div>
          <div class="user-meta">
            <div class="user-name">{{ auth.user?.fullName }}</div>
            <div class="user-role muted">Member</div>
          </div>
        </div>
        <el-button text @click="auth.logout(); router.push('/login')" style="color:var(--c-text3);width:100%">
          <el-icon><SwitchButton /></el-icon>
          <span v-show="!collapsed" style="margin-left:6px">Đăng xuất</span>
        </el-button>
      </div>
    </aside>

    <!-- Main -->
    <div class="main-area">
      <!-- Topbar -->
      <header class="topbar">
        <el-button text @click="collapsed = !collapsed" style="color:var(--c-text2)">
          <el-icon size="20"><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
        </el-button>
        <div style="flex:1" />
        <NotificationBell />
      </header>

      <!-- Page content -->
      <main class="page-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import NotificationBell from './NotificationBell.vue'

const auth      = useAuthStore()
const route     = useRoute()
const router    = useRouter()
const collapsed = ref(false)
const initials  = computed(() => {
  const n = auth.user?.fullName || 'U'
  return n.split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2)
})
</script>

<style scoped>
.layout { display:flex; height:100vh; overflow:hidden; }

.sidebar {
  width: 220px; height:100vh;
  background: var(--c-bg2);
  border-right: 1px solid var(--c-border);
  display: flex; flex-direction: column;
  transition: width 0.25s ease;
  flex-shrink: 0;
}
.sidebar.collapsed { width: 64px; }

.sidebar-logo {
  padding: 20px 16px; cursor: pointer;
  display: flex; align-items: center; gap: 2px;
  font-size: 1.6rem; border-bottom: 1px solid var(--c-border);
}
.sidebar-name { overflow: hidden; white-space: nowrap; }

.sidebar-menu { flex: 1; overflow-y: auto; overflow-x: hidden; padding: 8px 0; }

.sidebar-bottom {
  padding: 12px; border-top: 1px solid var(--c-border);
}
.user-info {
  display: flex; align-items: center; gap: 10px;
  margin-bottom: 8px; overflow: hidden;
}
.user-avatar {
  width: 32px; height: 32px; border-radius: 50%;
  background: var(--c-accent); color: #000;
  display: flex; align-items: center; justify-content: center;
  font-family: var(--font-display); font-size: 0.9rem;
  flex-shrink: 0;
}
.user-name { font-size: 0.85rem; font-weight: 600; white-space: nowrap; }
.user-role { font-size: 0.75rem; }

.main-area { flex:1; display:flex; flex-direction:column; overflow:hidden; }

.topbar {
  height: 56px; padding: 0 20px;
  display: flex; align-items: center; gap: 12px;
  background: var(--c-bg); border-bottom: 1px solid var(--c-border);
  flex-shrink: 0;
}

.page-content {
  flex: 1; overflow-y: auto;
  padding: 28px 32px;
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>

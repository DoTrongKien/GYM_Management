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
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const collapsed = ref(false)
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
</style>
<template>
  <div class="layout">
    <aside class="sidebar" :class="{ collapsed }">
      <div class="sidebar-logo" @click="router.push('/admin/dashboard')">
        <span class="display accent">G</span>
        <span class="display sidebar-name" v-show="!collapsed">YMPRO</span>
        <span class="admin-badge" v-show="!collapsed">ADMIN</span>
      </div>

      <el-menu :default-active="route.path" router class="sidebar-menu" :collapse="collapsed">
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataAnalysis /></el-icon><template #title>Dashboard</template>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><UserFilled /></el-icon><template #title>Quản lý User</template>
        </el-menu-item>
        <el-menu-item index="/admin/memberships">
          <el-icon><CreditCard /></el-icon><template #title>Hóa đơn</template>
        </el-menu-item>
        <el-menu-item index="/admin/exercises">
          <el-icon><Trophy /></el-icon><template #title>Bài tập</template>
        </el-menu-item>
        <el-menu-item index="/admin/plans">
          <el-icon><Calendar /></el-icon><template #title>Giáo án</template>
        </el-menu-item>
        <el-menu-item index="/admin/notify">
          <el-icon><Bell /></el-icon><template #title>Thông báo</template>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-bottom">
        <div class="user-info" v-show="!collapsed">
          <div class="user-avatar admin">AD</div>
          <div class="user-meta">
            <div class="user-name">{{ auth.user?.fullName }}</div>
            <div class="user-role accent" style="font-size:0.7rem">ADMINISTRATOR</div>
          </div>
        </div>
        <el-button text @click="auth.logout(); router.push('/login')" style="color:var(--c-text3);width:100%">
          <el-icon><SwitchButton /></el-icon>
          <span v-show="!collapsed" style="margin-left:6px">Đăng xuất</span>
        </el-button>
      </div>
    </aside>

    <div class="main-area">
      <header class="topbar">
        <el-button text @click="collapsed = !collapsed" style="color:var(--c-text2)">
          <el-icon size="20"><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
        </el-button>
        <span class="display" style="font-size:1.1rem;color:var(--c-text2)">ADMIN PANEL</span>
        <div style="flex:1" />
        <el-tag type="warning" size="small">ADMIN</el-tag>
      </header>
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
  width:220px; height:100vh; background:var(--c-bg2);
  border-right:1px solid var(--c-border);
  display:flex; flex-direction:column;
  transition:width 0.25s; flex-shrink:0;
}
.sidebar.collapsed { width:64px; }
.sidebar-logo {
  padding:20px 16px; cursor:pointer;
  display:flex; align-items:center; gap:4px;
  font-size:1.6rem; border-bottom:1px solid var(--c-border);
}
.sidebar-name { overflow:hidden; white-space:nowrap; }
.admin-badge {
  font-family:var(--font-mono); font-size:0.5rem; letter-spacing:0.12em;
  color:var(--c-accent); border:1px solid var(--c-accent);
  padding:1px 5px; border-radius:2px; margin-left:auto;
}
.sidebar-menu { flex:1; overflow-y:auto; overflow-x:hidden; padding:8px 0; }
.sidebar-bottom { padding:12px; border-top:1px solid var(--c-border); }
.user-info { display:flex; align-items:center; gap:10px; margin-bottom:8px; overflow:hidden; }
.user-avatar {
  width:32px; height:32px; border-radius:4px;
  background:var(--c-accent2); color:#fff;
  display:flex; align-items:center; justify-content:center;
  font-family:var(--font-display); font-size:0.9rem; flex-shrink:0;
}
.user-name { font-size:0.85rem; font-weight:600; white-space:nowrap; }
.main-area { flex:1; display:flex; flex-direction:column; overflow:hidden; }
.topbar {
  height:56px; padding:0 20px; display:flex; align-items:center; gap:12px;
  background:var(--c-bg); border-bottom:1px solid var(--c-border); flex-shrink:0;
}
.page-content { flex:1; overflow-y:auto; padding:28px 32px; }
.fade-enter-active,.fade-leave-active { transition:opacity 0.2s; }
.fade-enter-from,.fade-leave-to { opacity:0; }
</style>
<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>GỬI THÔNG BÁO</h2>
    </div>

    <div class="grid-2" style="gap:24px;align-items:start">

      <!-- Broadcast -->
      <el-card header="📢 BROADCAST — GỬI TẤT CẢ USER">
        <el-form :model="broadcastForm" label-position="top">
          <el-form-item label="Tiêu đề">
            <el-input v-model="broadcastForm.title" placeholder="Ví dụ: Ưu đãi tháng 6" />
          </el-form-item>
          <el-form-item label="Nội dung">
            <el-input v-model="broadcastForm.message" type="textarea" :rows="4"
              placeholder="Nội dung thông báo gửi tới toàn bộ người dùng..." />
          </el-form-item>
          <el-form-item label="Loại thông báo">
            <el-select v-model="broadcastForm.type" style="width:100%">
              <el-option label="🎉 Khuyến mãi" value="PROMOTION" />
              <el-option label="📢 Hệ thống" value="SYSTEM" />
              <el-option label="💪 Nhắc nhở tập" value="WORKOUT_REMINDER" />
            </el-select>
          </el-form-item>
          <el-button type="primary" style="width:100%" @click="sendBroadcast" :loading="sendingBroadcast">
            📢 GỬI TỚI TẤT CẢ USER
          </el-button>
        </el-form>
      </el-card>

      <!-- Send to user -->
      <el-card header="👤 GỬI TỚI 1 USER CỤ THỂ">
        <el-form :model="userForm" label-position="top">
          <el-form-item label="User ID">
            <el-input-number v-model="userForm.userId" :min="1" style="width:100%" placeholder="Nhập ID của user" />
          </el-form-item>
          <el-form-item label="Tiêu đề">
            <el-input v-model="userForm.title" placeholder="Tiêu đề thông báo" />
          </el-form-item>
          <el-form-item label="Nội dung">
            <el-input v-model="userForm.message" type="textarea" :rows="4" placeholder="Nội dung..." />
          </el-form-item>
          <el-form-item label="Loại">
            <el-select v-model="userForm.type" style="width:100%">
              <el-option label="📢 Hệ thống" value="SYSTEM" />
              <el-option label="🎉 Khuyến mãi" value="PROMOTION" />
            </el-select>
          </el-form-item>
          <el-button style="width:100%" @click="sendToUser" :loading="sendingUser">
            📨 GỬI TỚI USER NÀY
          </el-button>
        </el-form>
      </el-card>

    </div>

    <!-- Tips -->
    <el-card header="💡 GỢI Ý NỘI DUNG" style="margin-top:24px">
      <div class="tips-grid">
        <div
          v-for="tip in tips" :key="tip.title"
          class="tip-card"
          @click="broadcastForm.title = tip.title; broadcastForm.message = tip.msg"
        >
          <div class="tip-icon">{{ tip.icon }}</div>
          <div>
            <div class="tip-title">{{ tip.title }}</div>
            <div class="tip-msg muted">{{ tip.msg }}</div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { adminAPI } from '@/api'
import { ElMessage } from 'element-plus'

const sendingBroadcast = ref(false)
const sendingUser      = ref(false)

const broadcastForm = reactive({ title: '', message: '', type: 'PROMOTION' })
const userForm      = reactive({ userId: null, title: '', message: '', type: 'SYSTEM' })

async function sendBroadcast() {
  if (!broadcastForm.title || !broadcastForm.message) { ElMessage.warning('Nhập đủ tiêu đề và nội dung'); return }
  sendingBroadcast.value = true
  try {
    await adminAPI.broadcast(broadcastForm)
    ElMessage.success('Đã gửi broadcast tới tất cả user! 📢')
    broadcastForm.title = ''; broadcastForm.message = ''
  } finally { sendingBroadcast.value = false }
}

async function sendToUser() {
  if (!userForm.userId || !userForm.title) { ElMessage.warning('Nhập ID user và tiêu đề'); return }
  sendingUser.value = true
  try {
    await adminAPI.sendToUser(userForm.userId, { title: userForm.title, message: userForm.message, type: userForm.type })
    ElMessage.success(`Đã gửi thông báo tới User #${userForm.userId}!`)
    userForm.title = ''; userForm.message = ''
  } finally { sendingUser.value = false }
}

const tips = [
  { icon:'🎁', title:'Ưu đãi thành viên mới', msg:'Đăng ký tháng này nhận ngay 1 tháng miễn phí! Hạn dùng 30/06/2025.' },
  { icon:'💪', title:'Challenge tuần này', msg:'Tham gia thử thách 7 ngày liên tiếp để nhận quà đặc biệt!' },
  { icon:'📊', title:'Cập nhật tính năng', msg:'Hệ thống vừa cập nhật giáo án AI mới. Hãy thử tạo lại giáo án của bạn!' },
  { icon:'🌟', title:'Chúc mừng tháng mới', msg:'Tháng mới, mục tiêu mới! Hãy kiên trì với kế hoạch luyện tập của bạn.' },
]
</script>

<style scoped>
.tips-grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(260px,1fr)); gap:12px; }
.tip-card {
  display:flex; gap:12px; align-items:flex-start;
  background:var(--c-bg3); border:1px solid var(--c-border);
  border-radius:var(--radius-lg); padding:14px; cursor:pointer;
  transition: border-color var(--transition);
}
.tip-card:hover { border-color:var(--c-accent); }
.tip-icon  { font-size:1.8rem; flex-shrink:0; }
.tip-title { font-weight:600; font-size:0.875rem; margin-bottom:4px; }
.tip-msg   { font-size:0.78rem; line-height:1.4;
  overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; }
</style>

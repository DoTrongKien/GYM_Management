<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>ADMIN DASHBOARD</h2>
      <span class="mono muted" style="font-size:0.8rem">{{ today }}</span>
    </div>

    <div v-if="loading"><el-skeleton :rows="6" animated /></div>

    <template v-else>
      <!-- Revenue stats -->
      <div class="grid-4" style="margin-bottom:24px">
        <div class="stat-card accent-card">
          <div class="label">TỔNG DOANH THU</div>
          <div class="value">{{ formatMoney(stats.totalRevenue) }}</div>
          <div class="sub">đồng</div>
          <div class="icon">💰</div>
        </div>
        <div class="stat-card">
          <div class="label">THÁNG NÀY</div>
          <div class="value">{{ formatMoney(stats.monthRevenue) }}</div>
          <div class="sub">đồng</div>
          <div class="icon">📅</div>
        </div>
        <div class="stat-card">
          <div class="label">TỔNG NGƯỜI DÙNG</div>
          <div class="value">{{ stats.totalUsers || 0 }}</div>
          <div class="sub">{{ stats.activeUsers || 0 }} đang hoạt động</div>
          <div class="icon">👥</div>
        </div>
        <div class="stat-card">
          <div class="label">THÀNH VIÊN ĐÃ TT</div>
          <div class="value">{{ stats.paidMembers || 0 }}</div>
          <div class="sub">/ {{ stats.totalMembers || 0 }} đăng ký</div>
          <div class="icon">💳</div>
        </div>
      </div>

      <!-- Quick links -->
      <div class="grid-3" style="margin-bottom:24px">
        <div class="quick-card" @click="$router.push('/admin/memberships')">
          <div class="q-icon">📋</div>
          <div class="q-title display">HÓA ĐƠN PENDING</div>
          <div class="q-desc muted">Xác nhận thanh toán</div>
        </div>
        <div class="quick-card" @click="$router.push('/admin/users')">
          <div class="q-icon">👤</div>
          <div class="q-title display">QUẢN LÝ USER</div>
          <div class="q-desc muted">Xem & quản lý tài khoản</div>
        </div>
        <div class="quick-card" @click="$router.push('/admin/notify')">
          <div class="q-icon">📢</div>
          <div class="q-title display">GỬI THÔNG BÁO</div>
          <div class="q-desc muted">Broadcast tới tất cả user</div>
        </div>
      </div>

      <!-- Recent memberships -->
      <el-card header="HÓA ĐƠN GẦN ĐÂY">
        <el-table :data="recentMemberships" stripe>
          <el-table-column label="User" prop="userName" min-width="150" />
          <el-table-column label="Email" prop="userEmail" min-width="180" />
          <el-table-column label="Gói" prop="membershipType" width="110" />
          <el-table-column label="Giá" width="140" align="right">
            <template #default="{row}">{{ Number(row.price).toLocaleString() }} đ</template>
          </el-table-column>
          <el-table-column label="Trạng thái" width="130" align="center">
            <template #default="{row}">
              <span class="badge" :class="payBadge(row.paymentStatus)">{{ row.paymentStatus }}</span>
            </template>
          </el-table-column>
          <el-table-column label="Thao tác" width="130" align="center">
            <template #default="{row}">
              <el-button v-if="row.paymentStatus==='PENDING'" type="primary" size="small" @click="confirm(row.id)">
                Xác nhận
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <div style="margin-top:12px;text-align:right">
          <el-button text @click="$router.push('/admin/memberships')">Xem tất cả →</el-button>
        </div>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminAPI } from '@/api'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const stats              = ref({})
const recentMemberships  = ref([])
const loading            = ref(true)
const today              = dayjs().format('dddd, DD/MM/YYYY')

async function load() {
  loading.value = true
  try {
    const [rev, mem] = await Promise.all([adminAPI.getRevenue(), adminAPI.getMemberships()])
    stats.value             = rev.data || {}
    recentMemberships.value = (mem.data || []).slice(0, 8)
  } finally { loading.value = false }
}

async function confirm(id) {
  await adminAPI.confirmPayment(id)
  ElMessage.success('Thanh toán đã xác nhận!')
  load()
}

function formatMoney(n) {
  if (!n) return '0'
  if (n >= 1_000_000) return (n / 1_000_000).toFixed(1) + 'M'
  return Number(n).toLocaleString()
}
function payBadge(s) {
  return { PAID:'badge-success', PENDING:'badge-warning', FAILED:'badge-danger', REFUNDED:'badge-info' }[s] || ''
}
onMounted(load)
</script>

<style scoped>
.quick-card {
  background:var(--c-bg2); border:1px solid var(--c-border);
  border-radius:var(--radius-lg); padding:24px; cursor:pointer;
  transition: border-color var(--transition), transform var(--transition);
  text-align:center;
}
.quick-card:hover { border-color:var(--c-accent); transform:translateY(-2px); }
.q-icon  { font-size:2.5rem; margin-bottom:10px; }
.q-title { font-size:1rem; margin-bottom:4px; }
.q-desc  { font-size:0.8rem; }
</style>

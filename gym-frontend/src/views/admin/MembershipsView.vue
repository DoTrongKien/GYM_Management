<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>QUẢN LÝ HÓA ĐƠN</h2>
      <div style="display:flex;gap:8px">
        <el-select v-model="filterStatus" placeholder="Lọc trạng thái" clearable style="width:180px" @change="applyFilter">
          <el-option label="Tất cả" value="" />
          <el-option label="⏳ Chờ TT" value="PENDING" />
          <el-option label="✅ Đã TT" value="PAID" />
          <el-option label="↩️ Hoàn tiền" value="REFUNDED" />
        </el-select>
      </div>
    </div>

    <!-- Summary cards -->
    <div class="grid-3" style="margin-bottom:24px">
      <div class="stat-card accent-card">
        <div class="label">TỔNG DOANH THU</div>
        <div class="value">{{ formatMoney(totalRevenue) }}</div>
        <div class="sub">từ gói đã thanh toán</div>
      </div>
      <div class="stat-card">
        <div class="label">CHỜ THANH TOÁN</div>
        <div class="value">{{ pending.length }}</div>
        <div class="sub">hóa đơn pending</div>
      </div>
      <div class="stat-card">
        <div class="label">TỔNG ĐƠN</div>
        <div class="value">{{ all.length }}</div>
        <div class="sub">hóa đơn</div>
      </div>
    </div>

    <el-table :data="displayed" v-loading="loading" stripe>
      <el-table-column label="ID" prop="id" width="60" align="center" />
      <el-table-column label="Khách hàng" prop="userName" min-width="150" />
      <el-table-column label="Email" prop="userEmail" min-width="190" />
      <el-table-column label="Gói" prop="membershipType" width="110" align="center" />
      <el-table-column label="Bắt đầu" prop="startDate" width="110" />
      <el-table-column label="Kết thúc" prop="endDate" width="110" />
      <el-table-column label="Giá (đ)" width="130" align="right">
        <template #default="{row}">{{ Number(row.price).toLocaleString() }}</template>
      </el-table-column>
      <el-table-column label="Hình thức" width="120" align="center">
        <template #default="{row}">{{ row.paymentMethod || '--' }}</template>
      </el-table-column>
      <el-table-column label="Trạng thái" width="120" align="center">
        <template #default="{row}">
          <span class="badge" :class="payBadge(row.paymentStatus)">{{ row.paymentStatus }}</span>
        </template>
      </el-table-column>
      <el-table-column label="Trạng thái" width="160" align="center" fixed="right">
        <template #default="{row}">
          <el-button
            v-if="row.paymentStatus === 'PENDING'"
            type="primary" size="small"
            @click="confirm(row.id)"
          >Xác nhận TT</el-button>
          <el-button
            v-if="row.paymentStatus === 'PAID'"
            type="danger" size="small" plain
            @click="refund(row.id)"
          >Hoàn tiền</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminAPI } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const all          = ref([])
const loading      = ref(true)
const filterStatus = ref('')

const pending  = computed(() => all.value.filter(m => m.paymentStatus === 'PENDING'))
const displayed = computed(() => filterStatus.value ? all.value.filter(m => m.paymentStatus === filterStatus.value) : all.value)
const totalRevenue = computed(() => all.value.filter(m => m.paymentStatus === 'PAID').reduce((s, m) => s + m.price, 0))

async function load() {
  loading.value = true
  try { const r = await adminAPI.getMemberships(); all.value = r.data || [] }
  finally { loading.value = false }
}

function applyFilter() {} // reactive computed handles it

async function confirm(id) {
  await adminAPI.confirmPayment(id)
  ElMessage.success('Đã xác nhận thanh toán!')
  load()
}

async function refund(id) {
  await ElMessageBox.confirm('Bạn chắc chắn muốn hoàn tiền?', 'Xác nhận', { type: 'warning' })
  await adminAPI.refund(id)
  ElMessage.success('Đã hoàn tiền!')
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

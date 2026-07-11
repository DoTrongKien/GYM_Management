<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>LỊCH SỬ GIAO DỊCH</h2>
      <div style="display:flex;gap:8px">
        <el-input v-model="search" placeholder="Tìm theo tên / email / mã GD..." prefix-icon="Search" style="width:280px" clearable/>
        <el-select v-model="filterStatus" placeholder="Lọc trạng thái" clearable style="width:170px">
          <el-option label="⏳ Chờ thanh toán" value="PENDING"/>
          <el-option label="✅ Đã thanh toán" value="PAID"/>
          <el-option label="⏰ Hết hạn" value="EXPIRED"/>
          <el-option label="🚫 Đã hủy" value="CANCELLED"/>
          <el-option label="❌ Thất bại" value="FAILED"/>
          <el-option label="↩️ Hoàn tiền" value="REFUNDED"/>
        </el-select>
      </div>
    </div>

    <!-- Summary -->
    <div class="grid-3" style="margin-bottom:24px">
      <div class="stat-card accent-card">
        <div class="label">TỔNG DOANH THU ĐÃ TT</div>
        <div class="value">{{ formatM(totalRevenue) }}</div>
        <div class="sub">đồng</div>
        <div class="icon">💰</div>
      </div>
      <div class="stat-card">
        <div class="label">ĐANG CHỜ THANH TOÁN</div>
        <div class="value">{{ pendingCount }}</div>
        <div class="sub">hóa đơn pending</div>
        <div class="icon">⏳</div>
      </div>
      <div class="stat-card">
        <div class="label">TỔNG SỐ GIAO DỊCH</div>
        <div class="value">{{ all.length }}</div>
        <div class="sub">kể từ đầu</div>
        <div class="icon">📋</div>
      </div>
    </div>

    <el-card>
      <template #header>
        <span>DANH SÁCH GIAO DỊCH ({{ displayed.length }})</span>
      </template>
      <el-table :data="displayed" v-loading="loading" stripe>
        <el-table-column label="ID" prop="id" width="60" align="center"/>
        <el-table-column label="Khách hàng" prop="userName" min-width="140"/>
        <el-table-column label="Email" prop="userEmail" min-width="190"/>
        <el-table-column label="Gói" prop="membershipType" width="90" align="center"/>
        <el-table-column label="Giá (đ)" width="120" align="right">
          <template #default="{row}">{{ Number(row.price).toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="Mã GD" prop="transferCode" width="110" align="center"/>
        <el-table-column label="Mã tham chiếu NH" prop="transactionId" min-width="150"/>
        <el-table-column label="Trạng thái" width="130" align="center">
          <template #default="{row}">
            <span class="badge" :class="payBadge(row.status)">{{ payLabel(row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Ngày tạo" width="150">
          <template #default="{row}">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="Ngày thanh toán" width="150">
          <template #default="{row}">{{ row.paidAt ? formatDate(row.paidAt) : '—' }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminAPI } from '@/api'

const all          = ref([])
const loading      = ref(true)
const search       = ref('')
const filterStatus = ref('')

const displayed = computed(() => {
  let list = all.value
  if (filterStatus.value) list = list.filter(i => i.status === filterStatus.value)
  if (search.value) {
    const q = search.value.toLowerCase()
    list = list.filter(i =>
      i.userName?.toLowerCase().includes(q) ||
      i.userEmail?.toLowerCase().includes(q) ||
      i.transferCode?.toLowerCase().includes(q) ||
      i.transactionId?.toLowerCase().includes(q)
    )
  }
  return list
})

const pendingCount  = computed(() => all.value.filter(i => i.status === 'PENDING').length)
const totalRevenue  = computed(() => all.value.filter(i => i.status === 'PAID').reduce((s, i) => s + i.price, 0))

async function load() {
  loading.value = true
  try { const r = await adminAPI.getInvoices(); all.value = r.data || [] }
  finally { loading.value = false }
}

function formatM(n) {
  if (!n) return '0'
  if (n >= 1_000_000) return (n / 1_000_000).toFixed(1) + 'M'
  return Number(n).toLocaleString()
}
function formatDate(str) { return new Date(str).toLocaleString('vi-VN') }
function payBadge(s) { return { PAID:'badge-success', PENDING:'badge-warning', EXPIRED:'badge-info', CANCELLED:'badge-info', FAILED:'badge-danger', REFUNDED:'badge-info' }[s] || '' }
function payLabel(s) { return { PAID:'Đã TT', PENDING:'Chờ TT', EXPIRED:'Hết hạn', CANCELLED:'Đã hủy', FAILED:'Thất bại', REFUNDED:'Hoàn tiền' }[s] || s }

onMounted(load)
</script>
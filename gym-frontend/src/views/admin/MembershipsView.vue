<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>QUẢN LÝ HÓA ĐƠN</h2>
      <el-select v-model="filterStatus" placeholder="Lọc trạng thái" clearable style="width:180px">
        <el-option label="⏳ Chờ thanh toán" value="PENDING"/>
        <el-option label="✅ Đã thanh toán" value="PAID"/>
        <el-option label="↩️ Hoàn tiền" value="REFUNDED"/>
      </el-select>
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
        <div class="label">CHỜ XÁC NHẬN</div>
        <div class="value">{{ pending.length }}</div>
        <div class="sub">hóa đơn pending</div>
        <div class="icon">⏳</div>
      </div>
      <div class="stat-card">
        <div class="label">TỔNG HÓA ĐƠN</div>
        <div class="value">{{ all.length }}</div>
        <div class="sub">kể từ đầu</div>
        <div class="icon">📋</div>
      </div>
    </div>

    <el-card>
      <template #header>
        <span>DANH SÁCH HÓA ĐƠN {{ filterStatus ? `(${filterStatus})` : '' }}</span>
      </template>
      <el-table :data="displayed" v-loading="loading" stripe>
        <el-table-column label="ID" prop="id" width="60" align="center"/>
        <el-table-column label="Khách hàng" prop="userName" min-width="140"/>
        <el-table-column label="Email" prop="userEmail" min-width="180"/>
        <el-table-column label="Gói" prop="membershipType" width="100" align="center"/>
        <el-table-column label="Từ ngày" prop="startDate" width="105"/>
        <el-table-column label="Đến ngày" prop="endDate" width="105"/>
        <el-table-column label="Giá (đ)" width="130" align="right">
          <template #default="{row}">{{ Number(row.price).toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="Hình thức TT" prop="paymentMethod" width="115" align="center"/>
        <el-table-column label="Trạng thái" width="130" align="center">
          <template #default="{row}">
            <span class="badge" :class="payBadge(row.paymentStatus)">{{ payLabel(row.paymentStatus) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Thao tác" width="150" align="center" fixed="right">
          <template #default="{row}">
            <el-button v-if="row.paymentStatus==='PENDING'" type="primary" size="small" @click="confirm(row.id)">Xác nhận</el-button>
            <el-button v-if="row.paymentStatus==='PAID'" type="danger" size="small" plain @click="refund(row.id)">Hoàn tiền</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminAPI } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const all          = ref([])
const loading      = ref(true)
const filterStatus = ref('')

const pending   = computed(() => all.value.filter(m => m.paymentStatus==='PENDING'))
const displayed  = computed(() => filterStatus.value ? all.value.filter(m => m.paymentStatus===filterStatus.value) : all.value)
const totalRevenue = computed(() => all.value.filter(m=>m.paymentStatus==='PAID').reduce((s,m)=>s+m.price,0))

async function load() {
  loading.value = true
  try { const r = await adminAPI.getMemberships(); all.value = r.data || [] }
  finally { loading.value = false }
}
async function confirm(id) {
  await adminAPI.confirmPayment(id); ElMessage.success('Đã xác nhận thanh toán!'); load()
}
async function refund(id) {
  await ElMessageBox.confirm('Xác nhận hoàn tiền?', 'Cảnh báo', { type:'warning' })
  await adminAPI.refund(id); ElMessage.success('Đã hoàn tiền!'); load()
}

function formatM(n) {
  if (!n) return '0'
  if (n >= 1_000_000) return (n/1_000_000).toFixed(1) + 'M'
  return Number(n).toLocaleString()
}
function payBadge(s) { return { PAID:'badge-success', PENDING:'badge-warning', FAILED:'badge-danger', REFUNDED:'badge-info' }[s]||'' }
function payLabel(s) { return { PAID:'Đã TT', PENDING:'Chờ TT', FAILED:'Thất bại', REFUNDED:'Hoàn tiền' }[s]||s }

onMounted(load)
</script>
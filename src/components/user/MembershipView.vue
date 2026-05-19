<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>GÓI TẬP</h2>
      <el-button type="primary" @click="purchaseDialog = true">MUA GÓI MỚI</el-button>
    </div>

    <!-- Active membership -->
    <div v-if="active" class="active-card">
      <div class="active-badge display">ĐANG ACTIVE</div>
      <div class="active-type display">{{ active.membershipType }}</div>
      <div class="active-dates muted">{{ active.startDate }} → {{ active.endDate }}</div>
      <div class="active-remaining">
        <span class="accent display" style="font-size:2rem">{{ active.daysRemaining }}</span>
        <span class="muted" style="margin-left:8px">ngày còn lại</span>
      </div>
      <div class="active-status">
        <span class="badge badge-success" v-if="active.paymentStatus === 'PAID'">ĐÃ THANH TOÁN</span>
        <span class="badge badge-warning" v-else>CHỜ THANH TOÁN</span>
      </div>
    </div>

    <!-- Package options -->
    <h3 class="display" style="margin:24px 0 16px">CÁC GÓI TẬP</h3>
    <div class="packages-grid">
      <div v-for="pkg in packages" :key="pkg.type" class="package-card" :class="{ featured: pkg.featured }">
        <div class="pkg-type display">{{ pkg.type }}</div>
        <div class="pkg-price"><span class="accent">{{ pkg.price }}</span> đ</div>
        <div class="pkg-duration muted">{{ pkg.duration }}</div>
        <ul class="pkg-features">
          <li v-for="f in pkg.features" :key="f">✓ {{ f }}</li>
        </ul>
        <el-button :type="pkg.featured ? 'primary' : 'default'" style="width:100%;margin-top:16px" @click="selectPkg(pkg)">
          CHỌN GÓI NÀY
        </el-button>
      </div>
    </div>

    <!-- History -->
    <el-card header="LỊCH SỬ ĐĂNG KÝ" style="margin-top:24px">
      <el-table :data="memberships" stripe>
        <el-table-column label="Loại" prop="membershipType" width="120" />
        <el-table-column label="Bắt đầu" prop="startDate" width="120" />
        <el-table-column label="Kết thúc" prop="endDate" width="120" />
        <el-table-column label="Giá" width="130" align="right">
          <template #default="{row}">{{ Number(row.price).toLocaleString() }} đ</template>
        </el-table-column>
        <el-table-column label="Trạng thái" width="140" align="center">
          <template #default="{row}">
            <span class="badge" :class="payStatus(row.paymentStatus)">{{ row.paymentStatus }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Purchase Dialog -->
    <el-dialog v-model="purchaseDialog" title="MUA GÓI TẬP" width="440px">
      <el-form :model="form" label-position="top">
        <el-form-item label="Gói tập">
          <el-select v-model="form.membershipType" style="width:100%">
            <el-option label="BASIC — 299,000đ / 1 tháng" value="BASIC" />
            <el-option label="STANDARD — 499,000đ / 3 tháng" value="STANDARD" />
            <el-option label="PREMIUM — 799,000đ / 6 tháng" value="PREMIUM" />
            <el-option label="VIP — 1,299,000đ / 12 tháng" value="VIP" />
          </el-select>
        </el-form-item>
        <el-form-item label="Phương thức thanh toán">
          <el-select v-model="form.paymentMethod" style="width:100%">
            <el-option label="💳 Thẻ ngân hàng" value="CARD" />
            <el-option label="📱 MoMo" value="MOMO" />
            <el-option label="🏦 VNPay" value="VNPAY" />
            <el-option label="💚 ZaloPay" value="ZALOPAY" />
            <el-option label="💵 Tiền mặt" value="CASH" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="purchaseDialog = false">Hủy</el-button>
        <el-button type="primary" @click="purchase">ĐĂNG KÝ</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { membershipAPI } from '@/api'
import { ElMessage } from 'element-plus'

const memberships = ref([]); const active = ref(null)
const purchaseDialog = ref(false)
const form = reactive({ membershipType: 'STANDARD', paymentMethod: 'MOMO' })

const packages = [
  { type:'BASIC', price:'299,000', duration:'1 tháng', features:['Tập không giới hạn','Giáo án cơ bản','Theo dõi tiến độ'], featured:false },
  { type:'STANDARD', price:'499,000', duration:'3 tháng', features:['Tất cả gói BASIC','Giáo án AI','Dinh dưỡng AI'], featured:true },
  { type:'PREMIUM', price:'799,000', duration:'6 tháng', features:['Tất cả gói STANDARD','Phân tích nâng cao','Ưu tiên hỗ trợ'], featured:false },
  { type:'VIP', price:'1,299,000', duration:'12 tháng', features:['Tất cả tính năng','Tư vấn 1-1','Không giới hạn'], featured:false },
]

async function load() {
  try {
    const [all, act] = await Promise.all([membershipAPI.getAll(), membershipAPI.getActive().catch(() => ({ data: null }))])
    memberships.value = all.data || []
    active.value = act.data
  } catch {}
}

function selectPkg(pkg) { form.membershipType = pkg.type; purchaseDialog.value = true }

async function purchase() {
  await membershipAPI.purchase(form)
  ElMessage.success('Đăng ký thành công! Chờ xác nhận thanh toán.')
  purchaseDialog.value = false; load()
}

function payStatus(s) {
  return { PAID:'badge-success', PENDING:'badge-warning', FAILED:'badge-danger', REFUNDED:'badge-info' }[s] || ''
}
onMounted(load)
</script>

<style scoped>
.active-card {
  background: linear-gradient(135deg, #1a1a00, #0a0a0a);
  border: 1px solid var(--c-accent); border-radius:var(--radius-lg);
  padding: 24px; margin-bottom:24px;
}
.active-badge { font-size:0.7rem; color:var(--c-accent); letter-spacing:0.15em; margin-bottom:4px; }
.active-type { font-size:2.5rem; margin-bottom:4px; }
.active-dates { margin-bottom:12px; font-size:0.85rem; }
.active-remaining { margin-bottom:12px; }
.packages-grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(220px,1fr)); gap:16px; }
.package-card {
  background:var(--c-bg2); border:1px solid var(--c-border); border-radius:var(--radius-lg); padding:20px;
}
.package-card.featured { border-color:var(--c-accent); }
.pkg-type { font-size:1.3rem; margin-bottom:8px; }
.pkg-price { font-size:1.5rem; font-weight:700; margin-bottom:2px; }
.pkg-duration { font-size:0.8rem; margin-bottom:12px; }
.pkg-features { list-style:none; padding:0; }
.pkg-features li { font-size:0.82rem; color:var(--c-text2); padding:3px 0; }
</style>
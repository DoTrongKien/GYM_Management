<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>GÓI TẬP</h2>
    </div>

    <!-- Active membership -->
    <el-card v-if="active" style="margin-bottom:24px;border-left:4px solid var(--c-accent)" class="active-card">
      <div class="active-inner">
        <div>
          <div style="font-size:0.72rem;text-transform:uppercase;letter-spacing:0.12em;color:var(--c-accent);font-weight:700;margin-bottom:4px">GÓI ĐANG ACTIVE</div>
          <div class="display" style="font-size:2rem;color:var(--c-text)">
            {{ active.membershipType }}
            <span v-if="active.membershipType==='VIP'" class="vip-chip">👑 VIP</span>
          </div>
          <div style="color:var(--c-text2);font-size:0.85rem;margin-top:4px">{{ active.startDate }} → {{ active.endDate }}</div>
          <div style="margin-top:10px">
            <span class="badge" :class="active.paymentStatus==='PAID'?'badge-success':'badge-warning'">
              {{ active.paymentStatus === 'PAID' ? '✅ Đã thanh toán' : '⏳ Chờ thanh toán' }}
            </span>
          </div>
        </div>
        <div class="days-remain">
          <div class="days-num">{{ active.daysRemaining }}</div>
          <div class="days-lbl">ngày còn lại</div>
        </div>
      </div>
      <el-progress
          v-if="active.membershipType!=='FREE'"
          :percentage="daysPercent" :color="active.daysRemaining < 7 ? '#C62828' : '#D4892A'"
          style="margin-top:16px" :show-text="false" :stroke-width="6"/>
    </el-card>

    <!-- Package grid: chỉ 2 gói - FREE (mặc định) và VIP (duy nhất trả phí) -->
    <h3 class="display" style="font-size:1.4rem;color:var(--c-text-inv);margin-bottom:16px">GÓI TẬP</h3>
    <div class="packages-grid">
      <!-- FREE -->
      <div class="pkg-card">
        <div class="pkg-type display">FREE</div>
        <div class="pkg-price"><span class="accent">0</span> <span style="font-size:0.8rem;color:var(--c-text2)">đ - mãi mãi</span></div>
        <ul class="pkg-features">
          <li v-for="f in freeFeatures" :key="f">✓ {{ f }}</li>
          <li class="limited">⚠ Giáo án: chỉ đổi 1 lần/tháng</li>
          <li class="limited">⚠ Dinh dưỡng: gợi ý cơ bản</li>
        </ul>
        <el-button style="width:100%;margin-top:auto" disabled>
          {{ active && active.membershipType === 'FREE' ? '✓ Gói hiện tại' : 'Gói mặc định' }}
        </el-button>
      </div>

      <!-- VIP -->
      <div class="pkg-card featured">
        <div class="pkg-badge">DUY NHẤT TRẢ PHÍ</div>
        <div class="pkg-type display">VIP 👑</div>
        <div class="pkg-price"><span class="accent">{{ vipPrice.toLocaleString() }}</span> <span style="font-size:0.8rem;color:var(--c-text2)">đ / tháng</span></div>
        <ul class="pkg-features">
          <li v-for="f in vipFeatures" :key="f">✓ {{ f }}</li>
          <li class="highlight">⭐ Giáo án: KHÔNG GIỚI HẠN số lần đổi/tháng</li>
          <li class="highlight">⭐ Dinh dưỡng: cá nhân hóa chi tiết (nhiều lựa chọn món + lưu ý riêng)</li>
        </ul>
        <el-button type="primary" style="width:100%;margin-top:auto"
                   :disabled="active && active.membershipType==='VIP'"
                   @click="purchaseDialog=true">
          {{ active && active.membershipType==='VIP' ? '✓ Đang dùng VIP' : 'Nâng cấp lên VIP' }}
        </el-button>
      </div>
    </div>

    <!-- History -->
    <el-card style="margin-top:24px">
      <template #header>LỊCH SỬ ĐĂNG KÝ</template>
      <el-table :data="memberships" stripe>
        <el-table-column label="Loại gói" width="110">
          <template #default="{row}">
            {{ row.membershipType }} <span v-if="row.membershipType==='VIP'">👑</span>
          </template>
        </el-table-column>
        <el-table-column label="Bắt đầu" prop="startDate" width="110"/>
        <el-table-column label="Kết thúc" prop="endDate" width="110"/>
        <el-table-column label="Giá (đ)" width="140" align="right">
          <template #default="{row}">{{ Number(row.price).toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="Hình thức TT" prop="paymentMethod" width="130" align="center"/>
        <el-table-column label="Trạng thái" width="140" align="center">
          <template #default="{row}">
            <span class="badge" :class="payBadge(row.paymentStatus)">{{ payLabel(row.paymentStatus) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Purchase Dialog - chỉ có 1 gói duy nhất để mua (VIP) -->
    <el-dialog v-model="purchaseDialog" title="NÂNG CẤP LÊN GÓI VIP" width="420px" align-center>
      <p style="color:var(--c-text2);font-size:0.85rem;margin-bottom:12px">
        Gói VIP: giáo án tập không giới hạn số lần đổi/tháng + thực đơn dinh dưỡng cá nhân hóa chi tiết.
      </p>
      <el-form-item label="Phương thức thanh toán" style="margin-top:8px">
        <el-select v-model="paymentMethod" style="width:100%">
          <el-option label="💳 Thẻ ngân hàng" value="CARD"/>
          <el-option label="📱 MoMo" value="MOMO"/>
          <el-option label="🏦 VNPay" value="VNPAY"/>
          <el-option label="💵 Tiền mặt tại quầy" value="CASH"/>
        </el-select>
      </el-form-item>
      <div class="price-summary">
        <span>Tổng thanh toán:</span>
        <strong class="accent">{{ vipPrice.toLocaleString() }} đ</strong>
      </div>
      <template #footer>
        <el-button @click="purchaseDialog=false">Hủy</el-button>
        <el-button type="primary" @click="purchase" :loading="purchasing">XÁC NHẬN ĐĂNG KÝ</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { membershipAPI, invoiceAPI } from '@/api'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()
const memberships    = ref([])
const active         = ref(null)
const purchaseDialog = ref(false)
const purchasing     = ref(false)
const paymentMethod  = ref('MOMO')

const vipPrice = 299000

const freeFeatures = ['Tập không giới hạn', 'Theo dõi tiến độ', 'Thư viện bài tập']
const vipFeatures  = ['Tất cả tính năng gói Free', 'Dashboard đầy đủ']

const daysPercent = computed(() => {
  if (!active.value) return 0
  const total = daysBetween(active.value.startDate, active.value.endDate)
  const left  = active.value.daysRemaining
  return Math.max(0, Math.round((left / total) * 100))
})
function daysBetween(a, b) {
  return Math.round((new Date(b) - new Date(a)) / 86400000)
}

async function load() {
  try {
    const [all, act] = await Promise.all([membershipAPI.getAll(), membershipAPI.getActive().catch(()=>({data:null}))])
    memberships.value = all.data || []
    active.value      = act.data
  } catch {}
}

async function purchase() {
  purchasing.value = true
  try {
    const response = await invoiceAPI.create('VIP')
    const data = response.data
    const invoiceId = data.id
    purchaseDialog.value = false
    if (invoiceId) {
      router.push(`/app/payment/${invoiceId}`)
    }
  } catch {} finally { purchasing.value = false }
}

function payBadge(s) { return { PAID:'badge-success', PENDING:'badge-warning', FAILED:'badge-danger', REFUNDED:'badge-info' }[s]||'' }
function payLabel(s) { return { PAID:'Đã TT', PENDING:'Chờ TT', FAILED:'Thất bại', REFUNDED:'Hoàn tiền' }[s]||s }

onMounted(load)
</script>

<style scoped>
.active-inner { display:flex; justify-content:space-between; align-items:flex-start; }
.days-remain  { text-align:right; }
.days-num  { font-family:var(--font-display); font-size:3rem; line-height:1; color:var(--c-accent); }
.days-lbl  { font-size:0.75rem; color:var(--c-text3); text-transform:uppercase; letter-spacing:0.08em; }
.vip-chip { font-size:0.9rem; background:var(--c-accent); color:#fff; padding:2px 10px; border-radius:20px; margin-left:8px; vertical-align:middle; }

.packages-grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(240px,1fr)); gap:16px; margin-bottom:8px; max-width:600px; }
.pkg-card {
  background:var(--c-card); border:2px solid var(--c-border2);
  border-radius:var(--radius-lg); padding:20px;
  display:flex; flex-direction:column; gap:8px;
  position:relative; box-shadow:var(--shadow);
}
.pkg-card.featured{ border-color:var(--c-accent); }
.pkg-badge {
  position:absolute; top:-10px; left:50%; transform:translateX(-50%);
  background:var(--c-accent); color:#fff; font-size:0.65rem; font-weight:700;
  letter-spacing:0.1em; padding:3px 12px; border-radius:20px;
}
.pkg-type  { font-size:1.3rem; color:var(--c-text); }
.pkg-price { font-size:1.4rem; font-weight:700; }
.pkg-features { list-style:none; padding:0; flex:1; }
.pkg-features li { font-size:0.82rem; color:var(--c-text2); padding:3px 0; }
.pkg-features li.limited { color:#b45309; }
.pkg-features li.highlight { color:var(--c-accent); font-weight:600; }

.price-summary {
  display:flex; justify-content:space-between; align-items:center;
  padding:12px 16px; background:var(--c-card2); border-radius:var(--radius-lg);
  margin-top:4px; font-size:0.875rem;
}
.price-summary strong { font-size:1.2rem; font-family:var(--font-display); }
</style>
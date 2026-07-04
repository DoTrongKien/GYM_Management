<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>GÓI TẬP</h2>
      <el-button type="primary" @click="purchaseDialog=true">🛒 MUA GÓI MỚI</el-button>
    </div>

    <!-- Active membership -->
    <el-card v-if="active" style="margin-bottom:24px;border-left:4px solid var(--c-accent)" class="active-card">
      <div class="active-inner">
        <div>
          <div style="font-size:0.72rem;text-transform:uppercase;letter-spacing:0.12em;color:var(--c-accent);font-weight:700;margin-bottom:4px">GÓI ĐANG ACTIVE</div>
          <div class="display" style="font-size:2rem;color:var(--c-text)">{{ active.membershipType }}</div>
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
          :percentage="daysPercent" :color="active.daysRemaining < 7 ? '#C62828' : '#D4892A'"
          style="margin-top:16px" :show-text="false" :stroke-width="6"/>
    </el-card>

    <!-- Package grid -->
    <h3 class="display" style="font-size:1.4rem;color:var(--c-text-inv);margin-bottom:16px">CHỌN GÓI TẬP</h3>
    <div class="packages-grid">
      <div v-for="pkg in packages" :key="pkg.type"
           class="pkg-card"
           :class="{featured: pkg.featured, selected: form.membershipType===pkg.type}"
           @click="form.membershipType=pkg.type"
      >
        <div v-if="pkg.featured" class="pkg-badge">PHỔ BIẾN</div>
        <div class="pkg-type display">{{ pkg.type }}</div>
        <div class="pkg-price"><span class="accent">{{ pkg.price }}</span> <span style="font-size:0.8rem;color:var(--c-text2)">đ/{{ pkg.duration }}</span></div>
        <ul class="pkg-features">
          <li v-for="f in pkg.features" :key="f">✓ {{ f }}</li>
        </ul>
        <el-button :type="pkg.featured?'primary':'default'" style="width:100%;margin-top:auto"
                   @click.stop="selectPkg(pkg)">
          {{ form.membershipType===pkg.type ? '✓ Đã chọn' : 'Chọn gói' }}
        </el-button>
      </div>
    </div>

    <!-- History -->
    <el-card style="margin-top:24px">
      <template #header>LỊCH SỬ ĐĂNG KÝ</template>
      <el-table :data="memberships" stripe>
        <el-table-column label="Loại gói" prop="membershipType" width="110"/>
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

    <!-- Purchase Dialog -->
    <el-dialog v-model="purchaseDialog" title="ĐĂNG KÝ GÓI TẬP" width="440px" align-center>
      <el-form :model="form" label-position="top">
        <el-form-item label="Gói tập">
          <el-select v-model="form.membershipType" style="width:100%">
            <el-option label="BASIC — 299,000đ / 1 tháng" value="BASIC"/>
            <el-option label="STANDARD — 499,000đ / 3 tháng" value="STANDARD"/>
            <el-option label="PREMIUM — 799,000đ / 6 tháng" value="PREMIUM"/>
            <el-option label="VIP — 1,299,000đ / 12 tháng" value="VIP"/>
          </el-select>
        </el-form-item>
        <el-form-item label="Phương thức thanh toán">
          <el-select v-model="form.paymentMethod" style="width:100%">
            <el-option label="💳 Thẻ ngân hàng" value="CARD"/>
            <el-option label="📱 MoMo" value="MOMO"/>
            <el-option label="🏦 VNPay" value="VNPAY"/>
            <el-option label="💚 ZaloPay" value="ZALOPAY"/>
            <el-option label="💵 Tiền mặt tại quầy" value="CASH"/>
          </el-select>
        </el-form-item>
        <div class="price-summary">
          <span>Tổng thanh toán:</span>
          <strong class="accent">{{ pkgPrice }} đ</strong>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="purchaseDialog=false">Hủy</el-button>
        <el-button type="primary" @click="purchase" :loading="purchasing">XÁC NHẬN ĐĂNG KÝ</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { membershipAPI, invoiceAPI } from '@/api'
import { ElMessage } from 'element-plus'

const router        = useRouter()
const memberships   = ref([])
const active        = ref(null)
const purchaseDialog= ref(false)
const purchasing    = ref(false)
const form = reactive({ membershipType:'STANDARD', paymentMethod:'MOMO' })

const packages = [
  { type:'BASIC',    price:'299,000',   duration:'tháng',  features:['Tập không giới hạn','Theo dõi tiến độ','Thư viện bài tập'], featured:false },
  { type:'STANDARD', price:'499,000',   duration:'3 tháng',features:['Tất cả BASIC','Giáo án AI','Dinh dưỡng AI','Dashboard'], featured:true },
  { type:'PREMIUM',  price:'799,000',   duration:'6 tháng',features:['Tất cả STANDARD','Phân tích nâng cao','Hỗ trợ ưu tiên'], featured:false },
  { type:'VIP',      price:'1,299,000', duration:'năm',    features:['Tất cả tính năng','Tư vấn 1-1','Ưu đãi đặc biệt'], featured:false },
]

const priceMap = { BASIC:299000, STANDARD:499000, PREMIUM:799000, VIP:1299000 }
const pkgPrice = computed(() => (priceMap[form.membershipType]||0).toLocaleString())

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

function selectPkg(pkg) { form.membershipType = pkg.type; purchaseDialog.value = true }

async function purchase() {
  purchasing.value = true
  try {
    // QUAN TRỌNG: gọi invoiceAPI.create() để tạo HÓA ĐƠN MoMo (có qrCodeUrl, price đúng gói),
    // KHÔNG gọi membershipAPI.purchase() (API đó tạo Membership cũ, không có QR).
    const response = await invoiceAPI.create(form.membershipType)

    purchaseDialog.value = false

    // response = ApiResponse { success, message, data: InvoiceResponse }
    const data = response.data || response
    const invoiceId = data.id

    if (invoiceId) {
      ElMessage.success('Đang chuyển hướng đến trang thanh toán...')
      router.push(`/app/payment/${invoiceId}`)
    } else {
      ElMessage.warning('Tạo hóa đơn thành công nhưng không tìm thấy mã hóa đơn để quét QR.')
      load()
    }
  } catch {
    ElMessage.error('Có lỗi xảy ra trong quá trình tạo hóa đơn thanh toán.')
  } finally {
    purchasing.value = false
  }
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

.packages-grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(210px,1fr)); gap:16px; margin-bottom:8px; }
.pkg-card {
  background:var(--c-card); border:2px solid var(--c-border2);
  border-radius:var(--radius-lg); padding:20px;
  display:flex; flex-direction:column; gap:8px;
  cursor:pointer; transition: all var(--transition);
  position:relative; box-shadow:var(--shadow);
}
.pkg-card:hover   { border-color:var(--c-accent); box-shadow:var(--shadow-lg); }
.pkg-card.featured{ border-color:var(--c-accent); }
.pkg-card.selected{ border-color:var(--c-accent); background:#FFF8F0; }
.pkg-badge {
  position:absolute; top:-10px; left:50%; transform:translateX(-50%);
  background:var(--c-accent); color:#fff; font-size:0.65rem; font-weight:700;
  letter-spacing:0.1em; padding:3px 12px; border-radius:20px;
}
.pkg-type  { font-size:1.3rem; color:var(--c-text); }
.pkg-price { font-size:1.4rem; font-weight:700; }
.pkg-features { list-style:none; padding:0; flex:1; }
.pkg-features li { font-size:0.82rem; color:var(--c-text2); padding:3px 0; }

.price-summary {
  display:flex; justify-content:space-between; align-items:center;
  padding:12px 16px; background:var(--c-card2); border-radius:var(--radius-lg);
  margin-top:4px; font-size:0.875rem;
}
.price-summary strong { font-size:1.2rem; font-family:var(--font-display); }
</style>
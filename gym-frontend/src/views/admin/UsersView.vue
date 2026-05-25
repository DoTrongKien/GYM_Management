<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>QUẢN LÝ NGƯỜI DÙNG</h2>
      <div style="display:flex;gap:8px">
        <el-input v-model="search" placeholder="Tìm tên / email..." prefix-icon="Search" style="width:260px" clearable/>
        <el-tag type="info" style="height:32px;line-height:30px">{{ filtered.length }} user</el-tag>
      </div>
    </div>

    <el-table :data="filtered" v-loading="loading" stripe>
      <el-table-column label="ID" prop="id" width="60" align="center"/>
      <el-table-column label="Họ tên" prop="fullName" min-width="150"/>
      <el-table-column label="Email" prop="email" min-width="200"/>
      <el-table-column label="SĐT" prop="phone" width="120"/>
      <el-table-column label="Vai trò" width="100" align="center">
        <template #default="{row}">
          <el-tag :type="row.role==='ROLE_ADMIN'?'danger':'info'" size="small">
            {{ row.role==='ROLE_ADMIN'?'Admin':'User' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Trạng thái" width="110" align="center">
        <template #default="{row}">
          <span class="badge" :class="row.status?'badge-success':'badge-danger'">
            {{ row.status ? 'Active' : 'Khóa' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="Email XN" width="100" align="center">
        <template #default="{row}">
          <el-icon :color="row.emailVerified?'var(--c-success)':'var(--c-danger)'">
            <CircleCheck v-if="row.emailVerified"/><CircleClose v-else/>
          </el-icon>
        </template>
      </el-table-column>
      <el-table-column label="Ngày tạo" width="110">
        <template #default="{row}">{{ row.createdAt?.substring(0,10) || '--' }}</template>
      </el-table-column>
      <el-table-column label="Thao tác" width="220" align="center" fixed="right">
        <template #default="{row}">
          <el-button size="small" @click="viewDetail(row)">Chi tiết</el-button>
          <el-button size="small" :type="row.status?'danger':'success'" @click="toggleStatus(row)">
            {{ row.status ? 'Khóa' : 'Mở' }}
          </el-button>
          <el-button size="small" type="warning" @click="openReset(row)">Reset PW</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Detail Dialog -->
    <el-dialog v-model="detailDialog" title="CHI TIẾT NGƯỜI DÙNG" width="580px" align-center>
      <div v-if="detailUser">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="ID">{{ detailUser.id }}</el-descriptions-item>
          <el-descriptions-item label="Họ tên">{{ detailUser.fullName }}</el-descriptions-item>
          <el-descriptions-item label="Email">{{ detailUser.email }}</el-descriptions-item>
          <el-descriptions-item label="SĐT">{{ detailUser.phone || '--' }}</el-descriptions-item>
          <el-descriptions-item label="Trạng thái">
            <span class="badge" :class="detailUser.status?'badge-success':'badge-danger'">{{ detailUser.status?'Active':'Khóa' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="Email XN">{{ detailUser.emailVerified?'✅ Đã xác nhận':'❌ Chưa' }}</el-descriptions-item>
          <el-descriptions-item label="Ngày tạo" :span="2">{{ detailUser.createdAt?.substring(0,10) || '--' }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="detailProfile" style="margin-top:16px">
          <div class="display" style="font-size:0.95rem;color:var(--c-accent);margin-bottom:10px;letter-spacing:0.06em">HỒ SƠ TẬP LUYỆN</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="Chiều cao">{{ detailProfile.height || '--' }} cm</el-descriptions-item>
            <el-descriptions-item label="Cân nặng">{{ detailProfile.weight || '--' }} kg</el-descriptions-item>
            <el-descriptions-item label="BMI">{{ detailProfile.bmi || '--' }}</el-descriptions-item>
            <el-descriptions-item label="Mục tiêu">{{ goalLabel(detailProfile.goal) }}</el-descriptions-item>
            <el-descriptions-item label="Trình độ">{{ levelLabel(detailProfile.fitnessLevel) }}</el-descriptions-item>
            <el-descriptions-item label="Lịch tập">{{ detailProfile.availableDaysPerWeek || '--' }} ngày/tuần</el-descriptions-item>
          </el-descriptions>
        </div>
        <div v-else style="margin-top:12px;padding:12px;background:var(--c-card2);border-radius:var(--radius);font-size:0.82rem;color:var(--c-text3)">
          ℹ️ User chưa hoàn thiện hồ sơ
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialog=false">Đóng</el-button>
        <el-button type="primary" @click="$router.push('/admin/memberships');detailDialog=false">Xem hóa đơn</el-button>
      </template>
    </el-dialog>

    <!-- Reset PW Dialog -->
    <el-dialog v-model="resetDialog" title="RESET MẬT KHẨU" width="380px" align-center>
      <div style="margin-bottom:12px;color:var(--c-text2);font-size:0.85rem">
        Đặt lại mật khẩu cho: <strong>{{ resetTarget?.email }}</strong>
      </div>
      <el-form label-position="top">
        <el-form-item label="Mật khẩu mới (tối thiểu 6 ký tự)">
          <el-input v-model="newPw" type="password" show-password placeholder="Nhập mật khẩu mới"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetDialog=false">Hủy</el-button>
        <el-button type="primary" @click="resetPassword">XÁC NHẬN RESET</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminAPI } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const users        = ref([])
const loading      = ref(true)
const search       = ref('')
const detailDialog = ref(false)
const resetDialog  = ref(false)
const detailUser   = ref(null)
const detailProfile= ref(null)
const resetTarget  = ref(null)
const newPw        = ref('')

const filtered = computed(() => {
  if (!search.value) return users.value
  const q = search.value.toLowerCase()
  return users.value.filter(u => u.fullName?.toLowerCase().includes(q) || u.email?.toLowerCase().includes(q))
})

async function load() {
  loading.value = true
  try { const r = await adminAPI.getUsers(); users.value = r.data || [] }
  finally { loading.value = false }
}

async function viewDetail(row) {
  detailUser.value = row; detailProfile.value = null; detailDialog.value = true
  try { const r = await adminAPI.getUserProfile(row.id); detailProfile.value = r.data } catch {}
}

async function toggleStatus(row) {
  const action = row.status ? 'khóa' : 'mở khóa'
  await ElMessageBox.confirm(`${action.charAt(0).toUpperCase()+action.slice(1)} tài khoản ${row.email}?`, 'Xác nhận', { type:'warning' })
  await adminAPI.toggleStatus(row.id, !row.status)
  ElMessage.success(`Đã ${action} tài khoản!`)
  load()
}

function openReset(row) { resetTarget.value = row; newPw.value = ''; resetDialog.value = true }

async function resetPassword() {
  if (newPw.value.length < 6) { ElMessage.warning('Mật khẩu tối thiểu 6 ký tự'); return }
  await adminAPI.resetPassword(resetTarget.value.id, newPw.value)
  ElMessage.success('Đã reset mật khẩu!'); resetDialog.value = false
}

function goalLabel(g)  { return { WEIGHT_LOSS:'Giảm cân', MUSCLE_GAIN:'Tăng cơ', ENDURANCE:'Sức bền', FLEXIBILITY:'Linh hoạt', MAINTENANCE:'Duy trì' }[g]||g||'--' }
function levelLabel(l) { return { BEGINNER:'Mới bắt đầu', INTERMEDIATE:'Trung bình', ADVANCED:'Nâng cao' }[l]||l||'--' }

onMounted(load)
</script>
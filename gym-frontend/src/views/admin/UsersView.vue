<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>QUẢN LÝ NGƯỜI DÙNG</h2>
      <el-input v-model="search" placeholder="Tìm theo tên / email..." prefix-icon="Search" style="width:260px" clearable />
    </div>

    <el-table :data="filtered" v-loading="loading" stripe>
      <el-table-column label="ID" prop="id" width="60" align="center" />
      <el-table-column label="Họ tên" prop="fullName" min-width="160" />
      <el-table-column label="Email" prop="email" min-width="200" />
      <el-table-column label="SĐT" prop="phone" width="130" />
      <el-table-column label="Vai trò" width="110" align="center">
        <template #default="{row}">
          <el-tag :type="row.role === 'ROLE_ADMIN' ? 'danger' : 'info'" size="small">
            {{ row.role === 'ROLE_ADMIN' ? 'Admin' : 'User' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Trạng thái" width="110" align="center">
        <template #default="{row}">
          <span class="badge" :class="row.status ? 'badge-success' : 'badge-danger'">
            {{ row.status ? 'Active' : 'Bị khóa' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="Email xác nhận" width="130" align="center">
        <template #default="{row}">
          <el-icon :color="row.emailVerified ? 'var(--c-success)' : 'var(--c-danger)'">
            <CircleCheck v-if="row.emailVerified" /><CircleClose v-else />
          </el-icon>
        </template>
      </el-table-column>
      <el-table-column label="Ngày tạo" width="120">
        <template #default="{row}">{{ row.createdAt?.substring(0,10) }}</template>
      </el-table-column>
      <el-table-column label="Thao tác" width="200" align="center" fixed="right">
        <template #default="{row}">
          <el-button-group>
            <el-button size="small" @click="viewDetail(row)">Chi tiết</el-button>
            <el-button
              size="small"
              :type="row.status ? 'danger' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status ? 'Khóa' : 'Mở' }}
            </el-button>
            <el-button size="small" type="warning" @click="openReset(row)">Reset PW</el-button>
          </el-button-group>
        </template>
      </el-table-column>
    </el-table>

    <!-- Detail Dialog -->
    <el-dialog v-model="detailDialog" title="CHI TIẾT NGƯỜI DÙNG" width="560px">
      <div v-if="detailUser">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="ID">{{ detailUser.id }}</el-descriptions-item>
          <el-descriptions-item label="Họ tên">{{ detailUser.fullName }}</el-descriptions-item>
          <el-descriptions-item label="Email">{{ detailUser.email }}</el-descriptions-item>
          <el-descriptions-item label="SĐT">{{ detailUser.phone || '--' }}</el-descriptions-item>
          <el-descriptions-item label="Trạng thái">{{ detailUser.status ? 'Active' : 'Bị khóa' }}</el-descriptions-item>
          <el-descriptions-item label="Email XN">{{ detailUser.emailVerified ? 'Đã xác nhận' : 'Chưa' }}</el-descriptions-item>
        </el-descriptions>

        <!-- Profile info -->
        <div v-if="detailProfile" style="margin-top:16px">
          <div class="display" style="font-size:1rem; margin-bottom:10px; color:var(--c-accent)">HỒ SƠ</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="Chiều cao">{{ detailProfile.height || '--' }} cm</el-descriptions-item>
            <el-descriptions-item label="Cân nặng">{{ detailProfile.weight || '--' }} kg</el-descriptions-item>
            <el-descriptions-item label="BMI">{{ detailProfile.bmi || '--' }}</el-descriptions-item>
            <el-descriptions-item label="Mục tiêu">{{ detailProfile.goal || '--' }}</el-descriptions-item>
            <el-descriptions-item label="Trình độ">{{ detailProfile.fitnessLevel || '--' }}</el-descriptions-item>
            <el-descriptions-item label="Ngày rảnh/tuần">{{ detailProfile.availableDaysPerWeek || '--' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </div>

      <div style="margin-top:16px; display:flex; gap:8px; justify-content:flex-end">
        <el-button @click="detailDialog=false">Đóng</el-button>
        <el-button type="primary" @click="$router.push(`/admin/memberships`); detailDialog=false">
          Xem hóa đơn
        </el-button>
      </div>
    </el-dialog>

    <!-- Reset Password Dialog -->
    <el-dialog v-model="resetDialog" title="RESET MẬT KHẨU" width="380px">
      <el-form label-position="top">
        <el-form-item label="Mật khẩu mới">
          <el-input v-model="newPassword" type="password" show-password placeholder="Tối thiểu 6 ký tự" />
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
const newPassword  = ref('')
const resetTarget  = ref(null)

const filtered = computed(() => {
  if (!search.value) return users.value
  const q = search.value.toLowerCase()
  return users.value.filter(u =>
    u.fullName?.toLowerCase().includes(q) || u.email?.toLowerCase().includes(q)
  )
})

async function load() {
  loading.value = true
  try { const r = await adminAPI.getUsers(); users.value = r.data || [] }
  finally { loading.value = false }
}

async function viewDetail(row) {
  detailUser.value = row
  detailProfile.value = null
  detailDialog.value = true
  try {
    const r = await adminAPI.getUserProfile(row.id)
    detailProfile.value = r.data
  } catch {}
}

async function toggleStatus(row) {
  const action = row.status ? 'khóa' : 'kích hoạt'
  await ElMessageBox.confirm(`Bạn muốn ${action} tài khoản ${row.email}?`, 'Xác nhận', { type: 'warning' })
  await adminAPI.toggleStatus(row.id, !row.status)
  ElMessage.success(`Đã ${action} tài khoản!`)
  load()
}

function openReset(row) { resetTarget.value = row; newPassword.value = ''; resetDialog.value = true }

async function resetPassword() {
  if (newPassword.value.length < 6) { ElMessage.warning('Mật khẩu tối thiểu 6 ký tự'); return }
  await adminAPI.resetPassword(resetTarget.value.id, newPassword.value)
  ElMessage.success('Đã reset mật khẩu thành công!')
  resetDialog.value = false
}

onMounted(load)
</script>

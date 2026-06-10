<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>BUỔI TẬP</h2>
      <el-button type="primary" @click="scheduleDialog=true">+ Đặt lịch mới</el-button>
    </div>

    <el-card style="margin-bottom:16px">
      <div style="display:flex;gap:12px;flex-wrap:wrap">
        <el-select v-model="filterStatus" placeholder="Trạng thái" clearable style="width:160px">
          <el-option label="⏳ Chờ" value="SCHEDULED"/>
          <el-option label="🏃 Đang tập" value="CHECKED_IN"/>
          <el-option label="✅ Hoàn thành" value="COMPLETED"/>
          <el-option label="❌ Bỏ qua" value="SKIPPED"/>
        </el-select>
        <el-date-picker v-model="filterDate" type="daterange" format="DD/MM/YYYY"
                        value-format="YYYY-MM-DD" range-separator="→" start-placeholder="Từ" end-placeholder="Đến"
                        style="width:260px"/>
        <el-button @click="filterStatus='';filterDate=null">Xóa lọc</el-button>
      </div>
    </el-card>

    <el-table :data="filtered" v-loading="loading" stripe>
      <el-table-column label="Ngày" width="110">
        <template #default="{row}">{{ fmtDate(row.sessionDate) }}</template>
      </el-table-column>
      <el-table-column label="Giờ" width="80" align="center">
        <template #default="{row}">{{ row.scheduledTime ? row.scheduledTime.substring(0,5) : '--' }}</template>
      </el-table-column>
      <el-table-column label="Buổi tập" min-width="180">
        <template #default="{row}">{{ row.customSessionName || row.planName || 'Buổi tập' }}</template>
      </el-table-column>
      <el-table-column label="Ngày trong tuần" prop="dayName" width="130">
        <template #default="{row}">{{ row.dayName || (row.isCustom ? 'Tự đặt' : '--') }}</template>
      </el-table-column>
      <el-table-column label="Tuần" width="65" align="center">
        <template #default="{row}">{{ row.weekNumber ? 'W' + row.weekNumber : '--' }}</template>
      </el-table-column>
      <el-table-column label="Trạng thái" width="130" align="center">
        <template #default="{row}">
          <span class="badge" :class="statusBadge(row.status)">{{ statusLabel(row.status) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="Calories" width="100" align="right">
        <template #default="{row}">{{ row.totalCaloriesBurned ? row.totalCaloriesBurned + ' kcal' : '--' }}</template>
      </el-table-column>
      <el-table-column label="Thời gian" width="100" align="right">
        <template #default="{row}">{{ row.durationMinutes ? row.durationMinutes + ' phút' : '--' }}</template>
      </el-table-column>
      <el-table-column label="Thao tác" width="180" align="center" fixed="right">
        <template #default="{row}">
          <el-button v-if="row.status==='SCHEDULED'" type="primary" size="small" @click="checkIn(row.id)">Check-in</el-button>
          <el-button v-if="row.status==='CHECKED_IN'" type="success" size="small" @click="openCheckOut(row)">Hoàn thành</el-button>
          <el-button v-if="row.status==='SCHEDULED'" size="small" plain @click="skip(row.id)">Bỏ</el-button>
          <el-button v-if="row.status==='SCHEDULED' && row.isCustom" size="small" type="danger" plain @click="del(row.id)">Xóa</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="scheduleDialog" title="ĐẶT LỊCH TẬP" width="440px" align-center>
      <el-form :model="schedForm" label-position="top">
        <el-form-item label="Tên buổi tập">
          <el-input v-model="schedForm.customSessionName" placeholder="VD: Tập ngực + tay sau"/>
        </el-form-item>
        <div class="grid-2">
          <el-form-item label="Ngày tập">
            <el-date-picker v-model="schedForm.sessionDate" type="date"
                            format="DD/MM/YYYY" value-format="YYYY-MM-DD" style="width:100%" :disabled-date="d=>d<new Date(new Date().setHours(0,0,0,0))"/>
          </el-form-item>
          <el-form-item label="Giờ bắt đầu">
            <el-time-picker v-model="schedForm.scheduledTime" format="HH:mm"
                            value-format="HH:mm:ss" placeholder="06:00" style="width:100%"/>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="scheduleDialog=false">Hủy</el-button>
        <el-button type="primary" @click="scheduleSession">ĐẶT LỊCH</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="checkOutDialog" title="CHECK-OUT BUỔI TẬP" width="460px" align-center>
      <el-form label-position="top">
        <el-form-item required>
          <template #label>
            <span style="font-weight:700">Tỉ lệ hoàn thành * </span>
            <span style="color:var(--c-text3);font-size:0.8rem">(bắt buộc)</span>
          </template>
          <div style="display:flex;align-items:center;gap:12px">
            <el-slider v-model="coForm.completionRate" :min="0" :max="100" :step="5" style="flex:1"
                       :marks="{0:'0%',50:'50%',90:'90%',100:'100%'}"/>
            <span class="rate-badge" :class="rateClass">{{ coForm.completionRate }}%</span>
          </div>
          <div class="rate-hint">{{ rateHint }}</div>
        </el-form-item>

        <template v-if="coSession?.isLastSessionOfWeek">
          <el-divider><span style="color:var(--c-accent);font-size:0.82rem">📊 TIẾN ĐỘ CUỐI TUẦN (BẮT BUỘC)</span></el-divider>
          <el-form-item label="Cân nặng hiện tại (kg) *">
            <el-input-number v-model="coForm.checkoutWeight" :min="30" :max="300"
                             :precision="1" style="width:100%" placeholder="Nhập cân nặng"/>
          </el-form-item>
          <div class="info-box">
            ℹ️ Dữ liệu dùng để điều chỉnh giáo án tuần tiếp theo.
          </div>
        </template>

        <el-form-item label="Ghi chú" style="margin-top:10px">
          <el-input v-model="coForm.notes" type="textarea" :rows="2" placeholder="Cảm giác hôm nay..."/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="checkOutDialog=false">Hủy</el-button>
        <el-button type="primary" @click="submitCheckOut" :loading="checkingOut">
          ✅ XÁC NHẬN CHECK-OUT
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { sessionAPI } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'

const sessions        = ref([])
const loading         = ref(true)
const filterStatus    = ref('')
const filterDate      = ref(null)
const scheduleDialog  = ref(false)
const checkOutDialog  = ref(false)
const checkingOut     = ref(false)
const coSession       = ref(null)

const schedForm = reactive({ customSessionName: '', sessionDate: dayjs().format('YYYY-MM-DD'), scheduledTime: '06:00:00' })
const coForm    = reactive({ completionRate: 100, checkoutWeight: null, notes: '' })

const filtered = computed(() => {
  let list = sessions.value
  if (filterStatus.value) list = list.filter(s => s.status === filterStatus.value)
  if (filterDate.value && filterDate.value.length === 2) {
    list = list.filter(s => s.sessionDate >= filterDate.value[0] && s.sessionDate <= filterDate.value[1])
  }
  return list
})

const rateClass = computed(() => {
  const r = coForm.completionRate
  if (r >= 80) return 'text-success'
  if (r >= 50) return 'text-warning'
  return 'text-danger'
})

const rateHint = computed(() => {
  const r = coForm.completionRate
  if (r === 100) return 'Hoàn thành xuất sắc toàn bộ bài tập!'
  if (r >= 80) return 'Hoàn thành hầu hết các bài tập chính.'
  if (r >= 50) return 'Hoàn thành được một nửa buổi tập.'
  return 'Tập được rất ít hoặc phải nghỉ sớm.'
})

async function load() {
  loading.value = true
  try { const r = await sessionAPI.getAll(); sessions.value = r.data || [] }
  finally { loading.value = false }
}
async function checkIn(id) {
  await sessionAPI.checkIn(id); ElMessage.success('Check-in thành công! 💪'); load()
}
function openCheckOut(row) {
  coSession.value = row
  coForm.completionRate = 100
  coForm.checkoutWeight = row.checkoutWeight || null
  coForm.notes = ''
  checkOutDialog.value = true
}
async function submitCheckOut() {
  if (coSession.value?.isLastSessionOfWeek && !coForm.checkoutWeight) {
    ElMessage.warning('Vui lòng nhập cân nặng cuối tuần!')
    return
  }
  checkingOut.value = true
  try {
    await sessionAPI.complete(coSession.value.id, {
      sessionId: coSession.value.id,
      exerciseLogs: [],
      ...coForm
    })
    ElMessage.success('Hoàn thành buổi tập! 🎉')
    checkOutDialog.value = false
    load()
  } finally {
    checkingOut.value = false
  }
}
async function skip(id) {
  await sessionAPI.skip(id, ''); ElMessage.info('Đã bỏ qua buổi tập'); load()
}
async function del(id) {
  await ElMessageBox.confirm('Xóa lịch tập này?', 'Xác nhận', { type: 'warning' })
  await sessionAPI.delete(id); ElMessage.success('Đã xóa'); load()
}
async function scheduleSession() {
  if (!schedForm.sessionDate) { ElMessage.warning('Chọn ngày tập'); return }
  await sessionAPI.schedule({ ...schedForm })
  ElMessage.success('Đã đặt lịch tập!'); scheduleDialog.value = false; load()
}

function fmtDate(d) { return dayjs(d).format('DD/MM/YYYY') }
function statusLabel(s) { return { SCHEDULED:'Chờ', CHECKED_IN:'Đang tập', COMPLETED:'Hoàn thành', SKIPPED:'Bỏ qua' }[s] || s }
function statusBadge(s) { return { SCHEDULED:'badge-info', CHECKED_IN:'badge-warning', COMPLETED:'badge-success', SKIPPED:'badge-danger' }[s] || '' }

onMounted(load)
</script>
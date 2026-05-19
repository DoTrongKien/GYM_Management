<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>LỊCH SỬ BUỔI TẬP</h2>
    </div>

    <el-table :data="sessions" v-loading="loading" stripe>
      <el-table-column label="Ngày" prop="sessionDate" width="110">
        <template #default="{row}">{{ formatDate(row.sessionDate) }}</template>
      </el-table-column>
      <el-table-column label="Giáo án" prop="planName" min-width="160" />
      <el-table-column label="Buổi" prop="dayName" width="110" />
      <el-table-column label="Tuần" prop="weekNumber" width="70" align="center">
        <template #default="{row}">W{{ row.weekNumber }}</template>
      </el-table-column>
      <el-table-column label="Trạng thái" width="130" align="center">
        <template #default="{row}">
          <span class="badge" :class="statusBadge(row.status)">{{ statusLabel(row.status) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="Calories" width="100" align="right">
        <template #default="{row}">{{ row.totalCaloriesBurned || '--' }} kcal</template>
      </el-table-column>
      <el-table-column label="Thời gian" width="100" align="right">
        <template #default="{row}">{{ row.durationMinutes ? row.durationMinutes + ' phút' : '--' }}</template>
      </el-table-column>
      <el-table-column label="Thao tác" width="160" align="center">
        <template #default="{row}">
          <el-button v-if="row.status==='SCHEDULED'" type="primary" size="small" @click="checkIn(row.id)">Check-in</el-button>
          <el-button v-if="row.status==='CHECKED_IN'" type="success" size="small" @click="openComplete(row)">Hoàn thành</el-button>
          <el-button v-if="row.status==='SCHEDULED'" type="info" size="small" plain @click="skip(row.id)">Bỏ qua</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Complete Dialog -->
    <el-dialog v-model="completeDialog" title="HOÀN THÀNH BUỔI TẬP" width="480px">
      <el-form label-position="top">
        <p class="muted" style="margin-bottom:12px">Nhập kết quả hoặc nhấn Lưu nhanh</p>
        <el-form-item label="Ghi chú">
          <el-input v-model="completeNote" type="textarea" :rows="2" placeholder="Cảm giác hôm nay..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialog=false">Hủy</el-button>
        <el-button type="primary" @click="submitComplete">LƯU NHANH</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { sessionAPI } from '@/api'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const sessions       = ref([])
const loading        = ref(true)
const completeDialog = ref(false)
const currentId      = ref(null)
const completeNote   = ref('')

async function load() {
  loading.value = true
  try { const r = await sessionAPI.getAll(); sessions.value = r.data || [] }
  finally { loading.value = false }
}
async function checkIn(id) {
  await sessionAPI.checkIn(id); ElMessage.success('Check-in!'); load()
}
function openComplete(row) { currentId.value = row.id; completeDialog.value = true }
async function submitComplete() {
  await sessionAPI.complete(currentId.value, { sessionId: currentId.value, exerciseLogs: [] })
  ElMessage.success('Hoàn thành! 🎉'); completeDialog.value = false; load()
}
async function skip(id) {
  await sessionAPI.skip(id, ''); ElMessage.info('Đã bỏ qua buổi tập'); load()
}
function formatDate(d) { return dayjs(d).format('DD/MM/YYYY') }
function statusLabel(s) {
  return { SCHEDULED:'Chờ', CHECKED_IN:'Đang tập', COMPLETED:'Hoàn thành', SKIPPED:'Bỏ qua' }[s] || s
}
function statusBadge(s) {
  return { SCHEDULED:'badge-info', CHECKED_IN:'badge-warning', COMPLETED:'badge-success', SKIPPED:'badge-danger' }[s] || ''
}
onMounted(load)
</script>
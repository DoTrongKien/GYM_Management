<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>BUỔI TẬP</h2>
    </div>

    <PetWidget/>

    <!-- Số liệu tổng quan -->
    <div class="stats-bar">
      <div class="stat-item">
        <span class="stat-number">{{ totalCompleted }}</span>
        <span class="stat-label">buổi đã hoàn thành</span>
      </div>
      <div class="stat-item">
        <span class="stat-number">{{ sessions.length }}</span>
        <span class="stat-label">tổng số buổi</span>
      </div>
    </div>

    <el-table :data="sessions" v-loading="loading" stripe>
      <el-table-column label="Ngày" width="110">
        <template #default="{row}">{{ fmtDate(row.sessionDate) }}</template>
      </el-table-column>
      <el-table-column label="Giờ" width="80" align="center">
        <template #default="{row}">{{ row.scheduledTime ? row.scheduledTime.substring(0,5) : '--' }}</template>
      </el-table-column>
      <el-table-column label="Buổi tập" min-width="180">
        <template #default="{row}">{{ row.customSessionName || row.planName || 'Buổi tập' }}</template>
      </el-table-column>
      <el-table-column label="Ngày trong tuần" width="130">
        <template #default="{row}">{{ fmtDayOfWeek(row.sessionDate) }}</template>
      </el-table-column>
      <el-table-column label="Tuần" width="65" align="center">
        <template #default="{row}">{{ row.weekNumber ? 'W' + row.weekNumber : '--' }}</template>
      </el-table-column>
      <el-table-column label="Trạng thái" width="140" align="center">
        <template #default="{row}">
          <span class="status-pill" :class="statusClass(row.status)">
            {{ statusIcon(row.status) }} {{ statusLabel(row.status) }}
          </span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { sessionAPI } from '@/api'
import PetWidget from '@/components/pet/PetWidget.vue'
import dayjs from 'dayjs'
import 'dayjs/locale/vi'
dayjs.locale('vi')

const sessions = ref([])
const loading  = ref(true)

// Không filter — luôn hiển thị toàn bộ lịch sử buổi tập, kể cả SKIPPED, CHECKED_IN dở dang...
const totalCompleted = computed(() =>
  sessions.value.filter(s => s.status === 'COMPLETED').length
)

async function load() {
  loading.value = true
  try {
    const r = await sessionAPI.getAll()
    // Sắp xếp mới nhất lên trên; nếu BE đã sort thì bỏ dòng dưới cũng không sao
    sessions.value = (r.data || []).sort((a, b) => (a.sessionDate < b.sessionDate ? 1 : -1))
  } finally {
    loading.value = false
  }
}

function fmtDate(d) { return dayjs(d).format('DD/MM/YYYY') }
function fmtDayOfWeek(d) { return d ? dayjs(d).format('dddd') : '--' }

function statusIcon(s) {
  return { SCHEDULED: '⏳', CHECKED_IN: '🏃', COMPLETED: '✅', SKIPPED: '❌' }[s] || '❔'
}
function statusLabel(s) {
  return { SCHEDULED: 'Chờ', CHECKED_IN: 'Đang tập', COMPLETED: 'Hoàn thành', SKIPPED: 'Bỏ qua' }[s] || s
}
function statusClass(s) {
  return {
    SCHEDULED: 'status-scheduled',
    CHECKED_IN: 'status-checkedin',
    COMPLETED: 'status-completed',
    SKIPPED: 'status-skipped'
  }[s] || ''
}

onMounted(load)
</script>

<style scoped>
.stats-bar {
  display: flex;
  gap: 32px;
  margin-bottom: 20px;
  padding: 16px 20px;
  background: var(--c-surface, #fff);
  border-radius: 10px;
}
.stat-item { display: flex; flex-direction: column; align-items: flex-start; }
.stat-number { font-size: 1.6rem; font-weight: 800; line-height: 1; }
.stat-label { font-size: 0.8rem; color: var(--c-text3, #888); margin-top: 4px; }

.status-pill {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 0.82rem;
  font-weight: 600;
}
.status-scheduled  { background: #e8f0fe; color: #1a56db; }
.status-checkedin  { background: #fff4e0; color: #b45309; }
.status-completed  { background: #e6f7ea; color: #15803d; }
.status-skipped    { background: #fdecec; color: #b91c1c; }
</style>
<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>DASHBOARD</h2>
      <span class="mono" style="font-size:0.8rem;color:var(--c-text-inv2)">{{ today }}</span>
    </div>

    <div v-if="loading"><el-skeleton :rows="6" animated style="background:var(--c-card);padding:24px;border-radius:12px"/></div>

    <template v-else>
      <!-- Stat cards -->
      <div class="grid-4" style="margin-bottom:24px">
        <div class="stat-card accent-card" style="cursor:pointer" @click="$router.push('/app/sessions')">
          <div class="label">BUỔI HOÀN THÀNH</div>
          <div class="value">{{ data.completedSessions || 0 }}</div>
          <div class="sub">/ {{ data.totalSessions || 0 }} tổng buổi</div>
          <div class="icon">💪</div>
        </div>
        <div class="stat-card">
          <div class="label">CALORIES ĐÃ ĐỐT</div>
          <div class="value">{{ formatNum(data.totalCaloriesBurned) }}</div>
          <div class="sub">kcal tổng cộng</div>
          <div class="icon">🔥</div>
        </div>
        <div class="stat-card">
          <div class="label">STREAK HIỆN TẠI</div>
          <div class="value">{{ data.currentStreak || 0 }}</div>
          <div class="sub">ngày liên tiếp (kỷ lục: {{ data.longestStreak || 0 }})</div>
          <div class="icon">⚡</div>
        </div>
        <div class="stat-card">
          <div class="label">CÂN NẶNG</div>
          <div class="value">{{ data.currentWeight || '--' }}</div>
          <div class="sub" :style="{color: weightColor}">{{ weightText }}</div>
          <div class="icon">⚖️</div>
        </div>
      </div>

      <!-- Charts row -->
      <div class="grid-2" style="margin-bottom:24px">
        <el-card>
          <template #header>CALORIES TUẦN NÀY (kcal)</template>
          <div style="height:200px;position:relative">
            <canvas ref="calChart"></canvas>
            <div v-if="noCalories" class="chart-empty">Chưa có dữ liệu tuần này</div>
          </div>
        </el-card>
        <el-card>
          <template #header>SỐ BUỔI TẬP THEO TUẦN</template>
          <div style="height:200px;position:relative">
            <canvas ref="wkChart"></canvas>
            <div v-if="noWorkouts" class="chart-empty">Chưa có dữ liệu</div>
          </div>
        </el-card>
      </div>

      <!-- Week schedule -->
      <el-card style="margin-bottom:24px">
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <span>LỊCH TẬP TUẦN NÀY</span>
            <el-button type="primary" size="small" @click="scheduleDialog=true">+ Đặt lịch</el-button>
          </div>
        </template>

        <div v-if="!weekSessions.length" class="empty-state" style="padding:24px">
          Chưa có lịch tập. <router-link to="/app/plan">Tạo giáo án →</router-link>
          hoặc <el-button text type="primary" @click="scheduleDialog=true">đặt lịch tự do</el-button>
        </div>

        <div v-else class="session-grid">
          <div
              v-for="s in weekSessions" :key="s.id"
              class="session-card"
              :class="s.status.toLowerCase()"
              @click="goToSession(s)"
          >
            <div class="session-day">{{ s.dayName || 'Tự do' }}</div>
            <div class="session-date">{{ fmtDate(s.sessionDate) }}</div>
            <div v-if="s.scheduledTime" class="session-time">🕐 {{ s.scheduledTime }}</div>
            <div class="session-plan">{{ s.customSessionName || s.planName || 'Buổi tập' }}</div>
            <span class="badge mt-4" :class="statusBadge(s.status)">{{ statusLabel(s.status) }}</span>
            <div class="session-actions" @click.stop>
              <el-button v-if="s.status==='SCHEDULED'" type="primary" size="small" @click="checkIn(s.id)">CHECK-IN</el-button>
              <el-button v-if="s.status==='CHECKED_IN'" size="small" type="success" @click="openComplete(s)">XONG</el-button>
            </div>
          </div>
        </div>
      </el-card>
    </template>

    <!-- Schedule Dialog -->
    <el-dialog v-model="scheduleDialog" title="ĐẶT LỊCH TẬP" width="440px" align-center>
      <el-form :model="schedForm" label-position="top">
        <el-form-item label="Tên buổi tập">
          <el-input v-model="schedForm.customSessionName" placeholder="VD: Tập ngực + tay"/>
        </el-form-item>
        <div class="grid-2">
          <el-form-item label="Ngày tập">
            <el-date-picker v-model="schedForm.sessionDate" type="date" format="DD/MM/YYYY"
                            value-format="YYYY-MM-DD" style="width:100%" :disabled-date="disablePast"/>
          </el-form-item>
          <el-form-item label="Giờ tập">
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

    <!-- Complete Dialog -->
    <el-dialog v-model="completeDialog" title="HOÀN THÀNH BUỔI TẬP" width="460px" align-center>
      <p style="color:var(--c-text2);margin-bottom:12px">Ghi nhận kết quả nhanh:</p>
      <el-form label-position="top">
        <el-form-item label="Ghi chú">
          <el-input v-model="completeNote" type="textarea" :rows="2" placeholder="Cảm giác hôm nay..."/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialog=false">Hủy</el-button>
        <el-button type="primary" @click="submitComplete">LƯU KẾT QUẢ</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, nextTick } from 'vue'
import { Chart, registerables } from 'chart.js'
import { dashboardAPI, sessionAPI } from '@/api'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'

Chart.register(...registerables)

const router = useRouter()
const data         = ref({})
const weekSessions = ref([])
const loading      = ref(true)
const scheduleDialog = ref(false)
const completeDialog = ref(false)
const currentId    = ref(null)
const completeNote = ref('')
const calChart     = ref(null)
const wkChart      = ref(null)
let calChartInst   = null
let wkChartInst    = null

const today = dayjs().format('dddd, DD/MM/YYYY')

const schedForm = reactive({ customSessionName: '', sessionDate: dayjs().format('YYYY-MM-DD'), scheduledTime: '06:00:00' })

const disablePast = (d) => d < new Date(new Date().setHours(0,0,0,0))

const weightColor = computed(() => {
  const c = data.value.weightChange
  if (!c) return 'var(--c-text3)'
  return c < 0 ? 'var(--c-success)' : 'var(--c-warning)'
})
const weightText = computed(() => {
  const c = data.value.weightChange
  if (!c) return 'kg'
  return `${c > 0 ? '+' : ''}${c} kg từ ban đầu`
})

const noCalories = computed(() => !Object.values(data.value.weeklyCalories || {}).some(v => v > 0))
const noWorkouts = computed(() => !Object.values(data.value.weeklyWorkouts || {}).some(v => v > 0))

async function load() {
  loading.value = true
  try {
    const [dash, week] = await Promise.all([dashboardAPI.get(), sessionAPI.getWeek()])
    data.value         = dash.data || {}
    weekSessions.value = week.data || []
  } catch {} finally {
    loading.value = false
    nextTick(drawCharts)
  }
}

function drawCharts() {
  const CAL_COLOR = '#D4892A'
  const WK_COLOR  = '#6B4226'
  const GRID      = 'rgba(196,154,108,0.3)'
  const TICK      = '#4A3728'

  const baseOpts = (yLabel) => ({
    responsive: true, maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      x: { grid: { color: GRID }, ticks: { color: TICK, font: { size: 11 } } },
      y: { grid: { color: GRID }, ticks: { color: TICK, font: { size: 11 } },
        beginAtZero: true, title: { display: !!yLabel, text: yLabel, color: TICK } }
    }
  })

  // Calories chart
  if (calChart.value && data.value.weeklyCalories) {
    if (calChartInst) calChartInst.destroy()
    calChartInst = new Chart(calChart.value, {
      type: 'bar',
      data: {
        labels: Object.keys(data.value.weeklyCalories),
        datasets: [{ data: Object.values(data.value.weeklyCalories),
          backgroundColor: CAL_COLOR, borderRadius: 6, borderSkipped: false }]
      },
      options: baseOpts('kcal')
    })
  }

  // Weekly workouts chart
  if (wkChart.value && data.value.weeklyWorkouts) {
    if (wkChartInst) wkChartInst.destroy()
    wkChartInst = new Chart(wkChart.value, {
      type: 'bar',
      data: {
        labels: Object.keys(data.value.weeklyWorkouts),
        datasets: [{ data: Object.values(data.value.weeklyWorkouts),
          backgroundColor: WK_COLOR, borderRadius: 6, borderSkipped: false }]
      },
      options: baseOpts('buổi')
    })
  }
}

async function checkIn(id) {
  await sessionAPI.checkIn(id); ElMessage.success('Check-in thành công! 💪'); load()
}
function openComplete(s) { currentId.value = s.id; completeNote.value = ''; completeDialog.value = true }
async function submitComplete() {
  await sessionAPI.complete(currentId.value, { sessionId: currentId.value, exerciseLogs: [] })
  ElMessage.success('Hoàn thành! 🎉'); completeDialog.value = false; load()
}
async function scheduleSession() {
  if (!schedForm.sessionDate) { ElMessage.warning('Chọn ngày tập'); return }
  await sessionAPI.schedule(schedForm)
  ElMessage.success('Đã đặt lịch tập!'); scheduleDialog.value = false; load()
}
function goToSession(s) { router.push('/app/sessions') }

function fmtDate(d)     { return dayjs(d).format('ddd DD/MM') }
function formatNum(n)   { return n ? Number(n).toLocaleString() : '0' }
function statusLabel(s) { return { SCHEDULED:'Chờ', CHECKED_IN:'Đang tập', COMPLETED:'Xong', SKIPPED:'Bỏ' }[s] || s }
function statusBadge(s) { return { SCHEDULED:'badge-info', CHECKED_IN:'badge-warning', COMPLETED:'badge-success', SKIPPED:'badge-danger' }[s] || '' }

onMounted(load)
</script>

<style scoped>
.session-grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(150px,1fr)); gap:12px; }
.session-card {
  background:var(--c-card); border:1px solid var(--c-border2); border-radius:var(--radius-lg);
  padding:14px; cursor:pointer; transition: all var(--transition);
  display:flex; flex-direction:column; gap:4px;
}
.session-card:hover { border-color:var(--c-accent); box-shadow:var(--shadow); }
.session-card.completed { border-left:3px solid var(--c-success); }
.session-card.checked_in { border-left:3px solid var(--c-warning); }
.session-day  { font-family:var(--font-display); font-size:1rem; color:var(--c-accent); }
.session-date { font-size:0.75rem; color:var(--c-text3); }
.session-time { font-size:0.75rem; color:var(--c-info); font-weight:600; }
.session-plan { font-size:0.8rem; color:var(--c-text2); overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.session-actions { margin-top:8px; }
.mt-4 { margin-top:4px; }

.chart-empty {
  position:absolute; inset:0; display:flex; align-items:center; justify-content:center;
  color:var(--c-text3); font-size:0.85rem;
}
</style>
<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>DASHBOARD</h2>
      <span class="muted mono" style="font-size:0.8rem">{{ today }}</span>
    </div>

    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="6" animated />
    </div>

    <template v-else>
      <!-- Stats Grid -->
      <div class="grid-4" style="margin-bottom:24px">
        <div class="stat-card accent-card">
          <div class="label">BUỔI HOÀN THÀNH</div>
          <div class="value">{{ data.completedSessions || 0 }}</div>
          <div class="sub">/ {{ data.totalSessions || 0 }} tổng</div>
          <div class="icon">💪</div>
        </div>
        <div class="stat-card">
          <div class="label">CALORIES ĐÃ ĐỐT</div>
          <div class="value">{{ (data.totalCaloriesBurned || 0).toLocaleString() }}</div>
          <div class="sub">kcal tổng cộng</div>
          <div class="icon">🔥</div>
        </div>
        <div class="stat-card">
          <div class="label">STREAK HIỆN TẠI</div>
          <div class="value">{{ data.currentStreak || 0 }}</div>
          <div class="sub">ngày liên tiếp</div>
          <div class="icon">⚡</div>
        </div>
        <div class="stat-card">
          <div class="label">CÂN NẶNG</div>
          <div class="value">{{ data.currentWeight || '--' }}</div>
          <div class="sub" :class="weightChangeClass">
            {{ weightChangeText }}
          </div>
          <div class="icon">⚖️</div>
        </div>
      </div>

      <!-- Charts row -->
      <div class="grid-2" style="margin-bottom:24px">
        <!-- Weekly Calories Chart -->
        <el-card header="CALORIES TUẦN NÀY">
          <div style="height:200px">
            <Bar v-if="caloriesChartData" :data="caloriesChartData" :options="barOptions" />
          </div>
        </el-card>

        <!-- Weekly Workouts Chart -->
        <el-card header="BUỔI TẬP 4 TUẦN GẦN">
          <div style="height:200px">
            <Bar v-if="workoutsChartData" :data="workoutsChartData" :options="barOptions" />
          </div>
        </el-card>
      </div>

      <!-- This week sessions -->
      <el-card header="LỊCH TẬP TUẦN NÀY">
        <div v-if="weekSessions.length === 0" class="empty-state">
          <p>Chưa có buổi tập nào. <router-link to="/app/plan">Tạo giáo án →</router-link></p>
        </div>
        <div v-else class="session-grid">
          <div
              v-for="s in weekSessions" :key="s.id"
              class="session-card"
              :class="s.status.toLowerCase()"
          >
            <div class="session-day">{{ s.dayName }}</div>
            <div class="session-date">{{ formatDate(s.sessionDate) }}</div>
            <div class="session-plan">{{ s.planName }}</div>
            <div class="session-status">
              <span class="badge" :class="statusBadge(s.status)">{{ statusLabel(s.status) }}</span>
            </div>
            <div class="session-actions" v-if="s.status === 'SCHEDULED'">
              <el-button type="primary" size="small" @click="checkIn(s.id)">CHECK-IN</el-button>
            </div>
            <div class="session-actions" v-if="s.status === 'CHECKED_IN'">
              <el-button type="success" size="small" @click="openComplete(s)">HOÀN THÀNH</el-button>
            </div>
          </div>
        </div>
      </el-card>
    </template>

    <!-- Complete Session Dialog -->
    <el-dialog v-model="completeDialog" title="HOÀN THÀNH BUỔI TẬP" width="500px">
      <p class="muted" style="margin-bottom:16px">Ghi nhận kết quả buổi tập của bạn:</p>
      <div v-if="currentSession" class="exercise-log-list">
        <div v-for="ex in planExercises" :key="ex.exerciseId" class="exercise-log-item">
          <div class="ex-name">{{ ex.exerciseName }}</div>
          <div class="ex-inputs">
            <el-input-number v-model="logs[ex.exerciseId].sets" :min="0" :max="20" size="small" controls-position="right" />
            <span class="muted">sets ×</span>
            <el-input-number v-model="logs[ex.exerciseId].reps" :min="0" :max="100" size="small" controls-position="right" />
            <span class="muted">reps</span>
            <el-checkbox v-model="logs[ex.exerciseId].done">Xong</el-checkbox>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="completeDialog = false">Hủy</el-button>
        <el-button type="primary" @click="submitComplete">LƯU KẾT QUẢ</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { Bar } from 'vue-chartjs'
import { Chart as ChartJS, CategoryScale, LinearScale, BarElement, Tooltip } from 'chart.js'
import { dashboardAPI, sessionAPI } from '@/api'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

ChartJS.register(CategoryScale, LinearScale, BarElement, Tooltip)

const data         = ref({})
const weekSessions = ref([])
const loading      = ref(true)
const completeDialog = ref(false)
const currentSession = ref(null)
const planExercises  = ref([])
const logs = reactive({})

const today = dayjs().format('dddd, DD/MM/YYYY')

async function load() {
  loading.value = true
  try {
    const [dash, week] = await Promise.all([dashboardAPI.get(), sessionAPI.getWeek()])
    data.value         = dash.data || {}
    weekSessions.value = week.data || []
  } catch {} finally {
    loading.value = false
  }
}

async function checkIn(id) {
  await sessionAPI.checkIn(id)
  ElMessage.success('Check-in thành công! Cố lên! 💪')
  load()
}

function openComplete(session) {
  currentSession.value = session
  planExercises.value  = session.exerciseLogs || []
  // pre-fill logs
  planExercises.value.forEach(ex => {
    logs[ex.exerciseId] = { sets: ex.setsCompleted || 3, reps: ex.repsCompleted || 10, done: true }
  })
  completeDialog.value = true
}

async function submitComplete() {
  const exerciseLogs = Object.entries(logs).map(([id, v]) => ({
    exerciseId: Number(id), setsCompleted: v.sets, repsCompleted: v.reps, isCompleted: v.done
  }))
  await sessionAPI.complete(currentSession.value.id, { sessionId: currentSession.value.id, exerciseLogs })
  ElMessage.success('Buổi tập hoàn thành! Tuyệt vời! 🎉')
  completeDialog.value = false
  load()
}

// Charts
const caloriesChartData = computed(() => {
  const wc = data.value.weeklyCalories
  if (!wc) return null
  return {
    labels: Object.keys(wc),
    datasets: [{ data: Object.values(wc), backgroundColor: '#e8ff00', borderRadius: 4 }]
  }
})

const workoutsChartData = computed(() => {
  const ww = data.value.weeklyWorkouts
  if (!ww) return null
  return {
    labels: Object.keys(ww),
    datasets: [{ data: Object.values(ww), backgroundColor: '#ff4d00', borderRadius: 4 }]
  }
})

const barOptions = {
  responsive: true, maintainAspectRatio: false,
  plugins: { legend: { display: false } },
  scales: {
    x: { grid: { color: '#2a2a2a' }, ticks: { color: '#888' } },
    y: { grid: { color: '#2a2a2a' }, ticks: { color: '#888' } }
  }
}

const weightChangeClass = computed(() => {
  const c = data.value.weightChange
  if (!c) return 'muted'
  return c < 0 ? 'accent' : 'muted'
})

const weightChangeText = computed(() => {
  const c = data.value.weightChange
  if (!c) return 'kg'
  return `${c > 0 ? '+' : ''}${c} kg so với ban đầu`
})

function formatDate(d) { return dayjs(d).format('DD/MM') }
function statusLabel(s) {
  return { SCHEDULED: 'Chờ', CHECKED_IN: 'Đang tập', COMPLETED: 'Xong', SKIPPED: 'Bỏ qua' }[s] || s
}
function statusBadge(s) {
  return { SCHEDULED: 'badge-info', CHECKED_IN: 'badge-warning', COMPLETED: 'badge-success', SKIPPED: 'badge-danger' }[s] || ''
}

onMounted(load)
</script>

<style scoped>
.loading-state { padding: 40px 0; }
.empty-state { text-align:center; padding:32px; color:var(--c-text2); }

.session-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 12px;
}
.session-card {
  background: var(--c-bg3); border: 1px solid var(--c-border);
  border-radius: var(--radius-lg); padding: 14px;
  transition: border-color var(--transition);
}
.session-card.completed { border-color: var(--c-success); }
.session-card.checked_in { border-color: var(--c-warning); }
.session-day { font-family:var(--font-display); font-size:1.1rem; color:var(--c-accent); }
.session-date { font-size:0.78rem; color:var(--c-text3); margin-bottom:4px; }
.session-plan { font-size:0.8rem; color:var(--c-text2); margin-bottom:8px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.session-actions { margin-top:8px; }

.exercise-log-list { display:flex; flex-direction:column; gap:12px; }
.exercise-log-item { padding:10px; background:var(--c-bg3); border-radius:var(--radius); }
.ex-name { font-weight:600; margin-bottom:8px; }
.ex-inputs { display:flex; align-items:center; gap:8px; flex-wrap:wrap; }
</style>
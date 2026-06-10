<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>DASHBOARD</h2>
      <span class="mono" style="font-size:0.8rem;color:var(--c-text-inv2)">{{ today }}</span>
    </div>

    <div v-if="loading">
      <el-skeleton :rows="6" animated style="background:var(--c-card);padding:24px;border-radius:12px"/>
    </div>

    <template v-else>
      <!-- Stats -->
      <div class="grid-4" style="margin-bottom:24px">
        <div class="stat-card accent-card" style="cursor:pointer" @click="$router.push('/app/sessions')">
          <div class="label">BUỔI HOÀN THÀNH</div>
          <div class="value">{{ data.completedSessions || 0 }}</div>
          <div class="sub">/ {{ data.totalSessions || 0 }} đã đăng ký</div>
          <div class="icon">💪</div>
        </div>
        <div class="stat-card" style="cursor:pointer" @click="$router.push('/app/sessions')">
          <div class="label">CALORIES ĐÃ ĐỐT</div>
          <div class="value">{{ formatNum(data.totalCaloriesBurned) }}</div>
          <div class="sub">kcal tổng cộng</div>
          <div class="icon">🔥</div>
        </div>
        <div class="stat-card">
          <div class="label">STREAK</div>
          <div class="value">{{ data.currentStreak || 0 }}</div>
          <div class="sub">ngày · kỷ lục: {{ data.longestStreak || 0 }}</div>
          <div class="icon">⚡</div>
        </div>
        <div class="stat-card" style="cursor:pointer" @click="$router.push('/app/progress')">
          <div class="label">CÂN NẶNG</div>
          <div class="value">{{ data.currentWeight || '--' }}</div>
          <div class="sub" :style="{color:weightColor}">{{ weightText }}</div>
          <div class="icon">⚖️</div>
        </div>
      </div>

      <!-- Charts -->
      <div class="grid-2" style="margin-bottom:24px">
        <el-card>
          <template #header>CALORIES TUẦN NÀY (kcal)</template>
          <div style="height:200px;position:relative">
            <canvas ref="calChart"></canvas>
            <div v-if="noCalories" class="chart-empty">Chưa có dữ liệu</div>
          </div>
        </el-card>
        <el-card>
          <template #header>BUỔI TẬP THEO TUẦN</template>
          <div style="height:200px;position:relative">
            <canvas ref="wkChart"></canvas>
            <div v-if="noWorkouts" class="chart-empty">Chưa có dữ liệu</div>
          </div>
        </el-card>
      </div>

      <!-- This week sessions -->
      <el-card>
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <span>LỊCH TẬP TUẦN NÀY</span>
            <div style="display:flex;gap:8px">
              <el-button size="small" plain @click="$router.push('/app/plan')">📋 Đăng ký buổi</el-button>
              <el-button type="primary" size="small" @click="$router.push('/app/sessions')">Tất cả →</el-button>
            </div>
          </div>
        </template>

        <div v-if="!weekSessions.length" class="empty-state" style="padding:24px">
          Chưa có lịch tập tuần này.
          <el-button text type="primary" @click="$router.push('/app/plan')" style="padding:0 4px">
            Đăng ký buổi tập →
          </el-button>
        </div>

        <div v-else class="session-grid">
          <div v-for="s in weekSessions" :key="s.id"
               class="session-card" :class="s.status.toLowerCase()"
               @click="$router.push('/app/sessions')">
            <div class="session-day">{{ s.dayName || s.customSessionName || 'Tự do' }}</div>
            <div class="session-date">{{ fmtDate(s.sessionDate) }}</div>
            <div v-if="s.scheduledTime" class="session-time">🕐 {{ s.scheduledTime.substring(0,5) }}</div>
            <div v-if="s.isLastSessionOfWeek" class="session-last">⭐ Cuối tuần</div>
            <div style="margin-top:6px">
              <span class="badge" :class="statusBadge(s.status)">{{ statusLabel(s.status) }}</span>
            </div>
            <div v-if="s.completionRate != null" style="font-size:0.75rem;margin-top:4px"
                 :style="{color: s.completionRate>=90?'var(--c-success)':s.completionRate<50?'var(--c-danger)':'var(--c-warning)'}">
              {{ s.completionRate }}% hoàn thành
            </div>

            <!-- Actions -->
            <div class="session-actions" @click.stop style="margin-top:8px">
              <el-button v-if="s.status==='SCHEDULED'"  type="primary" size="small" @click.stop="checkIn(s.id)">
                Check-in
              </el-button>
              <el-button v-if="s.status==='CHECKED_IN'" type="success" size="small" @click.stop="openCheckOut(s)">
                Check-out
              </el-button>
            </div>
          </div>
        </div>
      </el-card>
    </template>

    <!-- ── Check-out Dialog ──────────────────────────────────── -->
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

        <!-- Cuối tuần → nhập cân nặng -->
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
import { ref, computed, reactive, onMounted, nextTick } from 'vue'
import { Chart, registerables } from 'chart.js'
import { dashboardAPI, sessionAPI } from '@/api'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'

Chart.register(...registerables)

const router       = useRouter()
const data         = ref({})
const weekSessions = ref([])
const loading      = ref(true)
const calChart     = ref(null)
const wkChart      = ref(null)
let   calInst = null, wkInst = null

const checkOutDialog = ref(false)
const coSession      = ref(null)
const checkingOut    = ref(false)
const coForm = reactive({ completionRate:80, notes:'', checkoutWeight:null })

const today = dayjs().format('dddd, DD/MM/YYYY')

const weightColor = computed(() => {
  const c = data.value.weightChange
  if (!c) return 'var(--c-text3)'
  return c < 0 ? 'var(--c-success)' : 'var(--c-warning)'
})
const weightText = computed(() => {
  const c = data.value.weightChange
  if (c == null) return 'kg'
  return `${c > 0 ? '+' : ''}${c} kg từ ban đầu`
})
const noCalories = computed(() => !Object.values(data.value.weeklyCalories || {}).some(v => v > 0))
const noWorkouts = computed(() => !Object.values(data.value.weeklyWorkouts || {}).some(v => v > 0))

const rateClass = computed(() => {
  const r = coForm.completionRate
  if (r >= 90) return 'rate-high'
  if (r >= 60) return 'rate-mid'
  return 'rate-low'
})
const rateHint = computed(() => {
  const r = coForm.completionRate
  if (r >= 90) return '🔥 Xuất sắc! Hệ thống sẽ tăng độ khó tuần sau.'
  if (r >= 70) return '✅ Tốt! Giữ nguyên cường độ.'
  if (r >= 50) return '💪 Vừa đủ. Cố gắng hơn nhé!'
  return '⚠️ Hệ thống sẽ điều chỉnh nhẹ hơn cho tuần sau.'
})

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
  const GRID = 'rgba(196,154,108,0.3)', TICK = '#4A3728'
  const opts = (label) => ({
    responsive:true, maintainAspectRatio:false,
    plugins:{ legend:{ display:false } },
    scales:{
      x:{ grid:{color:GRID}, ticks:{color:TICK,font:{size:11}} },
      y:{ grid:{color:GRID}, ticks:{color:TICK,font:{size:11}}, beginAtZero:true,
        title:{display:!!label,text:label,color:TICK} }
    }
  })

  if (calChart.value && data.value.weeklyCalories) {
    if (calInst) calInst.destroy()
    calInst = new Chart(calChart.value, {
      type:'bar',
      data:{ labels:Object.keys(data.value.weeklyCalories),
        datasets:[{ data:Object.values(data.value.weeklyCalories), backgroundColor:'#D4892A', borderRadius:6, borderSkipped:false }] },
      options: opts('kcal')
    })
  }
  if (wkChart.value && data.value.weeklyWorkouts) {
    if (wkInst) wkInst.destroy()
    wkInst = new Chart(wkChart.value, {
      type:'bar',
      data:{ labels:Object.keys(data.value.weeklyWorkouts),
        datasets:[{ data:Object.values(data.value.weeklyWorkouts), backgroundColor:'#6B4226', borderRadius:6, borderSkipped:false }] },
      options: opts('buổi')
    })
  }
}

async function checkIn(id) {
  await sessionAPI.checkIn(id); ElMessage.success('Check-in! 💪'); load()
}

function openCheckOut(s) {
  coSession.value = s
  coForm.completionRate  = 80
  coForm.notes           = ''
  coForm.checkoutWeight  = null
  checkOutDialog.value   = true
}

async function submitCheckOut() {
  if (coForm.completionRate == null) { ElMessage.warning('Nhập tỉ lệ hoàn thành'); return }
  if (coSession.value?.isLastSessionOfWeek && !coForm.checkoutWeight) {
    ElMessage.warning('Đây là buổi cuối tuần! Vui lòng nhập cân nặng.'); return
  }
  checkingOut.value = true
  try {
    await sessionAPI.checkOut(coSession.value.id, {
      completionRate:  coForm.completionRate,
      notes:           coForm.notes,
      checkoutWeight:  coForm.checkoutWeight || null,
      exerciseLogs:    []
    })
    ElMessage.success('Check-out thành công! 🎉')
    checkOutDialog.value = false
    load()
  } catch {} finally { checkingOut.value = false }
}

function fmtDate(d)     { return dayjs(d).format('ddd DD/MM') }
function formatNum(n)   { return n ? Number(n).toLocaleString() : '0' }
function statusLabel(s) { return { SCHEDULED:'Chờ', CHECKED_IN:'Đang tập', COMPLETED:'Xong', SKIPPED:'Bỏ' }[s]||s }
function statusBadge(s) { return { SCHEDULED:'badge-info', CHECKED_IN:'badge-warning', COMPLETED:'badge-success', SKIPPED:'badge-danger' }[s]||'' }

onMounted(load)
</script>

<style scoped>
.session-grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(155px,1fr)); gap:12px; }
.session-card {
  background:var(--c-card); border:1px solid var(--c-border2);
  border-radius:var(--radius-lg); padding:14px; cursor:pointer;
  transition:all var(--transition); display:flex; flex-direction:column; gap:3px;
}
.session-card:hover      { border-color:var(--c-accent); box-shadow:var(--shadow); }
.session-card.completed  { border-left:3px solid var(--c-success); }
.session-card.checked_in { border-left:3px solid var(--c-warning); }
.session-day   { font-family:var(--font-display); font-size:1rem; color:var(--c-accent); }
.session-date  { font-size:0.75rem; color:var(--c-text3); }
.session-time  { font-size:0.75rem; color:var(--c-info); font-weight:600; }
.session-last  { font-size:0.7rem; color:var(--c-warning); font-weight:600; }

.chart-empty { position:absolute; inset:0; display:flex; align-items:center; justify-content:center; color:var(--c-text3); font-size:0.85rem; }

.rate-badge { min-width:52px; text-align:center; padding:4px 10px; border-radius:20px; font-weight:700; font-size:0.9rem; }
.rate-high  { background:#E8F5E9; color:#2E7D32; }
.rate-mid   { background:#FFF3E0; color:#E65100; }
.rate-low   { background:#FFEBEE; color:#C62828; }
.rate-hint  { font-size:0.78rem; color:var(--c-text3); margin-top:6px; }
.info-box   { padding:10px 14px; background:#FFF8F0; border:1px solid var(--c-border); border-radius:var(--radius-lg); font-size:0.8rem; color:var(--c-text2); }
</style>
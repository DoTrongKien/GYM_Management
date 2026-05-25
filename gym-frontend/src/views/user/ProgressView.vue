<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>THEO DÕI TIẾN ĐỘ</h2>
      <el-button type="primary" @click="addDialog=true">+ GHI NHẬN</el-button>
    </div>

    <!-- Stats row -->
    <div class="grid-4" style="margin-bottom:24px" v-if="progressList.length">
      <div class="stat-card accent-card">
        <div class="label">CÂN NẶNG HIỆN TẠI</div>
        <div class="value">{{ latest?.weight || '--' }}</div>
        <div class="sub">kg</div>
        <div class="icon">⚖️</div>
      </div>
      <div class="stat-card">
        <div class="label">BMI</div>
        <div class="value">{{ latest?.bmi || '--' }}</div>
        <div class="sub" :style="{color: bmiColor}">{{ bmiCat(latest?.bmi) }}</div>
        <div class="icon">📊</div>
      </div>
      <div class="stat-card">
        <div class="label">% MỠ CƠ THỂ</div>
        <div class="value">{{ latest?.bodyFatPercentage || '--' }}</div>
        <div class="sub">%</div>
        <div class="icon">🔬</div>
      </div>
      <div class="stat-card" v-if="weightChange !== null">
        <div class="label">THAY ĐỔI</div>
        <div class="value" :style="{color: weightChange<0?'var(--c-success)':'var(--c-danger)'}">
          {{ weightChange > 0 ? '+' : '' }}{{ weightChange }}
        </div>
        <div class="sub">kg từ ban đầu</div>
        <div class="icon">{{ weightChange < 0 ? '📉' : '📈' }}</div>
      </div>
    </div>

    <!-- Weight chart -->
    <el-card style="margin-bottom:24px" v-if="progressList.length > 1">
      <template #header>BIỂU ĐỒ CÂN NẶNG</template>
      <div style="height:240px;position:relative">
        <canvas ref="chartRef"></canvas>
      </div>
    </el-card>

    <!-- Measurements -->
    <el-card style="margin-bottom:24px" v-if="latest && hasBodyMeasure">
      <template #header>SỐ ĐO CƠ THỂ (cm)</template>
      <div class="measurements">
        <div class="meas-item" v-for="m in measurements" :key="m.key">
          <div class="meas-icon">{{ m.icon }}</div>
          <div class="meas-label">{{ m.label }}</div>
          <div class="meas-val">{{ latest?.[m.key] || '--' }} cm</div>
        </div>
      </div>
    </el-card>

    <!-- History table -->
    <el-card>
      <template #header>LỊCH SỬ GHI NHẬN</template>
      <el-table :data="progressList.slice().reverse()" stripe>
        <el-table-column label="Ngày" prop="recordedDate" width="110"/>
        <el-table-column label="Cân nặng (kg)" prop="weight" width="130" align="center"/>
        <el-table-column label="BMI" prop="bmi" width="80" align="center"/>
        <el-table-column label="% Mỡ" width="90" align="center">
          <template #default="{row}">{{ row.bodyFatPercentage || '--' }}</template>
        </el-table-column>
        <el-table-column label="Eo (cm)" width="90" align="center">
          <template #default="{row}">{{ row.waistCm || '--' }}</template>
        </el-table-column>
        <el-table-column label="Thay đổi" width="110" align="center">
          <template #default="{row}">
            <span v-if="row.weightChange != null"
                  :style="{color: row.weightChange<0?'var(--c-success)':'var(--c-danger)', fontWeight:700}">
              {{ row.weightChange > 0 ? '+' : '' }}{{ row.weightChange }} kg
            </span>
            <span v-else class="muted">--</span>
          </template>
        </el-table-column>
        <el-table-column label="Ghi chú" min-width="160">
          <template #default="{row}">{{ row.notes || '--' }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Add dialog -->
    <el-dialog v-model="addDialog" title="GHI NHẬN TIẾN ĐỘ" width="480px" align-center>
      <el-form :model="form" label-position="top">
        <div class="grid-2">
          <el-form-item label="Cân nặng (kg)">
            <el-input-number v-model="form.weight" :min="30" :max="300" :precision="1" style="width:100%"/>
          </el-form-item>
          <el-form-item label="% Mỡ cơ thể">
            <el-input-number v-model="form.bodyFatPercentage" :min="0" :max="60" :precision="1" style="width:100%"/>
          </el-form-item>
        </div>
        <div class="grid-2">
          <el-form-item label="Vòng eo (cm)">
            <el-input-number v-model="form.waistCm" :min="0" :max="200" style="width:100%"/>
          </el-form-item>
          <el-form-item label="Vòng ngực (cm)">
            <el-input-number v-model="form.chestCm" :min="0" :max="200" style="width:100%"/>
          </el-form-item>
        </div>
        <div class="grid-2">
          <el-form-item label="Vòng hông (cm)">
            <el-input-number v-model="form.hipCm" :min="0" :max="200" style="width:100%"/>
          </el-form-item>
          <el-form-item label="Vòng cánh tay (cm)">
            <el-input-number v-model="form.armCm" :min="0" :max="100" style="width:100%"/>
          </el-form-item>
        </div>
        <el-form-item label="Ngày ghi nhận">
          <el-date-picker v-model="form.recordedDate" type="date"
                          format="DD/MM/YYYY" value-format="YYYY-MM-DD" style="width:100%"/>
        </el-form-item>
        <el-form-item label="Ghi chú">
          <el-input v-model="form.notes" type="textarea" :rows="2" placeholder="Cảm nhận, điều kiện đo..."/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialog=false">Hủy</el-button>
        <el-button type="primary" @click="addProgress">LƯU TIẾN ĐỘ</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, nextTick } from 'vue'
import { Chart, registerables } from 'chart.js'
import { progressAPI } from '@/api'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

Chart.register(...registerables)

const progressList = ref([])
const addDialog    = ref(false)
const chartRef     = ref(null)
let   chartInst    = null

const form = reactive({
  weight:null, bodyFatPercentage:null, waistCm:null, chestCm:null,
  hipCm:null, armCm:null, recordedDate:dayjs().format('YYYY-MM-DD'), notes:''
})

const latest      = computed(() => progressList.value.at(-1))
const firstRecord = computed(() => progressList.value[0])
const weightChange = computed(() => {
  if (!latest.value?.weight || !firstRecord.value?.weight) return null
  return Math.round((latest.value.weight - firstRecord.value.weight) * 10) / 10
})
const hasBodyMeasure = computed(() =>
    latest.value?.waistCm || latest.value?.chestCm || latest.value?.hipCm || latest.value?.armCm
)

const bmiColor = computed(() => {
  const b = latest.value?.bmi
  if (!b) return 'var(--c-text3)'
  if (b < 18.5) return 'var(--c-info)'
  if (b < 25)   return 'var(--c-success)'
  if (b < 30)   return 'var(--c-warning)'
  return 'var(--c-danger)'
})

const measurements = [
  { key:'chestCm', label:'Ngực', icon:'💪' },
  { key:'waistCm', label:'Eo',   icon:'📏' },
  { key:'hipCm',   label:'Hông', icon:'🎯' },
  { key:'armCm',   label:'Tay',  icon:'🦾' },
  { key:'thighCm', label:'Đùi',  icon:'🦵' },
]

async function load() {
  try {
    const r = await progressAPI.getAll()
    // sort asc by date
    progressList.value = (r.data || []).sort((a,b) => a.recordedDate.localeCompare(b.recordedDate))
  } catch {}
  nextTick(drawChart)
}

function drawChart() {
  if (!chartRef.value || progressList.value.length < 2) return
  if (chartInst) chartInst.destroy()
  chartInst = new Chart(chartRef.value, {
    type: 'line',
    data: {
      labels: progressList.value.map(p => dayjs(p.recordedDate).format('DD/MM')),
      datasets: [{
        label: 'Cân nặng (kg)',
        data: progressList.value.map(p => p.weight),
        borderColor: '#D4892A',
        backgroundColor: 'rgba(212,137,42,0.1)',
        tension: 0.4,
        pointBackgroundColor: '#D4892A',
        pointRadius: 5,
        fill: true
      }]
    },
    options: {
      responsive: true, maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: {
        x: { grid: { color:'rgba(196,154,108,0.3)' }, ticks: { color:'#4A3728', font:{size:11} } },
        y: { grid: { color:'rgba(196,154,108,0.3)' }, ticks: { color:'#4A3728', font:{size:11} }, beginAtZero:false }
      }
    }
  })
}

async function addProgress() {
  if (!form.weight) { ElMessage.warning('Nhập cân nặng'); return }
  await progressAPI.add(form)
  ElMessage.success('Đã ghi nhận tiến độ! 📈')
  addDialog.value = false
  Object.assign(form, { weight:null, bodyFatPercentage:null, waistCm:null, chestCm:null, hipCm:null, armCm:null, notes:'' })
  load()
}

function bmiCat(b) {
  if (!b) return ''
  if (b < 18.5) return 'Thiếu cân'
  if (b < 25)   return 'Bình thường'
  if (b < 30)   return 'Thừa cân'
  return 'Béo phì'
}

onMounted(load)
</script>

<style scoped>
.measurements { display:grid; grid-template-columns:repeat(auto-fill,minmax(110px,1fr)); gap:12px; }
.meas-item {
  text-align:center; padding:14px; background:var(--c-card2);
  border:1px solid var(--c-border2); border-radius:var(--radius-lg);
}
.meas-icon  { font-size:1.5rem; margin-bottom:4px; }
.meas-label { font-size:0.72rem; color:var(--c-text3); text-transform:uppercase; letter-spacing:0.08em; margin-bottom:4px; }
.meas-val   { font-family:var(--font-display); font-size:1.1rem; color:var(--c-text); }
</style>
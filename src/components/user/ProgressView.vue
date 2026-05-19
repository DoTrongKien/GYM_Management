<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>TIẾN ĐỘ</h2>
      <el-button type="primary" @click="addDialog = true">+ GHI NHẬN</el-button>
    </div>

    <!-- Stats -->
    <div class="grid-4" style="margin-bottom:24px">
      <div class="stat-card" v-if="latest">
        <div class="label">CÂN NẶNG HIỆN TẠI</div>
        <div class="value accent">{{ latest.weight || '--' }}</div>
        <div class="sub">kg</div>
      </div>
      <div class="stat-card" v-if="latest">
        <div class="label">BMI</div>
        <div class="value">{{ latest.bmi || '--' }}</div>
        <div class="sub">{{ bmiCat(latest.bmi) }}</div>
      </div>
      <div class="stat-card" v-if="latest">
        <div class="label">% MỠ CƠ THỂ</div>
        <div class="value">{{ latest.bodyFatPercentage || '--' }}</div>
        <div class="sub">%</div>
      </div>
      <div class="stat-card" v-if="firstRecord && latest">
        <div class="label">THAY ĐỔI</div>
        <div class="value" :style="{color: weightChange < 0 ? 'var(--c-success)' : 'var(--c-danger)'}">
          {{ weightChange > 0 ? '+' : '' }}{{ weightChange }}
        </div>
        <div class="sub">kg từ ban đầu</div>
      </div>
    </div>

    <!-- Chart -->
    <el-card header="BIỂU ĐỒ CÂN NẶNG" style="margin-bottom:24px" v-if="progressList.length > 1">
      <div style="height:220px">
        <Line :data="chartData" :options="chartOptions" />
      </div>
    </el-card>

    <!-- History table -->
    <el-card header="LỊCH SỬ">
      <el-table :data="progressList" stripe>
        <el-table-column label="Ngày" width="120">
          <template #default="{row}">{{ row.recordedDate }}</template>
        </el-table-column>
        <el-table-column label="Cân nặng (kg)" prop="weight" width="130" align="center" />
        <el-table-column label="BMI" prop="bmi" width="90" align="center" />
        <el-table-column label="% Mỡ" prop="bodyFatPercentage" width="90" align="center">
          <template #default="{row}">{{ row.bodyFatPercentage || '--' }}</template>
        </el-table-column>
        <el-table-column label="Eo (cm)" prop="waistCm" width="100" align="center">
          <template #default="{row}">{{ row.waistCm || '--' }}</template>
        </el-table-column>
        <el-table-column label="Thay đổi" width="100" align="center">
          <template #default="{row}">
            <span v-if="row.weightChange != null" :style="{color: row.weightChange < 0 ? 'var(--c-success)' : 'var(--c-danger)'}">
              {{ row.weightChange > 0 ? '+' : '' }}{{ row.weightChange }} kg
            </span>
            <span v-else class="muted">--</span>
          </template>
        </el-table-column>
        <el-table-column label="Ghi chú" prop="notes" min-width="160">
          <template #default="{row}">{{ row.notes || '--' }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Add Dialog -->
    <el-dialog v-model="addDialog" title="GHI NHẬN TIẾN ĐỘ" width="460px">
      <el-form :model="form" label-position="top">
        <div class="grid-2">
          <el-form-item label="Cân nặng (kg)">
            <el-input-number v-model="form.weight" :min="30" :max="300" :precision="1" style="width:100%" />
          </el-form-item>
          <el-form-item label="% Mỡ cơ thể">
            <el-input-number v-model="form.bodyFatPercentage" :min="0" :max="60" :precision="1" style="width:100%" />
          </el-form-item>
        </div>
        <div class="grid-2">
          <el-form-item label="Vòng eo (cm)">
            <el-input-number v-model="form.waistCm" :min="0" :max="200" style="width:100%" />
          </el-form-item>
          <el-form-item label="Vòng ngực (cm)">
            <el-input-number v-model="form.chestCm" :min="0" :max="200" style="width:100%" />
          </el-form-item>
        </div>
        <el-form-item label="Ngày ghi nhận">
          <el-date-picker v-model="form.recordedDate" type="date" format="YYYY-MM-DD" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="Ghi chú">
          <el-input v-model="form.notes" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialog = false">Hủy</el-button>
        <el-button type="primary" @click="addProgress">LƯU</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { Line } from 'vue-chartjs'
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Tooltip } from 'chart.js'
import { progressAPI } from '@/api'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip)

const progressList = ref([])
const addDialog    = ref(false)
const form = reactive({ weight: null, bodyFatPercentage: null, waistCm: null, chestCm: null, recordedDate: dayjs().format('YYYY-MM-DD'), notes: '' })

const latest      = computed(() => progressList.value.at(-1))
const firstRecord = computed(() => progressList.value[0])
const weightChange = computed(() => {
  if (!latest.value?.weight || !firstRecord.value?.weight) return 0
  return Math.round((latest.value.weight - firstRecord.value.weight) * 10) / 10
})

const chartData = computed(() => ({
  labels: progressList.value.map(p => p.recordedDate),
  datasets: [{
    label: 'Cân nặng (kg)', data: progressList.value.map(p => p.weight),
    borderColor: '#e8ff00', backgroundColor: 'rgba(232,255,0,0.1)',
    tension: 0.4, pointBackgroundColor: '#e8ff00'
  }]
}))

const chartOptions = {
  responsive: true, maintainAspectRatio: false,
  plugins: { legend: { display: false } },
  scales: {
    x: { grid: { color: '#2a2a2a' }, ticks: { color: '#888' } },
    y: { grid: { color: '#2a2a2a' }, ticks: { color: '#888' } }
  }
}

async function load() {
  try { const r = await progressAPI.getAll(); progressList.value = r.data || [] } catch {}
}

async function addProgress() {
  await progressAPI.add(form)
  ElMessage.success('Đã ghi nhận tiến độ!')
  addDialog.value = false; load()
}

function bmiCat(bmi) {
  if (!bmi) return ''
  if (bmi < 18.5) return 'Thiếu cân'
  if (bmi < 25) return 'Bình thường'
  if (bmi < 30) return 'Thừa cân'
  return 'Béo phì'
}

onMounted(load)
</script>
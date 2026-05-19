<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>HỒ SƠ CÁ NHÂN</h2>
      <el-button type="primary" @click="save" :loading="saving">LƯU HỒ SƠ</el-button>
    </div>

    <div class="grid-2" style="gap:24px">
      <!-- Form -->
      <el-card header="THÔNG TIN THỂ TRẠNG">
        <el-form :model="form" label-position="top" label-width="auto">
          <div class="grid-2">
            <el-form-item label="Chiều cao (cm)">
              <el-input-number v-model="form.height" :min="100" :max="250" style="width:100%" />
            </el-form-item>
            <el-form-item label="Cân nặng (kg)">
              <el-input-number v-model="form.weight" :min="30" :max="300" style="width:100%" />
            </el-form-item>
          </div>
          <div class="grid-2">
            <el-form-item label="Tuổi">
              <el-input-number v-model="form.age" :min="10" :max="100" style="width:100%" />
            </el-form-item>
            <el-form-item label="Giới tính">
              <el-select v-model="form.gender" style="width:100%">
                <el-option label="Nam" value="male" />
                <el-option label="Nữ" value="female" />
                <el-option label="Khác" value="other" />
              </el-select>
            </el-form-item>
          </div>
          <el-form-item label="Mục tiêu">
            <el-select v-model="form.goal" style="width:100%">
              <el-option label="🔥 Giảm cân" value="WEIGHT_LOSS" />
              <el-option label="💪 Tăng cơ" value="MUSCLE_GAIN" />
              <el-option label="🏃 Tăng sức bền" value="ENDURANCE" />
              <el-option label="🤸 Tăng linh hoạt" value="FLEXIBILITY" />
              <el-option label="⚖️ Duy trì" value="MAINTENANCE" />
            </el-select>
          </el-form-item>
          <el-form-item label="Trình độ">
            <el-select v-model="form.fitnessLevel" style="width:100%">
              <el-option label="🌱 Mới bắt đầu" value="BEGINNER" />
              <el-option label="🔄 Trung bình" value="INTERMEDIATE" />
              <el-option label="⚡ Nâng cao" value="ADVANCED" />
            </el-select>
          </el-form-item>
          <div class="grid-2">
            <el-form-item label="Số ngày rảnh/tuần">
              <el-input-number v-model="form.availableDaysPerWeek" :min="1" :max="7" style="width:100%" />
            </el-form-item>
            <el-form-item label="Thời gian/buổi (phút)">
              <el-input-number v-model="form.preferredSessionDuration" :min="20" :max="180" style="width:100%" />
            </el-form-item>
          </div>
          <el-form-item label="Bệnh lý / Chấn thương (nếu có)">
            <el-input v-model="form.medicalConditions" type="textarea" :rows="2" placeholder="Ví dụ: đau lưng, chấn thương gối..." />
          </el-form-item>
        </el-form>
      </el-card>

      <!-- Stats panel -->
      <div>
        <el-card header="CHỈ SỐ CƠ THỂ" style="margin-bottom:16px">
          <div v-if="profile" class="bmi-display">
            <div class="bmi-number">{{ profile.bmi || '--' }}</div>
            <div class="bmi-label">BMI</div>
            <div class="bmi-category" :class="bmiClass(profile.bmiCategory)">
              {{ profile.bmiCategory || 'Chưa tính' }}
            </div>
          </div>
          <el-descriptions :column="1" size="small" style="margin-top:16px" v-if="profile">
            <el-descriptions-item label="Chiều cao">{{ profile.height || '--' }} cm</el-descriptions-item>
            <el-descriptions-item label="Cân nặng">{{ profile.weight || '--' }} kg</el-descriptions-item>
            <el-descriptions-item label="Mục tiêu">{{ goalLabel(profile.goal) }}</el-descriptions-item>
            <el-descriptions-item label="Trình độ">{{ levelLabel(profile.fitnessLevel) }}</el-descriptions-item>
          </el-descriptions>
          <div v-else class="muted" style="text-align:center;padding:20px">Chưa có hồ sơ</div>
        </el-card>

        <el-card header="GỢI Ý">
          <div class="tips">
            <div class="tip-item" v-for="tip in tips" :key="tip">
              <span class="accent">▸</span> {{ tip }}
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { profileAPI } from '@/api'
import { ElMessage } from 'element-plus'

const profile = ref(null)
const saving  = ref(false)
const form    = reactive({
  height: 170, weight: 65, age: 25, gender: 'male',
  goal: 'WEIGHT_LOSS', fitnessLevel: 'BEGINNER',
  availableDaysPerWeek: 3, preferredSessionDuration: 60, medicalConditions: ''
})

async function load() {
  try {
    const res = await profileAPI.get()
    profile.value = res.data
    Object.assign(form, res.data)
  } catch {}
}

async function save() {
  saving.value = true
  try {
    const res = await profileAPI.save(form)
    profile.value = res.data
    ElMessage.success('Hồ sơ đã được lưu!')
  } finally {
    saving.value = false }
}

function bmiClass(cat) {
  if (!cat) return ''
  if (cat === 'Normal') return 'bmi-normal'
  if (cat === 'Underweight') return 'bmi-under'
  return 'bmi-over'
}
function goalLabel(g) {
  return { WEIGHT_LOSS:'Giảm cân', MUSCLE_GAIN:'Tăng cơ', ENDURANCE:'Sức bền', FLEXIBILITY:'Linh hoạt', MAINTENANCE:'Duy trì' }[g] || g
}
function levelLabel(l) {
  return { BEGINNER:'Mới bắt đầu', INTERMEDIATE:'Trung bình', ADVANCED:'Nâng cao' }[l] || l
}

const tips = computed(() => {
  const t = []
  if (form.goal === 'WEIGHT_LOSS') t.push('Tập cardio ít nhất 3 buổi/tuần', 'Duy trì thâm hụt 300-500 kcal/ngày', 'Uống đủ 2-3 lít nước/ngày')
  if (form.goal === 'MUSCLE_GAIN') t.push('Ưu tiên bài tập compound (squat, deadlift)', 'Ăn đủ protein 1.6-2.2g/kg cân nặng', 'Ngủ đủ 7-9 tiếng/đêm')
  if (!form.goal) t.push('Hãy chọn mục tiêu để nhận gợi ý phù hợp')
  return t.length ? t : ['Hoàn thiện hồ sơ để nhận giáo án AI phù hợp nhất']
})

onMounted(load)
</script>

<style scoped>
.bmi-display { text-align:center; padding:16px 0; }
.bmi-number { font-family:var(--font-display); font-size:3.5rem; line-height:1; color:var(--c-text); }
.bmi-label  { font-size:0.75rem; text-transform:uppercase; letter-spacing:0.1em; color:var(--c-text3); }
.bmi-category { font-size:0.9rem; font-weight:600; margin-top:4px; }
.bmi-normal { color:var(--c-success); }
.bmi-under  { color:var(--c-info); }
.bmi-over   { color:var(--c-danger); }

.tips { display:flex; flex-direction:column; gap:8px; }
.tip-item { font-size:0.85rem; color:var(--c-text2); }
</style>
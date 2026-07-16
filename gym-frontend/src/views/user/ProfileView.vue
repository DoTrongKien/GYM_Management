<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>HỒ SƠ CÁ NHÂN</h2>
      <el-button type="primary" @click="save" :loading="saving">💾 LƯU HỒ SƠ</el-button>
    </div>

    <div class="grid-2" style="gap:24px;align-items:start">
      <!-- Form card -->
      <el-card>
        <template #header>THÔNG TIN THỂ TRẠNG & MỤC TIÊU</template>
        <el-form :model="form" label-position="top">
          <div class="grid-2">
            <el-form-item label="Chiều cao (cm)">
              <el-input-number v-model="form.height" :min="100" :max="250" style="width:100%"/>
            </el-form-item>
            <el-form-item label="Cân nặng (kg)">
              <el-input-number v-model="form.weight" :min="30" :max="300" :precision="1" style="width:100%"/>
            </el-form-item>
          </div>
          <div class="grid-2">
            <el-form-item label="Ngày sinh">
              <el-date-picker
                v-model="form.dateOfBirth"
                type="date"
                placeholder="Chọn ngày sinh"
                format="DD/MM/YYYY"
                value-format="YYYY-MM-DD"
                style="width:100%"
              />
            </el-form-item>
            <el-form-item label="Giới tính">
              <el-select v-model="form.gender" style="width:100%">
                <el-option label="Nam" value="male"/>
                <el-option label="Nữ" value="female"/>
                <el-option label="Khác" value="other"/>
              </el-select>
            </el-form-item>
          </div>
          <el-form-item label="Mục tiêu tập luyện">
            <el-select v-model="form.goal" style="width:100%">
              <el-option label="🔥 Giảm cân / Đốt mỡ" value="WEIGHT_LOSS"/>
              <el-option label="💪 Tăng cơ / Tăng sức mạnh" value="MUSCLE_GAIN"/>
              <el-option label="🏃 Tăng sức bền" value="ENDURANCE"/>
              <el-option label="⚖️ Duy trì thể hình" value="MAINTENANCE"/>
            </el-select>
          </el-form-item>
          <el-form-item label="Trình độ hiện tại">
            <el-select v-model="form.fitnessLevel" style="width:100%">
              <el-option label="🌱 Mới bắt đầu (< 6 tháng)" value="BEGINNER"/>
              <el-option label="🔄 Trung bình (6 tháng - 2 năm)" value="INTERMEDIATE"/>
              <el-option label="⚡ Nâng cao (> 2 năm)" value="ADVANCED"/>
            </el-select>
          </el-form-item>
          <div class="grid-2">
            <el-form-item label="Số ngày rảnh / tuần">
              <el-input-number v-model="form.availableDaysPerWeek" :min="1" :max="7" style="width:100%"/>
            </el-form-item>
          </div>

        </el-form>
      </el-card>

      <!-- Right column -->
      <div style="display:flex;flex-direction:column;gap:16px">

        <!-- BMI card -->
        <el-card>
          <template #header>CHỈ SỐ CƠ THỂ</template>
          <div class="bmi-display">
            <div class="bmi-circle" :style="{borderColor: bmiColor}">
              <div class="bmi-num">{{ profile?.bmi || '--' }}</div>
              <div class="bmi-lbl">BMI</div>
            </div>
            <div class="bmi-info">
              <div class="bmi-cat" :style="{color: bmiColor}">{{ profile?.bmiCategory || 'Chưa tính' }}</div>
              <div class="bmi-ranges">
                <div :class="{active: profile?.bmiCategory==='Underweight'}">Thiếu cân: &lt; 18.5</div>
                <div :class="{active: profile?.bmiCategory==='Normal'}">Bình thường: 18.5–24.9</div>
                <div :class="{active: profile?.bmiCategory==='Overweight'}">Thừa cân: 25–29.9</div>
                <div :class="{active: profile?.bmiCategory==='Obese'}">Béo phì: ≥ 30</div>
              </div>
            </div>
          </div>

          <el-descriptions :column="1" size="small" border style="margin-top:14px" v-if="profile">
            <el-descriptions-item label="Chiều cao">{{ profile.height || '--' }} cm</el-descriptions-item>
            <el-descriptions-item label="Cân nặng">{{ profile.weight || '--' }} kg</el-descriptions-item>
            <el-descriptions-item label="Mục tiêu">{{ goalLabel(profile.goal) }}</el-descriptions-item>
            <el-descriptions-item label="Trình độ">{{ levelLabel(profile.fitnessLevel) }}</el-descriptions-item>
            <el-descriptions-item label="Lịch tập">{{ profile.availableDaysPerWeek }} ngày/tuần · {{ profile.preferredSessionDuration }} phút/buổi</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- Tips card -->
        <el-card>
          <template #header>💡 GỢI Ý CHO BẠN</template>
          <div class="tips-list">
            <div class="tip-item" v-for="tip in tips" :key="tip">
              <span style="color:var(--c-accent);font-weight:700;margin-right:6px">▸</span>{{ tip }}
            </div>
          </div>
          <div style="margin-top:16px;padding-top:14px;border-top:1px solid var(--c-border2)">
            <el-button type="primary" style="width:100%" @click="$router.push('/app/plan')">
              ✨ Tạo giáo án AI theo hồ sơ
            </el-button>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { profileAPI } from '@/api'
import { ElMessage } from 'element-plus'

const profile = ref(null)
const saving  = ref(false)

const form    = reactive({
  height: 170,
  weight: 65,
  dateOfBirth: '',
  gender: 'male',
  goal: 'WEIGHT_LOSS',
  fitnessLevel: 'BEGINNER',
  availableDaysPerWeek: 3,
  preferredSessionDuration: 60,
  medicalConditions: ''
})

const bmiColor = computed(() => {
  const cat = profile.value?.bmiCategory
  if (!cat) return 'var(--c-border)'
  if (cat === 'Normal') return 'var(--c-success)'
  if (cat === 'Underweight') return 'var(--c-info)'
  if (cat === 'Overweight') return 'var(--c-warning)'
  return 'var(--c-danger)'
})

const tips = computed(() => {
  const g = form.goal
  if (g === 'WEIGHT_LOSS') return [
    'Tập cardio ít nhất 3 buổi/tuần (30–45 phút)',
    'Duy trì thâm hụt 300–500 kcal/ngày',
    'Ưu tiên protein cao để giữ cơ khi giảm cân',
    'Uống đủ 2–3 lít nước mỗi ngày'
  ]
  if (g === 'MUSCLE_GAIN') return [
    'Ưu tiên bài tập compound: squat, deadlift, bench',
    'Ăn đủ protein 1.6–2.2g/kg cân nặng mỗi ngày',
    'Ngủ đủ 7–9 tiếng để cơ phục hồi',
    'Tăng tải trọng từ từ mỗi 1–2 tuần'
  ]
  if (g === 'ENDURANCE') return [
    'Tập cardio dài và đều: chạy bộ, đạp xe, bơi lội',
    'Tăng dần thời gian và cường độ mỗi tuần',
    'Bổ sung carbs đầy đủ để có năng lượng duy trì'
  ]
  return ['Hoàn thiện hồ sơ để nhận gợi ý phù hợp nhất từ hệ thống AI']
})

async function load() {
  try {
    const r = await profileAPI.get()
    profile.value = r.data
    // Cập nhật mapping dữ liệu
    Object.assign(form, {
      height: r.data.height,
      weight: r.data.weight,
      dateOfBirth: r.data.dateOfBirth, // Lấy dateOfBirth từ response
      gender: r.data.gender,
      goal: r.data.goal,
      fitnessLevel: r.data.fitnessLevel,
      availableDaysPerWeek: r.data.availableDaysPerWeek,
      preferredSessionDuration: r.data.preferredSessionDuration,
      medicalConditions: r.data.medicalConditions || ''
    })
  } catch {}
}


async function save() {
  saving.value = true
  try {
    const r = await profileAPI.save(form)
    profile.value = r.data
    ElMessage.success('Hồ sơ đã được lưu thành công! 🎉')
  } finally { saving.value = false }
}

function goalLabel(g)  { return { WEIGHT_LOSS:'Giảm cân', MUSCLE_GAIN:'Tăng cơ', ENDURANCE:'Sức bền', FLEXIBILITY:'Linh hoạt', MAINTENANCE:'Duy trì' }[g]||g }
function levelLabel(l) { return { BEGINNER:'Mới bắt đầu', INTERMEDIATE:'Trung bình', ADVANCED:'Nâng cao' }[l]||l }

onMounted(load)
</script>

<style scoped>
.bmi-display { display:flex; gap:20px; align-items:center; }
.bmi-circle {
  width:90px; height:90px; border-radius:50%; border:4px solid var(--c-border);
  display:flex; flex-direction:column; align-items:center; justify-content:center;
  flex-shrink:0; transition:border-color 0.3s;
}
.bmi-num { font-family:var(--font-display); font-size:1.8rem; line-height:1; color:var(--c-text); }
.bmi-lbl { font-size:0.65rem; text-transform:uppercase; letter-spacing:0.1em; color:var(--c-text3); }
.bmi-info { flex:1; }
.bmi-cat  { font-family:var(--font-display); font-size:1.2rem; margin-bottom:6px; }
.bmi-ranges div { font-size:0.75rem; color:var(--c-text3); padding:2px 0; }
.bmi-ranges div.active { color:var(--c-text); font-weight:700; }

.tips-list { display:flex; flex-direction:column; gap:10px; }
.tip-item  { font-size:0.84rem; color:var(--c-text2); line-height:1.5; }
</style>
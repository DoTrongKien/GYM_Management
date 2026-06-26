<template>
  <div class="fade-in">

    <div class="page-header">
      <h2>GIÁO ÁN MẪU (ADMIN TẠO TAY)</h2>

      <div style="display:flex;gap:10px">
        <el-input
            v-model="search"
            placeholder="Lọc theo tên giáo án..."
            style="width:260px"
            clearable
        />

        <el-button type="primary" @click="openCreate">
          + Tạo giáo án mẫu
        </el-button>
      </div>
    </div>

    <div v-if="loading" style="padding:40px 0">
      <el-skeleton :rows="5" animated />
    </div>

    <div v-else>
      <el-table :data="filtered" stripe>
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="Tên giáo án" prop="planName" />
        <el-table-column label="Mục tiêu">
          <template #default="{row}">{{ goalLabel(row.goal) }}</template>
        </el-table-column>
        <el-table-column label="Trình độ">
          <template #default="{row}">{{ levelLabel(row.targetLevel) }}</template>
        </el-table-column>
        <el-table-column label="Số tuần" prop="durationWeeks" width="90" />
        <el-table-column label="Buổi/tuần" prop="sessionsPerWeek" width="100" />
        <el-table-column label="Ngày tạo" width="120">
          <template #default="{row}">{{ row.createdAt?.substring(0,10) }}</template>
        </el-table-column>
        <el-table-column label="Thao tác" width="220">
          <template #default="{row}">
            <el-button size="small" type="primary" @click="openEdit(row)">Sửa</el-button>
            <el-button size="small" type="danger" @click="remove(row.id)">Xóa</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!filtered.length" class="empty-state" style="text-align:center;padding:40px;color:var(--c-text3)">
        Chưa có giáo án mẫu nào. Bấm "Tạo giáo án mẫu" để bắt đầu.
      </div>
    </div>

    <!-- Dialog tạo / sửa template -->
    <el-dialog
        v-model="dialogVisible"
        :title="editing ? 'Sửa giáo án mẫu' : 'Tạo giáo án mẫu'"
        width="96vw"
        top="2vh"
        align-center
        append-to-body
        destroy-on-close
        class="template-dialog"
    >
      <el-form :model="form" label-position="top">

        <div class="grid-2">
          <el-form-item label="Tên giáo án" required>
            <el-input v-model="form.planName" placeholder="VD: Tăng cơ toàn thân 8 tuần" />
          </el-form-item>

          <el-form-item label="Mục tiêu" required>
            <el-select v-model="form.goal" style="width:100%">
              <el-option label="🔥 Giảm cân" value="WEIGHT_LOSS" />
              <el-option label="💪 Tăng cơ" value="MUSCLE_GAIN" />
              <el-option label="🏃 Sức bền" value="ENDURANCE" />
              <el-option label="🤸 Linh hoạt" value="FLEXIBILITY" />
              <el-option label="⚖️ Duy trì" value="MAINTENANCE" />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="Mô tả">
          <el-input type="textarea" v-model="form.description" rows="2" />
        </el-form-item>

        <div class="grid-3">
          <el-form-item label="Trình độ" required>
            <el-select v-model="form.targetLevel" style="width:100%">
              <el-option label="Beginner" value="BEGINNER" />
              <el-option label="Intermediate" value="INTERMEDIATE" />
              <el-option label="Advanced" value="ADVANCED" />
            </el-select>
          </el-form-item>

          <el-form-item label="Số tuần" required>
            <el-input-number v-model="form.durationWeeks" :min="1" :max="52" style="width:100%" />
          </el-form-item>

          <el-form-item label="Số ngày tập / tuần" required>
            <el-input-number
                v-model="form.sessionsPerWeek"
                :min="1" :max="7"
                style="width:100%"
                @change="syncDayColumns"
            />
          </el-form-item>
        </div>

        <el-divider />

        <div style="font-weight:700;color:var(--c-text);margin-bottom:12px">
          📅 Chọn bài tập cho từng ngày ({{ form.days.length }} ngày)
        </div>

        <div class="days-editor-scroll">
          <div class="days-editor-grid" :style="{ gridTemplateColumns: `repeat(${form.days.length}, minmax(260px, 1fr))` }">
            <div v-for="(day, dIdx) in form.days" :key="dIdx" class="day-col">

              <div class="day-col-header">
                <span>Buổi {{ dIdx + 1 }}</span>
                <el-select v-model="day.dayOfWeek" size="small" style="width:120px" @change="onDowChange(day)">
                  <el-option v-for="opt in dowOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
              </div>

              <div class="day-ex-list">
                <div v-for="(ex, eIdx) in day.exercises" :key="eIdx" class="ex-edit-row">
                  <el-select
                      v-model="ex.exerciseId"
                      filterable
                      placeholder="Chọn bài tập..."
                      size="small"
                      style="width:100%;margin-bottom:6px"
                      @change="onExerciseSelect(ex, $event)"
                  >
                    <el-option
                        v-for="opt in exercises"
                        :key="opt.id"
                        :label="opt.name"
                        :value="opt.id"
                    >
                      <span>{{ opt.name }}</span>
                      <span style="float:right;color:var(--c-text3);font-size:0.75rem">{{ muscleLabel(opt.muscleGroup) }}</span>
                    </el-option>
                  </el-select>

                  <div v-if="ex.exerciseId" class="ex-params-grid">
                    <el-input-number v-model="ex.sets" :min="1" :max="10" size="small" placeholder="Sets" />
                    <el-input-number v-model="ex.reps" :min="0" :max="100" size="small" placeholder="Reps" />
                    <el-input-number v-model="ex.restSeconds" :min="0" :max="300" :step="5" size="small" placeholder="Nghỉ(s)" />
                  </div>

                  <div style="display:flex;justify-content:flex-end;margin-top:4px" v-if="ex.exerciseId">
                    <el-button type="danger" link size="small" @click="day.exercises.splice(eIdx,1)">Xóa bài</el-button>
                  </div>
                </div>

                <el-button
                    size="small" plain style="width:100%;margin-top:4px"
                    @click="day.exercises.push(blankExercise())"
                >
                  + Thêm bài tập
                </el-button>
              </div>
            </div>
          </div>
        </div>

      </el-form>

      <template #footer>
        <el-button @click="dialogVisible=false">Hủy</el-button>
        <el-button type="primary" :loading="saving" @click="save">Lưu giáo án mẫu</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminAPI, exerciseAPI } from '@/api'

const plans = ref([])
const exercises = ref([])
const loading = ref(false)
const saving = ref(false)
const search = ref('')

const dialogVisible = ref(false)
const editing = ref(false)
const editingId = ref(null)

const dowOptions = [
  { value: 1, label: 'Thứ Hai' },
  { value: 2, label: 'Thứ Ba' },
  { value: 3, label: 'Thứ Tư' },
  { value: 4, label: 'Thứ Năm' },
  { value: 5, label: 'Thứ Sáu' },
  { value: 6, label: 'Thứ Bảy' },
  { value: 7, label: 'Chủ Nhật' }
]
const dowNames = ['Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday']

function blankExercise() {
  return { exerciseId: null, sets: 3, reps: 12, durationSeconds: null, restSeconds: 60, notes: '' }
}

function blankDay(dayOfWeek) {
  return { dayOfWeek, dayName: dowNames[dayOfWeek - 1], exercises: [blankExercise()] }
}

const form = ref({
  planName: '',
  description: '',
  goal: 'MUSCLE_GAIN',
  targetLevel: 'BEGINNER',
  durationWeeks: 6,
  sessionsPerWeek: 3,
  days: [blankDay(1), blankDay(3), blankDay(5)]
})

const filtered = computed(() => {
  if (!search.value) return plans.value
  return plans.value.filter(p => p.planName?.toLowerCase().includes(search.value.toLowerCase()))
})

function syncDayColumns(n) {
  const cur = form.value.days.length
  if (n > cur) {
    // Thêm ngày mới, tự gợi ý dayOfWeek tiếp theo chưa dùng
    const used = new Set(form.value.days.map(d => d.dayOfWeek))
    let next = 1
    for (let i = cur; i < n; i++) {
      while (used.has(next) && next <= 7) next++
      form.value.days.push(blankDay(next > 7 ? 1 : next))
      used.add(next)
    }
  } else if (n < cur) {
    form.value.days.splice(n)
  }
}

function onDowChange(day) {
  day.dayName = dowNames[day.dayOfWeek - 1]
}

function onExerciseSelect(ex, exerciseId) {
  const found = exercises.value.find(e => e.id === exerciseId)
  if (found) {
    ex.sets = found.defaultSets || 3
    ex.reps = found.defaultReps || 12
    ex.durationSeconds = found.defaultDurationSeconds || null
    ex.restSeconds = found.restSeconds || 60
  }
}

async function load() {
  loading.value = true
  try {
    const [planRes, exRes] = await Promise.all([
      adminAPI.getTemplates(),
      exerciseAPI.getAll()
    ])
    plans.value = planRes.data || []
    exercises.value = exRes.data || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = false
  editingId.value = null
  form.value = {
    planName: '',
    description: '',
    goal: 'MUSCLE_GAIN',
    targetLevel: 'BEGINNER',
    durationWeeks: 6,
    sessionsPerWeek: 3,
    days: [blankDay(1), blankDay(3), blankDay(5)]
  }
  dialogVisible.value = true
}

function openEdit(plan) {
  editing.value = true
  editingId.value = plan.id

  const days = (plan.planDays || []).map(d => ({
    dayOfWeek: d.dayOfWeek,
    dayName: d.dayName,
    exercises: (d.exercises || []).map(e => ({
      exerciseId: e.exerciseId,
      sets: e.sets,
      reps: e.reps,
      durationSeconds: e.durationSeconds,
      restSeconds: e.restSeconds,
      notes: e.notes || ''
    }))
  }))

  form.value = {
    planName: plan.planName,
    description: plan.description,
    goal: plan.goal,
    targetLevel: plan.targetLevel,
    durationWeeks: plan.durationWeeks,
    sessionsPerWeek: days.length || plan.sessionsPerWeek,
    days: days.length ? days : [blankDay(1)]
  }

  dialogVisible.value = true
}

function validate() {
  if (!form.value.planName?.trim()) { ElMessage.warning('Vui lòng nhập tên giáo án'); return false }
  if (!form.value.days.length) { ElMessage.warning('Cần ít nhất 1 ngày tập'); return false }
  for (const [i, day] of form.value.days.entries()) {
    const validEx = day.exercises.filter(e => e.exerciseId)
    if (!validEx.length) {
      ElMessage.warning(`Buổi ${i + 1} chưa chọn bài tập nào`)
      return false
    }
  }
  return true
}

async function save() {
  if (!validate()) return

  const payload = {
    planName: form.value.planName,
    description: form.value.description,
    goal: form.value.goal,
    targetLevel: form.value.targetLevel,
    durationWeeks: form.value.durationWeeks,
    sessionsPerWeek: form.value.days.length,
    days: form.value.days.map(d => ({
      dayOfWeek: d.dayOfWeek,
      dayName: d.dayName,
      exercises: d.exercises
          .filter(e => e.exerciseId)
          .map((e, idx) => ({
            exerciseId: e.exerciseId,
            sets: e.sets,
            reps: e.reps,
            durationSeconds: e.durationSeconds,
            restSeconds: e.restSeconds,
            orderIndex: idx + 1,
            notes: e.notes
          }))
    }))
  }

  saving.value = true
  try {
    if (editing.value) {
      await adminAPI.updateTemplate(editingId.value, payload)
      ElMessage.success('Cập nhật giáo án mẫu thành công')
    } else {
      await adminAPI.createTemplate(payload)
      ElMessage.success('Tạo giáo án mẫu thành công')
    }
    dialogVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Có lỗi xảy ra')
  } finally {
    saving.value = false
  }
}

async function remove(id) {
  try {
    await ElMessageBox.confirm('Xóa giáo án mẫu này?', 'Xác nhận')
    await adminAPI.deleteTemplate(id)
    ElMessage.success('Đã xóa')
    await load()
  } catch {}
}

function goalLabel(g) {
  return {
    WEIGHT_LOSS: '🔥 Giảm cân', MUSCLE_GAIN: '💪 Tăng cơ', ENDURANCE: '🏃 Sức bền',
    FLEXIBILITY: '🤸 Linh hoạt', MAINTENANCE: '⚖️ Duy trì'
  }[g] || g
}
function levelLabel(l) {
  return { BEGINNER: 'Beginner', INTERMEDIATE: 'Intermediate', ADVANCED: 'Advanced' }[l] || l
}
function muscleLabel(m) {
  return {
    CHEST: 'Ngực', BACK: 'Lưng', SHOULDERS: 'Vai', ARMS: 'Tay',
    LEGS: 'Chân', CORE: 'Cơ lõi', CARDIO: 'Cardio', FULL_BODY: 'Toàn thân'
  }[m] || m
}

onMounted(load)
</script>

<style scoped>
.grid-2 { display:grid; grid-template-columns:1fr 1fr; gap:16px; }
.grid-3 { display:grid; grid-template-columns:1fr 1fr 1fr; gap:16px; }

/* Vùng chứa các cột ngày: cho phép cuộn ngang riêng khi nhiều ngày,
   không phụ thuộc vào chiều rộng dialog */
.days-editor-scroll {
  overflow-x: auto;
  padding-bottom: 8px;
}

.days-editor-grid {
  display: grid;
  gap: 14px;
  /* grid-template-columns được set inline theo số ngày, mỗi cột tối thiểu 260px */
  align-items: start;
}

.day-col {
  background:var(--c-card2);
  border:1px solid var(--c-border2);
  border-radius:var(--radius-lg);
  padding:12px;
  display:flex;
  flex-direction:column;
  min-height:120px;
  min-width: 260px;
}
.day-col-header {
  display:flex; justify-content:space-between; align-items:center;
  font-weight:700; color:var(--c-text); margin-bottom:10px;
}
.day-ex-list { display:flex; flex-direction:column; gap:10px; }
.ex-edit-row {
  background:var(--c-card); border:1px solid var(--c-border2);
  border-radius:var(--radius); padding:8px;
}
.ex-params-grid {
  display:grid; grid-template-columns:1fr 1fr 1fr; gap:6px;
}
.ex-params-grid :deep(.el-input-number) { width:100%; }


:deep(.el-dialog.template-dialog) {
  width: 96vw !important;
  max-width: none !important;

  height: 92vh !important;

  margin: 2vh auto !important;

  display: flex;
  flex-direction: column;
}

:deep(.el-dialog.template-dialog .el-dialog__header) {
  flex-shrink: 0;
}

:deep(.el-dialog.template-dialog .el-dialog__body) {
  flex: 1;

  overflow-y: auto !important;
  overflow-x: hidden !important;

  height: auto !important;
  max-height: none !important;

  padding-bottom: 24px;
}

:deep(.el-dialog.template-dialog .el-dialog__footer) {
  flex-shrink: 0;
}
</style>
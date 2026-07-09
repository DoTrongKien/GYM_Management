<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>QUẢN LÝ BÀI TẬP</h2>
      <el-button type="primary" @click="openAdd">+ THÊM BÀI TẬP</el-button>
    </div>

    <el-card>
      <el-table :data="exercises" v-loading="loading" stripe>
        <el-table-column label="ID"   prop="id"   width="55" align="center"/>
        <el-table-column label="Tên"  prop="name" min-width="150"/>
        <el-table-column label="Nhóm cơ" prop="muscleGroup" width="110"/>
        <el-table-column label="Độ khó" width="110" align="center">
          <template #default="{row}">
            <span class="badge" :class="diffBadge(row.difficulty)">{{ diffLabel(row.difficulty) }}</span>
          </template>
        </el-table-column>
        <!-- Score columns -->
        <el-table-column label="💪 Tăng cơ" width="85" align="center">
          <template #default="{row}">
            <span :style="{color: scoreColor(row.muscleGainScore), fontWeight:700}">{{ row.muscleGainScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="🔥 Giảm cân" width="90" align="center">
          <template #default="{row}">
            <span :style="{color: scoreColor(row.weightLossScore), fontWeight:700}">{{ row.weightLossScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="🏃 Sức bền" width="85" align="center">
          <template #default="{row}">
            <span :style="{color: scoreColor(row.enduranceScore), fontWeight:700}">{{ row.enduranceScore }}</span>
          </template>
        </el-table-column>
        <!-- MỚI: cột thể lực tiêu hao -->
        <el-table-column label="⚡ Thể lực" width="85" align="center">
          <template #default="{row}">
            <span style="color:#f59e0b;font-weight:700">{{ row.staminaCost ?? 10 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Trạng thái" width="95" align="center">
          <template #default="{row}">
            <span class="badge" :class="row.isActive?'badge-success':'badge-danger'">{{ row.isActive?'Active':'Ẩn' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Thao tác" width="130" align="center" fixed="right">
          <template #default="{row}">
            <el-button size="small" @click="openEdit(row)">Sửa</el-button>
            <el-button size="small" type="danger" plain @click="remove(row.id)">Ẩn</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Add/Edit Dialog -->
    <el-dialog v-model="formDialog" :title="editId?'SỬA BÀI TẬP':'THÊM BÀI TẬP'" width="580px" align-center>
      <el-form :model="form" label-position="top">
        <!-- Thông tin cơ bản -->
        <el-form-item label="Tên bài tập">
          <el-input v-model="form.name" placeholder="VD: Bench Press"/>
        </el-form-item>
        <el-form-item label="Động tác">
          <el-input v-model="form.description" type="textarea" :rows="2"/>
        </el-form-item>
        <div class="grid-2">
          <el-form-item label="Nhóm cơ">
            <el-select v-model="form.muscleGroup" style="width:100%">
              <el-option v-for="m in muscles" :key="m" :label="m" :value="m"/>
            </el-select>
          </el-form-item>
          <el-form-item label="Độ khó">
            <el-select v-model="form.difficulty" style="width:100%">
              <el-option label="🟢 Dễ" value="EASY"/>
              <el-option label="🟡 Trung bình" value="MEDIUM"/>
              <el-option label="🔴 Khó" value="HARD"/>
            </el-select>
          </el-form-item>
        </div>
        <div class="grid-3">
          <el-form-item label="Số sets">
            <el-input-number v-model="form.defaultSets" :min="1" :max="20" style="width:100%"/>
          </el-form-item>
          <el-form-item label="Số reps">
            <el-input-number v-model="form.defaultReps" :min="0" :max="100" style="width:100%"/>
          </el-form-item>
          <el-form-item label="Kcal/set">
            <el-input-number v-model="form.caloriesBurned" :min="0" :max="200" style="width:100%"/>
          </el-form-item>
        </div>
        <el-form-item label="Link video YouTube">
          <el-input v-model="form.videoUrl" placeholder="https://youtube.com/watch?v=..."/>
        </el-form-item>

        <!-- MỚI: Thể lực (mana) tiêu hao khi tập bài này -->
        <div class="stamina-box">
          <div style="font-weight:700;color:#b45309;margin-bottom:4px">⚡ THỂ LỰC TIÊU HAO (0–200)</div>
          <div style="font-size:0.78rem;color:var(--c-text3);margin-bottom:10px">
            Số thể lực (mana) sẽ bị trừ khi người dùng tập bài này với tỉ lệ hoàn thành 100%.
            Nếu hoàn thành thấp hơn, thể lực trừ sẽ tính theo tỉ lệ tương ứng.
          </div>
          <div style="display:flex;align-items:center;gap:12px">
            <el-slider v-model="form.staminaCost" :min="0" :max="200" :step="5" style="flex:1"/>
            <span style="font-weight:700;width:50px;text-align:right;color:#f59e0b">{{ form.staminaCost }}</span>
          </div>
        </div>

        <!-- Điểm hiệu quả theo mục tiêu -->
        <div class="score-section">
          <div class="score-section-title">⭐ ĐIỂM HIỆU QUẢ THEO MỤC TIÊU (0–10)</div>
          <div style="font-size:0.78rem;color:var(--c-text3);margin-bottom:14px">
            Điểm càng cao → bài tập càng được ưu tiên khi tạo giáo án với mục tiêu đó
          </div>
          <div class="score-inputs">
            <div class="score-input-item">
              <div class="score-input-label">💪 Tăng cơ</div>
              <el-slider v-model="form.muscleGainScore" :min="0" :max="10" :step="1" show-stops/>
              <div class="score-input-val" :style="{color:scoreColor(form.muscleGainScore)}">{{ form.muscleGainScore }}/10</div>
            </div>
            <div class="score-input-item">
              <div class="score-input-label">🔥 Giảm cân</div>
              <el-slider v-model="form.weightLossScore" :min="0" :max="10" :step="1" show-stops/>
              <div class="score-input-val" :style="{color:scoreColor(form.weightLossScore)}">{{ form.weightLossScore }}/10</div>
            </div>
            <div class="score-input-item">
              <div class="score-input-label">🏃 Sức bền</div>
              <el-slider v-model="form.enduranceScore" :min="0" :max="10" :step="1" show-stops/>
              <div class="score-input-val" :style="{color:scoreColor(form.enduranceScore)}">{{ form.enduranceScore }}/10</div>
            </div>
            <div class="score-input-item">
              <div class="score-input-label">🤸 Linh hoạt</div>
              <el-slider v-model="form.flexibilityScore" :min="0" :max="10" :step="1" show-stops/>
              <div class="score-input-val" :style="{color:scoreColor(form.flexibilityScore)}">{{ form.flexibilityScore }}/10</div>
            </div>
            <div class="score-input-item">
              <div class="score-input-label">⚖️ Duy trì</div>
              <el-slider v-model="form.maintenanceScore" :min="0" :max="10" :step="1" show-stops/>
              <div class="score-input-val" :style="{color:scoreColor(form.maintenanceScore)}">{{ form.maintenanceScore }}/10</div>
            </div>
          </div>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="formDialog=false">Hủy</el-button>
        <el-button type="primary" @click="submit">{{ editId?'CẬP NHẬT':'THÊM MỚI' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { exerciseAPI } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const exercises  = ref([])
const loading    = ref(true)
const formDialog = ref(false)
const editId     = ref(null)
const muscles    = ['CHEST','BACK','SHOULDERS','ARMS','LEGS','CORE','CARDIO','FULL_BODY']

const defaultForm = () => ({
  name:'', description:'', muscleGroup:'CHEST', difficulty:'MEDIUM',
  defaultSets:3, defaultReps:10, caloriesBurned:8, videoUrl:'', restSeconds:60,
  muscleGainScore:5, weightLossScore:5, enduranceScore:5, flexibilityScore:5, maintenanceScore:5,
  staminaCost: 10
})
const form = reactive(defaultForm())

async function load() {
  loading.value = true
  try { const r = await exerciseAPI.getAll(); exercises.value = r.data || [] }
  finally { loading.value = false }
}

function openAdd() {
  editId.value = null
  Object.assign(form, defaultForm())
  formDialog.value = true
}

function openEdit(row) {
  editId.value = row.id
  Object.assign(form, {
    name: row.name, description: row.description||'',
    muscleGroup: row.muscleGroup, difficulty: row.difficulty,
    defaultSets: row.defaultSets||3, defaultReps: row.defaultReps||0,
    caloriesBurned: row.caloriesBurned||0, videoUrl: row.videoUrl||'',
    restSeconds: row.restSeconds||60,
    muscleGainScore:  row.muscleGainScore  ?? 5,
    weightLossScore:  row.weightLossScore  ?? 5,
    enduranceScore:   row.enduranceScore   ?? 5,
    flexibilityScore: row.flexibilityScore ?? 5,
    maintenanceScore: row.maintenanceScore ?? 5,
    staminaCost: row.staminaCost ?? 10,
  })
  formDialog.value = true
}

async function submit() {
  if (!form.name) { ElMessage.warning('Nhập tên bài tập'); return }
  if (editId.value) { await exerciseAPI.update(editId.value, { ...form }); ElMessage.success('Đã cập nhật!') }
  else              { await exerciseAPI.create({ ...form }); ElMessage.success('Đã thêm bài tập!') }
  formDialog.value = false; load()
}

async function remove(id) {
  await ElMessageBox.confirm('Ẩn bài tập này?', 'Xác nhận', { type:'warning' })
  await exerciseAPI.delete(id); ElMessage.success('Đã ẩn bài tập'); load()
}

function scoreColor(v) {
  if (!v && v !== 0) return 'var(--c-text3)'
  if (v >= 8) return 'var(--c-success)'
  if (v >= 5) return 'var(--c-warning)'
  return 'var(--c-danger)'
}
function diffLabel(d) { return { EASY:'Dễ',MEDIUM:'Trung bình',HARD:'Khó' }[d]||d }
function diffBadge(d) { return { EASY:'badge-success',MEDIUM:'badge-warning',HARD:'badge-danger' }[d]||'' }

onMounted(load)
</script>

<style scoped>
.score-section {
  background:var(--c-card2); border:1px solid var(--c-border2);
  border-radius:var(--radius-lg); padding:16px; margin-top:8px;
}
.score-section-title {
  font-family:var(--font-display); font-size:0.9rem; letter-spacing:0.06em;
  color:var(--c-accent); margin-bottom:6px;
}
.score-inputs { display:flex; flex-direction:column; gap:12px; }
.score-input-item { display:flex; align-items:center; gap:12px; }
.score-input-label { width:90px; font-size:0.82rem; font-weight:600; color:var(--c-text2); flex-shrink:0; }
.score-input-val   { width:38px; font-family:var(--font-mono); font-size:0.8rem; font-weight:700; text-align:right; flex-shrink:0; }
.el-slider { flex:1; }

/* MỚI */
.stamina-box {
  background:#fffbeb; border:1px solid #fde68a; border-radius:var(--radius-lg);
  padding:14px; margin-top:14px;
}
</style>
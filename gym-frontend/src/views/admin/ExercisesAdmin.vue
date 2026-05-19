<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>QUẢN LÝ BÀI TẬP</h2>
      <el-button type="primary" @click="openAdd">+ THÊM BÀI TẬP</el-button>
    </div>

    <el-table :data="exercises" v-loading="loading" stripe>
      <el-table-column label="ID"   prop="id"   width="60" align="center" />
      <el-table-column label="Tên"  prop="name" min-width="160" />
      <el-table-column label="Nhóm cơ" prop="muscleGroup" width="130" />
      <el-table-column label="Độ khó" width="110" align="center">
        <template #default="{row}">
          <span class="badge" :class="diffBadge(row.difficulty)">{{ diffLabel(row.difficulty) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="Sets" prop="defaultSets" width="70" align="center" />
      <el-table-column label="Reps" width="70" align="center">
        <template #default="{row}">{{ row.defaultReps || `${row.defaultDurationSeconds}s` }}</template>
      </el-table-column>
      <el-table-column label="Kcal/set" prop="caloriesBurned" width="90" align="center" />
      <el-table-column label="Trạng thái" width="100" align="center">
        <template #default="{row}">
          <span class="badge" :class="row.isActive ? 'badge-success' : 'badge-danger'">
            {{ row.isActive ? 'Active' : 'Ẩn' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="Thao tác" width="150" align="center" fixed="right">
        <template #default="{row}">
          <el-button size="small" @click="openEdit(row)">Sửa</el-button>
          <el-button size="small" type="danger" plain @click="remove(row.id)">Ẩn</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Add / Edit Dialog -->
    <el-dialog v-model="formDialog" :title="editId ? 'SỬA BÀI TẬP' : 'THÊM BÀI TẬP'" width="520px">
      <el-form :model="form" label-position="top">
        <el-form-item label="Tên bài tập">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="Mô tả">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <div class="grid-2">
          <el-form-item label="Nhóm cơ">
            <el-select v-model="form.muscleGroup" style="width:100%">
              <el-option v-for="m in muscleGroups" :key="m" :label="m" :value="m" />
            </el-select>
          </el-form-item>
          <el-form-item label="Độ khó">
            <el-select v-model="form.difficulty" style="width:100%">
              <el-option label="Dễ"       value="EASY"   />
              <el-option label="Trung bình" value="MEDIUM" />
              <el-option label="Khó"       value="HARD"   />
            </el-select>
          </el-form-item>
        </div>
        <div class="grid-3">
          <el-form-item label="Số sets">
            <el-input-number v-model="form.defaultSets" :min="1" :max="20" style="width:100%" />
          </el-form-item>
          <el-form-item label="Số reps">
            <el-input-number v-model="form.defaultReps" :min="0" :max="100" style="width:100%" />
          </el-form-item>
          <el-form-item label="Kcal/set">
            <el-input-number v-model="form.caloriesBurned" :min="0" :max="200" style="width:100%" />
          </el-form-item>
        </div>
        <el-form-item label="Link video YouTube (tuỳ chọn)">
          <el-input v-model="form.videoUrl" placeholder="https://youtube.com/..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialog = false">Hủy</el-button>
        <el-button type="primary" @click="submit">{{ editId ? 'CẬP NHẬT' : 'THÊM MỚI' }}</el-button>
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
const muscleGroups = ['CHEST','BACK','SHOULDERS','ARMS','LEGS','CORE','CARDIO','FULL_BODY']

const form = reactive({
  name:'', description:'', muscleGroup:'CHEST', difficulty:'MEDIUM',
  defaultSets:3, defaultReps:10, caloriesBurned:8, videoUrl:''
})

async function load() {
  loading.value = true
  try { const r = await exerciseAPI.getAll(); exercises.value = r.data || [] }
  finally { loading.value = false }
}

function openAdd() {
  editId.value = null
  Object.assign(form, { name:'', description:'', muscleGroup:'CHEST', difficulty:'MEDIUM', defaultSets:3, defaultReps:10, caloriesBurned:8, videoUrl:'' })
  formDialog.value = true
}

function openEdit(row) {
  editId.value = row.id
  Object.assign(form, row)
  formDialog.value = true
}

async function submit() {
  if (!form.name) { ElMessage.warning('Nhập tên bài tập'); return }
  if (editId.value) {
    await exerciseAPI.update(editId.value, form)
    ElMessage.success('Đã cập nhật!')
  } else {
    await exerciseAPI.create(form)
    ElMessage.success('Đã thêm bài tập!')
  }
  formDialog.value = false; load()
}

async function remove(id) {
  await ElMessageBox.confirm('Ẩn bài tập này?', 'Xác nhận', { type: 'warning' })
  await exerciseAPI.delete(id)
  ElMessage.success('Đã ẩn bài tập'); load()
}

function diffLabel(d) { return { EASY:'Dễ', MEDIUM:'Trung bình', HARD:'Khó' }[d] || d }
function diffBadge(d) { return { EASY:'badge-success', MEDIUM:'badge-warning', HARD:'badge-danger' }[d] || '' }

onMounted(load)
</script>

```vue
<template>
  <div class="fade-in">

    <div class="page-header">
      <h2>GIÁO ÁN CỦA USER</h2>

      <div style="display:flex;gap:10px">
        <el-input
          v-model="search"
          placeholder="Lọc theo tên giáo án..."
          style="width:260px"
          clearable
        />

        <el-button
          type="primary"
          @click="openCreate"
        >
          Thêm giáo án
        </el-button>
      </div>
    </div>

    <div v-if="loading" style="padding:40px 0">
      <el-skeleton :rows="5" animated />
    </div>

    <div v-else>

      <el-table :data="filtered" stripe>

        <el-table-column
          label="ID"
          prop="id"
          width="70"
        />

        <el-table-column
          label="Tên giáo án"
          prop="planName"
        />

        <el-table-column
          label="Mục tiêu"
          prop="goal"
        />

        <el-table-column
          label="Trình độ"
          prop="targetLevel"
        />

        <el-table-column
          label="Số tuần"
          prop="durationWeeks"
        />

        <el-table-column
          label="Buổi/tuần"
          prop="sessionsPerWeek"
        />

        <el-table-column
          label="Ngày tạo"
        >
          <template #default="{row}">
            {{ row.createdAt?.substring(0,10) }}
          </template>
        </el-table-column>

        <el-table-column
          label="Thao tác"
          width="220"
        >
          <template #default="{row}">

            <el-button
              size="small"
              type="primary"
              @click="openEdit(row)"
            >
              Sửa
            </el-button>

            <el-button
              size="small"
              type="danger"
              @click="remove(row.id)"
            >
              Xóa
            </el-button>

          </template>
        </el-table-column>

      </el-table>

    </div>

    <!-- Dialog -->

    <el-dialog
      v-model="dialogVisible"
      :title="editing ? 'Sửa giáo án' : 'Thêm giáo án'"
      width="600"
    >

      <el-form :model="form">

        <el-form-item label="Tên giáo án">
          <el-input v-model="form.planName" />
        </el-form-item>

        <el-form-item label="Mô tả">
          <el-input
            type="textarea"
            v-model="form.description"
          />
        </el-form-item>

        <el-form-item label="Mục tiêu">
          <el-select v-model="form.goal">

            <el-option
              label="Giảm cân"
              value="WEIGHT_LOSS"
            />

            <el-option
              label="Tăng cơ"
              value="MUSCLE_GAIN"
            />

            <el-option
              label="Duy trì"
              value="MAINTENANCE"
            />

          </el-select>
        </el-form-item>

        <el-form-item label="Trình độ">
          <el-select v-model="form.targetLevel">

            <el-option
              label="Beginner"
              value="BEGINNER"
            />

            <el-option
              label="Intermediate"
              value="INTERMEDIATE"
            />

            <el-option
              label="Advanced"
              value="ADVANCED"
            />

          </el-select>
        </el-form-item>

        <el-form-item label="Số tuần">
          <el-input-number
            v-model="form.durationWeeks"
            :min="1"
          />
        </el-form-item>

        <el-form-item label="Buổi / tuần">
          <el-input-number
            v-model="form.sessionsPerWeek"
            :min="1"
            :max="7"
          />
        </el-form-item>

      </el-form>

      <template #footer>

        <el-button
          @click="dialogVisible=false"
        >
          Hủy
        </el-button>

        <el-button
          type="primary"
          @click="save"
        >
          Lưu
        </el-button>

      </template>

    </el-dialog>

  </div>
</template>

<script setup>

import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminAPI } from '@/api'

const plans = ref([])
const loading = ref(false)
const search = ref('')

const dialogVisible = ref(false)
const editing = ref(false)

const form = ref({
  id: null,
  planName: '',
  description: '',
  goal: 'WEIGHT_LOSS',
  targetLevel: 'BEGINNER',
  durationWeeks: 8,
  sessionsPerWeek: 4
})

const filtered = computed(() => {

  if (!search.value)
    return plans.value

  return plans.value.filter(p =>
    p.planName?.toLowerCase()
      .includes(search.value.toLowerCase())
  )
})

async function load() {

  loading.value = true

  try {

    const res = await adminAPI.getPlans()

    plans.value = res.data || []

  } finally {

    loading.value = false

  }
}

function openCreate() {

  editing.value = false

  form.value = {
    planName: '',
    description: '',
    goal: 'WEIGHT_LOSS',
    targetLevel: 'BEGINNER',
    durationWeeks: 8,
    sessionsPerWeek: 4
  }

  dialogVisible.value = true
}

function openEdit(plan) {

  editing.value = true

  form.value = {
    ...plan
  }

  dialogVisible.value = true
}

async function save() {

  try {

    if (editing.value) {

      await adminAPI.updatePlan(
        form.value.id,
        form.value
      )

      ElMessage.success('Cập nhật thành công')

    } else {

      await adminAPI.createPlan(
        form.value
      )

      ElMessage.success('Tạo thành công')
    }

    dialogVisible.value = false

    await load()

  } catch (e) {

    ElMessage.error('Có lỗi xảy ra')
  }
}

async function remove(id) {

  try {

    await ElMessageBox.confirm(
      'Xóa giáo án?',
      'Xác nhận'
    )

    await adminAPI.deletePlan(id)

    ElMessage.success('Đã xóa')

    await load()

  } catch {}
}

onMounted(load)

</script>
```

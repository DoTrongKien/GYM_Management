<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>GIÁO ÁN CỦA USER</h2>
      <el-input v-model="search" placeholder="Lọc theo tên giáo án..." prefix-icon="Search" style="width:260px" clearable />
    </div>

    <div v-if="loading" style="padding:40px 0"><el-skeleton :rows="5" animated /></div>

    <div v-else>
      <el-table :data="filtered" stripe>
        <el-table-column label="ID"     prop="id"     width="60" align="center" />
        <el-table-column label="Tên giáo án" prop="planName" min-width="200" />
        <el-table-column label="Mục tiêu" prop="goal" width="130" />
        <el-table-column label="Trình độ" prop="targetLevel" width="130" />
        <el-table-column label="Số tuần" prop="durationWeeks" width="90" align="center" />
        <el-table-column label="Buổi/tuần" prop="sessionsPerWeek" width="100" align="center" />
        <el-table-column label="AI" width="70" align="center">
          <template #default="{row}">
            <el-icon :color="row.isAiGenerated ? 'var(--c-accent)' : 'var(--c-text3)'">
              <MagicStick v-if="row.isAiGenerated" /><Minus v-else />
            </el-icon>
          </template>
        </el-table-column>
        <el-table-column label="Ngày tạo" width="120">
          <template #default="{row}">{{ row.createdAt?.substring(0,10) }}</template>
        </el-table-column>
        <el-table-column label="Số ngày" width="90" align="center">
          <template #default="{row}">{{ row.planDays?.length || 0 }} ngày</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminAPI } from '@/api'

const plans   = ref([])
const loading = ref(true)
const search  = ref('')

const filtered = computed(() => {
  if (!search.value) return plans.value
  return plans.value.filter(p => p.planName?.toLowerCase().includes(search.value.toLowerCase()))
})

async function load() {
  loading.value = true
  try { const r = await adminAPI.getPlans(); plans.value = r.data || [] }
  finally { loading.value = false }
}
onMounted(load)
</script>

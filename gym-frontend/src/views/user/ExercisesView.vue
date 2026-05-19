<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>THƯ VIỆN BÀI TẬP</h2>
    </div>

    <!-- Filters -->
    <div class="filters" style="margin-bottom:20px">
      <el-select v-model="filterMuscle" placeholder="Nhóm cơ" clearable style="width:160px" @change="load">
        <el-option v-for="m in muscleGroups" :key="m.value" :label="m.label" :value="m.value" />
      </el-select>
      <el-select v-model="filterDiff" placeholder="Độ khó" clearable style="width:140px" @change="load">
        <el-option label="Dễ" value="EASY" />
        <el-option label="Trung bình" value="MEDIUM" />
        <el-option label="Khó" value="HARD" />
      </el-select>
      <el-input v-model="search" placeholder="Tìm bài tập..." clearable style="width:220px" prefix-icon="Search" />
    </div>

    <div v-if="loading" style="padding:40px 0"><el-skeleton :rows="4" animated /></div>

    <div v-else class="exercise-grid">
      <div
        v-for="ex in filtered"
        :key="ex.id"
        class="exercise-card"
        @click="selected = ex; detailDialog = true"
      >
        <div class="ex-muscle-tag">{{ ex.muscleGroup }}</div>
        <div class="ex-name">{{ ex.name }}</div>
        <div class="ex-desc muted">{{ ex.description }}</div>
        <div class="ex-meta">
          <span class="badge" :class="diffBadge(ex.difficulty)">{{ diffLabel(ex.difficulty) }}</span>
          <span class="ex-cal mono" v-if="ex.caloriesBurned">🔥 {{ ex.caloriesBurned }} kcal/set</span>
        </div>
        <div class="ex-default mono" style="font-size:0.75rem; color:var(--c-text3); margin-top:8px">
          <span v-if="ex.defaultReps">{{ ex.defaultSets }}×{{ ex.defaultReps }} reps</span>
          <span v-else-if="ex.defaultDurationSeconds">{{ ex.defaultSets }}×{{ ex.defaultDurationSeconds }}s</span>
        </div>
      </div>
    </div>

    <div v-if="!loading && filtered.length === 0" class="empty-state">
      Không tìm thấy bài tập nào
    </div>

    <!-- Detail Dialog -->
    <el-dialog v-model="detailDialog" :title="selected?.name" width="480px" v-if="selected">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="Nhóm cơ">{{ selected.muscleGroup }}</el-descriptions-item>
        <el-descriptions-item label="Độ khó">
          <span class="badge" :class="diffBadge(selected.difficulty)">{{ diffLabel(selected.difficulty) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="Số set mặc định">{{ selected.defaultSets }}</el-descriptions-item>
        <el-descriptions-item label="Số rep / thời gian">
          <span v-if="selected.defaultReps">{{ selected.defaultReps }} reps</span>
          <span v-else>{{ selected.defaultDurationSeconds }}s</span>
        </el-descriptions-item>
        <el-descriptions-item label="Calories / set">{{ selected.caloriesBurned || '--' }} kcal</el-descriptions-item>
        <el-descriptions-item label="Nghỉ giữa set">{{ selected.restSeconds || '--' }}s</el-descriptions-item>
        <el-descriptions-item label="Mô tả" :span="2">{{ selected.description || 'Chưa có mô tả' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { exerciseAPI } from '@/api'

const exercises    = ref([])
const loading      = ref(true)
const filterMuscle = ref('')
const filterDiff   = ref('')
const search       = ref('')
const selected     = ref(null)
const detailDialog = ref(false)

const muscleGroups = [
  { value:'CHEST', label:'Ngực' }, { value:'BACK', label:'Lưng' },
  { value:'SHOULDERS', label:'Vai' }, { value:'ARMS', label:'Tay' },
  { value:'LEGS', label:'Chân' }, { value:'CORE', label:'Cơ lõi' },
  { value:'CARDIO', label:'Cardio' }, { value:'FULL_BODY', label:'Toàn thân' },
]

const filtered = computed(() => {
  let list = exercises.value
  if (search.value) list = list.filter(e => e.name.toLowerCase().includes(search.value.toLowerCase()))
  return list
})

async function load() {
  loading.value = true
  try {
    const params = {}
    if (filterMuscle.value) params.muscleGroup = filterMuscle.value
    if (filterDiff.value)   params.difficulty  = filterDiff.value
    const r = await exerciseAPI.getAll(params)
    exercises.value = r.data || []
  } finally { loading.value = false }
}

function diffLabel(d) { return { EASY:'Dễ', MEDIUM:'Trung bình', HARD:'Khó' }[d] || d }
function diffBadge(d) { return { EASY:'badge-success', MEDIUM:'badge-warning', HARD:'badge-danger' }[d] || '' }

onMounted(load)
</script>

<style scoped>
.filters { display:flex; gap:12px; flex-wrap:wrap; }
.exercise-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 16px;
}
.exercise-card {
  background:var(--c-bg2); border:1px solid var(--c-border);
  border-radius:var(--radius-lg); padding:16px; cursor:pointer;
  transition: border-color var(--transition), transform var(--transition);
}
.exercise-card:hover { border-color:var(--c-accent); transform:translateY(-2px); }
.ex-muscle-tag {
  font-size:0.68rem; text-transform:uppercase; letter-spacing:0.12em;
  color:var(--c-accent); margin-bottom:6px;
}
.ex-name { font-weight:600; font-size:0.95rem; margin-bottom:4px; }
.ex-desc { font-size:0.8rem; margin-bottom:10px; line-height:1.4;
  overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; }
.ex-meta { display:flex; align-items:center; justify-content:space-between; gap:8px; }
.ex-cal  { font-size:0.75rem; color:var(--c-text3); }
.empty-state { text-align:center; padding:40px; color:var(--c-text3); }
</style>

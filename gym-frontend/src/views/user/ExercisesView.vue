<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>THƯ VIỆN BÀI TẬP</h2>
    </div>

    <!-- Filters -->
    <el-card style="margin-bottom:20px">
      <div style="display:flex;gap:12px;flex-wrap:wrap;align-items:center">
        <el-select v-model="filterMuscle" placeholder="Nhóm cơ" clearable style="width:155px" @change="load">
          <el-option v-for="m in muscles" :key="m.v" :label="m.l" :value="m.v"/>
        </el-select>
        <el-select v-model="filterDiff" placeholder="Độ khó" clearable style="width:140px" @change="load">
          <el-option label="🟢 Dễ" value="EASY"/>
          <el-option label="🟡 Trung bình" value="MEDIUM"/>
          <el-option label="🔴 Khó" value="HARD"/>
        </el-select>
        <el-input v-model="search" placeholder="Tìm bài tập..." prefix-icon="Search" style="width:220px" clearable/>
        <span class="muted" style="font-size:0.82rem;color:var(--c-text3)">{{ filtered.length }} bài tập</span>
      </div>
    </el-card>

    <div v-if="loading"><el-skeleton :rows="4" animated style="background:var(--c-card);padding:24px;border-radius:12px"/></div>

    <div v-else class="exercise-grid">
      <div v-for="ex in filtered" :key="ex.id" class="exercise-card" @click="open(ex)">
        <div class="ex-tag">{{ muscleLabel(ex.muscleGroup) }}</div>
        <div class="ex-name">{{ ex.name }}</div>
        <div class="ex-desc">{{ ex.description }}</div>
        <div class="ex-footer">
          <span class="badge" :class="diffBadge(ex.difficulty)">{{ diffLabel(ex.difficulty) }}</span>
          <span style="font-size:0.75rem;color:var(--c-text3)" v-if="ex.caloriesBurned">🔥 {{ ex.caloriesBurned }} kcal/set</span>
        </div>
        <div class="ex-sets">
          <span v-if="ex.defaultReps">{{ ex.defaultSets }}×{{ ex.defaultReps }} reps</span>
          <span v-else-if="ex.defaultDurationSeconds">{{ ex.defaultSets }}×{{ ex.defaultDurationSeconds }}s</span>
        </div>
      </div>
    </div>

    <div v-if="!loading && !filtered.length" class="empty-state">Không tìm thấy bài tập nào</div>

    <!-- Detail Dialog with YouTube embed -->
    <el-dialog v-model="detailDialog" :title="sel?.name" width="600px" align-center v-if="sel">
      <!-- YouTube video embed -->
      <div v-if="sel.videoUrl" class="video-wrap">
        <iframe
            :src="youtubeEmbed(sel.videoUrl)"
            frameborder="0"
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
            allowfullscreen
            style="width:100%;height:280px;border-radius:8px"
        ></iframe>
      </div>
      <div v-else class="no-video">📹 Chưa có video hướng dẫn</div>

      <el-descriptions :column="2" border size="small" style="margin-top:16px">
        <el-descriptions-item label="Nhóm cơ">{{ muscleLabel(sel.muscleGroup) }}</el-descriptions-item>
        <el-descriptions-item label="Độ khó">
          <span class="badge" :class="diffBadge(sel.difficulty)">{{ diffLabel(sel.difficulty) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="Số sets">{{ sel.defaultSets }}</el-descriptions-item>
        <el-descriptions-item label="Reps / Thời gian">
          <span v-if="sel.defaultReps">{{ sel.defaultReps }} reps</span>
          <span v-else>{{ sel.defaultDurationSeconds }}s</span>
        </el-descriptions-item>
        <el-descriptions-item label="Calories/set">{{ sel.caloriesBurned || '--' }} kcal</el-descriptions-item>
        <el-descriptions-item label="Nghỉ">{{ sel.restSeconds || '--' }}s</el-descriptions-item>
        <el-descriptions-item label="Mô tả" :span="2">{{ sel.description || 'Chưa có mô tả' }}</el-descriptions-item>
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
const sel          = ref(null)
const detailDialog = ref(false)

const muscles = [
  { v:'CHEST', l:'💪 Ngực' }, { v:'BACK', l:'🔙 Lưng' },
  { v:'SHOULDERS', l:'🔝 Vai' }, { v:'ARMS', l:'💪 Tay' },
  { v:'LEGS', l:'🦵 Chân' }, { v:'CORE', l:'🎯 Cơ lõi' },
  { v:'CARDIO', l:'❤️ Cardio' }, { v:'FULL_BODY', l:'⭐ Toàn thân' },
]

const filtered = computed(() => {
  let list = exercises.value
  if (search.value) list = list.filter(e => e.name.toLowerCase().includes(search.value.toLowerCase()))
  return list
})

async function load() {
  loading.value = true
  try {
    const p = {}
    if (filterMuscle.value) p.muscleGroup = filterMuscle.value
    if (filterDiff.value)   p.difficulty  = filterDiff.value
    const r = await exerciseAPI.getAll(p)
    exercises.value = r.data || []
  } finally { loading.value = false }
}

function open(ex) { sel.value = ex; detailDialog.value = true }

function youtubeEmbed(url) {
  if (!url) return ''
  // Convert watch?v=ID or youtu.be/ID to embed
  const m = url.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/)([\w-]+)/)
  return m ? `https://www.youtube.com/embed/${m[1]}` : url
}

function muscleLabel(m) { return muscles.find(x=>x.v===m)?.l || m }
function diffLabel(d) { return { EASY:'Dễ', MEDIUM:'Trung bình', HARD:'Khó' }[d] || d }
function diffBadge(d) { return { EASY:'badge-success', MEDIUM:'badge-warning', HARD:'badge-danger' }[d] || '' }

onMounted(load)
</script>

<style scoped>
.exercise-grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(220px,1fr)); gap:16px; }
.exercise-card {
  background:var(--c-card); border:1px solid var(--c-border2);
  border-radius:var(--radius-lg); padding:16px; cursor:pointer;
  transition: all var(--transition); display:flex; flex-direction:column; gap:6px;
  box-shadow: var(--shadow);
}
.exercise-card:hover { border-color:var(--c-accent); box-shadow:var(--shadow-lg); transform:translateY(-2px); }
.ex-tag { font-size:0.68rem; text-transform:uppercase; letter-spacing:0.1em; color:var(--c-accent); font-weight:700; }
.ex-name { font-weight:700; font-size:0.95rem; color:var(--c-text); }
.ex-desc { font-size:0.8rem; color:var(--c-text2); line-height:1.4;
  overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; }
.ex-footer { display:flex; align-items:center; justify-content:space-between; }
.ex-sets { font-size:0.75rem; color:var(--c-accent); font-family:var(--font-mono); font-weight:700; }

.video-wrap { border-radius:8px; overflow:hidden; background:#000; }
.no-video { text-align:center; padding:24px; color:var(--c-text3); background:var(--c-card2); border-radius:8px; }
</style>
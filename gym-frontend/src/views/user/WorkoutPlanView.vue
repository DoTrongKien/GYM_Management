<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>GIÁO ÁN TẬP</h2>
      <div style="display:flex;gap:8px">
        <el-button @click="allPlansDialog=true" plain>📋 Xem tất cả giáo án</el-button>
        <el-button type="primary" @click="generate" :loading="generating">✨ TẠO GIÁO ÁN AI</el-button>
      </div>
    </div>

    <!-- Empty state -->
    <div v-if="!plan && !loading" class="empty-plan">
      <div class="empty-icon">🤖</div>
      <h3 class="display" style="font-size:1.8rem;color:var(--c-text);margin-bottom:8px">CHƯA CÓ GIÁO ÁN</h3>
      <p style="color:var(--c-text2);margin-bottom:20px">Hệ thống AI sẽ tự động tạo lịch tập 8 tuần dựa trên hồ sơ của bạn.</p>
      <el-button type="primary" size="large" @click="generate" :loading="generating">✨ TẠO GIÁO ÁN AI NGAY</el-button>
    </div>

    <div v-if="loading" style="padding:40px 0"><el-skeleton :rows="8" animated style="background:var(--c-card);padding:24px;border-radius:12px"/></div>

    <template v-if="plan && !loading">
      <!-- Plan header card -->
      <el-card style="margin-bottom:24px;border-left:4px solid var(--c-accent)">
        <div style="display:flex;justify-content:space-between;align-items:flex-start;flex-wrap:wrap;gap:12px">
          <div>
            <div class="display" style="font-size:1.6rem;color:var(--c-text)">{{ plan.planName }}</div>
            <div style="color:var(--c-text2);margin:4px 0 10px">{{ plan.description }}</div>
            <div style="display:flex;gap:8px;flex-wrap:wrap">
              <el-tag type="warning">{{ goalLabel(plan.goal) }}</el-tag>
              <el-tag type="info">{{ levelLabel(plan.targetLevel) }}</el-tag>
              <el-tag>{{ plan.durationWeeks }} tuần</el-tag>
              <el-tag>{{ plan.sessionsPerWeek }} buổi/tuần</el-tag>
              <el-tag v-if="plan.isAiGenerated" type="success">✨ AI Generated</el-tag>
            </div>
          </div>
          <el-button @click="showCustomAdd=true" plain size="small">+ Thêm ngày tập</el-button>
        </div>
      </el-card>

      <!-- Days grid -->
      <div class="days-grid">
        <el-card v-for="day in plan.planDays" :key="day.id" class="day-card">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span class="display accent" style="font-size:1.1rem">{{ day.dayName }}</span>
              <span style="font-size:0.75rem;color:var(--c-text3)">{{ day.exercises?.length || 0 }} bài</span>
            </div>
          </template>
          <div class="exercise-list">
            <div
                v-for="ex in day.exercises" :key="ex.id"
                class="ex-row"
                @click="goToExercise(ex)"
                title="Xem chi tiết bài tập"
            >
              <div class="ex-info">
                <div class="ex-name">{{ ex.exerciseName }}</div>
                <div class="ex-sub">{{ muscleLabel(ex.muscleGroup) }} · {{ diffLabel(ex.difficulty) }}</div>
              </div>
              <div class="ex-sets">
                <span v-if="ex.reps">{{ ex.sets }}×{{ ex.reps }}</span>
                <span v-else>{{ ex.sets }}×{{ ex.durationSeconds }}s</span>
              </div>
              <el-icon style="color:var(--c-text3);font-size:12px"><ArrowRight/></el-icon>
            </div>
          </div>
        </el-card>
      </div>
    </template>

    <!-- Tất cả giáo án -->
    <el-dialog v-model="allPlansDialog" title="TẤT CẢ GIÁO ÁN" width="640px" align-center>
      <div v-if="!allPlans.length" class="empty-state">Chưa có giáo án nào</div>
      <div v-else class="plans-list">
        <div v-for="p in allPlans" :key="p.id" class="plan-item" :class="{active: p.isActive}">
          <div style="flex:1">
            <div style="font-weight:700;color:var(--c-text)">{{ p.planName }}</div>
            <div style="font-size:0.8rem;color:var(--c-text3);margin-top:2px">
              {{ goalLabel(p.goal) }} · {{ levelLabel(p.targetLevel) }} · {{ p.durationWeeks }} tuần
            </div>
          </div>
          <div style="display:flex;gap:6px;align-items:center">
            <el-tag v-if="p.isActive" type="success" size="small">Active</el-tag>
            <el-tag v-if="p.isAiGenerated" size="small">AI</el-tag>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="allPlansDialog=false">Đóng</el-button>
        <el-button type="primary" @click="generate(); allPlansDialog=false" :loading="generating">✨ Tạo giáo án AI mới</el-button>
      </template>
    </el-dialog>

    <!-- Exercise detail dialog (navigate from plan) -->
    <el-dialog v-model="exDetailDialog" :title="selEx?.exerciseName" width="540px" align-center v-if="selEx">
      <div v-if="selEx.videoUrl" class="video-wrap">
        <iframe :src="ytEmbed(selEx.videoUrl)" frameborder="0" allowfullscreen
                style="width:100%;height:260px;border-radius:8px"></iframe>
      </div>
      <div v-else class="no-video">📹 Chưa có video</div>
      <el-descriptions :column="2" border size="small" style="margin-top:14px">
        <el-descriptions-item label="Nhóm cơ">{{ muscleLabel(selEx.muscleGroup) }}</el-descriptions-item>
        <el-descriptions-item label="Độ khó"><span class="badge" :class="diffBadge(selEx.difficulty)">{{ diffLabel(selEx.difficulty) }}</span></el-descriptions-item>
        <el-descriptions-item label="Sets">{{ selEx.sets }}</el-descriptions-item>
        <el-descriptions-item label="Reps / Thời gian">
          <span v-if="selEx.reps">{{ selEx.reps }} reps</span><span v-else>{{ selEx.durationSeconds }}s</span>
        </el-descriptions-item>
        <el-descriptions-item label="Nghỉ">{{ selEx.restSeconds }}s</el-descriptions-item>
        <el-descriptions-item label="Calories/set">{{ selEx.caloriesBurned }} kcal</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="exDetailDialog=false">Đóng</el-button>
        <el-button type="primary" @click="$router.push('/app/exercises'); exDetailDialog=false">Xem thư viện bài tập</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { planAPI } from '@/api'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router        = useRouter()
const plan          = ref(null)
const allPlans      = ref([])
const loading       = ref(true)
const generating    = ref(false)
const allPlansDialog= ref(false)
const showCustomAdd = ref(false)
const exDetailDialog= ref(false)
const selEx         = ref(null)

async function load() {
  loading.value = true
  try {
    const [act, all] = await Promise.all([planAPI.getActive().catch(()=>({data:null})), planAPI.getAll()])
    plan.value     = act.data
    allPlans.value = all.data || []
  } finally { loading.value = false }
}

async function generate() {
  generating.value = true
  try {
    const r = await planAPI.generate()
    plan.value = r.data
    ElMessage.success('Giáo án AI đã được tạo thành công! 🎉')
    load()
  } catch {} finally { generating.value = false }
}

function goToExercise(ex) { selEx.value = ex; exDetailDialog.value = true }
function ytEmbed(url) {
  const m = (url||'').match(/(?:youtube\.com\/watch\?v=|youtu\.be\/)([\w-]+)/)
  return m ? `https://www.youtube.com/embed/${m[1]}` : url
}

function goalLabel(g) { return { WEIGHT_LOSS:'🔥 Giảm cân', MUSCLE_GAIN:'💪 Tăng cơ', ENDURANCE:'🏃 Sức bền', FLEXIBILITY:'🤸 Linh hoạt', MAINTENANCE:'⚖️ Duy trì' }[g] || g }
function levelLabel(l) { return { BEGINNER:'Mới bắt đầu', INTERMEDIATE:'Trung bình', ADVANCED:'Nâng cao' }[l] || l }
function muscleLabel(m) { return { CHEST:'Ngực', BACK:'Lưng', SHOULDERS:'Vai', ARMS:'Tay', LEGS:'Chân', CORE:'Cơ lõi', CARDIO:'Cardio', FULL_BODY:'Toàn thân' }[m] || m }
function diffLabel(d) { return { EASY:'Dễ', MEDIUM:'Trung bình', HARD:'Khó' }[d] || d }
function diffBadge(d) { return { EASY:'badge-success', MEDIUM:'badge-warning', HARD:'badge-danger' }[d] || '' }

onMounted(load)
</script>

<style scoped>
.empty-plan { text-align:center; padding:80px 40px; background:var(--c-card); border-radius:var(--radius-lg); box-shadow:var(--shadow); }
.empty-icon { font-size:4rem; margin-bottom:16px; }
.days-grid  { display:grid; grid-template-columns:repeat(auto-fill,minmax(280px,1fr)); gap:16px; }
.day-card   {}
.exercise-list { display:flex; flex-direction:column; gap:6px; }
.ex-row {
  display:flex; align-items:center; gap:8px;
  padding:8px 10px; background:var(--c-card2); border-radius:var(--radius);
  cursor:pointer; transition: background var(--transition);
}
.ex-row:hover { background:#EDE0D0; }
.ex-info { flex:1; min-width:0; }
.ex-name { font-size:0.875rem; font-weight:600; color:var(--c-text); }
.ex-sub  { font-size:0.72rem; color:var(--c-text3); margin-top:1px; }
.ex-sets { font-size:0.8rem; color:var(--c-accent); font-family:var(--font-mono); font-weight:700; white-space:nowrap; }

.plans-list { display:flex; flex-direction:column; gap:10px; max-height:400px; overflow-y:auto; }
.plan-item {
  display:flex; align-items:center; gap:12px;
  padding:12px 14px; background:var(--c-card2); border:1px solid var(--c-border2);
  border-radius:var(--radius-lg); transition: border-color var(--transition);
}
.plan-item.active { border-color:var(--c-accent); }

.video-wrap { border-radius:8px; overflow:hidden; }
.no-video { text-align:center; padding:20px; color:var(--c-text3); background:var(--c-card2); border-radius:8px; }
</style>
<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>GIÁO ÁN TẬP</h2>
      <div style="display:flex;gap:8px;flex-wrap:wrap">
        <el-button @click="allPlansDialog=true" plain>📋 Tất cả giáo án</el-button>
        <el-button @click="goalDialog=true" type="primary">✨ Tạo giáo án </el-button>
      </div>
    </div>

    <!-- Empty -->
    <div v-if="!plan && !loading" class="empty-plan">
      <div style="font-size:4rem;margin-bottom:16px">🤖</div>
      <h3 class="display" style="font-size:1.8rem;color:var(--c-text);margin-bottom:8px">CHƯA CÓ GIÁO ÁN</h3>
      <p style="color:var(--c-text2);margin-bottom:20px;max-width:440px;margin-left:auto;margin-right:auto">
        Hệ thống sẽ tự động chọn bài tập phù hợp nhất theo mục tiêu của bạn, dựa trên chỉ số benefit của từng bài.
      </p>
      <el-button type="primary" size="large" @click="goalDialog=true">✨ Chọn mục tiêu & Tạo giáo án</el-button>
    </div>

    <div v-if="loading" style="padding:40px 0">
      <el-skeleton :rows="8" animated style="background:var(--c-card);padding:24px;border-radius:12px"/>
    </div>

    <template v-if="plan && !loading">
      <!-- Plan header -->
      <el-card style="margin-bottom:24px;border-left:4px solid var(--c-accent)">
        <div style="display:flex;justify-content:space-between;align-items:flex-start;flex-wrap:wrap;gap:12px">
          <div>
            <div class="display" style="font-size:1.6rem;color:var(--c-text)">{{ plan.planName }}</div>
            <div style="color:var(--c-text2);margin:4px 0 10px;font-size:0.875rem">{{ plan.description }}</div>
            <div style="display:flex;gap:8px;flex-wrap:wrap">
              <el-tag type="warning">{{ goalLabel(plan.goal) }}</el-tag>
              <el-tag type="info">{{ levelLabel(plan.targetLevel) }}</el-tag>
              <el-tag>{{ plan.durationWeeks }} tuần</el-tag>
              <el-tag>{{ plan.sessionsPerWeek }} buổi/tuần</el-tag>
              <el-tag v-if="plan.isAiGenerated" type="success">✨ Hệ Thống</el-tag>
            </div>
          </div>
          <el-button type="primary" plain size="small" @click="goalDialog=true">
            🔄 Tạo lại với mục tiêu khác
          </el-button>
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
            <div v-for="ex in day.exercises" :key="ex.id" class="ex-row" @click="openExDetail(ex)">
              <div class="ex-info">
                <div class="ex-name">{{ ex.exerciseName }}</div>
                <div class="ex-sub">{{ muscleLabel(ex.muscleGroup) }} · {{ diffLabel(ex.difficulty) }}</div>
                <div v-if="ex.notes" class="ex-note">{{ ex.notes }}</div>
              </div>
              <div class="ex-meta">
                <div class="ex-sets">
                  <span v-if="ex.reps">{{ ex.sets }}×{{ ex.reps }}</span>
                  <span v-else-if="ex.durationSeconds">{{ ex.sets }}×{{ ex.durationSeconds }}s</span>
                </div>
                <div v-if="ex.restSeconds" style="font-size:0.7rem;color:var(--c-text3)">nghỉ {{ ex.restSeconds }}s</div>
              </div>
              <el-icon style="color:var(--c-text3);font-size:12px;flex-shrink:0"><ArrowRight/></el-icon>
            </div>
          </div>
        </el-card>
      </div>
    </template>

    <!-- ── Dialog chọn mục tiêu AI ─────────────────────────── -->
    <el-dialog v-model="goalDialog" title="TẠO GIÁO ÁN " width="520px" align-center>
      <div style="margin-bottom:20px">
        <div style="font-weight:700;color:var(--c-text);margin-bottom:12px">🎯 Chọn mục tiêu chính</div>
        <div class="goal-grid">
          <div
              v-for="g in goals" :key="g.value"
              class="goal-card"
              :class="{selected: genForm.goal === g.value}"
              @click="genForm.goal = g.value"
          >
            <div class="goal-icon">{{ g.icon }}</div>
            <div class="goal-label">{{ g.label }}</div>
            <div class="goal-desc">{{ g.desc }}</div>
          </div>
        </div>
      </div>

      <el-divider/>

      <div style="margin-bottom:16px">
        <div style="font-weight:700;color:var(--c-text);margin-bottom:10px">⚙️ Tuỳ chỉnh (tuỳ chọn)</div>
        <div class="grid-2">
          <el-form-item label="Trình độ" style="margin-bottom:0">
            <el-select v-model="genForm.fitnessLevel" placeholder="Lấy từ hồ sơ" clearable style="width:100%">
              <el-option label="🌱 Mới bắt đầu" value="BEGINNER"/>
              <el-option label="🔄 Trung bình" value="INTERMEDIATE"/>
              <el-option label="⚡ Nâng cao" value="ADVANCED"/>
            </el-select>
          </el-form-item>
          <el-form-item label="Số ngày/tuần" style="margin-bottom:0">
            <el-select v-model="genForm.daysPerWeek" placeholder="Lấy từ hồ sơ" clearable style="width:100%">
              <el-option v-for="d in [2,3,4,5,6]" :key="d" :label="d + ' ngày'" :value="d"/>
            </el-select>
          </el-form-item>
        </div>
      </div>

      <!-- Info box -->
      <div class="info-box" v-if="genForm.goal">
        <div style="font-weight:700;margin-bottom:6px;color:var(--c-accent)">
          {{ goals.find(g=>g.value===genForm.goal)?.icon }} {{ goals.find(g=>g.value===genForm.goal)?.label }}
        </div>
        <div style="font-size:0.82rem;color:var(--c-text2)">
          {{ goals.find(g=>g.value===genForm.goal)?.aiNote }}
        </div>
      </div>

      <template #footer>
        <el-button @click="goalDialog=false">Hủy</el-button>
        <el-button type="primary" @click="generateWithGoal" :loading="generating" :disabled="!genForm.goal">
          ✨ TẠO GIÁO ÁN NGAY
        </el-button>
      </template>
    </el-dialog>

    <!-- ── Dialog tất cả giáo án ──────────────────────────── -->
    <el-dialog v-model="allPlansDialog" title="TẤT CẢ GIÁO ÁN" width="600px" align-center>
      <div v-if="!allPlans.length" class="empty-state">Chưa có giáo án nào</div>
      <div v-else class="plans-list">
        <div v-for="p in allPlans" :key="p.id" class="plan-item" :class="{active:p.isActive}">
          <div style="flex:1">
            <div style="font-weight:700;color:var(--c-text)">{{ p.planName }}</div>
            <div style="font-size:0.8rem;color:var(--c-text3);margin-top:2px">
              {{ goalLabel(p.goal) }} · {{ levelLabel(p.targetLevel) }} · {{ p.durationWeeks }} tuần
            </div>
          </div>
          <div style="display:flex;gap:6px;align-items:center">
            <el-tag v-if="p.isActive" type="success" size="small">Active</el-tag>
            <el-tag v-if="p.isAiGenerated" size="small">✨ AI</el-tag>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="allPlansDialog=false">Đóng</el-button>
        <el-button type="primary" @click="goalDialog=true;allPlansDialog=false">✨ Tạo giáo án mới</el-button>
      </template>
    </el-dialog>

    <!-- ── Dialog chi tiết bài tập ─────────────────────────── -->
    <el-dialog v-model="exDetailDialog" :title="selEx?.exerciseName" width="540px" align-center v-if="selEx">
      <div v-if="selEx.videoUrl" class="video-wrap">
        <iframe :src="ytEmbed(selEx.videoUrl)" frameborder="0" allowfullscreen
                style="width:100%;height:260px;border-radius:8px"/>
      </div>
      <div v-else class="no-video">📹 Chưa có video hướng dẫn</div>

      <el-descriptions :column="2" border size="small" style="margin-top:14px">
        <el-descriptions-item label="Nhóm cơ">{{ muscleLabel(selEx.muscleGroup) }}</el-descriptions-item>
        <el-descriptions-item label="Độ khó">
          <span class="badge" :class="diffBadge(selEx.difficulty)">{{ diffLabel(selEx.difficulty) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="Sets">{{ selEx.sets }}</el-descriptions-item>
        <el-descriptions-item label="Reps / Thời gian">
          <span v-if="selEx.reps">{{ selEx.reps }} reps</span>
          <span v-else>{{ selEx.durationSeconds }}s</span>
        </el-descriptions-item>
        <el-descriptions-item label="Nghỉ giữa set">{{ selEx.restSeconds || '--' }}s</el-descriptions-item>
        <el-descriptions-item label="Calories/set">{{ selEx.caloriesBurned || '--' }} kcal</el-descriptions-item>
        <el-descriptions-item v-if="selEx.notes" label="Ghi chú" :span="2">
          {{ selEx.notes }}
        </el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="exDetailDialog=false">Đóng</el-button>
        <el-button type="primary" @click="$router.push('/app/exercises');exDetailDialog=false">
          Xem thư viện bài tập
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { planAPI } from '@/api'
import { ElMessage } from 'element-plus'

const plan          = ref(null)
const allPlans      = ref([])
const loading       = ref(true)
const generating    = ref(false)
const goalDialog    = ref(false)
const allPlansDialog= ref(false)
const exDetailDialog= ref(false)
const selEx         = ref(null)

const genForm = reactive({ goal: '', fitnessLevel: null, daysPerWeek: null })

const goals = [
  {
    value:'MUSCLE_GAIN', icon:'💪', label:'Tăng cơ / Sức mạnh',
    desc:'Tăng khối cơ, sức mạnh tối đa',
    aiNote:'AI ưu tiên bài tập compound nặng (Bench Press, Deadlift, Squat) có muscleGainScore cao nhất. Sets ít, reps thấp, tạ nặng.'
  },
  {
    value:'WEIGHT_LOSS', icon:'🔥', label:'Giảm cân / Đốt mỡ',
    desc:'Đốt calories, giảm mỡ thừa',
    aiNote:'AI ưu tiên Cardio và HIIT có weightLossScore cao. Nhiều reps, ít nghỉ để tim đập nhanh và đốt nhiều calories.'
  },
  {
    value:'ENDURANCE', icon:'🏃', label:'Tăng sức bền',
    desc:'Cải thiện tim mạch, dẻo dai',
    aiNote:'AI chọn bài tập Cardio và Full Body có enduranceScore cao. Thời gian dài, cường độ vừa, ít nghỉ.'
  },
  {
    value:'FLEXIBILITY', icon:'🤸', label:'Tăng linh hoạt',
    desc:'Kéo giãn, cải thiện phạm vi chuyển động',
    aiNote:'AI ưu tiên bài tập Yoga, kéo giãn có flexibilityScore cao. Sets ít, giữ lâu, nghỉ ngắn.'
  },
  {
    value:'MAINTENANCE', icon:'⚖️', label:'Duy trì thể hình',
    desc:'Giữ nguyên cân nặng và thể lực',
    aiNote:'AI cân bằng tất cả nhóm cơ theo maintenanceScore. Sets và reps tiêu chuẩn, phù hợp lâu dài.'
  },
]

async function load() {
  loading.value = true
  try {
    const [act, all] = await Promise.all([
      planAPI.getActive().catch(() => ({ data: null })),
      planAPI.getAll()
    ])
    plan.value     = act.data
    allPlans.value = all.data || []
  } finally { loading.value = false }
}

async function generateWithGoal() {
  if (!genForm.goal) { ElMessage.warning('Hãy chọn mục tiêu'); return }
  generating.value = true
  try {
    const payload = {
      goal: genForm.goal,
      fitnessLevel: genForm.fitnessLevel || null,
      daysPerWeek: genForm.daysPerWeek || null
    }
    const r = await planAPI.generateWithGoal(payload)
    plan.value = r.data
    goalDialog.value = false
    ElMessage.success('Giáo án đã được tạo theo mục tiêu ' + genForm.goal + '! 🎉')
    genForm.goal = ''; genForm.fitnessLevel = null; genForm.daysPerWeek = null
    load()
  } catch {} finally { generating.value = false }
}

function openExDetail(ex) { selEx.value = ex; exDetailDialog.value = true }

function ytEmbed(url) {
  const m = (url||'').match(/(?:youtube\.com\/watch\?v=|youtu\.be\/)([\w-]+)/)
  return m ? `https://www.youtube.com/embed/${m[1]}` : url
}

function goalLabel(g)   { return { WEIGHT_LOSS:'🔥 Giảm cân', MUSCLE_GAIN:'💪 Tăng cơ', ENDURANCE:'🏃 Sức bền', FLEXIBILITY:'🤸 Linh hoạt', MAINTENANCE:'⚖️ Duy trì' }[g]||g }
function levelLabel(l)  { return { BEGINNER:'Mới bắt đầu', INTERMEDIATE:'Trung bình', ADVANCED:'Nâng cao' }[l]||l }
function muscleLabel(m) { return { CHEST:'Ngực', BACK:'Lưng', SHOULDERS:'Vai', ARMS:'Tay', LEGS:'Chân', CORE:'Cơ lõi', CARDIO:'Cardio', FULL_BODY:'Toàn thân' }[m]||m }
function diffLabel(d)   { return { EASY:'Dễ', MEDIUM:'Trung bình', HARD:'Khó' }[d]||d }
function diffBadge(d)   { return { EASY:'badge-success', MEDIUM:'badge-warning', HARD:'badge-danger' }[d]||'' }

onMounted(load)
</script>

<style scoped>
.empty-plan {
  text-align:center; padding:80px 40px;
  background:var(--c-card); border-radius:var(--radius-lg); box-shadow:var(--shadow);
}

/* Goal selection */
.goal-grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(140px,1fr)); gap:10px; }
.goal-card {
  border:2px solid var(--c-border2); border-radius:var(--radius-lg); padding:14px 10px;
  text-align:center; cursor:pointer; transition:all var(--transition);
  background:var(--c-card2);
}
.goal-card:hover  { border-color:var(--c-accent); }
.goal-card.selected { border-color:var(--c-accent); background:#FFF8F0; }
.goal-icon  { font-size:1.8rem; margin-bottom:6px; }
.goal-label { font-weight:700; font-size:0.82rem; color:var(--c-text); margin-bottom:3px; }
.goal-desc  { font-size:0.72rem; color:var(--c-text3); }

.info-box {
  padding:12px 14px; background:#FFF8F0;
  border:1px solid var(--c-border); border-radius:var(--radius-lg);
  margin-top:12px;
}

/* Plan days */
.days-grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(280px,1fr)); gap:16px; }
.exercise-list { display:flex; flex-direction:column; gap:6px; }
.ex-row {
  display:flex; align-items:center; gap:10px;
  padding:8px 10px; background:var(--c-card2); border-radius:var(--radius);
  cursor:pointer; transition:background var(--transition);
}
.ex-row:hover { background:#EDE0D0; }
.ex-info { flex:1; min-width:0; }
.ex-name { font-size:0.875rem; font-weight:600; color:var(--c-text); }
.ex-sub  { font-size:0.72rem; color:var(--c-text3); margin-top:1px; }
.ex-note { font-size:0.7rem; color:var(--c-accent); margin-top:2px; }
.ex-meta { text-align:right; flex-shrink:0; }
.ex-sets { font-size:0.82rem; color:var(--c-accent); font-family:var(--font-mono); font-weight:700; }

/* Plans list */
.plans-list { display:flex; flex-direction:column; gap:10px; max-height:400px; overflow-y:auto; }
.plan-item {
  display:flex; align-items:center; gap:12px;
  padding:12px 14px; background:var(--c-card2); border:1px solid var(--c-border2);
  border-radius:var(--radius-lg); transition:border-color var(--transition);
}
.plan-item.active { border-color:var(--c-accent); }

/* Video */
.video-wrap { border-radius:8px; overflow:hidden; }
.no-video { text-align:center; padding:20px; color:var(--c-text3); background:var(--c-card2); border-radius:8px; }
</style>
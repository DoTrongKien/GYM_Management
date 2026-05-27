<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>GIÁO ÁN TẬP</h2>
      <div style="display:flex;gap:8px">
        <el-button @click="allPlansDialog=true" plain>📋 Xem tất cả</el-button>
        <el-button type="primary" @click="generateDialog=true">✨ TẠO GIÁO ÁN AI</el-button>
      </div>
    </div>

    <!-- Empty -->
    <div v-if="!plan && !loading" class="empty-plan">
      <div style="font-size:3.5rem;margin-bottom:12px">🤖</div>
      <h3 class="display" style="font-size:1.8rem;color:var(--c-text);margin-bottom:8px">CHƯA CÓ GIÁO ÁN</h3>
      <p style="color:var(--c-text2);margin-bottom:20px;max-width:420px;margin-inline:auto">
        Hệ thống AI sẽ chọn những bài tập có điểm hiệu quả cao nhất cho mục tiêu của bạn, rồi tạo lịch tập 8 tuần hoàn chỉnh.
      </p>
      <el-button type="primary" size="large" @click="generateDialog=true">✨ TẠO GIÁO ÁN AI NGAY</el-button>
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
              <el-tag v-if="plan.isAiGenerated" type="success">✨ AI</el-tag>
            </div>
          </div>
          <el-button type="primary" plain size="small" @click="generateDialog=true">
            🔄 Tạo giáo án mới
          </el-button>
        </div>
      </el-card>

      <!-- Days grid -->
      <div class="days-grid">
        <el-card v-for="day in plan.planDays" :key="day.id">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span class="display accent" style="font-size:1.1rem">{{ day.dayName }}</span>
              <span style="font-size:0.75rem;color:var(--c-text3)">{{ day.exercises?.length||0 }} bài</span>
            </div>
          </template>
          <div class="exercise-list">
            <div
                v-for="ex in day.exercises" :key="ex.id"
                class="ex-row"
                @click="openExDetail(ex)"
                title="Xem chi tiết"
            >
              <div class="ex-info">
                <div class="ex-name">{{ ex.exerciseName }}</div>
                <div class="ex-sub">{{ muscleLabel(ex.muscleGroup) }} · {{ diffLabel(ex.difficulty) }}</div>
                <div v-if="ex.notes" class="ex-note">{{ ex.notes }}</div>
              </div>
              <div class="ex-right">
                <div class="ex-sets">
                  <span v-if="ex.reps">{{ ex.sets }}×{{ ex.reps }}</span>
                  <span v-else>{{ ex.sets }}×{{ ex.durationSeconds }}s</span>
                </div>
                <el-icon style="color:var(--c-text3);font-size:11px"><ArrowRight/></el-icon>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </template>

    <!-- ── Generate Dialog ── -->
    <el-dialog v-model="generateDialog" title="✨ TẠO GIÁO ÁN AI" width="500px" align-center>
      <div class="gen-intro">
        Chọn mục tiêu và hệ thống sẽ ưu tiên bài tập có <strong>điểm hiệu quả cao nhất</strong> cho mục tiêu đó.
      </div>

      <el-form :model="genForm" label-position="top" style="margin-top:16px">
        <!-- Mục tiêu -->
        <el-form-item label="🎯 Mục tiêu tập luyện">
          <div class="goal-grid">
            <div
                v-for="g in goals" :key="g.value"
                class="goal-card"
                :class="{selected: genForm.goal===g.value}"
                @click="genForm.goal=g.value"
            >
              <div class="goal-icon">{{ g.icon }}</div>
              <div class="goal-name">{{ g.label }}</div>
              <div class="goal-desc">{{ g.desc }}</div>
            </div>
          </div>
        </el-form-item>

        <!-- Trình độ -->
        <el-form-item label="📊 Trình độ hiện tại">
          <div class="level-row">
            <div
                v-for="l in levels" :key="l.value"
                class="level-card"
                :class="{selected: genForm.fitnessLevel===l.value}"
                @click="genForm.fitnessLevel=l.value"
            >
              <div>{{ l.icon }} {{ l.label }}</div>
              <div class="level-desc">{{ l.desc }}</div>
            </div>
          </div>
        </el-form-item>

        <!-- Số ngày -->
        <el-form-item label="📅 Số ngày tập mỗi tuần">
          <el-slider v-model="genForm.availableDaysPerWeek" :min="2" :max="6" :step="1"
                     :marks="{2:'2',3:'3',4:'4',5:'5',6:'6'}" show-stops style="padding:0 8px"/>
          <div style="text-align:center;margin-top:8px;color:var(--c-accent);font-family:var(--font-display);font-size:1.2rem">
            {{ genForm.availableDaysPerWeek }} ngày / tuần
          </div>
        </el-form-item>

        <!-- Preview điểm ưu tiên -->
        <div class="score-preview" v-if="genForm.goal">
          <div class="score-title">Bài tập được ưu tiên theo điểm:</div>
          <div class="score-bars">
            <div v-for="s in scorePriority" :key="s.label" class="score-row">
              <span class="score-label">{{ s.label }}</span>
              <div class="score-bar-wrap">
                <div class="score-bar" :style="{width: s.pct+'%', background: s.color}"></div>
              </div>
              <span class="score-val">{{ s.val }}/10</span>
            </div>
          </div>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="generateDialog=false">Hủy</el-button>
        <el-button type="primary" @click="generate" :loading="generating" :disabled="!genForm.goal">
          ✨ TẠO GIÁO ÁN
        </el-button>
      </template>
    </el-dialog>

    <!-- ── All Plans Dialog ── -->
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
            <el-tag v-if="p.isAiGenerated" size="small">AI</el-tag>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="allPlansDialog=false">Đóng</el-button>
        <el-button type="primary" @click="allPlansDialog=false;generateDialog=true">✨ Tạo giáo án mới</el-button>
      </template>
    </el-dialog>

    <!-- ── Exercise Detail Dialog ── -->
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
        <el-descriptions-item label="Nghỉ giữa sets">{{ selEx.restSeconds }}s</el-descriptions-item>
        <el-descriptions-item label="Calories/set">{{ selEx.caloriesBurned || '--' }} kcal</el-descriptions-item>
      </el-descriptions>

      <div v-if="selEx.notes" class="ex-note-box">{{ selEx.notes }}</div>

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
import { ref, reactive, computed, onMounted } from 'vue'
import { planAPI } from '@/api'
import { ElMessage } from 'element-plus'

const plan          = ref(null)
const allPlans      = ref([])
const loading       = ref(true)
const generating    = ref(false)
const generateDialog= ref(false)
const allPlansDialog= ref(false)
const exDetailDialog= ref(false)
const selEx         = ref(null)

const genForm = reactive({
  goal: '',
  fitnessLevel: 'BEGINNER',
  availableDaysPerWeek: 3
})

const goals = [
  { value:'MUSCLE_GAIN', icon:'💪', label:'Tăng cơ',    desc:'Ưu tiên bài tập compound nặng' },
  { value:'WEIGHT_LOSS', icon:'🔥', label:'Giảm cân',   desc:'Ưu tiên cardio và HIIT' },
  { value:'ENDURANCE',   icon:'🏃', label:'Sức bền',    desc:'Ưu tiên bài tập tim mạch' },
  { value:'FLEXIBILITY', icon:'🤸', label:'Linh hoạt',  desc:'Ưu tiên giãn cơ và yoga' },
  { value:'MAINTENANCE', icon:'⚖️', label:'Duy trì',    desc:'Cân bằng toàn bộ nhóm cơ' },
]

const levels = [
  { value:'BEGINNER',     icon:'🌱', label:'Mới bắt đầu',  desc:'< 6 tháng' },
  { value:'INTERMEDIATE', icon:'🔄', label:'Trung bình',   desc:'6T - 2 năm' },
  { value:'ADVANCED',     icon:'⚡', label:'Nâng cao',     desc:'> 2 năm' },
]

// Hiển thị preview điểm ưu tiên theo mục tiêu chọn
const scoreExamples = {
  MUSCLE_GAIN:  [ {label:'Bench Press',  val:10,color:'#D4892A'}, {label:'Deadlift',    val:10,color:'#D4892A'}, {label:'Squat',       val:10,color:'#D4892A'}, {label:'Jump Rope',  val:2,color:'#C49A6C'} ],
  WEIGHT_LOSS:  [ {label:'Burpee',       val:10,color:'#D4892A'}, {label:'Treadmill',   val:10,color:'#D4892A'}, {label:'Jump Rope',   val:9,color:'#D4892A'},  {label:'Bench Press',val:5,color:'#C49A6C'} ],
  ENDURANCE:    [ {label:'Treadmill',    val:10,color:'#D4892A'}, {label:'Jump Rope',   val:9,color:'#D4892A'},  {label:'Cycling',     val:9,color:'#D4892A'},  {label:'Bicep Curl', val:4,color:'#C49A6C'} ],
  FLEXIBILITY:  [ {label:'Yoga Cat-Cow', val:10,color:'#D4892A'}, {label:'Hip Stretch', val:10,color:'#D4892A'}, {label:'Face Pull',   val:5,color:'#C49A6C'},  {label:'Deadlift',   val:3,color:'#C49A6C'} ],
  MAINTENANCE:  [ {label:'Squat',        val:9,color:'#D4892A'},  {label:'Pull Up',     val:8,color:'#D4892A'},  {label:'Treadmill',   val:7,color:'#D4892A'},  {label:'Plank',      val:7,color:'#C49A6C'} ],
}

const scorePriority = computed(() => {
  if (!genForm.goal) return []
  return (scoreExamples[genForm.goal] || []).map(s => ({
    ...s, pct: s.val * 10
  }))
})

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

async function generate() {
  if (!genForm.goal) { ElMessage.warning('Chọn mục tiêu tập luyện'); return }
  generating.value = true
  try {
    const r = await planAPI.generateWithGoal({
      goal:                genForm.goal,
      fitnessLevel:        genForm.fitnessLevel,
      availableDaysPerWeek:genForm.availableDaysPerWeek
    })
    plan.value = r.data
    generateDialog.value = false
    ElMessage.success('Giáo án AI đã tạo xong! Bài tập được chọn theo điểm tối ưu 🎉')
    load()
  } catch {} finally { generating.value = false }
}

function openExDetail(ex) { selEx.value = ex; exDetailDialog.value = true }

function ytEmbed(url) {
  const m = (url||'').match(/(?:youtube\.com\/watch\?v=|youtu\.be\/)([\w-]+)/)
  return m ? `https://www.youtube.com/embed/${m[1]}` : url
}

function goalLabel(g)   { return goals.find(x=>x.value===g)?.label || g }
function levelLabel(l)  { return levels.find(x=>x.value===l)?.label || l }
function muscleLabel(m) { return { CHEST:'Ngực',BACK:'Lưng',SHOULDERS:'Vai',ARMS:'Tay',LEGS:'Chân',CORE:'Cơ lõi',CARDIO:'Cardio',FULL_BODY:'Toàn thân' }[m]||m }
function diffLabel(d)   { return { EASY:'Dễ',MEDIUM:'Trung bình',HARD:'Khó' }[d]||d }
function diffBadge(d)   { return { EASY:'badge-success',MEDIUM:'badge-warning',HARD:'badge-danger' }[d]||'' }

onMounted(load)
</script>

<style scoped>
.empty-plan { text-align:center; padding:80px 40px; background:var(--c-card); border-radius:var(--radius-lg); box-shadow:var(--shadow); }

.days-grid  { display:grid; grid-template-columns:repeat(auto-fill,minmax(280px,1fr)); gap:16px; }
.exercise-list { display:flex; flex-direction:column; gap:6px; }
.ex-row {
  display:flex; align-items:center; gap:8px;
  padding:8px 10px; background:var(--c-card2); border-radius:var(--radius);
  cursor:pointer; transition:background var(--transition);
}
.ex-row:hover { background:#EDE0D0; }
.ex-info { flex:1; min-width:0; }
.ex-name { font-size:0.875rem; font-weight:600; color:var(--c-text); }
.ex-sub  { font-size:0.72rem; color:var(--c-text3); margin-top:1px; }
.ex-note { font-size:0.7rem; color:var(--c-accent); margin-top:2px; }
.ex-right { display:flex; align-items:center; gap:6px; flex-shrink:0; }
.ex-sets { font-size:0.8rem; color:var(--c-accent); font-family:var(--font-mono); font-weight:700; }

/* Generate dialog */
.gen-intro {
  background:#FFF8F0; border:1px solid #F0D9B5; border-radius:var(--radius);
  padding:10px 14px; font-size:0.85rem; color:var(--c-text2);
}
.goal-grid {
  display:grid; grid-template-columns:repeat(auto-fill,minmax(130px,1fr)); gap:10px;
}
.goal-card {
  background:var(--c-card2); border:2px solid var(--c-border2); border-radius:var(--radius-lg);
  padding:12px 10px; text-align:center; cursor:pointer; transition:all var(--transition);
}
.goal-card:hover   { border-color:var(--c-accent); }
.goal-card.selected{ border-color:var(--c-accent); background:#FFF0DC; }
.goal-icon { font-size:1.6rem; margin-bottom:4px; }
.goal-name { font-weight:700; font-size:0.85rem; color:var(--c-text); }
.goal-desc { font-size:0.72rem; color:var(--c-text3); margin-top:2px; line-height:1.3; }

.level-row { display:flex; gap:10px; }
.level-card {
  flex:1; background:var(--c-card2); border:2px solid var(--c-border2);
  border-radius:var(--radius-lg); padding:10px 8px; text-align:center;
  cursor:pointer; transition:all var(--transition); font-size:0.85rem; font-weight:600;
}
.level-card:hover   { border-color:var(--c-accent); }
.level-card.selected{ border-color:var(--c-accent); background:#FFF0DC; }
.level-desc { font-size:0.7rem; color:var(--c-text3); margin-top:3px; font-weight:400; }

/* Score preview */
.score-preview {
  background:var(--c-card2); border:1px solid var(--c-border2);
  border-radius:var(--radius-lg); padding:12px 14px; margin-top:4px;
}
.score-title { font-size:0.78rem; font-weight:700; color:var(--c-text2); margin-bottom:10px; text-transform:uppercase; letter-spacing:0.06em; }
.score-bars  { display:flex; flex-direction:column; gap:8px; }
.score-row   { display:flex; align-items:center; gap:8px; }
.score-label { font-size:0.78rem; color:var(--c-text2); width:120px; flex-shrink:0; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.score-bar-wrap { flex:1; background:var(--c-border2); border-radius:4px; height:8px; overflow:hidden; }
.score-bar   { height:100%; border-radius:4px; transition:width 0.5s ease; }
.score-val   { font-size:0.75rem; font-family:var(--font-mono); color:var(--c-text3); width:36px; text-align:right; flex-shrink:0; }

/* All plans list */
.plans-list { display:flex; flex-direction:column; gap:10px; max-height:380px; overflow-y:auto; }
.plan-item {
  display:flex; align-items:center; gap:12px;
  padding:12px 14px; background:var(--c-card2); border:1px solid var(--c-border2);
  border-radius:var(--radius-lg); transition:border-color var(--transition);
}
.plan-item.active { border-color:var(--c-accent); }

/* Exercise detail */
.video-wrap { border-radius:8px; overflow:hidden; }
.no-video { text-align:center; padding:20px; color:var(--c-text3); background:var(--c-card2); border-radius:8px; }
.ex-note-box {
  margin-top:12px; padding:10px 14px;
  background:#FFF8F0; border:1px solid #F0D9B5; border-radius:var(--radius);
  font-size:0.85rem; color:var(--c-accent); font-weight:600;
}
</style>
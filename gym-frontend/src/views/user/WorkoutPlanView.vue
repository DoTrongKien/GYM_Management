<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>GIÁO ÁN TẬP</h2>
      <el-button type="primary" @click="generate" :loading="generating">
        ✨ TẠO GIÁO ÁN AI
      </el-button>
    </div>

    <div v-if="!plan && !loading" class="empty-plan">
      <div class="empty-icon">🤖</div>
      <h3 class="display">CHƯA CÓ GIÁO ÁN</h3>
      <p class="muted">Hãy hoàn thiện hồ sơ rồi nhấn "Tạo giáo án AI" để hệ thống tự động tạo lịch tập 8 tuần phù hợp với bạn.</p>
      <el-button type="primary" @click="generate" :loading="generating" style="margin-top:16px">
        ✨ TẠO NGAY
      </el-button>
    </div>

    <div v-if="loading" style="padding:40px 0"><el-skeleton :rows="8" animated /></div>

    <template v-if="plan && !loading">
      <!-- Plan Info -->
      <div class="plan-header">
        <div>
          <div class="plan-title display">{{ plan.planName }}</div>
          <div class="plan-desc muted">{{ plan.description }}</div>
          <div class="plan-tags" style="margin-top:8px">
            <el-tag size="small" type="warning">{{ plan.goal }}</el-tag>
            <el-tag size="small" type="info">{{ plan.targetLevel }}</el-tag>
            <el-tag size="small">{{ plan.durationWeeks }} tuần</el-tag>
            <el-tag size="small">{{ plan.sessionsPerWeek }} buổi/tuần</el-tag>
            <el-tag v-if="plan.isAiGenerated" size="small" type="success">✨ AI</el-tag>
          </div>
        </div>
      </div>

      <!-- Days -->
      <div class="days-grid">
        <el-card v-for="day in plan.planDays" :key="day.id" class="day-card">
          <template #header>
            <div class="day-header">
              <span class="day-name display accent">{{ day.dayName }}</span>
              <span class="muted mono" style="font-size:0.75rem">{{ day.exercises?.length || 0 }} bài</span>
            </div>
          </template>

          <div class="exercise-list">
            <div v-for="ex in day.exercises" :key="ex.id" class="exercise-row">
              <div class="ex-info">
                <div class="ex-name">{{ ex.exerciseName }}</div>
                <div class="ex-muscle muted">{{ ex.muscleGroup }} · {{ ex.difficulty }}</div>
              </div>
              <div class="ex-sets mono">
                <span v-if="ex.reps">{{ ex.sets }}×{{ ex.reps }}</span>
                <span v-else>{{ ex.sets }}×{{ ex.durationSeconds }}s</span>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { planAPI } from '@/api'
import { ElMessage } from 'element-plus'

const plan       = ref(null)
const loading    = ref(true)
const generating = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await planAPI.getActive()
    plan.value = res.data
  } catch { plan.value = null }
  finally { loading.value = false }
}

async function generate() {
  generating.value = true
  try {
    const res = await planAPI.generate()
    plan.value = res.data
    ElMessage.success('Giáo án AI đã được tạo thành công! 🎉')
  } catch {} finally {
    generating.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.empty-plan {
  text-align:center; padding:80px 40px;
  background:var(--c-bg2); border:1px solid var(--c-border); border-radius:var(--radius-lg);
}
.empty-icon { font-size:4rem; margin-bottom:16px; }

.plan-header {
  background:var(--c-bg2); border:1px solid var(--c-accent);
  border-radius:var(--radius-lg); padding:20px 24px; margin-bottom:24px;
}
.plan-title { font-size:1.8rem; margin-bottom:4px; }
.plan-tags { display:flex; gap:8px; flex-wrap:wrap; }

.days-grid {
  display:grid; grid-template-columns:repeat(auto-fill, minmax(280px, 1fr)); gap:16px;
}

.day-header { display:flex; justify-content:space-between; align-items:center; }
.day-name   { font-size:1.1rem; }

.exercise-list { display:flex; flex-direction:column; gap:8px; }
.exercise-row {
  display:flex; justify-content:space-between; align-items:center;
  padding:8px; background:var(--c-bg3); border-radius:var(--radius);
}
.ex-name   { font-size:0.875rem; font-weight:500; }
.ex-muscle { font-size:0.75rem; margin-top:2px; }
.ex-sets   { font-size:0.8rem; color:var(--c-accent); white-space:nowrap; }
</style>

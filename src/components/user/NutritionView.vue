<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>DINH DƯỠNG</h2>
      <el-button type="primary" @click="generate" :loading="generating">✨ TẠO KẾ HOẠCH AI</el-button>
    </div>

    <div v-if="loading" style="padding:40px 0"><el-skeleton :rows="6" animated /></div>

    <template v-else-if="plan">
      <!-- Macros -->
      <div class="grid-4" style="margin-bottom:24px">
        <div class="stat-card accent-card">
          <div class="label">CALORIES / NGÀY</div>
          <div class="value">{{ plan.dailyCalories }}</div>
          <div class="sub">kcal</div>
        </div>
        <div class="stat-card">
          <div class="label">PROTEIN</div>
          <div class="value">{{ plan.proteinGrams }}</div>
          <div class="sub">gram</div>
        </div>
        <div class="stat-card">
          <div class="label">CARBS</div>
          <div class="value">{{ plan.carbsGrams }}</div>
          <div class="sub">gram</div>
        </div>
        <div class="stat-card">
          <div class="label">CHẤT BÉO</div>
          <div class="value">{{ plan.fatGrams }}</div>
          <div class="sub">gram</div>
        </div>
      </div>

      <!-- Meal suggestions -->
      <el-card header="GỢI Ý BỮA ĂN" v-if="meals">
        <div class="meal-grid">
          <div class="meal-item" v-for="(val, key) in meals" :key="key">
            <div class="meal-label display">{{ mealLabel(key) }}</div>
            <div class="meal-content">{{ val }}</div>
          </div>
        </div>
      </el-card>
    </template>

    <div v-else class="empty-plan">
      <div class="empty-icon">🥗</div>
      <h3 class="display">CHƯA CÓ KẾ HOẠCH DINH DƯỠNG</h3>
      <p class="muted">Hệ thống AI sẽ tính toán calories và macro tối ưu dựa trên hồ sơ và mục tiêu của bạn.</p>
      <el-button type="primary" @click="generate" :loading="generating" style="margin-top:16px">✨ TẠO NGAY</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { nutritionAPI } from '@/api'
import { ElMessage } from 'element-plus'

const plan = ref(null); const loading = ref(true); const generating = ref(false)
const meals = computed(() => { try { return JSON.parse(plan.value?.mealSuggestions || 'null') } catch { return null } })

async function load() {
  loading.value = true
  try { const r = await nutritionAPI.getLatest(); plan.value = r.data } catch { plan.value = null }
  finally { loading.value = false }
}
async function generate() {
  generating.value = true
  try { const r = await nutritionAPI.generate(); plan.value = r.data; ElMessage.success('Kế hoạch dinh dưỡng đã được tạo! 🥗') }
  catch {} finally { generating.value = false }
}
function mealLabel(k) {
  return { breakfast:'SÁNG', lunch:'TRƯA', dinner:'TỐI', snacks:'PHỤ' }[k] || k.toUpperCase()
}
onMounted(load)
</script>

<style scoped>
.empty-plan { text-align:center; padding:80px 40px; background:var(--c-bg2); border:1px solid var(--c-border); border-radius:var(--radius-lg); }
.empty-icon { font-size:4rem; margin-bottom:16px; }
.meal-grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(220px,1fr)); gap:16px; }
.meal-item { background:var(--c-bg3); border-radius:var(--radius-lg); padding:16px; }
.meal-label { font-size:0.9rem; color:var(--c-accent); margin-bottom:8px; }
.meal-content { font-size:0.85rem; color:var(--c-text2); line-height:1.5; }
</style>
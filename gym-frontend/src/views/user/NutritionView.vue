<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>DINH DƯỠNG</h2>
      <el-button type="primary" @click="generate" :loading="generating">✨ TẠO KẾ HOẠCH AI</el-button>
    </div>

    <div v-if="loading"><el-skeleton :rows="6" animated style="background:var(--c-card);padding:24px;border-radius:12px"/></div>

    <template v-else-if="plan">
      <!-- Macro cards -->
      <div class="grid-4" style="margin-bottom:24px">
        <div class="stat-card accent-card">
          <div class="label">CALORIES / NGÀY</div>
          <div class="value">{{ plan.dailyCalories?.toLocaleString() }}</div>
          <div class="sub">kcal</div>
          <div class="icon">🔥</div>
        </div>
        <div class="stat-card">
          <div class="label">PROTEIN</div>
          <div class="value">{{ plan.proteinGrams }}</div>
          <div class="sub">gram/ngày</div>
          <div class="icon">🥩</div>
        </div>
        <div class="stat-card">
          <div class="label">CARBS</div>
          <div class="value">{{ plan.carbsGrams }}</div>
          <div class="sub">gram/ngày</div>
          <div class="icon">🍚</div>
        </div>
        <div class="stat-card">
          <div class="label">CHẤT BÉO</div>
          <div class="value">{{ plan.fatGrams }}</div>
          <div class="sub">gram/ngày</div>
          <div class="icon">🥑</div>
        </div>
      </div>

      <!-- Macro ratios -->
      <el-card style="margin-bottom:24px">
        <template #header>TỶ LỆ MACRO</template>
        <div class="macro-bar">
          <div class="macro-seg protein" :style="{width: proteinPct+'%'}">
            Protein {{ proteinPct }}%
          </div>
          <div class="macro-seg carbs" :style="{width: carbsPct+'%'}">
            Carbs {{ carbsPct }}%
          </div>
          <div class="macro-seg fat" :style="{width: fatPct+'%'}">
            Fat {{ fatPct }}%
          </div>
        </div>
      </el-card>

      <!-- Meal suggestions -->
      <el-card header="GỢI Ý BỮA ĂN" v-if="meals" style="margin-bottom:24px">
        <div class="meal-grid">
          <div class="meal-card" v-for="(val,key) in meals" :key="key">
            <div class="meal-icon">{{ mealIcon(key) }}</div>
            <div class="meal-label display">{{ mealLabel(key) }}</div>
            <div class="meal-content">{{ val }}</div>
          </div>
        </div>
      </el-card>

      <!-- Custom food log -->
      <el-card>
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <span>NHẬT KÝ ĂN UỐNG HÔM NAY</span>
            <el-button type="primary" size="small" @click="addFoodDialog=true">+ Thêm món ăn</el-button>
          </div>
        </template>

        <div v-if="!foodLog.length" class="empty-state" style="padding:24px">Chưa ghi nhận món ăn nào hôm nay</div>
        <div v-else>
          <el-table :data="foodLog" stripe>
            <el-table-column label="Món ăn" prop="name" min-width="160"/>
            <el-table-column label="Calories" prop="calories" width="100" align="right">
              <template #default="{row}">{{ row.calories }} kcal</template>
            </el-table-column>
            <el-table-column label="Protein" prop="protein" width="90" align="right">
              <template #default="{row}">{{ row.protein }}g</template>
            </el-table-column>
            <el-table-column label="Carbs" prop="carbs" width="90" align="right">
              <template #default="{row}">{{ row.carbs }}g</template>
            </el-table-column>
            <el-table-column label="Fat" prop="fat" width="80" align="right">
              <template #default="{row}">{{ row.fat }}g</template>
            </el-table-column>
            <el-table-column width="60" align="center">
              <template #default="{row}">
                <el-button text type="danger" size="small" @click="removeFood(row)">✕</el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- Totals -->
          <div class="food-totals">
            <span>Tổng: <strong>{{ totalCal }} kcal</strong></span>
            <span>Còn lại: <strong :style="{color: remaining<0?'var(--c-danger)':'var(--c-success)'}">{{ remaining }} kcal</strong></span>
            <span>P: <strong>{{ totalProtein }}g</strong></span>
            <span>C: <strong>{{ totalCarbs }}g</strong></span>
            <span>F: <strong>{{ totalFat }}g</strong></span>
          </div>
        </div>
      </el-card>
    </template>

    <div v-else class="empty-plan">
      <div style="font-size:3rem;margin-bottom:12px">🥗</div>
      <h3 class="display" style="font-size:1.6rem;color:var(--c-text);margin-bottom:8px">CHƯA CÓ KẾ HOẠCH</h3>
      <p style="color:var(--c-text2);margin-bottom:20px">AI sẽ tính calories và macro tối ưu dựa trên hồ sơ và mục tiêu của bạn.</p>
      <el-button type="primary" size="large" @click="generate" :loading="generating">✨ TẠO NGAY</el-button>
    </div>

    <!-- Add food dialog -->
    <el-dialog v-model="addFoodDialog" title="THÊM MÓN ĂN" width="440px" align-center>
      <el-form :model="foodForm" label-position="top">
        <el-form-item label="Tên món ăn">
          <el-input v-model="foodForm.name" placeholder="VD: Cơm trắng 200g"/>
        </el-form-item>
        <div class="grid-2">
          <el-form-item label="Calories (kcal)">
            <el-input-number v-model="foodForm.calories" :min="0" :max="5000" style="width:100%"/>
          </el-form-item>
          <el-form-item label="Protein (g)">
            <el-input-number v-model="foodForm.protein" :min="0" :max="500" style="width:100%"/>
          </el-form-item>
          <el-form-item label="Carbs (g)">
            <el-input-number v-model="foodForm.carbs" :min="0" :max="500" style="width:100%"/>
          </el-form-item>
          <el-form-item label="Chất béo (g)">
            <el-input-number v-model="foodForm.fat" :min="0" :max="300" style="width:100%"/>
          </el-form-item>
        </div>
        <!-- Quick estimate note -->
        <div style="background:#FFF8F0;border:1px solid #F0D9B5;border-radius:6px;padding:10px;font-size:0.8rem;color:var(--c-text2)">
          💡 Ước lượng: Cơm trắng 200g ≈ 260kcal | Ức gà 100g ≈ 165kcal | Trứng 1 quả ≈ 70kcal
        </div>
      </el-form>
      <template #footer>
        <el-button @click="addFoodDialog=false">Hủy</el-button>
        <el-button type="primary" @click="addFood">THÊM</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { nutritionAPI } from '@/api'
import { ElMessage } from 'element-plus'

const plan         = ref(null)
const loading      = ref(true)
const generating   = ref(false)
const addFoodDialog= ref(false)
const foodLog      = ref([])

const foodForm = reactive({ name:'', calories:0, protein:0, carbs:0, fat:0 })

const meals = computed(() => { try { return JSON.parse(plan.value?.mealSuggestions||'null') } catch { return null } })

const totalCal  = computed(() => foodLog.value.reduce((s,f)=>s+f.calories,0))
const totalProtein = computed(() => foodLog.value.reduce((s,f)=>s+f.protein,0))
const totalCarbs   = computed(() => foodLog.value.reduce((s,f)=>s+f.carbs,0))
const totalFat     = computed(() => foodLog.value.reduce((s,f)=>s+f.fat,0))
const remaining    = computed(() => (plan.value?.dailyCalories||0) - totalCal.value)

const totalMacroG  = computed(() => {
  const p = (plan.value?.proteinGrams||0)*4
  const c = (plan.value?.carbsGrams||0)*4
  const f = (plan.value?.fatGrams||0)*9
  return p+c+f || 1
})
const proteinPct = computed(()=> Math.round(((plan.value?.proteinGrams||0)*4/totalMacroG.value)*100))
const carbsPct   = computed(()=> Math.round(((plan.value?.carbsGrams||0)*4/totalMacroG.value)*100))
const fatPct     = computed(()=> 100-proteinPct.value-carbsPct.value)

async function load() {
  loading.value = true
  try { const r = await nutritionAPI.getLatest(); plan.value = r.data } catch { plan.value = null }
  finally { loading.value = false }
}
async function generate() {
  generating.value = true
  try { const r = await nutritionAPI.generate(); plan.value = r.data; ElMessage.success('Kế hoạch dinh dưỡng đã tạo! 🥗') }
  catch {} finally { generating.value = false }
}
function addFood() {
  if (!foodForm.name) { ElMessage.warning('Nhập tên món ăn'); return }
  foodLog.value.push({ ...foodForm })
  Object.assign(foodForm, { name:'', calories:0, protein:0, carbs:0, fat:0 })
  addFoodDialog.value = false
  // Reminder based on goal
  if (remaining.value < 0) ElMessage.warning('⚠️ Đã vượt quá calories mục tiêu hôm nay!')
  else if (remaining.value < 200) ElMessage.info('💡 Còn ' + remaining.value + ' kcal — hãy ăn nhẹ nhàng thôi!')
}
function removeFood(row) { foodLog.value = foodLog.value.filter(f=>f!==row) }
function mealLabel(k) { return { breakfast:'BUỔI SÁNG', lunch:'BUỔI TRƯA', dinner:'BUỔI TỐI', snacks:'ĂN VẶT' }[k]||k.toUpperCase() }
function mealIcon(k)  { return { breakfast:'🌅', lunch:'☀️', dinner:'🌙', snacks:'🍎' }[k]||'🍽️' }

onMounted(load)
</script>

<style scoped>
.empty-plan { text-align:center; padding:80px 40px; background:var(--c-card); border-radius:var(--radius-lg); box-shadow:var(--shadow); }
.meal-grid  { display:grid; grid-template-columns:repeat(auto-fill,minmax(200px,1fr)); gap:14px; }
.meal-card  { background:var(--c-card2); border-radius:var(--radius-lg); padding:16px; border:1px solid var(--c-border2); }
.meal-icon  { font-size:2rem; margin-bottom:6px; }
.meal-label { font-size:0.85rem; color:var(--c-accent); margin-bottom:6px; }
.meal-content { font-size:0.82rem; color:var(--c-text2); line-height:1.5; }

.macro-bar  { display:flex; border-radius:var(--radius-lg); overflow:hidden; height:40px; }
.macro-seg  { display:flex; align-items:center; justify-content:center;
  font-size:0.78rem; font-weight:700; color:#fff; transition:width 0.5s; min-width:30px; white-space:nowrap; overflow:hidden; padding:0 6px; }
.macro-seg.protein { background:#D4892A; }
.macro-seg.carbs   { background:#6B4226; }
.macro-seg.fat     { background:#C49A6C; }

.food-totals { display:flex; gap:20px; flex-wrap:wrap; margin-top:14px; padding:12px 16px;
  background:var(--c-card2); border-radius:var(--radius-lg); font-size:0.875rem; color:var(--c-text2); }
</style>
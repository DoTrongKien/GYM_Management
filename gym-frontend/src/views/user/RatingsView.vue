<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>ĐÁNH GIÁ DỊCH VỤ</h2>
      <el-button type="primary" @click="addDialog = true">+ VIẾT ĐÁNH GIÁ</el-button>
    </div>

    <!-- Average scores -->
    <div class="grid-4" style="margin-bottom:24px">
      <div class="stat-card" v-for="(val, key) in averages" :key="key">
        <div class="label">{{ serviceLabel(key) }}</div>
        <div class="value accent">{{ val || '—' }}</div>
        <div style="margin-top:6px">
          <el-rate :model-value="Number(val)" disabled :max="5" size="small" />
        </div>
      </div>
    </div>

    <!-- Public ratings -->
    <el-card header="ĐÁNH GIÁ TỪ CỘNG ĐỒNG">
      <div class="ratings-list">
        <div v-for="r in publicRatings" :key="r.id" class="rating-item">
          <div class="rating-header">
            <div class="rating-user">
              <div class="avatar">{{ (r.user?.fullName || 'U')[0] }}</div>
              <div>
                <div class="name">{{ r.user?.fullName || 'Ẩn danh' }}</div>
                <div class="service-type muted">{{ serviceLabel(r.serviceType) }}</div>
              </div>
            </div>
            <div class="rating-score">
              <el-rate :model-value="r.rating" disabled :max="5" size="small" />
            </div>
          </div>
          <div class="rating-comment" v-if="r.comment">{{ r.comment }}</div>
          <div class="rating-date muted mono">{{ formatDate(r.createdAt) }}</div>
        </div>
        <div v-if="publicRatings.length === 0" class="empty-state">Chưa có đánh giá nào</div>
      </div>
    </el-card>

    <!-- Add Dialog -->
    <el-dialog v-model="addDialog" title="VIẾT ĐÁNH GIÁ" width="460px">
      <el-form :model="form" label-position="top">
        <el-form-item label="Dịch vụ">
          <el-select v-model="form.serviceType" style="width:100%">
            <el-option label="📋 Giáo án tập" value="WORKOUT_PLAN" />
            <el-option label="🥗 Dinh dưỡng" value="NUTRITION" />
            <el-option label="🏟️ Cơ sở vật chất" value="FACILITY" />
            <el-option label="👟 Huấn luyện viên" value="TRAINER" />
          </el-select>
        </el-form-item>
        <el-form-item label="Đánh giá">
          <el-rate v-model="form.rating" :max="5" size="large" />
        </el-form-item>
        <el-form-item label="Nhận xét">
          <el-input v-model="form.comment" type="textarea" :rows="3" placeholder="Chia sẻ trải nghiệm của bạn..." />
        </el-form-item>
        <el-form-item label="Hiển thị công khai">
          <el-switch v-model="form.isPublic" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialog = false">Hủy</el-button>
        <el-button type="primary" @click="submit">GỬI ĐÁNH GIÁ</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ratingAPI } from '@/api'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const publicRatings = ref([])
const averages      = ref({})
const addDialog     = ref(false)
const form = reactive({ serviceType: 'WORKOUT_PLAN', rating: 5, comment: '', isPublic: true })

async function load() {
  try {
    const [pub, avg] = await Promise.all([ratingAPI.getPublic(), ratingAPI.getAverages()])
    publicRatings.value = pub.data || []
    averages.value      = avg.data || {}
  } catch {}
}

async function submit() {
  if (!form.rating) { ElMessage.warning('Vui lòng chọn số sao'); return }
  await ratingAPI.add(form)
  ElMessage.success('Cảm ơn bạn đã đánh giá! ⭐')
  addDialog.value = false
  load()
}

function serviceLabel(s) {
  return { WORKOUT_PLAN:'Giáo án', NUTRITION:'Dinh dưỡng', FACILITY:'Cơ sở', TRAINER:'HLV' }[s] || s
}
function formatDate(d) { return dayjs(d).format('DD/MM/YYYY') }

onMounted(load)
</script>

<style scoped>
.ratings-list { display:flex; flex-direction:column; gap:16px; }
.rating-item {
  padding:16px; background:var(--c-bg3); border-radius:var(--radius-lg);
  border-left:3px solid var(--c-accent);
}
.rating-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:10px; }
.rating-user   { display:flex; gap:10px; align-items:center; }
.avatar {
  width:36px; height:36px; border-radius:50%; background:var(--c-accent);
  color:#000; display:flex; align-items:center; justify-content:center;
  font-family:var(--font-display); font-size:1rem; flex-shrink:0;
}
.name         { font-weight:600; font-size:0.875rem; }
.service-type { font-size:0.75rem; }
.rating-comment { font-size:0.875rem; color:var(--c-text2); margin-bottom:8px; line-height:1.5; }
.rating-date    { font-size:0.72rem; }
.empty-state { text-align:center; padding:40px; color:var(--c-text3); }
</style>

<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>ĐÁNH GIÁ DỊCH VỤ</h2>
      <el-button type="primary" @click="addDialog=true">+ Viết đánh giá</el-button>
    </div>

    <!-- Averages -->
    <div class="grid-4" style="margin-bottom:24px">
      <div class="stat-card" v-for="(val,key) in averages" :key="key">
        <div class="label">{{ serviceLabel(key) }}</div>
        <div class="value accent">{{ val || '—' }}</div>
        <el-rate :model-value="Number(val)" disabled :max="5" size="small" style="margin-top:6px"/>
      </div>
    </div>

    <!-- My ratings -->
    <el-card header="ĐÁNH GIÁ CỦA TÔI" style="margin-bottom:20px" v-if="myRatings.length">
      <div class="ratings-list">
        <div v-for="r in myRatings" :key="r.id" class="rating-item my">
          <div class="rating-top">
            <div>
              <span class="badge badge-info" style="margin-right:8px">{{ serviceLabel(r.serviceType) }}</span>
              <el-rate :model-value="r.rating" disabled size="small" style="display:inline-flex"/>
            </div>
            <span class="muted" style="font-size:0.75rem">{{ fmtDate(r.createdAt) }}</span>
          </div>
          <div class="rating-comment" v-if="r.comment">{{ r.comment }}</div>
          <!-- Admin reply -->
          <div v-if="r.adminReply" class="admin-reply">
            <span class="reply-label">💬 Admin:</span> {{ r.adminReply }}
          </div>
        </div>
      </div>
    </el-card>

    <!-- Public ratings -->
    <el-card header="ĐÁNH GIÁ CỘNG ĐỒNG">
      <div class="ratings-list">
        <div v-for="r in publicRatings" :key="r.id" class="rating-item">
          <div class="rating-top">
            <div style="display:flex;align-items:center;gap:10px">
              <div class="avatar">{{ (r.userName||'U')[0].toUpperCase() }}</div>
              <div>
                <div style="font-weight:600;font-size:0.875rem;color:var(--c-text)">{{ r.userName }}</div>
                <span class="badge badge-info" style="font-size:0.68rem">{{ serviceLabel(r.serviceType) }}</span>
              </div>
            </div>
            <div style="text-align:right">
              <el-rate :model-value="r.rating" disabled size="small"/>
              <div class="muted" style="font-size:0.72rem;margin-top:2px">{{ fmtDate(r.createdAt) }}</div>
            </div>
          </div>
          <div class="rating-comment" v-if="r.comment">{{ r.comment }}</div>
          <div v-if="r.adminReply" class="admin-reply">
            <span class="reply-label">💬 Admin:</span> {{ r.adminReply }}
          </div>
        </div>
        <div v-if="!publicRatings.length" class="empty-state">Chưa có đánh giá nào</div>
      </div>
    </el-card>

    <!-- Add Dialog -->
    <el-dialog v-model="addDialog" title="VIẾT ĐÁNH GIÁ" width="460px" align-center>
      <el-form :model="form" label-position="top">
        <el-form-item label="Dịch vụ đánh giá">
          <el-select v-model="form.serviceType" style="width:100%">
            <el-option label="📋 Giáo án tập" value="WORKOUT_PLAN"/>
            <el-option label="🥗 Dinh dưỡng" value="NUTRITION"/>
            <el-option label="🏟️ Cơ sở vật chất" value="FACILITY"/>
            <el-option label="👟 Huấn luyện viên" value="TRAINER"/>
          </el-select>
        </el-form-item>
        <el-form-item label="Số sao">
          <el-rate v-model="form.rating" :max="5" show-text :texts="['Tệ','Kém','Trung bình','Tốt','Tuyệt vời']"/>
        </el-form-item>
        <el-form-item label="Nhận xét">
          <el-input v-model="form.comment" type="textarea" :rows="3" placeholder="Chia sẻ trải nghiệm của bạn..."/>
        </el-form-item>
        <el-form-item label="Hiển thị công khai">
          <el-switch v-model="form.isPublic"/>
          <span style="margin-left:8px;font-size:0.82rem;color:var(--c-text3)">
            {{ form.isPublic ? 'Mọi người có thể xem' : 'Chỉ admin xem' }}
          </span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialog=false">Hủy</el-button>
        <el-button type="primary" @click="submit" :loading="submitting">GỬI ĐÁNH GIÁ ⭐</el-button>
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
const myRatings     = ref([])
const averages      = ref({})
const addDialog     = ref(false)
const submitting    = ref(false)
const form = reactive({ serviceType: 'WORKOUT_PLAN', rating: 5, comment: '', isPublic: true })

async function load() {
  try {
    const [pub, my, avg] = await Promise.all([ratingAPI.getPublic(), ratingAPI.getMy(), ratingAPI.getAverages()])
    publicRatings.value = pub.data || []
    myRatings.value     = my.data  || []
    averages.value      = avg.data || {}
  } catch {}
}

async function submit() {
  if (!form.rating) { ElMessage.warning('Vui lòng chọn số sao'); return }
  submitting.value = true
  try {
    await ratingAPI.add(form)
    ElMessage.success('Cảm ơn bạn đã đánh giá! ⭐')
    addDialog.value = false
    Object.assign(form, { serviceType:'WORKOUT_PLAN', rating:5, comment:'', isPublic:true })
    load()
  } catch {} finally { submitting.value = false }
}

function serviceLabel(s) { return { WORKOUT_PLAN:'Giáo án', NUTRITION:'Dinh dưỡng', FACILITY:'Cơ sở', TRAINER:'HLV' }[s] || s }
function fmtDate(d) { return d ? dayjs(d).format('DD/MM/YYYY HH:mm') : '' }

onMounted(load)
</script>

<style scoped>
.ratings-list { display:flex; flex-direction:column; gap:14px; }
.rating-item {
  padding:14px 16px; background:var(--c-card2); border-radius:var(--radius-lg);
  border-left:3px solid var(--c-border2);
}
.rating-item.my { border-left-color:var(--c-accent); }
.rating-top { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:8px; flex-wrap:wrap; gap:8px; }
.avatar {
  width:36px; height:36px; border-radius:50%; background:var(--c-accent); color:#fff;
  display:flex; align-items:center; justify-content:center;
  font-family:var(--font-display); font-size:1rem; flex-shrink:0;
}
.rating-comment { font-size:0.875rem; color:var(--c-text2); line-height:1.5; margin-bottom:8px; }
.admin-reply {
  background:#FFF8F0; border:1px solid #F0D9B5; border-radius:var(--radius);
  padding:8px 12px; font-size:0.82rem; color:var(--c-text2); margin-top:6px;
}
.reply-label { font-weight:700; color:var(--c-accent); }
</style>
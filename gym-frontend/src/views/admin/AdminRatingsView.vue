<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>QUẢN LÝ ĐÁNH GIÁ</h2>
      <div style="display:flex;gap:8px;align-items:center">
        <el-select v-model="filterType" placeholder="Loại dịch vụ" clearable style="width:160px">
          <el-option label="Giáo án" value="WORKOUT_PLAN"/>
          <el-option label="Dinh dưỡng" value="NUTRITION"/>
          <el-option label="Cơ sở" value="FACILITY"/>
          <el-option label="HLV" value="TRAINER"/>
        </el-select>
      </div>
    </div>

    <!-- Average scores -->
    <div class="grid-4" style="margin-bottom:24px">
      <div class="stat-card" v-for="(val,key) in averages" :key="key">
        <div class="label">{{ serviceLabel(key) }}</div>
        <div class="value accent">{{ val }}</div>
        <el-rate :model-value="Number(val)" disabled size="small" style="margin-top:6px"/>
      </div>
    </div>

    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between">
          <span>TẤT CẢ ĐÁNH GIÁ ({{ filtered.length }})</span>
        </div>
      </template>

      <div v-if="loading"><el-skeleton :rows="4" animated/></div>
      <div v-else class="ratings-list">
        <div v-for="r in filtered" :key="r.id" class="rating-card">
          <div class="rating-header">
            <div class="user-info">
              <div class="avatar">{{ (r.userName||'U')[0].toUpperCase() }}</div>
              <div>
                <div class="user-name">{{ r.userName }}</div>
                <div class="user-email muted">{{ r.userEmail }}</div>
              </div>
            </div>
            <div style="text-align:right">
              <div style="display:flex;gap:8px;align-items:center;justify-content:flex-end;margin-bottom:4px">
                <span class="badge badge-info">{{ serviceLabel(r.serviceType) }}</span>
                <span class="badge" :class="r.isPublic?'badge-success':'badge-warning'">{{ r.isPublic ? 'Công khai' : 'Riêng tư' }}</span>
              </div>
              <el-rate :model-value="r.rating" disabled size="small"/>
              <div class="muted" style="font-size:0.72rem;margin-top:2px">{{ fmtDate(r.createdAt) }}</div>
            </div>
          </div>

          <div class="rating-comment" v-if="r.comment">{{ r.comment }}</div>

          <!-- Existing reply -->
          <div v-if="r.adminReply" class="admin-reply-show">
            <span class="reply-label">💬 Phản hồi của bạn:</span>
            <span>{{ r.adminReply }}</span>
            <span class="muted" style="font-size:0.72rem;margin-left:8px">{{ fmtDate(r.repliedAt) }}</span>
          </div>

          <!-- Reply form -->
          <div class="reply-form" v-if="replyTarget===r.id">
            <el-input v-model="replyText" type="textarea" :rows="2"
                      placeholder="Nhập phản hồi cho khách hàng..." style="margin-bottom:8px"/>
            <div style="display:flex;gap:8px;justify-content:flex-end">
              <el-button size="small" @click="replyTarget=null">Hủy</el-button>
              <el-button type="primary" size="small" @click="sendReply(r.id)" :loading="replying">Gửi phản hồi</el-button>
            </div>
          </div>

          <div style="margin-top:10px">
            <el-button
                size="small" type="primary" plain
                @click="openReply(r)"
            >
              {{ r.adminReply ? '✏️ Sửa phản hồi' : '💬 Phản hồi' }}
            </el-button>
          </div>
        </div>

        <div v-if="!filtered.length" class="empty-state">Chưa có đánh giá nào</div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ratingAPI } from '@/api'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const ratings     = ref([])
const averages    = ref({})
const loading     = ref(true)
const filterType  = ref('')
const replyTarget = ref(null)
const replyText   = ref('')
const replying    = ref(false)

const filtered = computed(() =>
    filterType.value ? ratings.value.filter(r => r.serviceType === filterType.value) : ratings.value
)

async function load() {
  loading.value = true
  try {
    const [all, avg] = await Promise.all([ratingAPI.getAll(), ratingAPI.getAverages()])
    ratings.value  = all.data  || []
    averages.value = avg.data  || {}
  } finally { loading.value = false }
}

function openReply(r) {
  replyTarget.value = r.id
  replyText.value   = r.adminReply || ''
}

async function sendReply(id) {
  if (!replyText.value.trim()) { ElMessage.warning('Nhập nội dung phản hồi'); return }
  replying.value = true
  try {
    await ratingAPI.adminReply(id, replyText.value)
    ElMessage.success('Đã gửi phản hồi! Người dùng sẽ nhận thông báo.')
    replyTarget.value = null
    load()
  } catch {} finally { replying.value = false }
}

function serviceLabel(s) { return { WORKOUT_PLAN:'Giáo án', NUTRITION:'Dinh dưỡng', FACILITY:'Cơ sở', TRAINER:'HLV' }[s] || s }
function fmtDate(d) { return d ? dayjs(d).format('DD/MM/YYYY HH:mm') : '' }

onMounted(load)
</script>

<style scoped>
.ratings-list { display:flex; flex-direction:column; gap:16px; }
.rating-card {
  padding:16px; background:var(--c-card2); border:1px solid var(--c-border2);
  border-radius:var(--radius-lg); transition: border-color var(--transition);
}
.rating-card:hover { border-color:var(--c-accent); }
.rating-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:10px; flex-wrap:wrap; gap:10px; }
.user-info { display:flex; gap:10px; align-items:center; }
.avatar {
  width:38px; height:38px; border-radius:50%; background:var(--c-accent); color:#fff;
  display:flex; align-items:center; justify-content:center;
  font-family:var(--font-display); font-size:1rem; flex-shrink:0;
}
.user-name  { font-weight:700; font-size:0.875rem; color:var(--c-text); }
.user-email { font-size:0.75rem; }
.rating-comment { font-size:0.875rem; color:var(--c-text2); line-height:1.5; margin-bottom:10px; }
.admin-reply-show {
  background:#FFF8F0; border:1px solid #F0D9B5; border-radius:var(--radius);
  padding:8px 12px; font-size:0.82rem; color:var(--c-text2); margin-bottom:10px;
}
.reply-label { font-weight:700; color:var(--c-accent); margin-right:6px; }
.reply-form { margin-top:10px; padding:12px; background:var(--c-card); border-radius:var(--radius); border:1px solid var(--c-border2); }
</style>
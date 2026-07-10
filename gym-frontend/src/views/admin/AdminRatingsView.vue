<template>
  <div class="fade-in">
    <!-- Input chọn file dùng chung cho form phản hồi đang mở -->
    <input ref="replyFileInput" type="file" hidden @change="onReplyFile"/>

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
        <div v-for="r in filtered" :key="r.id" class="rating-card"
             :id="`rating-${r.id}`" :class="{ highlighted: highlightId === r.id }">
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
                <span class="badge badge-info">{{ r.serviceType ? serviceLabel(r.serviceType) : 'Chưa chọn dịch vụ' }}</span>
                <span class="badge" :class="r.isPublic?'badge-success':'badge-warning'">{{ r.isPublic ? 'Công khai' : 'Riêng tư' }}</span>
              </div>
              <el-rate :model-value="r.rating" disabled size="small"/>
              <div class="muted" style="font-size:0.72rem;margin-top:2px">{{ fmtDate(r.createdAt) }}</div>
            </div>
          </div>

          <div class="rating-title" v-if="r.title">{{ r.title }}</div>
          <div class="rating-comment" v-if="r.comment || r.attachmentUrl">
            <MessageBody :message="commentBody(r)"/>
          </div>

          <!-- Existing reply -->
          <div v-if="r.adminReply || r.replyAttachmentUrl" class="admin-reply-show">
            <span class="reply-label">💬 Phản hồi của bạn:</span>
            <span class="muted" style="font-size:0.72rem">{{ fmtDate(r.repliedAt) }}</span>
            <MessageBody :message="replyBody(r)"/>
          </div>

          <!-- Reply form -->
          <div class="reply-form" v-if="replyTarget===r.id">
            <el-input v-model="replyText" type="textarea" :rows="2"
                      placeholder="Nhập phản hồi cho khách hàng..." style="margin-bottom:8px"/>

            <!-- Đính kèm -->
            <div class="reply-attach">
              <div v-if="replyFile" class="file-chip">
                <el-icon :size="18"><Document/></el-icon>
                <span class="fc-name">{{ replyFile.name }}</span>
                <span class="fc-size">{{ prettyFileSize(replyFile.size) }}</span>
                <el-button text class="fc-remove" @click="replyFile=null" title="Bỏ file">
                  <el-icon><Close/></el-icon>
                </el-button>
              </div>
              <div v-else-if="r.replyAttachmentUrl && !replyRemoveAttachment" class="file-chip">
                <el-icon :size="18"><Document/></el-icon>
                <span class="fc-name">{{ r.replyAttachmentName || 'Tệp đính kèm' }}</span>
                <span class="fc-size">{{ prettyFileSize(r.replyAttachmentSize) }}</span>
                <el-button text class="fc-remove" @click="replyRemoveAttachment=true" title="Gỡ file">
                  <el-icon><Close/></el-icon>
                </el-button>
              </div>
              <el-button v-else plain size="small" @click="replyFileInput?.click()">
                <el-icon><Paperclip/></el-icon><span style="margin-left:6px">Đính kèm file</span>
              </el-button>
            </div>

            <div style="display:flex;gap:8px;justify-content:flex-end;margin-top:8px">
              <el-button size="small" @click="replyTarget=null">Hủy</el-button>
              <el-button type="primary" size="small" @click="sendReply(r.id)" :loading="replying">Gửi phản hồi</el-button>
            </div>
          </div>

          <div class="card-actions">
            <el-button size="small" type="primary" plain @click="openReply(r)">
              {{ r.adminReply || r.replyAttachmentUrl ? '✏️ Sửa phản hồi' : '💬 Phản hồi' }}
            </el-button>
            <el-button size="small" type="danger" plain @click="removeRating(r)">
              🗑️ Xóa đánh giá
            </el-button>
          </div>
        </div>

        <div v-if="!filtered.length" class="empty-state">Chưa có đánh giá nào</div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ratingAPI } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import MessageBody from '@/components/common/MessageBody.vue'
import dayjs from 'dayjs'

const MAX_FILE = 50 * 1024 * 1024   // 50MB, giống chat hỗ trợ

const ratings     = ref([])
const averages    = ref({})
const loading     = ref(true)
const filterType  = ref('')
const replyTarget = ref(null)
const replyText   = ref('')
const replying    = ref(false)

const replyFile             = ref(null)   // file mới chọn cho phản hồi
const replyRemoveAttachment = ref(false)  // admin gỡ file đã đính kèm trước đó
const replyFileInput        = ref(null)

// Nhận xét của user: MessageBody lo việc biến URL thành link và hiển thị file đính kèm
function commentBody(r) {
  return {
    content: r.comment,
    attachmentUrl:  r.attachmentUrl,
    attachmentName: r.attachmentName,
    attachmentType: r.attachmentType,
    attachmentSize: r.attachmentSize,
  }
}

// Ghép phản hồi thành dạng MessageBody hiểu được (chữ + file đính kèm)
function replyBody(r) {
  return {
    content: r.adminReply,
    attachmentUrl:  r.replyAttachmentUrl,
    attachmentName: r.replyAttachmentName,
    attachmentType: r.replyAttachmentType,
    attachmentSize: r.replyAttachmentSize,
  }
}

function onReplyFile(e) {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file) return
  if (file.size > MAX_FILE) { ElMessage.error('File tối đa 50MB'); return }
  replyFile.value = file
}

function prettyFileSize(b) {
  if (!b && b !== 0) return ''
  if (b < 1024) return b + ' B'
  if (b < 1024 * 1024) return (b / 1024).toFixed(1) + ' KB'
  return (b / 1024 / 1024).toFixed(1) + ' MB'
}

const filtered = computed(() =>
    filterType.value ? ratings.value.filter(r => r.serviceType === filterType.value) : ratings.value
)

const route = useRoute()
const highlightId = ref(null)   // đánh giá được mở từ thông báo

async function load() {
  loading.value = true
  try {
    const [all, avg] = await Promise.all([ratingAPI.getAll(), ratingAPI.getAverages()])
    ratings.value  = all.data  || []
    averages.value = avg.data  || {}
  } finally { loading.value = false }
}

/** Cuộn tới đánh giá mà thông báo trỏ đến rồi làm nổi nó trong giây lát. */
async function focusHighlighted() {
  const id = Number(route.query.highlight)
  if (!id) { highlightId.value = null; return }
  // Bộ lọc đang bật có thể đang ẩn chính đánh giá cần xem
  filterType.value = ''
  highlightId.value = id
  await nextTick()
  const el = document.getElementById(`rating-${id}`)
  // Đánh giá có thể đã bị xóa — khi đó không có gì để cuộn tới
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' })
  setTimeout(() => { if (highlightId.value === id) highlightId.value = null }, 2500)
}

// Bấm thông báo khác khi đang ở sẵn trang này thì phải làm nổi lại
watch(() => route.query.highlight, focusHighlighted)

function openReply(r) {
  replyTarget.value = r.id
  replyText.value   = r.adminReply || ''
  replyFile.value   = null
  replyRemoveAttachment.value = false
}

async function sendReply(id) {
  const text = replyText.value.trim()
  const rating = ratings.value.find(r => r.id === id)
  // Giữ lại file cũ nếu admin không chọn file mới và cũng không gỡ nó
  const keepsOldFile = !!rating?.replyAttachmentUrl && !replyRemoveAttachment.value
  if (!text && !replyFile.value && !keepsOldFile) {
    ElMessage.warning('Nhập nội dung phản hồi hoặc đính kèm file')
    return
  }
  replying.value = true
  try {
    const fd = new FormData()
    fd.append('reply', text)
    if (replyFile.value) fd.append('file', replyFile.value)
    if (replyRemoveAttachment.value) fd.append('removeAttachment', 'true')
    await ratingAPI.adminReply(id, fd)
    ElMessage.success('Đã gửi phản hồi! Người dùng sẽ nhận thông báo.')
    replyTarget.value = null
    replyFile.value = null
    replyRemoveAttachment.value = false
    load()
  } catch {} finally { replying.value = false }
}

async function removeRating(r) {
  try {
    await ElMessageBox.confirm(
        `Xóa đánh giá của "${r.userName || r.userEmail}"? Hành động không thể hoàn tác.`,
        'Xóa đánh giá',
        { type: 'warning', confirmButtonText: 'Xóa', cancelButtonText: 'Hủy' })
  } catch { return }   // admin bấm Hủy
  try {
    await ratingAPI.adminRemove(r.id)
    ElMessage.success('Đã xóa đánh giá')
    if (replyTarget.value === r.id) replyTarget.value = null
    load()
  } catch { /* lỗi đã hiển thị qua interceptor */ }
}

function serviceLabel(s) { return { WORKOUT_PLAN:'Giáo án', NUTRITION:'Dinh dưỡng', FACILITY:'Cơ sở', TRAINER:'HLV' }[s] || s }
function fmtDate(d) { return d ? dayjs(d).format('DD/MM/YYYY HH:mm') : '' }

onMounted(async () => {
  await load()
  focusHighlighted()
})
</script>

<style scoped>
.ratings-list { display:flex; flex-direction:column; gap:16px; }
.rating-card {
  padding:16px; background:var(--c-card2); border:1px solid var(--c-border2);
  border-radius:var(--radius-lg); transition: border-color var(--transition);
}
.rating-card:hover { border-color:var(--c-accent); }
/* Đánh giá vừa mở từ thông báo: nhấp nháy viền cam rồi tắt dần */
.rating-card.highlighted {
  animation: highlight-pulse 2.5s ease-out;
  box-shadow:0 0 0 2px var(--c-accent);
}
@keyframes highlight-pulse {
  0%   { box-shadow:0 0 0 4px var(--c-accent); background:rgba(212,137,42,0.18); }
  100% { box-shadow:0 0 0 2px transparent;    background:var(--c-card2); }
}
.rating-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:10px; flex-wrap:wrap; gap:10px; }
.user-info { display:flex; gap:10px; align-items:center; }
.avatar {
  width:38px; height:38px; border-radius:50%; background:var(--c-accent); color:#fff;
  display:flex; align-items:center; justify-content:center;
  font-family:var(--font-display); font-size:1rem; flex-shrink:0;
}
.user-name  { font-weight:700; font-size:0.875rem; color:var(--c-text); }
.user-email { font-size:0.75rem; }
/* Tiêu đề phải nổi rõ hơn hẳn phần nhận xét bên dưới (0.875rem) */
.rating-title { font-weight:700; font-size:1.35rem; line-height:1.3; color:var(--c-text); margin-bottom:2px; }
/* Cách tiêu đề một dòng trống để phân biệt rạch ròi tiêu đề với nội dung */
.rating-title + .rating-comment { margin-top:14px; }
.rating-comment { font-size:0.875rem; color:var(--c-text2); line-height:1.5; margin-bottom:10px; }
.admin-reply-show {
  background:#FFF8F0; border:1px solid #F0D9B5; border-radius:var(--radius);
  padding:8px 12px; font-size:0.82rem; color:var(--c-text2); margin-bottom:10px;
}
.reply-label { font-weight:700; color:var(--c-accent); margin-right:6px; }
.reply-form { margin-top:10px; padding:12px; background:var(--c-card); border-radius:var(--radius); border:1px solid var(--c-border2); }
.card-actions { display:flex; gap:8px; margin-top:10px; }
.reply-attach { display:flex; }
.file-chip {
  display:flex; align-items:center; gap:8px; padding:6px 10px;
  background:var(--c-card2); border:1px solid var(--c-border2); border-radius:var(--radius);
  font-size:0.8rem; color:var(--c-text2); max-width:100%;
}
.fc-name   { font-weight:600; color:var(--c-text); overflow:hidden; text-overflow:ellipsis; white-space:nowrap; max-width:220px; }
.fc-size   { color:var(--c-text3); font-size:0.72rem; }
.fc-remove { padding:0; min-height:auto; color:var(--c-text3); }
</style>
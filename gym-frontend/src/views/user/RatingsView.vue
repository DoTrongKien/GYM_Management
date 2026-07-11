<template>
  <div class="fade-in">
    <!-- Input chọn file cho dialog viết/sửa đánh giá -->
    <input ref="fileInput" type="file" hidden @change="onPickFile"/>

    <div class="page-header">
      <h2>ĐÁNH GIÁ DỊCH VỤ</h2>
      <el-button type="primary" @click="openAdd">+ Viết đánh giá</el-button>
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
        <div v-for="r in myRatings" :key="r.id" class="rating-item my"
             :id="`rating-${r.id}`" :class="{ highlighted: highlightId === r.id }">
          <div class="rating-top">
            <div>
              <span v-if="r.serviceType" class="badge badge-info" style="margin-right:8px">{{ serviceLabel(r.serviceType) }}</span>
              <el-rate :model-value="r.rating" disabled size="small" style="display:inline-flex"/>
            </div>
            <span class="muted" style="font-size:0.75rem">
              {{ fmtDate(r.createdAt) }}
              <em v-if="r.updatedAt" style="font-style:normal"> · đã sửa {{ fmtDate(r.updatedAt) }}</em>
            </span>
          </div>
          <div class="rating-title" v-if="r.title">{{ r.title }}</div>
          <div class="rating-comment" v-if="r.comment || r.attachmentUrl">
            <MessageBody :message="commentBody(r)"/>
          </div>
          <!-- Admin reply -->
          <div v-if="r.adminReply || r.replyAttachmentUrl" class="admin-reply">
            <span class="reply-label">💬 Admin:</span>
            <MessageBody :message="replyBody(r)"/>
          </div>
          <!-- Sửa / xóa đánh giá của chính mình -->
          <div class="rating-actions">
            <el-button size="small" plain @click="openEdit(r)">✏️ Sửa</el-button>
            <el-button size="small" type="danger" plain @click="removeRating(r)">🗑️ Xóa</el-button>
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
                <span v-if="r.serviceType" class="badge badge-info" style="font-size:0.68rem">{{ serviceLabel(r.serviceType) }}</span>
              </div>
            </div>
            <div style="text-align:right">
              <el-rate :model-value="r.rating" disabled size="small"/>
              <div class="muted" style="font-size:0.72rem;margin-top:2px">{{ fmtDate(r.createdAt) }}</div>
            </div>
          </div>
          <div class="rating-title" v-if="r.title">{{ r.title }}</div>
          <div class="rating-comment" v-if="r.comment || r.attachmentUrl">
            <MessageBody :message="commentBody(r)"/>
          </div>
          <div v-if="r.adminReply || r.replyAttachmentUrl" class="admin-reply">
            <span class="reply-label">💬 Admin:</span>
            <MessageBody :message="replyBody(r)"/>
          </div>
        </div>
        <div v-if="!publicRatings.length" class="empty-state">Chưa có đánh giá nào</div>
      </div>
    </el-card>

    <!-- Add / Edit Dialog -->
    <el-dialog v-model="addDialog" :title="editingId ? 'SỬA ĐÁNH GIÁ' : 'VIẾT ĐÁNH GIÁ'" width="460px" align-center>
      <el-form :model="form" label-position="top">
        <el-form-item label="Tiêu đề">
          <el-input v-model="form.title" maxlength="120" show-word-limit
                    placeholder="Tóm tắt ngắn gọn cảm nhận của bạn (không bắt buộc)"/>
        </el-form-item>
        <el-form-item label="Dịch vụ đánh giá (không bắt buộc)">
          <el-select v-model="form.serviceType" style="width:100%" clearable
                     placeholder="Chưa chọn dịch vụ">
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
          <el-input v-model="form.comment" type="textarea" :rows="3"
                    placeholder="Chia sẻ trải nghiệm của bạn... (dán link sẽ tự thành liên kết bấm được)"/>
        </el-form-item>
        <el-form-item label="Đính kèm ảnh / video / tài liệu">
          <div v-if="pickedFile" class="file-chip">
            <el-icon :size="18"><Document/></el-icon>
            <span class="fc-name">{{ pickedFile.name }}</span>
            <span class="fc-size">{{ prettyFileSize(pickedFile.size) }}</span>
            <el-button text class="fc-remove" @click="pickedFile=null" title="Bỏ file">
              <el-icon><Close/></el-icon>
            </el-button>
          </div>
          <div v-else-if="existingAttachment && !removeAttachment" class="file-chip">
            <el-icon :size="18"><Document/></el-icon>
            <span class="fc-name">{{ existingAttachment.name || 'Tệp đính kèm' }}</span>
            <span class="fc-size">{{ prettyFileSize(existingAttachment.size) }}</span>
            <el-button text class="fc-remove" @click="removeAttachment=true" title="Gỡ file">
              <el-icon><Close/></el-icon>
            </el-button>
          </div>
          <el-button v-else plain size="small" @click="fileInput?.click()">
            <el-icon><Paperclip/></el-icon><span style="margin-left:6px">Chọn file</span>
          </el-button>
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
        <el-button type="primary" @click="submit" :loading="submitting">
          {{ editingId ? 'LƯU THAY ĐỔI' : 'GỬI ĐÁNH GIÁ ⭐' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ratingAPI } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import MessageBody from '@/components/common/MessageBody.vue'
import dayjs from 'dayjs'

const publicRatings = ref([])
const myRatings     = ref([])
const averages      = ref({})
const addDialog     = ref(false)
const submitting    = ref(false)
const editingId     = ref(null)   // null = đang viết mới, khác null = đang sửa
// serviceType để trống: dịch vụ là tùy chọn, user không bắt buộc phải chọn
const form = reactive({ serviceType: '', rating: 5, title: '', comment: '', isPublic: true })

const MAX_FILE = 50 * 1024 * 1024   // 50MB, giống chat hỗ trợ

const pickedFile        = ref(null)   // file mới chọn
const existingAttachment = ref(null)  // file đã đính kèm từ trước (khi đang sửa)
const removeAttachment  = ref(false)  // user gỡ file cũ
const fileInput         = ref(null)

function onPickFile(e) {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file) return
  if (file.size > MAX_FILE) { ElMessage.error('File tối đa 50MB'); return }
  pickedFile.value = file
}

function prettyFileSize(b) {
  if (!b && b !== 0) return ''
  if (b < 1024) return b + ' B'
  if (b < 1024 * 1024) return (b / 1024).toFixed(1) + ' KB'
  return (b / 1024 / 1024).toFixed(1) + ' MB'
}

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

// Ghép phản hồi của admin thành dạng MessageBody hiểu được (chữ + file đính kèm)
function replyBody(r) {
  return {
    content: r.adminReply,
    attachmentUrl:  r.replyAttachmentUrl,
    attachmentName: r.replyAttachmentName,
    attachmentType: r.replyAttachmentType,
    attachmentSize: r.replyAttachmentSize,
  }
}

const route = useRoute()
const highlightId = ref(null)   // đánh giá được mở từ thông báo

async function load() {
  try {
    const [pub, my, avg] = await Promise.all([ratingAPI.getPublic(), ratingAPI.getMy(), ratingAPI.getAverages()])
    publicRatings.value = pub.data || []
    myRatings.value     = my.data  || []
    averages.value      = avg.data || {}
  } catch {}
}

/** Cuộn tới đánh giá mà thông báo trỏ đến rồi làm nổi nó trong giây lát. */
async function focusHighlighted() {
  const id = Number(route.query.highlight)
  if (!id) { highlightId.value = null; return }
  highlightId.value = id
  await nextTick()
  const el = document.getElementById(`rating-${id}`)
  // Đánh giá có thể đã bị xóa — khi đó không có gì để cuộn tới
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' })
  setTimeout(() => { if (highlightId.value === id) highlightId.value = null }, 2500)
}

// Bấm thông báo khác khi đang ở sẵn trang này thì phải làm nổi lại
watch(() => route.query.highlight, focusHighlighted)

/** Đưa dialog về trạng thái trống. */
function resetForm() {
  Object.assign(form, { serviceType:'', rating:5, title:'', comment:'', isPublic:true })
  pickedFile.value = null
  existingAttachment.value = null
  removeAttachment.value = false
}

function openAdd() {
  editingId.value = null
  resetForm()
  addDialog.value = true
}

function openEdit(r) {
  editingId.value = r.id
  resetForm()
  Object.assign(form, { serviceType: r.serviceType || '', rating: r.rating,
    title: r.title || '', comment: r.comment || '', isPublic: r.isPublic !== false })
  if (r.attachmentUrl) {
    existingAttachment.value = { name: r.attachmentName, size: r.attachmentSize }
  }
  addDialog.value = true
}

async function submit() {
  if (!form.rating) { ElMessage.warning('Vui lòng chọn số sao'); return }
  submitting.value = true
  try {
    const fd = new FormData()
    fd.append('rating', form.rating)
    fd.append('title', form.title || '')
    fd.append('comment', form.comment || '')
    fd.append('serviceType', form.serviceType || '')   // rỗng → backend lưu null
    fd.append('isPublic', form.isPublic)
    if (pickedFile.value) fd.append('file', pickedFile.value)

    if (editingId.value) {
      if (removeAttachment.value) fd.append('removeAttachment', 'true')
      await ratingAPI.update(editingId.value, fd)
      ElMessage.success('Đã cập nhật đánh giá')
    } else {
      await ratingAPI.add(fd)
      ElMessage.success('Cảm ơn bạn đã đánh giá! ⭐')
    }
    addDialog.value = false
    editingId.value = null
    resetForm()
    load()
  } catch {} finally { submitting.value = false }
}

async function removeRating(r) {
  try {
    await ElMessageBox.confirm(
        'Bạn có chắc muốn xóa đánh giá này? Hành động không thể hoàn tác.',
        'Xóa đánh giá',
        { type: 'warning', confirmButtonText: 'Xóa', cancelButtonText: 'Hủy' })
  } catch { return }   // người dùng bấm Hủy
  try {
    await ratingAPI.remove(r.id)
    ElMessage.success('Đã xóa đánh giá')
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
.ratings-list { display:flex; flex-direction:column; gap:14px; }
.rating-item {
  padding:14px 16px; background:var(--c-card2); border-radius:var(--radius-lg);
  border-left:3px solid var(--c-border2);
}
.rating-item.my { border-left-color:var(--c-accent); }
/* Đánh giá vừa mở từ thông báo: nhấp nháy viền cam rồi tắt dần */
.rating-item.highlighted {
  animation: highlight-pulse 2.5s ease-out;
  box-shadow:0 0 0 2px var(--c-accent);
}
@keyframes highlight-pulse {
  0%   { box-shadow:0 0 0 4px var(--c-accent); background:rgba(212,137,42,0.18); }
  100% { box-shadow:0 0 0 2px transparent;    background:var(--c-card2); }
}
.rating-top { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:8px; flex-wrap:wrap; gap:8px; }
.avatar {
  width:36px; height:36px; border-radius:50%; background:var(--c-accent); color:#fff;
  display:flex; align-items:center; justify-content:center;
  font-family:var(--font-display); font-size:1rem; flex-shrink:0;
}
/* Tiêu đề phải nổi rõ hơn hẳn phần nhận xét bên dưới (0.875rem) */
.rating-title { font-weight:700; font-size:1.35rem; line-height:1.3; color:var(--c-text); margin-bottom:2px; }
/* Cách tiêu đề một dòng trống để phân biệt rạch ròi tiêu đề với nội dung */
.rating-title + .rating-comment { margin-top:14px; }
.rating-comment { font-size:0.875rem; color:var(--c-text2); line-height:1.5; margin-bottom:8px; }
.admin-reply {
  background:#FFF8F0; border:1px solid #F0D9B5; border-radius:var(--radius);
  padding:8px 12px; font-size:0.82rem; color:var(--c-text2); margin-top:6px;
}
.reply-label { font-weight:700; color:var(--c-accent); }
.rating-actions { display:flex; gap:8px; justify-content:flex-end; margin-top:10px; }
.file-chip {
  display:flex; align-items:center; gap:8px; padding:6px 10px;
  background:var(--c-card2); border:1px solid var(--c-border2); border-radius:var(--radius);
  font-size:0.8rem; color:var(--c-text2); max-width:100%;
}
.fc-name   { font-weight:600; color:var(--c-text); overflow:hidden; text-overflow:ellipsis; white-space:nowrap; max-width:220px; }
.fc-size   { color:var(--c-text3); font-size:0.72rem; }
.fc-remove { padding:0; min-height:auto; color:var(--c-text3); }
</style>
<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>HỖ TRỢ / CHAT VỚI USER</h2>
      <div style="display:flex;gap:10px;align-items:center">
        <el-tag v-if="pending.length" type="warning" effect="dark" round>
          {{ pending.length }} yêu cầu chờ
        </el-tag>
        <el-button type="primary" @click="openStartDialog">✉️ Nhắn tin cho user</el-button>
      </div>
    </div>

    <!-- Input chọn file cho dialog nhắn tin -->
    <input ref="startFileInput" type="file" hidden @change="onStartFile"/>

    <!-- Admin chủ động mở cuộc trò chuyện -->
    <el-dialog v-model="startDialog" title="NHẮN TIN CHO USER" width="480px" align-center>
      <el-form :model="startForm" label-position="top">
        <el-form-item label="Người nhận">
          <el-select v-model="startForm.userId" filterable style="width:100%"
                     placeholder="Tìm theo tên hoặc email" :loading="loadingUsers">
            <el-option v-for="u in users" :key="u.id"
                       :label="`${u.fullName || 'Không tên'} — ${u.email}`" :value="u.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="Tiêu đề">
          <el-input v-model="startForm.subject" maxlength="120"
                    placeholder="Ví dụ: Nhắc gia hạn gói tập (không bắt buộc)"/>
        </el-form-item>
        <el-form-item label="Nội dung">
          <el-input v-model="startForm.content" type="textarea" :rows="4"
                    placeholder="Nhập tin nhắn gửi tới người dùng..."/>
        </el-form-item>
        <el-form-item label="Đính kèm">
          <div v-if="startFile" class="file-chip">
            <el-icon :size="18"><Document/></el-icon>
            <span class="fc-name">{{ startFile.name }}</span>
            <span class="fc-size">{{ prettyFileSize(startFile.size) }}</span>
            <el-button text class="fc-remove" @click="startFile=null" title="Bỏ file">
              <el-icon><Close/></el-icon>
            </el-button>
          </div>
          <el-button v-else plain size="small" @click="startFileInput?.click()">
            <el-icon><Paperclip/></el-icon><span style="margin-left:6px">Chọn file</span>
          </el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="startDialog=false">Hủy</el-button>
        <el-button type="primary" @click="startChat" :loading="starting">GỬI TIN NHẮN</el-button>
      </template>
    </el-dialog>

    <div class="support-wrap">
      <!-- Danh sách phiên -->
      <el-card class="list-col" body-style="padding:0;">
        <div class="list-section-title">Yêu cầu chờ ({{ pending.length }})</div>
        <div v-if="!pending.length" class="empty-mini">Không có yêu cầu mới</div>
        <div v-for="s in pending" :key="s.id" class="session-item pending">
          <div class="session-info">
            <div class="avatar">{{ nameInitials(s.userName) }}</div>
            <div>
              <div class="s-name ellipsis">{{ s.subject || 'Hỗ trợ' }}</div>
              <div class="s-meta">{{ s.userName || s.userEmail }} · {{ fmtTime(s.createdAt) }}</div>
            </div>
          </div>
          <div v-if="s.lastMessage" class="s-content">{{ s.lastMessage }}</div>
          <div class="session-actions">
            <el-button size="small" type="success" @click="accept(s)">Chấp nhận</el-button>
            <el-button size="small" type="danger" plain @click="reject(s)">Từ chối</el-button>
          </div>
        </div>

        <div class="list-section-title">Đang chat ({{ active.length }})</div>
        <div v-if="!active.length" class="empty-mini">Chưa có cuộc trò chuyện nào</div>
        <div
          v-for="s in active" :key="s.id"
          class="session-item active" :class="{ selected: selected?.id === s.id }"
          @click="select(s)">
          <div class="session-info">
            <div class="avatar on">{{ nameInitials(s.userName) }}</div>
            <div>
              <div class="s-name ellipsis">{{ s.subject || 'Hỗ trợ' }}</div>
              <div class="s-meta ellipsis">{{ s.userName }} · {{ s.lastMessage || 'Bắt đầu trò chuyện…' }}</div>
            </div>
            <span v-if="isUnread(s)" class="unread-dot" title="Có tin nhắn mới"/>
          </div>
        </div>
      </el-card>

      <!-- Khung chat -->
      <el-card class="chat-col" body-style="padding:0; display:flex; flex-direction:column; height:100%;">
        <template v-if="selected">
          <div class="chat-head">
            <div class="session-info">
              <div class="avatar on">{{ nameInitials(selected.userName) }}</div>
              <div>
                <div class="s-name">{{ selected.subject || 'Hỗ trợ' }}</div>
                <div class="s-meta">{{ selected.userName || selected.userEmail }} · {{ selected.userEmail }}</div>
              </div>
            </div>
            <el-button size="small" type="danger" plain @click="closeSession">Kết thúc phiên</el-button>
          </div>

          <div ref="scrollRef" class="messages">
            <div v-for="(m, i) in messages" :key="i" class="msg-row" :class="m.senderRole === 'ADMIN' ? 'from-me' : 'from-them'">
              <div v-if="m.senderRole === 'USER'" class="avatar sm">{{ nameInitials(selected.userName) }}</div>
              <div class="bubble" :class="m.senderRole === 'ADMIN' ? 'bubble-me' : 'bubble-them'">
                <MessageBody :message="m" />
                <div class="bubble-time">{{ fmtTime(m.createdAt) }}</div>
              </div>
            </div>
            <div v-if="showSent" class="sent-status" :class="{ err: sendStatus === 'failed' }">{{ sentText }}</div>
            <div v-if="!messages.length" class="empty-state">Chưa có tin nhắn</div>
          </div>

          <div class="input-bar">
            <input ref="fileInput" type="file" hidden @change="onFile"/>
            <el-button class="attach-btn" text :loading="uploading" @click="pickFile" title="Đính kèm file">
              <el-icon :size="20"><Paperclip/></el-icon>
            </el-button>
            <el-input v-model="draft" placeholder="Nhập tin nhắn gửi user…" @keyup.enter="send" clearable/>
            <el-button type="primary" :disabled="!draft.trim()" @click="send">
              <el-icon><Promotion/></el-icon>
            </el-button>
          </div>
        </template>

        <div v-else class="empty-state pick">
          <el-icon :size="40"><ChatLineRound/></el-icon>
          <div>Chọn một cuộc trò chuyện để bắt đầu</div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { adminSupportAPI, adminAPI } from '@/api'
import { ElMessage } from 'element-plus'
import MessageBody from '@/components/common/MessageBody.vue'
import { setSessions, markRead, isUnread } from '@/stores/supportUnread'
import dayjs from 'dayjs'

const route      = useRoute()
const sessions   = ref([])
const selected   = ref(null)
const messages   = ref([])
const draft      = ref('')
const sendStatus = ref(null)      // null | 'sending' | 'sent' | 'failed'
const scrollRef  = ref(null)
const fileInput  = ref(null)
const uploading  = ref(false)
let pollTimer = null

const MAX_FILE = 50 * 1024 * 1024   // 50MB

// Admin chủ động mở cuộc trò chuyện
const startDialog    = ref(false)
const starting       = ref(false)
const users          = ref([])
const loadingUsers   = ref(false)
const startFile      = ref(null)
const startFileInput = ref(null)
const startForm = reactive({ userId: null, subject: '', content: '' })

const pending = computed(() => sessions.value.filter(s => s.status === 'PENDING'))
const active  = computed(() => sessions.value.filter(s => s.status === 'ACTIVE'))

// Chỉ hiện trạng thái gửi khi tin nhắn cuối là của admin
const showSent = computed(() => {
  const m = messages.value[messages.value.length - 1]
  return m && m.senderRole === 'ADMIN'
})
const sentText = computed(() =>
  sendStatus.value === 'sending' ? 'Đang gửi…'
  : sendStatus.value === 'failed' ? '⚠ Gửi lỗi, thử lại' : 'Đã gửi ✓')

async function loadSessions() {
  try { const res = await adminSupportAPI.sessions(); sessions.value = res.data || []; setSessions(sessions.value) }
  catch {}
  // Nếu phiên đang chọn đã bị đóng ở nơi khác
  if (selected.value && !active.value.find(s => s.id === selected.value.id)) {
    selected.value = null; messages.value = []
  }
}

async function accept(s) {
  try {
    await adminSupportAPI.accept(s.id)
    ElMessage.success('Đã chấp nhận yêu cầu')
    await loadSessions()
    const acc = active.value.find(x => x.id === s.id)
    if (acc) select(acc)
  } catch {}
}

async function reject(s) {
  try { await adminSupportAPI.reject(s.id); ElMessage.info('Đã từ chối yêu cầu'); loadSessions() }
  catch {}
}

// ── Admin chủ động nhắn tin cho user ─────────────────────
async function openStartDialog() {
  Object.assign(startForm, { userId: null, subject: '', content: '' })
  startFile.value = null
  startDialog.value = true
  if (users.value.length) return      // đã tải rồi thì thôi
  loadingUsers.value = true
  try { users.value = (await adminAPI.getUsers()).data || [] }
  catch {} finally { loadingUsers.value = false }
}

function onStartFile(e) {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file) return
  if (file.size > MAX_FILE) { ElMessage.error('File tối đa 50MB'); return }
  startFile.value = file
}

function prettyFileSize(b) {
  if (!b && b !== 0) return ''
  if (b < 1024) return b + ' B'
  if (b < 1024 * 1024) return (b / 1024).toFixed(1) + ' KB'
  return (b / 1024 / 1024).toFixed(1) + ' MB'
}

async function startChat() {
  if (!startForm.userId) { ElMessage.warning('Chọn người nhận'); return }
  if (!startForm.content.trim() && !startFile.value) {
    ElMessage.warning('Nhập nội dung hoặc đính kèm file'); return
  }
  starting.value = true
  try {
    const fd = new FormData()
    fd.append('userId', startForm.userId)
    fd.append('subject', startForm.subject || '')
    fd.append('content', startForm.content || '')
    if (startFile.value) fd.append('file', startFile.value)
    const res = await adminSupportAPI.start(fd)
    ElMessage.success('Đã gửi tin nhắn tới người dùng')
    startDialog.value = false
    await loadSessions()
    // Mở luôn phiên vừa tạo để admin chat tiếp
    const created = sessions.value.find(s => s.id === res.data?.id)
    if (created) select(created)
  } catch {} finally { starting.value = false }
}

async function select(s) {
  selected.value = s
  sendStatus.value = null
  markRead(s.id)                    // mở xem → hết chưa đọc
  await loadMessages()
}

async function loadMessages() {
  if (!selected.value) return
  try { const res = await adminSupportAPI.messages(selected.value.id); messages.value = res.data || []; scrollBottom() }
  catch {}
}

async function send() {
  const content = draft.value.trim()
  if (!content || !selected.value) return
  const optimistic = { senderRole: 'ADMIN', content, createdAt: new Date().toISOString() }
  messages.value.push(optimistic)
  draft.value = ''
  sendStatus.value = 'sending'
  scrollBottom()
  try { await adminSupportAPI.send(selected.value.id, content); sendStatus.value = 'sent'; await loadMessages() }
  catch { sendStatus.value = 'failed'; messages.value = messages.value.filter(m => m !== optimistic) }
}

function pickFile() { fileInput.value?.click() }

async function onFile(e) {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file || !selected.value) return
  if (file.size > MAX_FILE) { ElMessage.error('File tối đa 50MB'); return }
  uploading.value = true
  sendStatus.value = 'sending'
  try {
    const fd = new FormData()
    fd.append('file', file)
    if (draft.value.trim()) fd.append('caption', draft.value.trim())
    await adminSupportAPI.sendFile(selected.value.id, fd)
    draft.value = ''
    sendStatus.value = 'sent'
    await loadMessages()
  } catch { sendStatus.value = 'failed' }
  finally { uploading.value = false }
}

async function closeSession() {
  if (!selected.value) return
  try {
    await adminSupportAPI.close(selected.value.id)
    ElMessage.success('Đã kết thúc phiên chat')
    selected.value = null; messages.value = []
    loadSessions()
  } catch {}
}

async function pollTick() {
  await loadSessions()
  if (selected.value) {
    markRead(selected.value.id)     // đang mở xem thì luôn coi là đã đọc
    await loadMessages()
  }
}

function nameInitials(name) { return (name || 'U').split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2) }
function scrollBottom() { nextTick(() => { const el = scrollRef.value; if (el) el.scrollTop = el.scrollHeight }) }
function fmtTime(d) { return d ? dayjs(d).format('DD/MM HH:mm') : '' }

/** Mở phiên chat mà thông báo trỏ đến (?session=<id>). */
function openSessionFromRoute() {
  const id = Number(route.query.session)
  if (!id) return
  const s = sessions.value.find(x => x.id === id)
  // Phiên đã đóng hoặc đã bị từ chối thì không còn trong danh sách
  if (!s) { ElMessage.info('Cuộc trò chuyện này không còn mở'); return }
  // Yêu cầu chưa được nhận thì chưa có khung chat để mở
  if (s.status === 'PENDING') { ElMessage.info('Yêu cầu này đang chờ bạn chấp nhận'); return }
  select(s)
}

// Bấm thông báo khác khi đang ở sẵn trang này
watch(() => route.query.session, openSessionFromRoute)

onMounted(async () => {
  await loadSessions()
  openSessionFromRoute()
  pollTimer = setInterval(pollTick, 3000)
})
onUnmounted(() => { if (pollTimer) clearInterval(pollTimer) })
</script>

<style scoped>
.support-wrap { display:flex; gap:16px; height:calc(100vh - 160px); }
.list-col { width:320px; flex-shrink:0; overflow-y:auto; }
.chat-col { flex:1; overflow:hidden; }

.list-section-title {
  padding:12px 16px 6px; font-family:var(--font-display); letter-spacing:0.06em;
  font-size:0.78rem; color:var(--c-text3); text-transform:uppercase;
}
.empty-mini { padding:6px 16px 12px; color:var(--c-text3); font-size:0.82rem; }

.session-item {
  padding:12px 16px; border-top:1px solid var(--c-bg3);
  display:flex; flex-direction:column; gap:10px;
}
.session-item.active { cursor:pointer; transition:background 0.15s; }
.session-item.active:hover { background:var(--c-card2); }
.session-item.selected { background:var(--c-card2); border-left:3px solid var(--c-accent); }
.session-info { display:flex; align-items:center; gap:10px; }

/* Chấm đỏ báo có tin nhắn chưa đọc */
.unread-dot {
  width:9px; height:9px; border-radius:50%; background:#f56c6c; flex-shrink:0;
  margin-left:auto; box-shadow:0 0 0 3px rgba(245,108,108,0.18);
}

.avatar {
  width:38px; height:38px; border-radius:50%; flex-shrink:0;
  background:var(--c-accent); color:#fff;
  display:flex; align-items:center; justify-content:center;
  font-family:var(--font-display); font-size:0.9rem;
}
.avatar.on { background:#1f6f43; }
.avatar.sm { width:30px; height:30px; font-size:0.8rem; }

.s-name { font-size:0.88rem; font-weight:600; color:var(--c-text); }
.s-meta { font-size:0.74rem; color:var(--c-text3); }
.s-content {
  font-size:0.8rem; color:var(--c-text2); margin-top:4px; max-width:250px;
  display:-webkit-box; -webkit-line-clamp:2; line-clamp:2;
  -webkit-box-orient:vertical; overflow:hidden;
}
.ellipsis { max-width:190px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.session-actions { display:flex; gap:8px; }

.chat-head {
  height:60px; padding:0 18px; flex-shrink:0;
  display:flex; align-items:center; justify-content:space-between;
  border-bottom:1px solid var(--c-bg3); background:var(--c-card);
}

.messages { flex:1; overflow-y:auto; padding:20px; display:flex; flex-direction:column; gap:12px; background:var(--c-bg); }
.msg-row { display:flex; align-items:flex-end; gap:8px; max-width:75%; }
.from-them { align-self:flex-start; }
.from-me   { align-self:flex-end; }

.bubble { padding:10px 14px; border-radius:var(--radius-lg); box-shadow:var(--shadow); }
.bubble-them { background:var(--c-card); color:var(--c-text); border-bottom-left-radius:4px; }
.bubble-me   { background:#1f6f43; color:#fff; border-bottom-right-radius:4px; }
.bubble-text { font-size:0.9rem; line-height:1.5; white-space:pre-line; word-break:break-word; }
.bubble-time { font-size:0.68rem; opacity:0.6; margin-top:4px; text-align:right; }

.sent-status { align-self:flex-end; font-size:0.68rem; color:var(--c-text3); margin:-6px 6px 0 0; }
.sent-status.err { color:#c0392b; }

.input-bar { display:flex; align-items:center; gap:10px; padding:14px 16px; border-top:1px solid var(--c-bg3); background:var(--c-card); }
.input-bar .el-input { flex:1; }
.attach-btn { color:var(--c-text2) !important; padding:0 4px; }
.attach-btn:hover { color:var(--c-accent) !important; }

.empty-state.pick {
  flex:1; display:flex; flex-direction:column; align-items:center; justify-content:center;
  gap:12px; color:var(--c-text3);
}

/* Chip file trong dialog nhắn tin cho user */
.file-chip {
  display:flex; align-items:center; gap:8px; padding:6px 10px;
  background:var(--c-card2); border:1px solid var(--c-border2); border-radius:var(--radius);
  font-size:0.8rem; color:var(--c-text2); max-width:100%;
}
.fc-name   { font-weight:600; color:var(--c-text); overflow:hidden; text-overflow:ellipsis; white-space:nowrap; max-width:220px; }
.fc-size   { color:var(--c-text3); font-size:0.72rem; }
.fc-remove { padding:0; min-height:auto; color:var(--c-text3); }
</style>

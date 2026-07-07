<template>
  <div class="fade-in">
    <div class="page-header">
      <h2>HỖ TRỢ / CHAT VỚI USER</h2>
      <el-tag v-if="pending.length" type="warning" effect="dark" round>
        {{ pending.length }} yêu cầu chờ
      </el-tag>
    </div>

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
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { adminSupportAPI } from '@/api'
import { ElMessage } from 'element-plus'
import MessageBody from '@/components/common/MessageBody.vue'
import dayjs from 'dayjs'

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
  try { const res = await adminSupportAPI.sessions(); sessions.value = res.data || [] }
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

async function select(s) {
  selected.value = s
  sendStatus.value = null
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
  if (selected.value) await loadMessages()
}

function nameInitials(name) { return (name || 'U').split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2) }
function scrollBottom() { nextTick(() => { const el = scrollRef.value; if (el) el.scrollTop = el.scrollHeight }) }
function fmtTime(d) { return d ? dayjs(d).format('DD/MM HH:mm') : '' }

onMounted(() => { loadSessions(); pollTimer = setInterval(pollTick, 3000) })
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
</style>

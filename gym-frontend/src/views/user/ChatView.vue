<template>
  <div class="fade-in chat-page">
    <div class="page-header">
      <h2>TRỢ LÝ &amp; HỖ TRỢ</h2>
    </div>

    <div class="chat-layout">
      <!-- ── Danh sách hội thoại ─────────────────── -->
      <el-card class="conv-col" body-style="padding:0;">
        <!-- Trợ lý -->
        <div class="conv-item" :class="{ active: isBot }" @click="selectConv('bot')">
          <div class="avatar bot-avatar">🤖</div>
          <div class="conv-meta">
            <div class="conv-name">Trợ lý</div>
            <div class="conv-sub">Hỏi đáp tự động 24/7</div>
          </div>
        </div>

        <div class="conv-section">
          <span>Chat với admin</span>
          <el-button size="small" text @click="newConversation" title="Tạo cuộc hội thoại mới">
            <el-icon><Plus/></el-icon>
          </el-button>
        </div>

        <div v-if="!sessions.length" class="conv-empty">
          Chưa có cuộc hội thoại nào.<br/>Bấm <b>+</b> để nhắn với admin.
        </div>

        <div
          v-for="s in sessions" :key="s.id"
          class="conv-item" :class="{ active: activeConv === s.id }"
          @click="selectConv(s.id)">
          <div class="avatar admin-avatar">🎧</div>
          <div class="conv-meta">
            <div class="conv-name ellipsis">{{ s.subject }}</div>
            <div class="conv-sub ellipsis">
              {{ s.lastMessage || (s.status === 'PENDING' ? 'Đang chờ admin…' : 'Bắt đầu trò chuyện') }}
            </div>
          </div>
          <el-tag size="small" :type="s.status === 'ACTIVE' ? 'success' : 'warning'" effect="plain" round>
            {{ s.status === 'ACTIVE' ? 'Đang chat' : 'Chờ' }}
          </el-tag>
        </div>
      </el-card>

      <!-- ── Khung chat ──────────────────────────── -->
      <el-card class="chat-card" body-style="padding:0; display:flex; flex-direction:column; height:100%;">
        <div class="chat-head">
          <div class="head-info">
            <div class="avatar" :class="isBot ? 'bot-avatar' : 'admin-avatar'">{{ isBot ? '🤖' : '🎧' }}</div>
            <div>
              <div class="s-name">{{ isBot ? 'Trợ lý' : currentSession?.subject }}</div>
              <div class="s-meta">{{ headSub }}</div>
            </div>
          </div>
          <div class="head-actions">
            <el-button v-if="isBot && messages.length" text @click="clearChat" :loading="clearing" title="Xóa lịch sử">
              <el-icon><Delete/></el-icon>
            </el-button>
            <el-button v-if="isSupport" text type="danger" @click="closeCurrent">Kết thúc</el-button>
          </div>
        </div>

        <!-- Khung tin nhắn -->
        <div ref="scrollRef" class="messages">
          <!-- CHAT VỚI ADMIN -->
          <template v-if="isSupport">
            <div v-for="(m, i) in supportMessages" :key="'s'+i" class="msg-row" :class="m.senderRole === 'USER' ? 'from-user' : 'from-bot'">
              <div v-if="m.senderRole === 'ADMIN'" class="avatar admin-avatar sm">🎧</div>
              <div class="bubble" :class="m.senderRole === 'USER' ? 'bubble-user' : 'bubble-admin'">
                <MessageBody :message="m" />
                <div class="bubble-time">{{ fmtTime(m.createdAt) }}</div>
              </div>
              <div v-if="m.senderRole === 'USER'" class="avatar user-avatar sm">{{ initials }}</div>
            </div>

            <div v-if="showSent" class="sent-status" :class="{ err: sendStatus === 'failed' }">{{ sentText }}</div>

            <div v-if="!supportMessages.length && currentStatus === 'PENDING'" class="waiting-banner">
              <el-icon class="spin" :size="22"><Loading/></el-icon>
              <div>Yêu cầu đã được gửi. Đang chờ admin xác nhận để bắt đầu chat 1:1…</div>
            </div>
            <div v-else-if="!supportMessages.length" class="empty-state">Bắt đầu cuộc trò chuyện với admin…</div>
          </template>

          <!-- CHAT VỚI BOT -->
          <template v-else>
            <div v-for="(m, i) in messages" :key="i" class="msg-row" :class="m.sender === 'USER' ? 'from-user' : 'from-bot'">
              <div v-if="m.sender === 'BOT'" class="avatar bot-avatar sm">🤖</div>
              <div class="bubble" :class="m.sender === 'USER' ? 'bubble-user' : 'bubble-bot'">
                <MessageBody :message="m" />
                <div class="bubble-time">{{ fmtTime(m.createdAt) }}</div>
              </div>
              <div v-if="m.sender === 'USER'" class="avatar user-avatar sm">{{ initials }}</div>
            </div>

            <div v-if="showSent" class="sent-status" :class="{ err: sendStatus === 'failed' }">{{ sentText }}</div>

            <div v-if="typing" class="msg-row from-bot">
              <div class="avatar bot-avatar sm">🤖</div>
              <div class="bubble bubble-bot typing"><span class="dot"/><span class="dot"/><span class="dot"/></div>
            </div>
          </template>
        </div>

        <!-- Gợi ý (chỉ ở chế độ bot) -->
        <div v-if="isBot && suggestions.length" class="suggestions">
          <el-tag
            v-for="(s, i) in suggestions" :key="i"
            class="suggestion-chip" effect="plain" round
            @click="sendMessage(s)">
            {{ s }}
          </el-tag>
        </div>

        <!-- Ô nhập -->
        <div class="input-bar">
          <input ref="fileInput" type="file" hidden @change="onFile"/>
          <el-button
            v-if="isBot || (isSupport && currentStatus === 'ACTIVE')"
            class="attach-btn" text :loading="uploading" @click="pickFile" title="Đính kèm file">
            <el-icon :size="20"><Paperclip/></el-icon>
          </el-button>
          <el-input
            v-model="draft"
            :placeholder="inputPlaceholder"
            @keyup.enter="sendMessage()"
            :disabled="inputDisabled"
            clearable/>
          <el-button type="primary" :disabled="!canSend" :loading="isBot && typing" @click="sendMessage()">
            <el-icon><Promotion/></el-icon>
          </el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { chatAPI, supportAPI } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import MessageBody from '@/components/common/MessageBody.vue'
import dayjs from 'dayjs'

const auth = useAuthStore()

// 'bot' hoặc id (number) của một cuộc hội thoại với admin
const activeConv = ref('bot')

// Bot
const messages    = ref([])
const suggestions = ref([])
const typing      = ref(false)
const clearing    = ref(false)

// Admin
const sessions        = ref([])   // danh sách cuộc hội thoại đang mở
const supportMessages = ref([])   // tin nhắn của cuộc đang chọn
const prevStatuses    = new Map() // theo dõi PENDING → ACTIVE để báo

const draft      = ref('')
const sendStatus = ref(null)      // null | 'sending' | 'sent' | 'failed'
const scrollRef  = ref(null)
const fileInput  = ref(null)
const uploading  = ref(false)
let pollTimer = null

const MAX_FILE = 50 * 1024 * 1024   // 50MB

// ── Computed ──────────────────────────────────
const initials = computed(() =>
  (auth.user?.fullName || 'U').split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2))

const isBot        = computed(() => activeConv.value === 'bot')
const isSupport    = computed(() => typeof activeConv.value === 'number')
const currentSession = computed(() =>
  isSupport.value ? sessions.value.find(s => s.id === activeConv.value) : null)
const currentStatus  = computed(() => currentSession.value?.status)

const inputDisabled = computed(() =>
  (isBot.value && typing.value) || (isSupport.value && currentStatus.value !== 'ACTIVE'))

const canSend = computed(() => {
  if (!draft.value.trim()) return false
  return isBot.value ? !typing.value : currentStatus.value === 'ACTIVE'
})

const inputPlaceholder = computed(() => {
  if (isBot.value) return 'Nhập câu hỏi của bạn…'
  if (currentStatus.value === 'ACTIVE') return 'Nhắn với admin…'
  return 'Đang chờ admin xác nhận…'
})

const headSub = computed(() => {
  if (isBot.value) return 'Hỏi về gói tập, bài tập, lịch tập, dinh dưỡng…'
  if (currentStatus.value === 'ACTIVE')
    return `Admin ${currentSession.value?.adminName || ''} đang hỗ trợ`.trim()
  return 'Đang chờ admin xác nhận…'
})

// Chỉ hiện trạng thái gửi khi tin nhắn cuối là của user
const showSent = computed(() => {
  const list = isBot.value ? messages.value : supportMessages.value
  const last = list[list.length - 1]
  if (!last) return false
  return isBot.value ? last.sender === 'USER' : last.senderRole === 'USER'
})
const sentText = computed(() =>
  sendStatus.value === 'sending' ? 'Đang gửi…'
  : sendStatus.value === 'failed' ? '⚠ Gửi lỗi, thử lại' : 'Đã gửi ✓')

// ── Bot ───────────────────────────────────────
async function loadBot() {
  try {
    const [his, sug] = await Promise.all([chatAPI.getHistory(), chatAPI.suggestions()])
    messages.value    = his.data || []
    suggestions.value = sug.data || []
    if (!messages.value.length) {
      messages.value.push({
        sender: 'BOT',
        content: `Xin chào ${auth.user?.fullName || 'bạn'}! 👋 Mình là trợ lý của GymPro. `
          + 'Bạn có thể hỏi mình về gói tập, bài tập, lịch tập, dinh dưỡng và hồ sơ của bạn. '
          + 'Cần gặp người thật? Bấm dấu "+" ở cột bên trái để nhắn với admin nhé!',
        createdAt: new Date().toISOString()
      })
    }
  } catch {}
}

async function sendToBot(content) {
  messages.value.push({ sender: 'USER', content, createdAt: new Date().toISOString() })
  draft.value = ''
  suggestions.value = []
  typing.value = true
  sendStatus.value = 'sending'
  scrollBottom()
  try {
    const res = await chatAPI.send(content)
    sendStatus.value = 'sent'
    const data = res.data || {}
    messages.value.push({ sender: 'BOT', content: data.reply || 'Xin lỗi, đã có lỗi xảy ra.', createdAt: data.createdAt || new Date().toISOString() })
    suggestions.value = data.suggestions || []
  } catch {
    sendStatus.value = 'failed'
    messages.value.push({ sender: 'BOT', content: 'Xin lỗi, mình không kết nối được lúc này. Bạn thử lại sau nhé!', createdAt: new Date().toISOString() })
  } finally { typing.value = false; scrollBottom() }
}

async function clearChat() {
  clearing.value = true
  try { await chatAPI.clear(); messages.value = []; ElMessage.success('Đã xóa lịch sử chat'); loadBot() }
  catch {} finally { clearing.value = false }
}

// ── Admin (nhiều cuộc hội thoại) ──────────────
async function loadSessions() {
  try {
    const res = await supportAPI.sessions()
    const list = res.data || []
    list.forEach(s => {
      const prev = prevStatuses.get(s.id)
      if (prev === 'PENDING' && s.status === 'ACTIVE') {
        ElMessage.success(`Admin đã tham gia cuộc "${s.subject}"! 🎧`)
        if (activeConv.value === s.id) loadSessionMessages(s.id)
      }
      prevStatuses.set(s.id, s.status)
    })
    // Cuộc đang chọn biến mất (admin từ chối / kết thúc)
    if (isSupport.value && !list.find(s => s.id === activeConv.value)) {
      ElMessage.info('Cuộc hội thoại với admin đã kết thúc.')
      activeConv.value = 'bot'
      supportMessages.value = []
    }
    sessions.value = list
  } catch {}
}

async function loadSessionMessages(id) {
  try { const res = await supportAPI.messages(id); supportMessages.value = res.data || []; scrollBottom() }
  catch {}
}

async function newConversation() {
  try {
    const { value } = await ElMessageBox.prompt(
      'Bạn cần admin hỗ trợ vấn đề gì?', 'Cuộc hội thoại mới với admin',
      {
        confirmButtonText: 'Gửi yêu cầu',
        cancelButtonText: 'Hủy',
        inputPlaceholder: 'VD: Hỏi về gói tập, thanh toán, lịch tập…',
        inputValidator: v => (v && v.trim()) ? true : 'Vui lòng nhập vấn đề cần hỗ trợ'
      })
    const res = await supportAPI.request(value.trim())
    const s = res.data
    prevStatuses.set(s.id, s.status)
    await loadSessions()
    await selectConv(s.id)
    ElMessage.success('Đã gửi yêu cầu, đang chờ admin xác nhận…')
  } catch { /* người dùng bấm Hủy */ }
}

async function selectConv(target) {
  activeConv.value = target
  sendStatus.value = null
  draft.value = ''
  if (target === 'bot') { scrollBottom(); return }
  await loadSessionMessages(target)
}

async function sendToAdmin(content) {
  const id = activeConv.value
  const optimistic = { senderRole: 'USER', content, createdAt: new Date().toISOString() }
  supportMessages.value.push(optimistic)
  draft.value = ''
  sendStatus.value = 'sending'
  scrollBottom()
  try { await supportAPI.send(id, content); sendStatus.value = 'sent'; await loadSessionMessages(id) }
  catch { sendStatus.value = 'failed'; supportMessages.value = supportMessages.value.filter(m => m !== optimistic) }
}

function pickFile() { fileInput.value?.click() }

async function onFile(e) {
  const file = e.target.files?.[0]
  e.target.value = ''                       // cho phép chọn lại cùng file
  if (!file) return
  if (file.size > MAX_FILE) { ElMessage.error('File tối đa 50MB'); return }
  if (isBot.value) { sendBotFile(file, draft.value.trim()); return }

  const id = activeConv.value
  uploading.value = true
  sendStatus.value = 'sending'
  try {
    const fd = new FormData()
    fd.append('file', file)
    if (draft.value.trim()) fd.append('caption', draft.value.trim())
    await supportAPI.sendFile(id, fd)
    draft.value = ''
    sendStatus.value = 'sent'
    await loadSessionMessages(id)
  } catch { sendStatus.value = 'failed' }
  finally { uploading.value = false }
}

async function sendBotFile(file, caption) {
  uploading.value = true
  typing.value = true
  sendStatus.value = 'sending'
  suggestions.value = []
  scrollBottom()
  try {
    const fd = new FormData()
    fd.append('file', file)
    if (caption) fd.append('caption', caption)
    const res = await chatAPI.sendFile(fd)
    draft.value = ''
    sendStatus.value = 'sent'
    await loadBot()                          // tải lại lịch sử để hiển thị đính kèm + trả lời của bot
    suggestions.value = res.data?.suggestions || suggestions.value
  } catch { sendStatus.value = 'failed' }
  finally { uploading.value = false; typing.value = false; scrollBottom() }
}

async function closeCurrent() {
  const id = activeConv.value
  if (typeof id !== 'number') return
  try { await supportAPI.close(id) } catch {}
  prevStatuses.delete(id)
  activeConv.value = 'bot'
  supportMessages.value = []
  await loadSessions()
  ElMessage.info('Đã kết thúc cuộc hội thoại với admin')
}

// ── Điều phối gửi tin ─────────────────────────
function sendMessage(text) {
  const content = (text ?? draft.value).trim()
  if (!content) return
  if (isBot.value) { if (typing.value) return; sendToBot(content) }
  else { if (currentStatus.value !== 'ACTIVE') return; sendToAdmin(content) }
}

// ── Polling ───────────────────────────────────
function startPolling() { stopPolling(); pollTimer = setInterval(pollTick, 3000) }
function stopPolling() { if (pollTimer) { clearInterval(pollTimer); pollTimer = null } }

async function pollTick() {
  await loadSessions()
  if (isSupport.value && currentStatus.value === 'ACTIVE') {
    await loadSessionMessages(activeConv.value)
  }
}

function scrollBottom() { nextTick(() => { const el = scrollRef.value; if (el) el.scrollTop = el.scrollHeight }) }
function fmtTime(d) { return d ? dayjs(d).format('HH:mm') : '' }

onMounted(async () => { await loadBot(); await loadSessions(); startPolling(); scrollBottom() })
onUnmounted(stopPolling)
</script>

<style scoped>
.chat-page { display:flex; flex-direction:column; height:100%; }

.chat-layout { flex:1; min-height:0; display:flex; gap:16px; }

/* ── Cột danh sách hội thoại ── */
.conv-col { width:280px; flex-shrink:0; overflow-y:auto; }
.conv-item {
  display:flex; align-items:center; gap:10px; padding:12px 14px;
  cursor:pointer; border-bottom:1px solid var(--c-bg3); transition:background 0.15s;
}
.conv-item:hover { background:var(--c-card2); }
.conv-item.active { background:var(--c-card2); border-left:3px solid var(--c-accent); }
.conv-meta { flex:1; min-width:0; }
.conv-name { font-size:0.9rem; font-weight:600; color:var(--c-text); }
.conv-sub  { font-size:0.75rem; color:var(--c-text3); margin-top:2px; }
.conv-section {
  display:flex; align-items:center; justify-content:space-between;
  padding:10px 14px 4px; font-family:var(--font-display); letter-spacing:0.05em;
  font-size:0.75rem; text-transform:uppercase; color:var(--c-text3);
}
.conv-empty { padding:10px 14px; font-size:0.8rem; color:var(--c-text3); line-height:1.5; }

/* ── Khung chat ── */
.chat-card { flex:1; min-width:0; overflow:hidden; display:flex; flex-direction:column; }
.chat-head {
  height:62px; padding:0 18px; flex-shrink:0;
  display:flex; align-items:center; justify-content:space-between;
  border-bottom:1px solid var(--c-bg3); background:var(--c-card);
}
.head-info { display:flex; align-items:center; gap:10px; }
.s-name { font-size:0.95rem; font-weight:600; color:var(--c-text); }
.s-meta { font-size:0.75rem; color:var(--c-text3); }
.head-actions { display:flex; align-items:center; gap:6px; }

.messages {
  flex:1; overflow-y:auto; padding:20px;
  display:flex; flex-direction:column; gap:14px; background:var(--c-bg);
}
.msg-row { display:flex; align-items:flex-end; gap:8px; max-width:80%; }
.from-bot  { align-self:flex-start; }
.from-user { align-self:flex-end; }

.avatar {
  width:32px; height:32px; border-radius:50%; flex-shrink:0;
  display:flex; align-items:center; justify-content:center;
  font-family:var(--font-display); font-size:0.85rem;
}
.avatar.sm { width:30px; height:30px; }
.bot-avatar   { background:var(--c-bg2); font-size:1rem; }
.admin-avatar { background:#1f6f43; font-size:1rem; }
.user-avatar  { background:var(--c-accent); color:#fff; }

.bubble { padding:10px 14px; border-radius:var(--radius-lg); box-shadow:var(--shadow); }
.bubble-bot   { background:var(--c-card); color:var(--c-text); border-bottom-left-radius:4px; }
.bubble-admin { background:#EAF7EF; color:var(--c-text); border-bottom-left-radius:4px; border:1px solid #C8E6D4; }
.bubble-user  { background:var(--c-accent); color:#fff; border-bottom-right-radius:4px; }
.bubble-text  { font-size:0.9rem; line-height:1.55; white-space:pre-line; word-break:break-word; }
.bubble-time  { font-size:0.68rem; opacity:0.6; margin-top:4px; text-align:right; }

.sent-status { align-self:flex-end; font-size:0.68rem; color:var(--c-text3); margin:-8px 42px 0 0; }
.sent-status.err { color:#c0392b; }

.typing { display:flex; gap:4px; padding:14px; }
.dot { width:7px; height:7px; border-radius:50%; background:var(--c-text3); animation:blink 1.2s infinite ease-in-out; }
.dot:nth-child(2){ animation-delay:0.2s; }
.dot:nth-child(3){ animation-delay:0.4s; }
@keyframes blink { 0%,80%,100%{ opacity:0.3; } 40%{ opacity:1; } }

.waiting-banner {
  align-self:center; text-align:center; max-width:380px; margin-top:8px;
  display:flex; flex-direction:column; align-items:center; gap:10px;
  padding:18px 20px; background:var(--c-card); border:1px dashed var(--c-accent);
  border-radius:var(--radius-lg); color:var(--c-text2); font-size:0.86rem; line-height:1.5;
}
.spin { animation:rot 1s linear infinite; }
@keyframes rot { to { transform:rotate(360deg); } }

.suggestions {
  display:flex; flex-wrap:wrap; gap:8px;
  padding:12px 16px; border-top:1px solid var(--c-bg3); background:var(--c-card);
}
.suggestion-chip { cursor:pointer; transition:all 0.15s; }
.suggestion-chip:hover { background:var(--c-accent) !important; color:#fff !important; border-color:var(--c-accent) !important; }

.input-bar { display:flex; align-items:center; gap:10px; padding:14px 16px; border-top:1px solid var(--c-bg3); background:var(--c-card); }
.input-bar .el-input { flex:1; }
.attach-btn { color:var(--c-text2) !important; padding:0 4px; }
.attach-btn:hover { color:var(--c-accent) !important; }
</style>

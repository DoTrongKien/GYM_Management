import { ref, computed } from 'vue'

/**
 * Theo dõi tin nhắn hỗ trợ chưa đọc.
 *
 * Mỗi phiên được đánh dấu "đã đọc tới thời điểm X" (lastMessageAt lúc user/admin
 * mở xem). Một phiên là chưa đọc khi tin nhắn cuối do PHÍA BÊN KIA gửi và mới
 * hơn mốc đã đọc. Mốc đã đọc lưu vào sessionStorage để không mất khi F5.
 */

const readAt    = ref({})        // { [sessionId]: ISO string }
const sessions  = ref([])        // danh sách phiên mới nhất từ poll
const otherRole = ref('ADMIN')   // vai trò của "phía bên kia"
let storageKey  = 'support_read_at_ADMIN'

function persist() {
  try { sessionStorage.setItem(storageKey, JSON.stringify(readAt.value)) } catch {}
}

/** Gọi một lần ở layout: 'ADMIN' cho phía user, 'USER' cho phía admin. */
export function setOtherRole(role) {
  otherRole.value = role
  storageKey = `support_read_at_${role}`
  try { readAt.value = JSON.parse(sessionStorage.getItem(storageKey) || '{}') }
  catch { readAt.value = {} }
}

/** Cập nhật danh sách phiên sau mỗi lần poll. */
export function setSessions(list) {
  sessions.value = Array.isArray(list) ? list : []
}

/** Đánh dấu đã đọc một phiên (khi mở xem cuộc hội thoại đó). */
export function markRead(sessionId) {
  const s = sessions.value.find(x => x.id === sessionId)
  if (!s?.lastMessageAt) return
  if (readAt.value[sessionId] === s.lastMessageAt) return
  readAt.value = { ...readAt.value, [sessionId]: s.lastMessageAt }
  persist()
}

/** Phiên này có tin nhắn chưa đọc từ phía bên kia không? */
export function isUnread(s) {
  if (!s || s.lastMessageRole !== otherRole.value || !s.lastMessageAt) return false
  const r = readAt.value[s.id]
  return !r || new Date(s.lastMessageAt).getTime() > new Date(r).getTime()
}

/** Số cuộc hội thoại đang có tin nhắn chưa đọc. */
export const unreadCount = computed(() => sessions.value.filter(isUnread).length)

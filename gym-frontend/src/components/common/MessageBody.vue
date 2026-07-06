<template>
  <div class="msg-body">
    <!-- ── Đính kèm ── -->
    <template v-if="message.attachmentUrl">
      <!-- Ảnh -->
      <div v-if="isImage" class="att-media">
        <img :src="message.attachmentUrl" class="att-img" @click="openFile(message.attachmentUrl)" loading="lazy"/>
        <a :href="message.attachmentUrl" :download="message.attachmentName" class="att-dl-link">
          <el-icon><Download/></el-icon> Tải xuống
        </a>
      </div>
      <!-- Video -->
      <div v-else-if="isVideo" class="att-media">
        <video :src="message.attachmentUrl" class="att-video" controls preload="metadata"/>
        <a :href="message.attachmentUrl" :download="message.attachmentName" class="att-dl-link">
          <el-icon><Download/></el-icon> Tải xuống
        </a>
      </div>
      <!-- File khác -->
      <a v-else :href="message.attachmentUrl" :download="message.attachmentName" class="att-file">
        <div class="att-icon"><el-icon :size="22"><Document/></el-icon></div>
        <div class="att-info">
          <div class="att-name">{{ message.attachmentName || 'Tệp đính kèm' }}</div>
          <div class="att-size">{{ prettySize }}</div>
        </div>
        <el-icon class="att-dl-ico" :size="18"><Download/></el-icon>
      </a>
    </template>

    <!-- ── Nội dung chữ + link ── -->
    <div v-if="message.content" class="msg-text">
      <template v-for="(p, i) in parts" :key="i"><a
        v-if="p.type === 'link'" :href="p.href" target="_blank" rel="noopener noreferrer" class="msg-link">{{ p.text }}</a><span v-else>{{ p.text }}</span></template>
    </div>

    <!-- ── Embed (YouTube / ảnh từ link) ── -->
    <div v-for="(e, i) in embeds" :key="'em'+i" class="embed">
      <div v-if="e.type === 'youtube'" class="embed-yt">
        <iframe :src="e.src" allow="accelerographer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen frameborder="0"/>
      </div>
      <img v-else-if="e.type === 'image'" :src="e.src" class="att-img embed-img" @click="openFile(e.src)" loading="lazy"/>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ message: { type: Object, required: true } })

const isImage = computed(() => (props.message.attachmentType || '').startsWith('image/'))
const isVideo = computed(() => (props.message.attachmentType || '').startsWith('video/'))

const prettySize = computed(() => {
  const b = props.message.attachmentSize
  if (!b && b !== 0) return ''
  if (b < 1024) return b + ' B'
  if (b < 1024 * 1024) return (b / 1024).toFixed(1) + ' KB'
  return (b / 1024 / 1024).toFixed(1) + ' MB'
})

// Tách nội dung thành đoạn chữ + link
const URL_RE = /(https?:\/\/[^\s<]+)/g
const parts = computed(() => {
  const text = props.message.content || ''
  const out = []
  let last = 0, m
  URL_RE.lastIndex = 0
  while ((m = URL_RE.exec(text)) !== null) {
    if (m.index > last) out.push({ type: 'text', text: text.slice(last, m.index) })
    out.push({ type: 'link', text: m[0], href: m[0] })
    last = m.index + m[0].length
  }
  if (last < text.length) out.push({ type: 'text', text: text.slice(last) })
  return out
})

function ytId(url) {
  const m = /(?:youtu\.be\/|youtube\.com\/(?:watch\?v=|embed\/|v\/|shorts\/))([\w-]{11})/.exec(url)
  return m ? m[1] : null
}
const IMG_RE = /\.(png|jpe?g|gif|webp|bmp|svg)(\?.*)?$/i

// Sinh embed từ các link (tối đa 2 để tránh rối)
const embeds = computed(() => {
  const links = parts.value.filter(p => p.type === 'link').map(p => p.href)
  const out = []
  for (const url of links) {
    const id = ytId(url)
    if (id) { out.push({ type: 'youtube', src: `https://www.youtube.com/embed/${id}` }) }
    else if (IMG_RE.test(url)) { out.push({ type: 'image', src: url }) }
    if (out.length >= 2) break
  }
  return out
})

function openFile(url) { window.open(url, '_blank', 'noopener') }
</script>

<style scoped>
.msg-body { display:flex; flex-direction:column; gap:8px; }

.msg-text { font-size:0.9rem; line-height:1.55; white-space:pre-line; word-break:break-word; }
.msg-link { color:inherit; text-decoration:underline; font-weight:600; word-break:break-all; }
.msg-link:hover { opacity:0.85; }

/* Ảnh / video */
.att-media { display:flex; flex-direction:column; gap:4px; }
.att-img { max-width:260px; max-height:260px; border-radius:10px; cursor:pointer; display:block; object-fit:cover; }
.att-video { max-width:300px; max-height:300px; border-radius:10px; background:#000; }
.att-dl-link {
  display:inline-flex; align-items:center; gap:4px; font-size:0.72rem;
  color:inherit; opacity:0.75; text-decoration:none;
}
.att-dl-link:hover { opacity:1; text-decoration:underline; }

/* File khác */
.att-file {
  display:flex; align-items:center; gap:10px; text-decoration:none; color:inherit;
  padding:10px 12px; border-radius:10px; background:rgba(0,0,0,0.06);
  min-width:200px; max-width:280px; transition:background 0.15s;
}
.att-file:hover { background:rgba(0,0,0,0.12); }
.att-icon { flex-shrink:0; opacity:0.8; }
.att-info { flex:1; min-width:0; }
.att-name { font-size:0.84rem; font-weight:600; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.att-size { font-size:0.7rem; opacity:0.65; }
.att-dl-ico { flex-shrink:0; opacity:0.7; }

/* Embed */
.embed-yt { position:relative; width:280px; max-width:100%; aspect-ratio:16/9; }
.embed-yt iframe { position:absolute; inset:0; width:100%; height:100%; border-radius:10px; }
.embed-img { max-width:280px; }
</style>

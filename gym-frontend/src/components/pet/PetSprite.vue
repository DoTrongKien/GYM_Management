<!--
============================================================
FILE: src/components/pet/PetSprite.vue
Pixel-art pet dạng "bé trai anh hùng" phong cách game 2D (Goku style).
- Canvas 32x48, có shading (bóng đổ) + highlight cho khối 3D.
- Cơ bụng (abs) cho các stage gầy/săn chắc, bụng phệ cho OVERWEIGHT.
- Walk animation mượt: chân sải bước + tay đánh nhịp theo legFrame.
- Recolor Goku: gi cam, đai + băng cổ tay xanh, tóc chóp vàng Super Saiyan.

API GIỮ NGUYÊN: props { stage, legFrame } -> KHÔNG cần đụng PetWidget.vue.
============================================================
-->
<template>
  <svg width="96" height="144" viewBox="0 0 32 48" shape-rendering="crispEdges">
    <!-- vẽ từ sau ra trước: chân -> quần -> thân -> tay -> đầu -> tóc -->
    <g v-html="legsHtml"></g>
    <g v-html="shortsHtml"></g>
    <g v-html="torsoHtml"></g>
    <g v-html="armsHtml"></g>
    <g v-html="headHtml"></g>
    <g v-html="hairHtml"></g>
  </svg>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  stage: { type: String, required: true }, // SLIM | LEAN | FIT | AVERAGE | OVERWEIGHT
  legFrame: { type: String, default: 'a' } // 'a' | 'b'
})

/* ---------- BẢNG MÀU (base / shadow / highlight) ---------- */
const skin = '#F0C9A0'; const skinD = '#D2A276'; const skinH = '#FBE3C1'
// Tóc vàng Super Saiyan
const hair = '#F5C400'; const hairD = '#C79A00'; const hairH = '#FFE873'
// Quần gi cam (dùng cho phần short/quần dưới)
const shorts = '#E8641E'; const shortsD = '#B84A12'; const shortsL = '#F5883F'
const shoe = '#2C2C2A'; const shoeH = '#4A4A46'
const eyeWhite = '#F7F2E8'; const pupil = '#2B2B29'; const mouth = '#B4553C'; const blush = '#E8A98A'
// Đai lưng + băng cổ tay xanh Goku
const belt = '#20489B'; const beltL = '#3A62BE'

/* helper tạo 1 pixel-rect */
const r = (x, y, w, h, f) => `<rect x="${x}" y="${y}" width="${w}" height="${h}" fill="${f}"/>`

/* ---------- CẤU HÌNH THEO THỂ TRẠNG ----------
   shoulderW/waistW: bề ngang vai/eo (px)
   armW/legW: độ dày tay/chân
   abs: hiện cơ bụng ; belly: hiện bụng phệ
   shirt/shirtD/shirtL: màu áo (gi cam) + bóng + sáng
*/
const STAGE_CONFIG = {
  SLIM:       { shoulderW: 9,  waistW: 7,  armW: 2, legW: 3, abs: true,  belly: false, shirt: '#E8641E', shirtD: '#B84A12', shirtL: '#F5883F' },
  LEAN:       { shoulderW: 11, waistW: 8,  armW: 2, legW: 3, abs: true,  belly: false, shirt: '#E8641E', shirtD: '#B84A12', shirtL: '#F5883F' },
  FIT:        { shoulderW: 13, waistW: 9,  armW: 3, legW: 4, abs: true,  belly: false, shirt: '#E8641E', shirtD: '#B84A12', shirtL: '#F5883F' },
  AVERAGE:    { shoulderW: 12, waistW: 12, armW: 3, legW: 4, abs: false, belly: false, shirt: '#E8641E', shirtD: '#B84A12', shirtL: '#F5883F' },
  OVERWEIGHT: { shoulderW: 13, waistW: 16, armW: 4, legW: 4, abs: false, belly: true,  shirt: '#E8641E', shirtD: '#B84A12', shirtL: '#F5883F' }
}
const cfg = computed(() => STAGE_CONFIG[props.stage] || STAGE_CONFIG.AVERAGE)

/* ---------- HÌNH HỌC THÂN ----------
   Thân là hình thang: rộng ở vai, thu ở eo. OVERWEIGHT phình ở bụng dưới.
*/
const CENTER = 16
const TORSO_TOP = 16
const TORSO_H = 14 // các hàng y: 16..29

function torsoWidthAt(row, c) {
  const t = row / (TORSO_H - 1) // 0=vai, 1=eo
  let w = Math.round(c.shoulderW + (c.waistW - c.shoulderW) * t)
  if (c.belly) {
    const bulge = Math.sin(t * Math.PI) // phình giữa
    w += Math.round(bulge * 2)
    if (t > 0.5) w += 2 // bụng dưới xệ ra
  }
  return w
}

/* ---------- ĐẦU + MẶT (cố định, không đổi theo stage) ---------- */
const headHtml = computed(() => {
  let s = ''
  s += r(14, 15, 4, 2, skin)
  s += r(14, 16, 4, 1, skinD)
  s += r(11, 6, 10, 9, skin)
  s += r(20, 7, 1, 7, skinD)
  s += r(12, 14, 8, 1, skinD)
  s += r(10, 10, 1, 3, skin) + r(21, 10, 1, 3, skin)
  s += r(10, 11, 1, 2, skinD) + r(21, 11, 1, 2, skinD)
  s += r(12, 9, 3, 1, hairD) + r(17, 9, 3, 1, hairD)
  s += r(12, 10, 3, 2, eyeWhite) + r(17, 10, 3, 2, eyeWhite)
  s += r(13, 10, 1, 2, pupil) + r(18, 10, 1, 2, pupil)
  s += r(14, 10, 1, 1, '#FFFFFF') + r(19, 10, 1, 1, '#FFFFFF')
  s += r(12, 12, 1, 1, blush) + r(19, 12, 1, 1, blush)
  s += r(16, 12, 1, 1, skinD)
  s += r(15, 13, 2, 1, mouth)
  return s
})

/* ---------- TÓC (spiky vàng Super Saiyan) ---------- */
const hairHtml = computed(() => {
  let s = ''
  s += r(11, 5, 10, 1, hair)
  s += r(12, 4, 8, 1, hair)
  s += r(12, 3, 1, 1, hair) + r(15, 3, 1, 1, hair) + r(18, 3, 1, 1, hair)
  s += r(15, 2, 1, 1, hair)
  s += r(11, 6, 2, 1, hair) + r(19, 6, 2, 1, hair)
  s += r(14, 6, 1, 1, hair) + r(16, 6, 1, 1, hair)
  s += r(10, 7, 1, 2, hair) + r(21, 7, 1, 2, hair)
  s += r(12, 5, 4, 1, hairH)
  s += r(11, 6, 1, 1, hairH)
  s += r(19, 5, 1, 1, hairD)
  return s
})

/* ---------- THÂN + ÁO GI CAM + CƠ BỤNG / BỤNG PHỆ + ĐAI XANH ---------- */
const torsoHtml = computed(() => {
  const c = cfg.value
  let s = ''
  for (let i = 0; i < TORSO_H; i++) {
    const y = TORSO_TOP + i
    const w = torsoWidthAt(i, c)
    const x = CENTER - Math.floor(w / 2)
    s += r(x, y, w, 1, c.shirt)
    s += r(x, y, 1, 1, c.shirtL)          // highlight cạnh trái
    s += r(x + w - 1, y, 1, 1, c.shirtD)  // bóng cạnh phải
  }
  // cổ áo
  s += r(13, TORSO_TOP, 6, 1, c.shirtD)
  s += r(14, TORSO_TOP, 4, 1, c.shirtL)

  if (c.abs) {
    s += r(12, 18, 3, 2, c.shirtL) + r(18, 18, 3, 2, c.shirtL)
    for (let ly = 20; ly <= 27; ly++) s += r(CENTER, ly, 1, 1, c.shirtD)
    ;[21, 24, 27].forEach(ay => { s += r(13, ay, 6, 1, c.shirtD) })
  }

  if (c.belly) {
    s += r(13, 22, 7, 1, c.shirtL)
    s += r(CENTER, 25, 1, 2, c.shirtD)
    s += r(11, 27, 11, 1, c.shirtD)
  }

  // Đai lưng xanh Goku ở đáy thân
  const waistW = torsoWidthAt(TORSO_H - 1, c)
  const waistX = CENTER - Math.floor(waistW / 2)
  s += r(waistX, 28, waistW, 2, belt)
  s += r(waistX, 28, waistW, 1, beltL)

  return s
})

/* ---------- TAY (áo tay ngắn + da + băng cổ tay xanh + bàn tay) ---------- */
function armColumn(x, y, w, h, c) {
  let s = ''
  const sleeve = 4
  s += r(x, y, w, sleeve, c.shirt)
  s += r(x, y, 1, sleeve, c.shirtL)
  s += r(x + w - 1, y, 1, sleeve, c.shirtD)
  s += r(x, y + sleeve, w, h - sleeve, skin)
  s += r(x, y + sleeve, 1, h - sleeve, skinH)
  s += r(x + w - 1, y + sleeve, 1, h - sleeve, skinD)
  // băng cổ tay xanh
  s += r(x, y + h - 1, w, 1, belt)
  // bàn tay
  s += r(x, y + h, w, 1, skinD)
  return s
}
const armsHtml = computed(() => {
  const c = cfg.value
  const shoulderW = torsoWidthAt(0, c)
  const lx = CENTER - Math.floor(shoulderW / 2) - c.armW
  const rx = CENTER + Math.ceil(shoulderW / 2)
  const swing = props.legFrame === 'a' ? 1 : -1
  const top = 16
  const h = 11
  let s = ''
  s += armColumn(lx, top + Math.max(0, -swing), c.armW, h, c)
  s += armColumn(rx, top + Math.max(0, swing), c.armW, h, c)
  return s
})

/* ---------- QUẦN SHORT (gi cam, dùng bộ màu shorts riêng) ---------- */
const shortsHtml = computed(() => {
  const c = cfg.value
  const w = torsoWidthAt(TORSO_H - 1, c)
  const x = CENTER - Math.floor(w / 2)
  const y = TORSO_TOP + TORSO_H // 30
  let s = ''
  s += r(x, y, w, 4, shorts)
  s += r(x, y, 1, 4, shortsL)
  s += r(x + w - 1, y, 1, 4, shortsD)
  s += r(CENTER, y + 2, 1, 2, shortsD) // khe 2 ống
  return s
})

/* ---------- CHÂN + GIÀY (walk animation) ---------- */
function legPiece(x, top, w, h, footShift) {
  let s = ''
  s += r(x, top, w, h, skin)
  s += r(x, top, 1, h, skinH)
  s += r(x + w - 1, top, 1, h, skinD)
  s += r(x + footShift, top + h, w + 1, 2, shoe)
  s += r(x + footShift, top + h, w + 1, 1, shoeH)
  return s
}
const legsHtml = computed(() => {
  const c = cfg.value
  const w = torsoWidthAt(TORSO_H - 1, c)
  const gap = 1
  const legTop = TORSO_TOP + TORSO_H + 4 // 34
  const legH = 8
  const leftX = CENTER - gap - c.legW
  const rightX = CENTER + gap
  const fwd = props.legFrame === 'a'
  let s = ''
  if (fwd) {
    s += legPiece(leftX, legTop + 1, c.legW, legH - 1, 1)
    s += legPiece(rightX, legTop, c.legW, legH, -1)
  } else {
    s += legPiece(leftX, legTop, c.legW, legH, -1)
    s += legPiece(rightX, legTop + 1, c.legW, legH - 1, 1)
  }
  return s
})
</script>
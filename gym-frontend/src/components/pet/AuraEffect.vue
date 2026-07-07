<!--
============================================================
FILE: src/components/pet/AuraEffect.vue
Hào quang lửa (kiểu Super Saiyan - Dragon Ball) BAO QUANH pet.
Ngọn lửa lớn, rộng, bốc lên cao hơn đầu + loe ra 2 bên.
Màu theo bậc streak (auraTier). Không hiện gì khi tier = NONE.

QUAN TRỌNG: KHÔNG set position/inset/width/height/z-index trong
file này nữa. Toàn bộ vị trí + kích thước + z-index do PetWidget.vue
(.layer-aura) quyết định, để tránh xung đột 2 nguồn CSS.
============================================================
-->
<template>
  <svg
    v-if="tier !== 'NONE'"
    class="aura-layer"
    viewBox="-30 -40 92 128"
    preserveAspectRatio="xMidYMid meet"
  >
    <defs>
      <linearGradient :id="gradId" x1="0" y1="1" x2="0" y2="0">
        <stop offset="0%" :stop-color="color" stop-opacity="0.25" />
        <stop offset="45%" :stop-color="color" stop-opacity="0.85" />
        <stop offset="100%" :stop-color="light" stop-opacity="1" />
      </linearGradient>
      <radialGradient :id="glowId" cx="50%" cy="55%" r="55%">
        <stop offset="0%" :stop-color="light" stop-opacity="0.35" />
        <stop offset="70%" :stop-color="color" stop-opacity="0.18" />
        <stop offset="100%" :stop-color="color" stop-opacity="0" />
      </radialGradient>
    </defs>

    <!-- Quầng sáng nền, bao trọn nhân vật -->
    <ellipse cx="16" cy="28" rx="42" ry="56" :fill="`url(#${glowId})`" class="glow" />

    <!-- Quầng lửa ngoài, LOE RỘNG 2 bên + cao qua đầu -->
    <path
      class="flame flame-outer"
      :fill="color"
      opacity="0.3"
      d="M16 -38
         L28 -8 L22 -12 L40 6 L31 2 L52 26 L42 22 L54 52
         Q54 78 16 82
         Q-22 78 -22 52
         L-10 22 L-20 26 L1 2 L-8 6 L10 -12 L4 -8 Z"
    />

    <!-- Ngọn lửa chính -->
    <path
      class="flame flame-main"
      :fill="`url(#${gradId})`"
      d="M16 -30
         L26 -6 L20 -10 L35 6 L27 2 L45 24 L36 20 L46 50
         Q46 74 16 78
         Q-14 74 -14 50
         L-4 20 L-13 24 L5 2 L-3 6 L12 -10 L6 -6 Z"
    />

    <!-- Lõi lửa sáng -->
    <path
      class="flame flame-core"
      :fill="light"
      opacity="0.85"
      d="M16 -14
         L23 6 L18 2 L28 20 L22 16 L32 44
         Q32 66 16 68
         Q0 66 0 44
         L10 16 L4 20 L14 2 L9 6 Z"
    />

    <!-- Tàn lửa bay lên -->
    <g :fill="light" class="embers">
      <circle cx="-10" cy="30" r="1.6" />
      <circle cx="44" cy="22" r="1.4" />
      <circle cx="2" cy="0" r="1.2" />
      <circle cx="34" cy="4" r="1.5" />
      <circle cx="16" cy="-22" r="1.4" />
      <circle cx="-4" cy="-6" r="1.1" />
    </g>
  </svg>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  tier: { type: String, default: 'NONE' } // NONE|BLUE|RED|PURPLE|YELLOW|GREEN|BLACK
})

const COLORS = {
  BLUE:   '#3B82F6',
  RED:    '#DC2626',
  PURPLE: '#9333EA',
  YELLOW: '#FACC15',
  GREEN:  '#16A34A',
  BLACK:  '#111111'
}
const LIGHTS = {
  BLUE:   '#BFDBFE',
  RED:    '#FCA5A5',
  PURPLE: '#E9D5FF',
  YELLOW: '#FEF9C3',
  GREEN:  '#BBF7D0',
  BLACK:  '#6B7280'
}

const color = computed(() => COLORS[props.tier] || '#3B82F6')
const light = computed(() => LIGHTS[props.tier] || '#BFDBFE')
const gradId = computed(() => `aura-grad-${props.tier}`)
const glowId = computed(() => `aura-glow-${props.tier}`)
</script>

<style scoped>
.aura-layer {
  /* KHÔNG set position/inset/width/height/z-index ở đây.
     PetWidget.vue (.layer-aura) toàn quyền định vị + kích thước + z-index. */
  pointer-events: none;
  overflow: visible;
  display: block;
  width: 100%;
  height: 100%;
}

.glow { animation: glow-pulse 2s ease-in-out infinite; }

.flame {
  transform-origin: 16px 80px; /* gốc lửa ở chân */
  will-change: transform, opacity;
}
.flame-outer { animation: flame-sway 1.6s ease-in-out infinite; }
.flame-main  { animation: flame-flicker 0.9s ease-in-out infinite; }
.flame-core  { animation: flame-flicker 0.6s ease-in-out infinite reverse; }

@keyframes flame-flicker {
  0%, 100% { transform: scaleY(1) scaleX(1); opacity: 0.95; }
  50%      { transform: scaleY(1.1) scaleX(0.95); opacity: 0.75; }
}
@keyframes flame-sway {
  0%, 100% { transform: scaleY(1) skewX(0deg); }
  50%      { transform: scaleY(1.14) skewX(-2deg); }
}
@keyframes glow-pulse {
  0%, 100% { opacity: 0.8; transform: scale(1); }
  50%      { opacity: 1;   transform: scale(1.06); }
}

.embers circle { animation: ember-rise 1.8s ease-in infinite; }
.embers circle:nth-child(2) { animation-delay: 0.4s; }
.embers circle:nth-child(3) { animation-delay: 0.8s; }
.embers circle:nth-child(4) { animation-delay: 1.1s; }
.embers circle:nth-child(5) { animation-delay: 1.4s; }
.embers circle:nth-child(6) { animation-delay: 1.6s; }

@keyframes ember-rise {
  0%   { transform: translateY(0); opacity: 0; }
  20%  { opacity: 1; }
  100% { transform: translateY(-26px); opacity: 0; }
}

@media (prefers-reduced-motion: reduce) {
  .flame, .embers circle, .glow { animation: none; }
}
</style>
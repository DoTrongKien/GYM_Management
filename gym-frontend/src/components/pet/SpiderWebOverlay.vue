<!--
============================================================
FILE MỚI: src/components/pet/SpiderWebOverlay.vue
Vẽ N mạng nhện quanh pet theo webCount (0-6), mỗi mạng ở 1 góc/vị trí khác nhau.
============================================================
-->
<template>
  <svg v-if="count > 0" class="web-layer" viewBox="0 0 32 48">
    <g v-for="(pos, i) in positions" :key="i" :transform="pos">
      <path d="M0 0 L4 0 M0 0 L0 4 M0 0 L3 3 M2 0 L2 2 M0 2 L2 2"
            stroke="#B8B8B8" stroke-width="0.4" fill="none" opacity="0.85"/>
    </g>
  </svg>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  count: { type: Number, default: 0 } // 0-6
})

// 6 vị trí neo mạng nhện quanh khung pet (viewBox 32x48)
const CORNERS = [
  'translate(0,0)',
  'translate(28,0) scale(-1,1)',
  'translate(0,44) scale(1,-1)',
  'translate(28,44) scale(-1,-1)',
  'translate(12,0)',
  'translate(12,44) scale(1,-1)'
]

const positions = computed(() => CORNERS.slice(0, Math.min(props.count, 6)))
</script>

<style scoped>
.web-layer {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}
</style>
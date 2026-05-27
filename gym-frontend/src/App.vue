<template>
  <router-view v-if="ready" />
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const ready = ref(false)

onMounted(() => {
  auth.checkToken()
  ready.value = true
})
</script>
<template>
  <div class="wrap">
    <div class="bar" :style="{ width: width + '%', background: color }"></div>
    <div class="text">{{ value }}</div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  value: { type: [Number, String], default: 0 }
})

const num = computed(() => {
  const v = Number(props.value)
  return Number.isFinite(v) ? v : 0
})

const width = computed(() => Math.max(0, Math.min(100, num.value)))
const color = computed(() => {
  if (num.value >= 80) return '#f56c6c'
  if (num.value >= 60) return '#e6a23c'
  if (num.value >= 40) return '#409eff'
  return '#67c23a'
})
</script>

<style scoped>
.wrap { position: relative; height: 18px; background: #f2f3f5; border-radius: 4px; overflow: hidden; }
.bar { height: 100%; }
.text { position:absolute; inset:0; display:flex; align-items:center; justify-content:center; font-size:12px; color:#111; }
</style>

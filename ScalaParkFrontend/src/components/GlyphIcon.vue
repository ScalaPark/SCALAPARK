<script setup lang="ts">
import { computed } from 'vue'

type GlyphName = 'operator' | 'analyst' | 'total' | 'valid' | 'invalid' | 'error'

interface Props {
  name: GlyphName
  danger?: boolean
  warning?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  danger: false,
  warning: false
})

const glyphColor = computed(() => {
  if (props.warning) return '#fbbf24'
  return props.danger ? '#f87171' : '#00ff88'
})
</script>

<template>
  <svg v-if="name === 'operator'" width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg" :style="{ color: glyphColor }">
    <path d="M8 1.5L2.5 8H6.3L4.8 14.5L13.5 6.8H9.6L11.1 1.5H8Z" fill="currentColor" />
  </svg>

  <svg v-else-if="name === 'analyst'" width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg" :style="{ color: glyphColor }">
    <rect x="2" y="8.5" width="2.2" height="5.5" rx="0.6" fill="currentColor" />
    <rect x="6.1" y="6.2" width="2.2" height="7.8" rx="0.6" fill="currentColor" />
    <rect x="10.2" y="3.8" width="2.2" height="10.2" rx="0.6" fill="currentColor" />
  </svg>

  <svg v-else-if="name === 'total'" width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg" :style="{ color: glyphColor }">
    <rect x="2" y="2" width="12" height="12" rx="2" stroke="currentColor" stroke-width="1.5" />
    <path d="M4.8 9.7L7 7.5L8.7 9.2L11.2 6.7" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
  </svg>

  <svg v-else-if="name === 'valid'" width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg" :style="{ color: glyphColor }">
    <circle cx="8" cy="8" r="6" stroke="currentColor" stroke-width="1.5" />
    <path d="M5 8.2L7 10.2L11 6.2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
  </svg>

  <svg v-else-if="name === 'invalid'" width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg" :style="{ color: glyphColor }">
    <circle cx="8" cy="8" r="6" stroke="currentColor" stroke-width="1.5" />
    <path d="M5.7 5.7L10.3 10.3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
    <path d="M10.3 5.7L5.7 10.3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
  </svg>

  <svg v-else width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg" :style="{ color: glyphColor }">
    <path d="M8 2.2L14 13H2L8 2.2Z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" />
    <path d="M8 6V9.2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
    <circle cx="8" cy="11.4" r="0.7" fill="currentColor" />
  </svg>
</template>
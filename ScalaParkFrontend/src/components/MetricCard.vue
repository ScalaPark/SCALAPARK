<script setup lang="ts">
import { computed } from 'vue'
import GlyphIcon from './GlyphIcon.vue'

type MetricGlyph = 'total' | 'valid' | 'invalid' | 'error'

interface Props {
  title: string
  value: string | number
  subtitle?: string
  icon?: MetricGlyph
  chart?: any
  danger?: boolean
  warning?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  danger: false,
  warning: false
})

const cardClasses = computed(() => {
  const baseClasses = 'border rounded-xl p-6 backdrop-blur-sm'
  const gradientClasses = props.warning
    ? 'bg-gradient-to-br from-amber-950/40 to-amber-900/20 border-amber-500/30'
    : props.danger
      ? 'bg-gradient-to-br from-red-950/40 to-red-900/20 border-red-500/30'
      : 'bg-gradient-to-br from-gray-900/80 to-gray-800/40 border-gray-700/50'
  return `${baseClasses} ${gradientClasses}`
})

const valueClasses = computed(() => {
  if (props.warning) return 'text-amber-300'
  return props.danger ? 'text-red-400' : 'text-white'
})

const iconBackgroundClasses = computed(() => {
  if (props.warning) return 'bg-amber-500/20'
  return props.danger ? 'bg-red-500/20' : 'bg-brand-20'
})
</script>

<template>
  <div :class="cardClasses">
    <div class="flex items-start justify-between mb-4">
      <div class="flex-1">
        <div class="text-sm text-gray-400 mb-1">{{ title }}</div>
        <div :class="`text-3xl font-bold ${valueClasses}`">
          {{ value }}
        </div>
        <div v-if="subtitle" class="text-xs text-gray-500 mt-1">
          {{ subtitle }}
        </div>
      </div>
      <div
        v-if="icon"
        :class="`w-12 h-12 rounded-lg flex items-center justify-center ${iconBackgroundClasses}`"
      >
        <GlyphIcon :name="icon" :danger="danger" :warning="warning" />
      </div>
    </div>
    <div v-if="chart" class="mt-4">
      <component :is="chart" />
    </div>
  </div>
</template>

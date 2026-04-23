<script setup lang="ts">
import { computed } from 'vue'
import GlyphIcon from './GlyphIcon.vue'

interface Props {
  view: 'operator' | 'analyst'
}

const props = defineProps<Props>()

const viewText = computed(() => {
  return props.view === 'operator' ? 'MONITOR' : 'ANALYTICS'
})

const currentViewText = computed(() => {
  return props.view === 'operator' ? 'System Monitor' : 'Business Analytics'
})

const iconName = computed<'operator' | 'analyst'>(() => {
  return props.view === 'operator' ? 'operator' : 'analyst'
})
</script>

<template>
  <div class="w-64 h-full bg-gradient-to-b from-[#0a0a0a] to-[#1a1a1a] border-r border-brand-30 sidebar-shell">
    <div class="p-6 border-b border-brand-30 sidebar-header">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 bg-brand-20 rounded-lg flex items-center justify-center border border-brand-30">
          <GlyphIcon :name="iconName" />
        </div>
        <div>
          <div class="font-bold text-[#00ff88]">E-COMM</div>
          <div class="text-xs text-gray-400">
            {{ viewText }}
          </div>
        </div>
      </div>
    </div>

    <div class="p-6 sidebar-info">
      <div class="text-sm text-gray-400">
        <div class="text-xs text-gray-500 mb-2">CURRENT VIEW</div>
        <div class="text-[#00ff88] font-medium">
          {{ currentViewText }}
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@media (max-width: 768px) {
  .sidebar-shell {
    width: 100%;
    height: auto;
    border-right: none;
    border-bottom: 1px solid rgba(0, 255, 136, 0.3);
  }

  .sidebar-header {
    padding: 0.75rem 1rem;
    border-bottom: none;
  }

  .sidebar-info {
    padding: 0 1rem 0.75rem;
  }
}
</style>

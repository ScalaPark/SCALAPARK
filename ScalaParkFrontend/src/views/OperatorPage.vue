<script setup lang="ts">
import { ref } from 'vue'
import Sidebar from '../components/Sidebar.vue'
import OperatorDashboard from '../components/OperatorDashboard.vue'
import AnalystDashboard from '../components/AnalystDashboard.vue'
import GlyphIcon from '../components/GlyphIcon.vue'

const view = ref<'operator' | 'analyst'>('operator')

const handleViewChange = (newView: 'operator' | 'analyst') => {
  view.value = newView
}
</script>

<template>
  <div class="size-full flex app-shell bg-gradient-to-br from-brand-start via-brand-mid to-brand-end">
    <Sidebar :view="view" />

    <div class="flex-1 flex flex-col app-main">
      <header class="h-16 border-b border-gray-700/50 bg-black/20 backdrop-blur-sm flex items-center justify-between px-8 app-header">
        <div class="flex items-center gap-4">
          <div class="text-sm text-gray-400 app-title">
            Real-Time E-commerce Monitoring System
          </div>
        </div>

        <div class="flex items-center gap-6 app-header-controls">
          <div class="flex items-center gap-2 bg-gray-900/60 border border-gray-700/50 rounded-lg p-1 view-switch">
            <button
              @click="handleViewChange('operator')"
              :class="[
                'flex items-center gap-2 px-4 py-2 rounded-md text-sm font-medium transition-all',
                view === 'operator'
                  ? 'bg-brand text-black'
                  : 'text-gray-400 hover:text-white'
              ]"
            >
              <GlyphIcon name="operator" :danger="false" />
              OPERATOR
            </button>
            <button
              @click="handleViewChange('analyst')"
              :class="[
                'flex items-center gap-2 px-4 py-2 rounded-md text-sm font-medium transition-all',
                view === 'analyst'
                  ? 'bg-brand text-black'
                  : 'text-gray-400 hover:text-white'
              ]"
            >
              <GlyphIcon name="analyst" :danger="false" />
              ANALYST
            </button>
          </div>

          <div class="flex items-center gap-3">
            <div class="w-8 h-8 bg-gradient-to-br from-brand-start to-brand-cyan rounded-full flex items-center justify-center text-black font-bold text-sm">
              SP
            </div>
          </div>
        </div>
      </header>

      <OperatorDashboard v-if="view === 'operator'" />
      <AnalystDashboard v-else />

      <footer class="border-t border-gray-700/50 bg-black/20 backdrop-blur-sm px-8 py-4 app-footer">
        <div class="flex items-center justify-center gap-8 text-sm text-gray-400 app-footer-inner">
          <div class="text-xs text-gray-500">Desarrolladores:</div>
          <div class="flex items-center gap-6 dev-list">
            <span class="text-gray-300">Anderson Fabian Garcia Nieto</span>
            <span class="text-gray-600">•</span>
            <span class="text-gray-300">David Alejandro Patacon Henao</span>
            <span class="text-gray-600">•</span>
            <span class="text-gray-300">Tulio Riaño Sanchez</span>
          </div>
        </div>
      </footer>
    </div>
  </div>
</template>

<style scoped>
@media (max-width: 1024px) {
  .app-header {
    padding-left: 1rem;
    padding-right: 1rem;
    height: auto;
    min-height: 4rem;
    padding-top: 0.75rem;
    padding-bottom: 0.75rem;
  }

  .app-header-controls {
    gap: 0.75rem;
  }

  .app-footer {
    padding-left: 1rem;
    padding-right: 1rem;
  }

  .app-footer-inner {
    flex-direction: column;
    gap: 0.5rem;
  }

  .dev-list {
    flex-wrap: wrap;
    justify-content: center;
    row-gap: 0.25rem;
  }
}

@media (max-width: 768px) {
  .app-shell {
    flex-direction: column;
  }

  .app-title {
    display: none;
  }

  .app-header {
    flex-direction: column;
    align-items: stretch;
    gap: 0.75rem;
  }

  .app-header-controls {
    justify-content: space-between;
    width: 100%;
  }

  .view-switch {
    flex: 1;
    justify-content: center;
    flex-wrap: wrap;
  }
}
</style>

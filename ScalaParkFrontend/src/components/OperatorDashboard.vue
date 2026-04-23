<script setup lang="ts">
import RealTimeMetrics from './RealTimeMetrics.vue'
import ValidatedOrdersStream from './ValidatedOrdersStream.vue'
import ValidationMetricsChart from './ValidationMetricsChart.vue'
</script>

<template>
  <div class="flex-1 overflow-auto p-8 dashboard-shell">
    <h1 class="text-4xl font-bold text-white mb-8 tracking-tight">
      SYSTEM MONITOR
    </h1>

    <div class="grid grid-cols-4 gap-6 mb-8 metrics-grid">
      <RealTimeMetrics />
    </div>

    <div class="space-y-6">
      <ValidationMetricsChart />
      <ValidatedOrdersStream />
    </div>
  </div>
</template>

<style scoped>
@media (max-width: 1024px) {
  .dashboard-shell {
    padding: 1rem;
  }

  .metrics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .metrics-grid {
    grid-template-columns: 1fr;
  }
}
</style>

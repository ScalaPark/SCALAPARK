<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'

interface MetricsHistory {
  id: string
  time: string
  total: number
  valid: number
  invalid: number
  errors: number
}

const history = ref<MetricsHistory[]>([])
let counter = 0
let interval: ReturnType<typeof setInterval> | null = null
let stream: EventSource | null = null

const chartWidth = 600
const chartHeight = 240
const pad = { top: 14, right: 18, bottom: 28, left: 48 }

const yMax = computed(() => {
  const peak = Math.max(...history.value.map((v) => Math.max(v.valid, v.invalid, v.errors)), 0)
  return Math.ceil((peak * 1.15) / 10) * 10 || 100
})

const xStep = computed(() => {
  const n = Math.max(history.value.length - 1, 1)
  return (chartWidth - pad.left - pad.right) / n
})

const toX = (index: number) => pad.left + index * xStep.value
const toY = (value: number) => {
  const h = chartHeight - pad.top - pad.bottom
  return pad.top + (1 - value / Math.max(yMax.value, 1)) * h
}

const pointsFor = (key: 'valid' | 'invalid' | 'errors') => {
  return history.value.map((item, i) => `${toX(i)},${toY(item[key])}`).join(' ')
}

const errorAreaPath = computed(() => {
  if (!history.value.length) return ''
  const startX = toX(0)
  const endX = toX(history.value.length - 1)
  const baseY = chartHeight - pad.bottom
  const line = pointsFor('errors')
  return `M ${startX} ${baseY} L ${line.replace(/,/g, ' ')} L ${endX} ${baseY} Z`
})

const validPath = computed(() => pointsFor('valid'))
const invalidPath = computed(() => pointsFor('invalid'))
const errorPath = computed(() => pointsFor('errors'))

const yTicks = computed(() => {
  const max = yMax.value
  const steps = 4
  return Array.from({ length: steps + 1 }, (_, i) => {
    const value = Math.round((max / steps) * i)
    return { value, y: toY(value) }
  })
})

const xTicks = computed(() => {
  if (!history.value.length) return []
  const count = Math.min(6, history.value.length)
  return Array.from({ length: count }, (_, i) => {
    const idx = Math.round((i * (history.value.length - 1)) / Math.max(count - 1, 1))
    const x = toX(idx)
    return { x, label: history.value[idx]?.time ?? '' }
  })
})

const fetchHistory = async () => {
  try {
    const response = await fetch('/api/operator/validation/history?limit=20')
    if (!response.ok) return
    const data = (await response.json()) as MetricsHistory[]
    history.value = data
    counter += 1
  } catch {
    // Keep the latest rendered data if the backend is unavailable.
  }
}

const connectMetricsStream = () => {
  stream = new EventSource('/api/operator/validation/stream')
  stream.onmessage = (event) => {
    try {
      const metric = JSON.parse(event.data) as {
        windowEnd: string
        total: number
        valid: number
        invalid: number
        deserializationErrors: number
      }
      const point: MetricsHistory = {
        id: `metric-${Date.now()}-${counter++}`,
        time: metric.windowEnd,
        total: metric.total,
        valid: metric.valid,
        invalid: metric.invalid,
        errors: metric.deserializationErrors
      }
      history.value = [...history.value, point].slice(-20)
    } catch {
      // Ignore malformed payload.
    }
  }
}

onMounted(() => {
  fetchHistory()
  connectMetricsStream()
  interval = setInterval(fetchHistory, 60000)
})

onUnmounted(() => {
  if (interval) clearInterval(interval)
  if (stream) stream.close()
})
</script>

<template>
  <div class="bg-gradient-to-br from-gray-900/80 to-gray-800/40 border border-gray-700/50 rounded-xl p-6 backdrop-blur-sm chart-shell">
    <div class="mb-4">
      <h3 class="text-lg font-bold text-white">Order Validation Metrics (Real-Time)</h3>
    </div>

    <div v-if="history.length > 0" class="space-y-4">
      <div class="chart-frame">
        <svg :viewBox="`0 0 ${chartWidth} ${chartHeight}`" preserveAspectRatio="none" class="chart-svg">
          <defs>
            <linearGradient id="errorAreaGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#fbbf24" stop-opacity="0.58" />
              <stop offset="100%" stop-color="#fbbf24" stop-opacity="0.06" />
            </linearGradient>
          </defs>

          <g>
            <line
              v-for="tick in yTicks"
              :key="`y-grid-${tick.value}`"
              :x1="pad.left"
              :x2="chartWidth - pad.right"
              :y1="tick.y"
              :y2="tick.y"
              stroke="#4b5563"
              stroke-opacity="0.35"
              stroke-dasharray="3 4"
            />
            <line
              v-for="(tick, i) in xTicks"
              :key="`x-grid-${i}`"
              :x1="tick.x"
              :x2="tick.x"
              :y1="pad.top"
              :y2="chartHeight - pad.bottom"
              stroke="#4b5563"
              stroke-opacity="0.3"
              stroke-dasharray="3 4"
            />
          </g>

          <path :d="errorAreaPath" fill="url(#errorAreaGradient)" />
          <polyline :points="validPath" fill="none" stroke="#00ff88" stroke-width="1.4" stroke-opacity="0.9" />
          <polyline :points="invalidPath" fill="none" stroke="#ff4d57" stroke-width="1.4" stroke-opacity="0.9" />
          <polyline :points="errorPath" fill="none" stroke="#fbbf24" stroke-width="1.8" />

          <line :x1="pad.left" :x2="pad.left" :y1="pad.top" :y2="chartHeight - pad.bottom" stroke="#9ca3af" stroke-opacity="0.55" />
          <line :x1="pad.left" :x2="chartWidth - pad.right" :y1="chartHeight - pad.bottom" :y2="chartHeight - pad.bottom" stroke="#9ca3af" stroke-opacity="0.55" />

          <text
            v-for="tick in yTicks"
            :key="`y-label-${tick.value}`"
            :x="pad.left - 8"
            :y="tick.y + 3"
            text-anchor="end"
            font-size="10"
            fill="#9ca3af"
          >
            {{ tick.value }}
          </text>

          <text
            v-for="(tick, i) in xTicks"
            :key="`x-label-${i}`"
            :x="tick.x"
            :y="chartHeight - 8"
            text-anchor="middle"
            font-size="10"
            fill="#9ca3af"
          >
            {{ tick.label }}
          </text>
        </svg>
      </div>

      <div class="legend-row">
        <div class="legend-item text-valid">
          <span class="legend-dot" />
          <span>Valid Orders</span>
        </div>
        <div class="legend-item text-invalid">
          <span class="legend-dot" />
          <span>Invalid Orders</span>
        </div>
        <div class="legend-item text-error">
          <span class="legend-dot" />
          <span>Deserialization Errors</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chart-shell {
  background-image: linear-gradient(160deg, rgba(8, 15, 45, 0.95), rgba(10, 16, 40, 0.72));
}

.chart-frame {
  border: 1px solid rgba(75, 85, 99, 0.45);
  border-radius: 10px;
  background: radial-gradient(circle at 20% 0%, rgba(29, 78, 216, 0.08), rgba(2, 6, 23, 0.6));
  padding: 8px;
}

.chart-svg {
  width: 100%;
  height: 290px;
  display: block;
}

.legend-row {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  padding-top: 6px;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  border: 1px solid currentColor;
}

.text-valid { color: #00ff88; }
.text-invalid { color: #ff4d57; }
.text-error { color: #fbbf24; }

@media (max-width: 768px) {
  .chart-svg {
    height: 230px;
  }
}
</style>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, computed } from 'vue'
import MetricCard from './MetricCard.vue'

interface ValidationMetrics {
  windowStart: string
  windowEnd: string
  total: number
  valid: number
  invalid: number
  deserializationErrors: number
}

interface HistoryItem {
  id: string
  value: number
}

const metrics = ref<ValidationMetrics>({
  windowStart: new Date().toISOString(),
  windowEnd: new Date().toISOString(),
  total: 0,
  valid: 0,
  invalid: 0,
  deserializationErrors: 0
})

const history = ref<HistoryItem[]>([])
let counter = 0
let interval: ReturnType<typeof setInterval> | null = null
let stream: EventSource | null = null

const validRate = computed(() => {
  return metrics.value.total > 0
    ? ((metrics.value.valid / metrics.value.total) * 100).toFixed(1)
    : '0'
})

const parseDate = (raw: string) => {
  const normalized = raw.includes('T') ? raw : raw.replace(' ', 'T')
  const dt = new Date(normalized)
  return Number.isNaN(dt.getTime()) ? null : dt
}

const formatWindowTime = (raw: string) => {
  const dt = parseDate(raw)
  return dt ? dt.toLocaleTimeString() : raw
}

const fetchMetrics = async () => {
  try {
    const response = await fetch('/api/operator/validation/window')
    if (!response.ok) return
    const data = (await response.json()) as ValidationMetrics
    metrics.value = data
    history.value = [...history.value, { id: `hist-${Date.now()}-${counter++}`, value: data.total }].slice(-20)
  } catch {
    // Keep previous value when the API is temporarily unavailable.
  }
}

const connectMetricsStream = () => {
  stream = new EventSource('/api/operator/validation/stream')
  stream.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data) as ValidationMetrics
      metrics.value = data
      history.value = [...history.value, { id: `hist-${Date.now()}-${counter++}`, value: data.total }].slice(-20)
    } catch {
      // Ignore malformed SSE payloads.
    }
  }
}

onMounted(() => {
  fetchMetrics()
  connectMetricsStream()
  interval = setInterval(fetchMetrics, 60000)
})

onUnmounted(() => {
  if (interval) clearInterval(interval)
  if (stream) stream.close()
})
</script>

<template>
  <MetricCard
    title="Total Orders (Current Window)"
    :value="metrics.total.toLocaleString()"
    :subtitle="`Window: ${formatWindowTime(metrics.windowStart)} - ${formatWindowTime(metrics.windowEnd)}`"
    icon="total"
  />

  <MetricCard
    title="Valid Orders"
    :value="metrics.valid.toLocaleString()"
    :subtitle="`${validRate}% success rate`"
    icon="valid"
  />

  <MetricCard
    title="Invalid Orders"
    :value="metrics.invalid.toLocaleString()"
    icon="invalid"
    :danger="true"
  />

  <MetricCard
    title="Deserialization Errors"
    :value="metrics.deserializationErrors.toLocaleString()"
    subtitle="Invalid character/format errors"
    icon="error"
    :warning="true"
  />
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'

interface OrderHeader {
  orderId: string
  timestamp: string
  source: string
}

interface Customer {
  customerId: string
  email: string
  name: string
}

interface Location {
  country: string
  city: string
  address: string
}

interface Payment {
  method: string
  amount: number
  currency: string
}

interface OrderItem {
  productId: string
  quantity: number
  price: number
}

interface OrderDetail {
  header?: OrderHeader
  customer?: Customer
  location?: Location
  payment?: Payment
  items?: OrderItem[]
}

interface ValidatedOrder {
  orderId: string
  correlationId: string
  status?: 'VALID' | 'INVALID'
  errors?: string[]
  processedAt: string
  order?: OrderDetail
}

const orders = ref<ValidatedOrder[]>([])
let interval: ReturnType<typeof setInterval> | null = null

const parseDate = (raw: string) => {
  const normalized = raw.includes('T') ? raw : raw.replace(' ', 'T')
  const dt = new Date(normalized)
  return Number.isNaN(dt.getTime()) ? null : dt
}

const formatTime = (raw: string) => {
  const dt = parseDate(raw)
  return dt ? dt.toLocaleTimeString() : raw
}

const formatAmount = (order: ValidatedOrder) => {
  const amount = order.order?.payment?.amount
  const currency = order.order?.payment?.currency ?? 'USD'
  if (typeof amount !== 'number') return `N/A ${currency}`
  return `$${amount.toFixed(2)} ${currency}`
}

const formatLocation = (order: ValidatedOrder) => {
  const city = order.order?.location?.city
  const country = order.order?.location?.country
  if (!city && !country) return 'N/A'
  return [city, country].filter(Boolean).join(', ')
}

const fetchOrders = async () => {
  try {
    const response = await fetch('/api/operator/orders/validated?limit=10')
    if (!response.ok) return
    const data = (await response.json()) as ValidatedOrder[]
    orders.value = data
  } catch {
    // Keep previous rows when fetch fails.
  }
}

onMounted(() => {
  fetchOrders()
  interval = setInterval(fetchOrders, 15000)
})

onUnmounted(() => {
  if (interval) clearInterval(interval)
})
</script>

<template>
  <div class="bg-gradient-to-br from-gray-900/80 to-gray-800/40 border border-gray-700/50 rounded-xl p-6 backdrop-blur-sm">
    <div class="mb-4">
      <h3 class="text-lg font-bold text-white">Real-Time Stream: Validated Orders</h3>
    </div>

    <div class="space-y-3">
      <div
        v-for="(order, idx) in orders"
        :key="order.correlationId"
        :class="[
          'border rounded-lg p-4 transition-all',
          idx === 0
            ? 'bg-[#00ff88]/10 border-[#00ff88]/30 animate-pulse'
            : 'bg-gray-800/40 border-gray-700/30'
        ]"
      >
        <div class="grid grid-cols-4 gap-4 text-sm stream-main-grid">
          <div>
            <div class="text-xs text-gray-500 mb-1">Order ID</div>
            <div class="text-[#00ff88] font-mono">{{ order.orderId }}</div>
          </div>
          <div>
            <div class="text-xs text-gray-500 mb-1">Customer</div>
            <div class="text-gray-300">{{ order.order?.customer?.name ?? 'N/A' }}</div>
          </div>
          <div>
            <div class="text-xs text-gray-500 mb-1">Amount</div>
            <div class="text-gray-300">{{ formatAmount(order) }}</div>
          </div>
          <div>
            <div class="text-xs text-gray-500 mb-1">Processed At</div>
            <div class="text-gray-300">{{ formatTime(order.processedAt) }}</div>
          </div>
        </div>
        <div class="mt-3 pt-3 border-t border-gray-700/50 grid grid-cols-3 gap-4 text-xs stream-meta-grid">
          <div>
            <span class="text-gray-500">Location:</span>
            <span class="text-gray-400"> {{ formatLocation(order) }}</span>
          </div>
          <div>
            <span class="text-gray-500">Payment:</span>
            <span class="text-gray-400"> {{ order.order?.payment?.method ?? 'N/A' }}</span>
          </div>
          <div>
            <span class="text-gray-500">Status:</span>
            <span :class="(order.status ?? 'INVALID') === 'VALID' ? 'text-[#00ff88]' : 'text-red-400'">
              {{ order.status ?? 'UNKNOWN' }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@media (max-width: 1024px) {
  .stream-main-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .stream-meta-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .stream-main-grid {
    grid-template-columns: 1fr;
  }
}
</style>

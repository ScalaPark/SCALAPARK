<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'

interface OrderHeader {
  orderId: string
  timestamp: string
  sourceApp: string
  correlationId: string
}

interface Customer {
  email: string
  docType: string
  docNumber: number
  phone: number
  ipAddress: string
}

interface Location {
  market: string
  city: string
  department: string
  zipCode: number
  address: string
}

interface Payment {
  cardBin: number
  cVV: number
  expirationDate: string
  currency: string
  installments: number
}

interface OrderItem {
  productId: number
  name: string
  price: number
  size: number
  quantity: number
  category: string
}

interface OrderDetail {
  header?: OrderHeader
  customer?: Customer
  location?: Location
  payment?: Payment
  items?: OrderItem[]
  totalAmount?: number
}

interface ValidatedOrder {
  orderId: string
  correlationId: string
  status?: 'VALID' | 'INVALID' | 'DESERIALIZATION_ERROR'
  errors?: string[]
  processedAt: string
  order?: OrderDetail
}

const orders = ref<ValidatedOrder[]>([])
const expandedOrders = ref<Set<string>>(new Set())
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
  const amount = order.order?.totalAmount
  const currency = order.order?.payment?.currency ?? ''
  if (typeof amount !== 'number') return 'N/A'
  return `$${amount.toLocaleString()} ${currency}`
}

const formatLocation = (order: ValidatedOrder) => {
  const city = order.order?.location?.city
  const department = order.order?.location?.department
  if (!city && !department) return 'N/A'
  return [city, department].filter(Boolean).join(', ')
}

const toggleItems = (orderId: string) => {
  if (expandedOrders.value.has(orderId)) {
    expandedOrders.value.delete(orderId)
  } else {
    expandedOrders.value.add(orderId)
  }
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
        <!-- Row 1: main fields -->
        <div class="grid grid-cols-4 gap-4 text-sm stream-main-grid">
          <div>
            <div class="text-xs text-gray-500 mb-1">Order ID</div>
            <div class="text-[#00ff88] font-mono text-xs truncate">{{ order.orderId }}</div>
          </div>
          <div>
            <div class="text-xs text-gray-500 mb-1">Status</div>
            <span
              :class="[
                'text-xs font-semibold px-2 py-0.5 rounded-full',
                order.status === 'VALID'
                  ? 'bg-[#00ff88]/20 text-[#00ff88]'
                  : 'bg-red-500/20 text-red-400'
              ]"
            >
              {{ order.status ?? 'UNKNOWN' }}
            </span>
          </div>
          <div>
            <div class="text-xs text-gray-500 mb-1">Total Amount</div>
            <div class="text-gray-300">{{ formatAmount(order) }}</div>
          </div>
          <div>
            <div class="text-xs text-gray-500 mb-1">Processed At</div>
            <div class="text-gray-300">{{ formatTime(order.processedAt) }}</div>
          </div>
        </div>

        <!-- Row 2: meta fields -->
        <div class="mt-3 pt-3 border-t border-gray-700/50 grid grid-cols-3 gap-4 text-xs stream-meta-grid">
          <div>
            <span class="text-gray-500">Customer: </span>
            <span class="text-gray-400">{{ order.order?.customer?.email ?? 'N/A' }}</span>
            <span v-if="order.order?.customer?.docType" class="ml-1 text-gray-600">({{ order.order.customer.docType }})</span>
          </div>
          <div>
            <span class="text-gray-500">Location: </span>
            <span class="text-gray-400">{{ formatLocation(order) }}</span>
          </div>
          <div>
            <span class="text-gray-500">Payment: </span>
            <span class="text-gray-400">
              {{ order.order?.payment?.currency ?? 'N/A' }}
              <span v-if="order.order?.payment?.installments"> · {{ order.order.payment.installments }} cuotas</span>
            </span>
          </div>
        </div>

        <!-- Row 3: items (collapsible) -->
        <div v-if="order.order?.items && order.order.items.length > 0" class="mt-3 pt-3 border-t border-gray-700/50">
          <button
            class="text-xs text-gray-500 hover:text-gray-300 transition-colors flex items-center gap-1"
            @click="toggleItems(order.orderId)"
          >
            <span>{{ expandedOrders.has(order.orderId) ? '▾' : '▸' }}</span>
            <span>Items ({{ order.order.items.length }})</span>
          </button>
          <div v-show="expandedOrders.has(order.orderId)" class="mt-2 space-y-1">
            <div
              v-for="item in order.order.items"
              :key="item.productId"
              class="grid grid-cols-4 gap-2 text-xs text-gray-400 bg-gray-900/40 rounded px-2 py-1"
            >
              <span class="truncate">{{ item.name }}</span>
              <span class="text-gray-500">{{ item.category }}</span>
              <span>${{ item.price.toLocaleString() }} × {{ item.quantity }}</span>
              <span class="text-right text-gray-300">${{ (item.price * item.quantity).toLocaleString() }}</span>
            </div>
          </div>
        </div>

        <!-- Errors (INVALID orders) -->
        <div v-if="order.errors && order.errors.length > 0" class="mt-2 text-xs text-red-400/80">
          {{ order.errors.join(' · ') }}
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

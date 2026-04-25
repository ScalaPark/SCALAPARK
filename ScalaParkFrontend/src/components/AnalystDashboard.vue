<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import MetricCard from './MetricCard.vue'

interface DailyStats {
  totalRevenue: number
  avgOrderValue: number
  newCustomers: number
  totalOrders: number
  averageItemsPerOrder: number
  averageTicketSize: number
  creditPurchaseRatio: number
}

interface Product {
  id: string
  name: string
  sales: number
}

interface Category {
  id: string
  name: string
  revenue: number
}
interface City {
  id: string
  name: string
  orders: number
}

interface Segment {
  id: string
  name: string
  value: number
  color: string
}

interface Category {
  id: string
  name: string
  revenue: number
}

interface City {
  id: string
  name: string
  orders: number
}

interface Hour {
  hour: number
  orders: number
}

interface Installment {
  installments: number
  orders: number
}

interface Currency {
  currency: string
  orders: number
}

const dailyStats = ref<DailyStats>({
  totalRevenue: 0,
  avgOrderValue: 0,
  newCustomers: 0,
  totalOrders: 0,
})

const topProducts = ref<Product[]>([])
const topCategories = ref<Category[]>([])
const topCities = ref<City[]>([])

const customerSegments = ref<Segment[]>([])
const topCategories = ref<Category[]>([])
const topCities = ref<City[]>([])
const hourlyDistribution = ref<Hour[]>([])
const installmentsDistribution = ref<Installment[]>([])
const currencyDistribution = ref<Currency[]>([])

const reportMeta = ref({
  generatedAt: '',
  averageItemsPerOrder: 0,
  averageTicketSize: 0,
  creditPurchaseRatio: 0,
})

let statsInterval: ReturnType<typeof setInterval> | null = null
let stream: EventSource | null = null

const maxProductSales = computed(() => {
  return Math.max(...topProducts.value.map((p) => p.sales), 1)
})

const maxCategoryRevenue = computed(() => {
  return Math.max(...topCategories.value.map((c) => c.revenue), 1)
})

const maxCityOrders = computed(() => {
  return Math.max(...topCities.value.map((c) => c.orders), 1)
})

const maxHourlyOrders = computed(() => {
  return Math.max(...hourlyDistribution.value.map((h) => h.orders), 1)
})

const donutStyle = computed(() => {
  const a = customerSegments.value[0]?.value ?? 0
  const b = customerSegments.value[1]?.value ?? 0
  const c = customerSegments.value[2]?.value ?? 0
  const c1 = customerSegments.value[0]?.color ?? '#00ff88'
  const c2 = customerSegments.value[1]?.color ?? '#ff4fd8'
  const c3 = customerSegments.value[2]?.color ?? '#00d4ff'
  return {
    background: `conic-gradient(${c1} 0% ${a}%, ${c2} ${a}% ${a + b}%, ${c3} ${a + b}% ${a + b + c}%)`,
  }
})

const fetchDailyStats = async () => {
  try {
    const response = await fetch('/api/analyst/daily')
    if (!response.ok) return
    dailyStats.value = (await response.json()) as DailyStats
  } catch {
    // Keep previous values while backend reconnects.
  }
}

const fetchLatestReport = async () => {
  try {
    const response = await fetch('/api/analyst/report/latest')
    if (!response.ok) return
    const report = (await response.json()) as {
      generatedAt: string
      averageItemsPerOrder: number
      averageTicketSize: number
      creditPurchaseRatio: number
      topProducts: Product[]
      topCategories: Category[]
      topCities: City[]
      customerSegments: Segment[]
      topCategories: Category[]
      topCities: City[]
      hourlyDistribution: Hour[]
      installmentsDistribution: Installment[]
      currencyDistribution: Currency[]
    }
    topProducts.value = report.topProducts ?? []
    topCategories.value = report.topCategories ?? []
    topCities.value = report.topCities ?? []
    customerSegments.value = report.customerSegments ?? []
    reportMeta.value = {
      generatedAt: report.generatedAt || new Date().toISOString(),
      averageItemsPerOrder: report.averageItemsPerOrder || 0,
      averageTicketSize: report.averageTicketSize || 0,
      creditPurchaseRatio: report.creditPurchaseRatio || 0,
    }
  } catch {
    // Keep previous values while backend reconnects.
  }
}

const refreshAnalystData = async () => {
  await Promise.all([fetchDailyStats(), fetchLatestReport()])
}

const connectAnalystStream = () => {
  stream = new EventSource('/api/analyst/daily/stream')
  stream.onmessage = async () => {
    await refreshAnalystData()
  }
}

onMounted(() => {
  refreshAnalystData()
  connectAnalystStream()
  statsInterval = setInterval(refreshAnalystData, 300000)
})

onUnmounted(() => {
  if (statsInterval) clearInterval(statsInterval)
  if (stream) stream.close()
})
</script>

<template>
  <div class="flex-1 overflow-auto p-8 analyst-shell">
    <h1 class="text-4xl font-bold text-white mb-8 tracking-tight">BUSINESS ANALYTICS</h1>

    <div class="grid grid-cols-4 gap-6 mb-8 analyst-metrics-grid">
      <MetricCard
        title="Total Daily Revenue"
        :value="`$${dailyStats.totalRevenue.toLocaleString()}`"
        :subtitle="
          reportMeta.generatedAt ? new Date(reportMeta.generatedAt).toLocaleDateString() : 'N/A'
        "
        icon="total"
      />

      <MetricCard
        title="Avg. Ticket Size"
        :value="`$${reportMeta.averageTicketSize.toFixed(2)}`"
        icon="valid"
      />

      <MetricCard
        title="New Customers Today"
        :value="dailyStats.newCustomers.toLocaleString()"
        icon="valid"
      />

      <MetricCard
        title="Valid Orders Processed"
        :value="dailyStats.totalOrders.toLocaleString()"
        subtitle="Source: orders-validated topic"
        icon="total"
      />

      <MetricCard
        title="Avg. Items Per Order"
        :value="reportMeta.averageItemsPerOrder.toFixed(1)"
        icon="valid"
      />

      <MetricCard
        title="Credit Purchase Ratio"
        :value="`${(reportMeta.creditPurchaseRatio * 100).toFixed(1)}%`"
        icon="valid"
      />
    </div>

    <div class="grid analyst-grid-2 gap-6 mb-6">
      <div
        class="bg-gradient-to-br from-gray-900/80 to-gray-800/40 border border-gray-700/50 rounded-xl p-6 backdrop-blur-sm"
      >
        <h3 class="text-lg font-bold text-white mb-4">Top 5 Products</h3>
        <div class="space-y-3">
          <div v-for="product in topProducts" :key="product.id" class="space-y-2">
            <div class="flex items-center justify-between text-sm">
              <span class="text-gray-300">{{ product.name }}</span>
              <span class="text-gray-400">{{ product.sales }}</span>
            </div>
            <div class="h-2 bg-gray-700 rounded overflow-hidden">
              <div
                class="h-2 bg-brand-60 rounded"
                :style="{ width: `${(product.sales / maxProductSales) * 100}%` }"
              />
            </div>
          </div>
        </div>
      </div>

      <div
        class="bg-gradient-to-br from-gray-900/80 to-gray-800/40 border border-gray-700/50 rounded-xl p-6 backdrop-blur-sm"
      >
        <h3 class="text-lg font-bold text-white mb-4">Top Categories & Cities</h3>
        <div class="space-y-4">
          <div>
            <h4 class="text-sm font-semibold text-gray-400 mb-2">Categories by Revenue</h4>
            <div class="space-y-2">
              <div v-for="cat in topCategories" :key="cat.id" class="flex justify-between text-sm">
                <span class="text-gray-300">{{ cat.name }}</span>
                <span class="text-[#00ff88]">${{ cat.revenue.toLocaleString() }}</span>
              </div>
            </div>
          </div>
          <div>
            <h4 class="text-sm font-semibold text-gray-400 mb-2">Cities by Orders</h4>
            <div class="space-y-2">
              <div v-for="city in topCities" :key="city.id" class="flex justify-between text-sm">
                <span class="text-gray-300">{{ city.name }}</span>
                <span class="text-[#00d4ff]">{{ city.orders.toLocaleString() }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="grid analyst-grid-2 gap-6">
      <div
        class="bg-gradient-to-br from-gray-900/80 to-gray-800/40 border border-gray-700/50 rounded-xl p-6 backdrop-blur-sm"
      >
        <h3 class="text-lg font-bold text-white mb-4">Customer Segmentation</h3>
        <div class="flex items-center gap-6 analyst-segment-layout">
          <div class="donut-wrap">
            <div class="donut" :style="donutStyle" />
            <div class="donut-center" />
          </div>

          <div class="space-y-3 flex-1">
            <div
              v-for="segment in customerSegments"
              :key="segment.id"
              class="flex items-center justify-between text-sm"
            >
              <div class="flex items-center gap-2">
                <span class="w-3 h-3 rounded" :style="{ backgroundColor: segment.color }" />
                <span class="text-gray-300">{{ segment.name }}</span>
              </div>
              <span class="text-gray-400">{{ segment.value }}%</span>
            </div>
          </div>
        </div>
      </div>

      <div class="bg-gradient-to-br from-gray-900/80 to-gray-800/40 border border-gray-700/50 rounded-xl p-6 backdrop-blur-sm">
        <h3 class="text-lg font-bold text-white mb-4">Top 5 Categories (Revenue)</h3>
        <div class="space-y-3">
          <div v-for="cat in topCategories" :key="cat.id" class="space-y-2">
            <div class="flex items-center justify-between text-sm">
              <span class="text-gray-300">{{ cat.name }}</span>
              <span class="text-gray-400">${{ cat.revenue.toLocaleString() }}</span>
            </div>
            <div class="h-2 bg-gray-700 rounded overflow-hidden">
              <div class="h-2 bg-brand-60 rounded" :style="{ width: `${(cat.revenue / maxCategoryRevenue) * 100}%` }" />
            </div>
          </div>
        </div>
      </div>

      <div class="bg-gradient-to-br from-gray-900/80 to-gray-800/40 border border-gray-700/50 rounded-xl p-6 backdrop-blur-sm">
        <h3 class="text-lg font-bold text-white mb-4">Top 5 Cities (Orders)</h3>
        <div class="space-y-3">
          <div v-for="city in topCities" :key="city.id" class="space-y-2">
            <div class="flex items-center justify-between text-sm">
              <span class="text-gray-300">{{ city.name }}</span>
              <span class="text-gray-400">{{ city.orders }}</span>
            </div>
            <div class="h-2 bg-gray-700 rounded overflow-hidden">
              <div class="h-2 bg-blue-500 rounded" :style="{ width: `${(city.orders / maxCityOrders) * 100}%` }" />
            </div>
          </div>
        </div>
      </div>

      <div class="bg-gradient-to-br from-gray-900/80 to-gray-800/40 border border-gray-700/50 rounded-xl p-6 backdrop-blur-sm">
        <h3 class="text-lg font-bold text-white mb-4">Hourly Distribution</h3>
        <div class="flex items-end gap-1 h-32 mt-4">
          <div v-for="h in hourlyDistribution" :key="h.hour" class="flex-1 flex flex-col items-center gap-1 group">
             <div class="w-full bg-purple-500 rounded-t" :style="{ height: `${(h.orders / maxHourlyOrders) * 100}%`, minHeight: '4px' }"></div>
             <span class="text-[10px] text-gray-500">{{ h.hour }}h</span>
          </div>
        </div>
      </div>

      <div class="bg-gradient-to-br from-gray-900/80 to-gray-800/40 border border-gray-700/50 rounded-xl p-6 backdrop-blur-sm">
        <h3 class="text-lg font-bold text-white mb-4">Misc Stats</h3>
        <div class="grid grid-cols-2 gap-4">
           <div>
               <h4 class="text-sm font-semibold text-gray-400 mb-2">Currency</h4>
               <ul class="space-y-1">
                   <li v-for="c in currencyDistribution" :key="c.currency" class="text-sm flex justify-between">
                       <span class="text-gray-300">{{ c.currency }}</span> <span class="text-brand-60">{{ c.orders }}</span>
                   </li>
               </ul>
           </div>
           <div>
               <h4 class="text-sm font-semibold text-gray-400 mb-2">Installments</h4>
               <ul class="space-y-1">
                   <li v-for="i in installmentsDistribution" :key="i.installments" class="text-sm flex justify-between">
                       <span class="text-gray-300">{{ i.installments }}x</span> <span class="text-brand-60">{{ i.orders }}</span>
                   </li>
               </ul>
           </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.analyst-grid-2 {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.donut-wrap {
  position: relative;
  width: 140px;
  height: 140px;
  flex-shrink: 0;
}

.donut {
  width: 100%;
  height: 100%;
  border-radius: 9999px;
}

.donut-center {
  position: absolute;
  width: 68px;
  height: 68px;
  border-radius: 9999px;
  background: #111111;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border: 1px solid rgba(45, 45, 45, 0.5);
}

@media (max-width: 1024px) {
  .analyst-shell {
    padding: 1rem;
  }

  .analyst-metrics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .analyst-grid-2 {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .analyst-metrics-grid {
    grid-template-columns: 1fr;
  }

  .analyst-segment-layout {
    flex-direction: column;
    align-items: flex-start;
  }

  .donut-wrap {
    width: 120px;
    height: 120px;
  }

  .donut-center {
    width: 56px;
    height: 56px;
  }
}
</style>

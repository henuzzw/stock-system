<template>
  <div class="symbol-detail-page">
    <div class="page-head">
      <el-button text @click="goBack">← 返回</el-button>
      <div class="title-wrap">
        <h2>{{ symbol?.name || '--' }}</h2>
        <el-tag type="info" effect="plain">{{ symbol?.code || '--' }}</el-tag>
      </div>
    </div>

    <el-row :gutter="16" class="kpi-row">
      <el-col :span="8">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-label">最新收盘</div>
          <div class="kpi-value">{{ fmtMoney(latestClose) }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-label">最新 PE(TTM)</div>
          <div class="kpi-value">{{ fmtNumber(latestPe) }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-label">最新 PB</div>
          <div class="kpi-value">{{ fmtNumber(latestPb) }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-head">日线趋势（Close / SMA20 / SMA60）</div>
          </template>
          <div ref="chartEl" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-head">分钟实时走势</div>
          </template>
          <div ref="intradayChartEl" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="section-card">
      <template #header>
        <div class="card-head">最新估值（近 10 条）</div>
      </template>
      <el-table :data="fundamentals" stripe size="small" v-if="fundamentals.length">
        <el-table-column prop="trade_date" label="日期" width="110" />
        <el-table-column prop="pe_ttm" label="PE" width="100">
          <template #default="{ row }">{{ fmtNumber(row.pe_ttm) }}</template>
        </el-table-column>
        <el-table-column prop="pb" label="PB" width="100">
          <template #default="{ row }">{{ fmtNumber(row.pb) }}</template>
        </el-table-column>
        <el-table-column prop="ps" label="PS" width="100">
          <template #default="{ row }">{{ fmtNumber(row.ps) }}</template>
        </el-table-column>
        <el-table-column prop="dividend_yield" label="股息率" width="110">
          <template #default="{ row }">{{ fmtPercent(row.dividend_yield) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无估值数据" />
    </el-card>

    <el-card class="section-card">
      <template #header>
        <div class="card-head">候选池信号历史</div>
      </template>
      <el-table :data="candidates" stripe size="small" v-if="candidates.length">
        <el-table-column prop="run_date" label="日期" width="110" />
        <el-table-column prop="left_signal_score" label="左侧分" width="90" />
        <el-table-column prop="right_signal_score" label="右侧分" width="90" />
        <el-table-column prop="left_triggered" label="左侧触发" width="90" />
        <el-table-column prop="right_triggered" label="右侧触发" width="90" />
        <el-table-column prop="rank_left" label="Left Rank" width="100" />
        <el-table-column prop="rank_right" label="Right Rank" width="100" />
      </el-table>
      <el-empty v-else description="暂无候选池数据" />
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import * as echarts from 'echarts'
import { getSymbolDetail } from '../api'

const router = useRouter()
const route = useRoute()

const symbol = ref(null)
const prices = ref([])
const fundamentals = ref([])
const candidates = ref([])
const technicals = ref([])
const intraday = ref([])
const chartEl = ref(null)
const intradayChartEl = ref(null)
let chart
let intradayChart

const latestClose = computed(() => prices.value?.[0]?.close)
const latestPe = computed(() => fundamentals.value?.[0]?.pe_ttm)
const latestPb = computed(() => fundamentals.value?.[0]?.pb)

const load = async () => {
  const code = route.params.code
  const res = await getSymbolDetail(code, 180)
  symbol.value = res.data.symbol
  prices.value = (res.data.prices || []).slice().reverse()
  technicals.value = (res.data.technicals || []).slice().reverse()
  fundamentals.value = (res.data.fundamentals || []).slice(0, 10)
  candidates.value = res.data.candidates || []
  intraday.value = res.data.intraday || []
  renderChart()
  renderIntradayChart()
}

const renderChart = () => {
  if (!chartEl.value) return
  if (!chart) chart = echarts.init(chartEl.value)

  const x = prices.value.map(p => p.trade_date)
  const close = prices.value.map(p => p.close)

  const sma20Map = new Map(technicals.value.map(t => [t.trade_date, t.sma_20]))
  const sma60Map = new Map(technicals.value.map(t => [t.trade_date, t.sma_60]))
  const sma20 = x.map(d => sma20Map.get(d) ?? null)
  const sma60 = x.map(d => sma60Map.get(d) ?? null)

  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['Close', 'SMA20', 'SMA60'] },
    xAxis: { type: 'category', data: x },
    yAxis: { type: 'value', scale: true },
    series: [
      { name: 'Close', data: close, type: 'line', smooth: true, showSymbol: false },
      { name: 'SMA20', data: sma20, type: 'line', smooth: true, showSymbol: false },
      { name: 'SMA60', data: sma60, type: 'line', smooth: true, showSymbol: false }
    ]
  })
}

const renderIntradayChart = () => {
  if (!intradayChartEl.value) return
  if (!intradayChart) intradayChart = echarts.init(intradayChartEl.value)

  const rows = intraday.value || []
  const x = rows.map(p => (p.ts || '').toString().slice(11, 16))
  const close = rows.map(p => Number(p.close || 0))
  const base = close.find(v => Number.isFinite(v) && v > 0) || 0
  const pct = close.map(v => (base > 0 ? ((v - base) / base) * 100 : 0))

  intradayChart.setOption({
    tooltip: {
      trigger: 'axis',
      valueFormatter: (v) => (v == null ? '--' : Number(v).toFixed(2))
    },
    legend: { data: ['Minute Close', '涨幅%'] },
    xAxis: { type: 'category', data: x },
    yAxis: [
      { type: 'value', scale: true, name: '价格' },
      { type: 'value', scale: true, name: '涨幅%', axisLabel: { formatter: '{value}%' } }
    ],
    series: [
      { name: 'Minute Close', data: close, type: 'line', smooth: true, showSymbol: false, yAxisIndex: 0 },
      { name: '涨幅%', data: pct, type: 'line', smooth: true, showSymbol: false, yAxisIndex: 1 }
    ]
  })
}

const fmtNumber = (v) => (v == null ? '--' : Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 2 }))
const fmtMoney = (v) => (v == null ? '--' : Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }))
const fmtPercent = (v) => (v == null ? '--' : `${Number(v).toFixed(2)}%`)

const goBack = () => router.push('/')

onMounted(load)
watch(() => route.params.code, load)
</script>

<style scoped>
.symbol-detail-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-wrap h2 {
  margin: 0;
  font-size: 22px;
}

.kpi-card {
  min-height: 88px;
}

.kpi-label {
  color: #909399;
  font-size: 13px;
}

.kpi-value {
  margin-top: 6px;
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.card-head {
  font-weight: 600;
}

.chart-box {
  height: 320px;
}

.section-card {
  margin-top: 0;
}
</style>

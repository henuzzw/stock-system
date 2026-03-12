<template>
  <div>
    <el-button type="text" @click="goBack">← 返回</el-button>
    <h2>{{ symbol?.name }} ({{ symbol?.code }})</h2>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card>
          <div ref="chartEl" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <h4>最新估值</h4>
          <el-table :data="fundamentals" style="width:100%" size="small">
            <el-table-column prop="trade_date" label="日期" width="110" />
            <el-table-column prop="pe_ttm" label="PE" width="90" />
            <el-table-column prop="pb" label="PB" width="90" />
            <el-table-column prop="ps" label="PS" width="90" />
            <el-table-column prop="dividend_yield" label="股息率" width="90" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 16px">
      <h4>候选池信号历史</h4>
      <el-table :data="candidates" style="width:100%" size="small">
        <el-table-column prop="run_date" label="日期" width="110" />
        <el-table-column prop="left_signal_score" label="左侧分" width="90" />
        <el-table-column prop="right_signal_score" label="右侧分" width="90" />
        <el-table-column prop="left_triggered" label="左侧触发" width="90" />
        <el-table-column prop="right_triggered" label="右侧触发" width="90" />
        <el-table-column prop="rank_left" label="Left Rank" width="90" />
        <el-table-column prop="rank_right" label="Right Rank" width="90" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
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
const chartEl = ref(null)
let chart

const load = async () => {
  const code = route.params.code
  const res = await getSymbolDetail(code, 180)
  symbol.value = res.data.symbol
  prices.value = (res.data.prices || []).slice().reverse()
  technicals.value = (res.data.technicals || []).slice().reverse()
  fundamentals.value = (res.data.fundamentals || []).slice(0, 10)
  candidates.value = res.data.candidates || []
  renderChart()
}

const renderChart = () => {
  if (!chartEl.value) return
  if (!chart) chart = echarts.init(chartEl.value)

  const x = prices.value.map(p => p.trade_date)
  const close = prices.value.map(p => p.close)

  const sma10Map = new Map(technicals.value.map(t => [t.trade_date, t.sma_10]))
  const sma20Map = new Map(technicals.value.map(t => [t.trade_date, t.sma_20]))
  const sma10 = x.map(d => sma10Map.get(d) ?? null)
  const sma20 = x.map(d => sma20Map.get(d) ?? null)

  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['收盘价', 'SMA10', 'SMA20'] },
    xAxis: { type: 'category', data: x },
    yAxis: { type: 'value' },
    series: [
      { name: '收盘价', data: close, type: 'line', smooth: true },
      { name: 'SMA10', data: sma10, type: 'line', smooth: true },
      { name: 'SMA20', data: sma20, type: 'line', smooth: true }
    ]
  })
}

const goBack = () => router.push('/')

onMounted(load)
watch(() => route.params.code, load)
</script>

<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in cards" :key="card.key">
        <el-card>
          <div class="label">{{ card.label }}</div>
          <div class="value">{{ card.value }}</div>
          <div class="sub">最新时间：{{ card.latest || '-' }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top:16px">
      <template #header>说明</template>
      <ul>
        <li>这里展示数据库中各模块的最新数据时间与记录量。</li>
        <li>如果最新日期/时间长期不更新，通常说明采集或研究任务没跑起来。</li>
        <li>更详细日志仍可看服务器日志文件和 journalctl。</li>
      </ul>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'

const cards = ref([])

const load = async () => {
  const res = await api.get('/status')
  const d = res.data
  cards.value = [
    { key: 'symbols', label: '激活股票数', value: d.symbols?.cnt ?? 0, latest: '' },
    { key: 'technical', label: '技术榜单', value: d.technical?.cnt ?? 0, latest: d.technical?.latest },
    { key: 'valuation', label: '估值榜单', value: d.valuation?.cnt ?? 0, latest: d.valuation?.latest },
    { key: 'candidates', label: '候选池记录', value: d.candidates?.cnt ?? 0, latest: d.candidates?.latest },
    { key: 'scores', label: '评分记录', value: d.scores?.cnt ?? 0, latest: d.scores?.latest },
    { key: 'minute_prices', label: '分钟线记录', value: d.minute_prices?.cnt ?? 0, latest: d.minute_prices?.latest },
  ]
}

onMounted(load)
</script>

<style scoped>
.label { color:#666; font-size:13px; }
.value { font-size:28px; font-weight:700; margin-top:8px; }
.sub { color:#999; margin-top:8px; font-size:12px; word-break: break-all; }
</style>

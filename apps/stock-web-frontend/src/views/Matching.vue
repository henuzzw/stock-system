<template>
  <div>
    <el-alert v-if="!token" title="请先登录后再执行撮合" type="warning" show-icon :closable="false">
      <template #default>
        <router-link to="/login">去登录</router-link>
      </template>
    </el-alert>

    <template v-else>
      <el-card style="margin-bottom:16px;">
        <template #header>
          <div style="display:flex; align-items:center; justify-content:space-between; gap:12px; flex-wrap:wrap;">
            <span>D7 撮合</span>
            <div style="display:flex; gap:8px;">
              <el-button type="primary" :loading="running" @click="runMatching">执行撮合</el-button>
              <el-button @click="loadLastRun">刷新</el-button>
            </div>
          </div>
        </template>

        <el-alert
          v-if="msg"
          :title="msg"
          :type="ok ? 'success' : 'error'"
          show-icon
          :closable="false"
          style="margin-bottom:16px;"
        />

        <el-descriptions :column="4" border v-if="currentRun">
          <el-descriptions-item label="本次扫描">{{ currentRun.scanned ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="本次成交">{{ currentRun.filled ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="本次跳过">{{ currentRun.skipped ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="本次金额">{{ fmtMoney(currentRun.totalAmount) }}</el-descriptions-item>
        </el-descriptions>

        <template v-if="runResults.length">
          <el-divider content-position="left">本次逐单结果</el-divider>
          <el-table :data="runResults" size="small" style="width:100%; margin-bottom:16px;">
            <el-table-column label="订单" min-width="180">
              <template #default="{ row }">
                <div>#{{ row.orderId }}</div>
                <div style="color:#606266; font-size:12px;">{{ row.code || '-' }} {{ row.name || '' }}</div>
              </template>
            </el-table-column>
            <el-table-column label="方向" min-width="120">
              <template #default="{ row }">{{ sideLabel(row.side) }} / {{ typeLabel(row.orderType) }}</template>
            </el-table-column>
            <el-table-column label="待撮数量" min-width="100">
              <template #default="{ row }">{{ fmtQty(row.remainingQuantity) }}</template>
            </el-table-column>
            <el-table-column label="撮合价" min-width="150">
              <template #default="{ row }">
                <span>{{ row.marketPrice == null ? '-' : fmtMoney(row.marketPrice) }}</span>
                <span v-if="row.priceSource" style="color:#909399;"> / {{ row.priceSource }}</span>
              </template>
            </el-table-column>
            <el-table-column label="结果" min-width="180">
              <template #default="{ row }">
                <el-tag :type="row.filled ? 'success' : 'warning'">{{ row.filled ? 'FILLED' : 'SKIPPED' }}</el-tag>
                <span v-if="row.skipReason" style="margin-left:8px;">{{ skipReasonLabel(row.skipReason) }}</span>
              </template>
            </el-table-column>
          </el-table>

          <el-divider content-position="left">跳过原因</el-divider>
          <el-empty v-if="!skippedResults.length" description="本次没有跳过订单" />
          <div v-else style="display:grid; gap:8px;">
            <el-alert
              v-for="row in skippedResults"
              :key="row.orderId"
              :title="`订单 #${row.orderId} ${row.code || '-'} ${skipReasonLabel(row.skipReason)}`"
              type="warning"
              show-icon
              :closable="false"
            >
              <template #default>
                {{ sideLabel(row.side) }} / {{ typeLabel(row.orderType) }}
                ，待撮 {{ fmtQty(row.remainingQuantity) }}
                ，限价 {{ row.limitPrice == null ? '-' : fmtMoney(row.limitPrice) }}
                ，市场价 {{ row.marketPrice == null ? '-' : fmtMoney(row.marketPrice) }}
                <span v-if="row.priceSource">，价格来源 {{ row.priceSource }}</span>
              </template>
            </el-alert>
          </div>
        </template>
        <el-empty v-else description="尚未执行过撮合" />
      </el-card>

      <el-card>
        <template #header><span>最近一次撮合</span></template>

        <el-descriptions v-if="lastRun" :column="2" border>
          <el-descriptions-item label="时间">{{ fmtDateTime(lastRun.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="扫描">{{ lastRun.scanned ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="成交">{{ lastRun.filled ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="跳过">{{ lastRun.skipped ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="总金额">{{ fmtMoney(lastRun.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="记录 ID">{{ lastRun.id ?? '-' }}</el-descriptions-item>
        </el-descriptions>
        <el-empty v-else description="暂无历史撮合记录" />
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../api'

const token = computed(() => localStorage.getItem('auth_token') || '')
const running = ref(false)
const msg = ref('')
const ok = ref(true)
const currentRun = ref(null)
const lastRun = ref(null)
const runResults = computed(() => currentRun.value?.results || [])
const skippedResults = computed(() => runResults.value.filter((row) => !row.filled))

const authHeader = () => ({ Authorization: `Bearer ${token.value}` })

const loadLastRun = async () => {
  if (!token.value) return
  msg.value = ''
  try {
    const res = await api.get('/matching/last', { headers: authHeader() })
    lastRun.value = res.data || null
    ok.value = true
  } catch (e) {
    ok.value = false
    msg.value = e?.response?.data?.message || '读取撮合结果失败'
  }
}

const runMatching = async () => {
  msg.value = ''
  running.value = true
  try {
    const res = await api.post('/matching/run', null, { headers: authHeader() })
    currentRun.value = res.data || null
    ok.value = true
    msg.value = '撮合执行完成'
    await loadLastRun()
  } catch (e) {
    ok.value = false
    msg.value = e?.response?.data?.message || '执行撮合失败'
  } finally {
    running.value = false
  }
}

const fmtMoney = (value) =>
  Number(value || 0).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 4
  })

const fmtQty = (value) =>
  Number(value || 0).toLocaleString('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 4
  })

const fmtDateTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false })
}

const sideLabel = (value) => {
  if (value === 'BUY') return '买入'
  if (value === 'SELL') return '卖出'
  return value || '-'
}

const typeLabel = (value) => {
  if (value === 'LIMIT') return '限价'
  if (value === 'MARKET') return '市价'
  return value || '-'
}

const skipReasonLabel = (value) => {
  const mapping = {
    NO_PRICE: '无可用价格',
    LIMIT_NOT_REACHED: '限价未触发',
    INSUFFICIENT_CASH: '现金不足',
    INSUFFICIENT_POSITION: '持仓不足',
    ORDER_NOT_OPEN: '订单不是可撮合状态',
    PROCESSING_ERROR: '处理异常'
  }
  return mapping[value] || value || '-'
}

onMounted(loadLastRun)
</script>

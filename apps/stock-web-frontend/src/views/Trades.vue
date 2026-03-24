<template>
  <div>
    <el-alert v-if="!token" title="请先登录后再查看成交记录" type="warning" show-icon :closable="false">
      <template #default>
        <router-link to="/login">去登录</router-link>
      </template>
    </el-alert>

    <el-card v-else>
      <template #header>
        <div style="display:flex; align-items:center; justify-content:space-between; gap:12px;">
          <span>成交记录</span>
          <el-button @click="loadTrades">刷新</el-button>
        </div>
      </template>

      <el-alert v-if="msg" :title="msg" :type="ok ? 'success' : 'error'" show-icon :closable="false" style="margin-bottom:16px;" />

      <el-table :data="trades" stripe>
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="code" label="代码" width="110" />
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="side" label="方向" width="90" />
        <el-table-column prop="quantity" label="数量" width="110" align="right">
          <template #default="{row}">{{ fmtNumber(row.quantity, 0, 4) }}</template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="110" align="right">
          <template #default="{row}">{{ fmtNumber(row.price, 2, 4) }}</template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="140" align="right">
          <template #default="{row}">{{ fmtNumber(row.amount, 2, 4) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" min-width="180">
          <template #default="{row}">{{ fmtDateTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../api'

const token = computed(() => localStorage.getItem('auth_token') || '')
const trades = ref([])
const msg = ref('')
const ok = ref(true)

const authHeader = () => ({ Authorization: `Bearer ${token.value}` })

const loadTrades = async () => {
  if (!token.value) return
  msg.value = ''
  try {
    const res = await api.get('/trades', { headers: authHeader() })
    trades.value = res.data || []
    ok.value = true
  } catch (e) {
    trades.value = []
    ok.value = false
    msg.value = e?.response?.data?.message || '读取成交记录失败'
  }
}

const fmtNumber = (value, minDigits = 2, maxDigits = 2) =>
  Number(value || 0).toLocaleString('zh-CN', {
    minimumFractionDigits: minDigits,
    maximumFractionDigits: maxDigits
  })

const fmtDateTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false })
}

onMounted(loadTrades)
</script>

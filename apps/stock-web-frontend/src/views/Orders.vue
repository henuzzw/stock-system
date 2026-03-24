<template>
  <div>
    <el-alert v-if="!token" title="请先登录后再下单" type="warning" show-icon :closable="false">
      <template #default>
        <router-link to="/login">去登录</router-link>
      </template>
    </el-alert>

    <template v-else>
      <el-card style="margin-bottom:16px;">
        <template #header><span>模拟下单</span></template>
        <el-form :model="form" inline>
          <el-form-item label="代码">
            <el-input v-model="form.symbol" placeholder="如 600519" style="width:140px" />
          </el-form-item>
          <el-form-item label="方向">
            <el-select v-model="form.side" style="width:110px">
              <el-option label="买入" value="BUY" />
              <el-option label="卖出" value="SELL" />
            </el-select>
          </el-form-item>
          <el-form-item label="类型">
            <el-select v-model="form.orderType" style="width:120px">
              <el-option label="限价" value="LIMIT" />
              <el-option label="市价" value="MARKET" />
            </el-select>
          </el-form-item>
          <el-form-item label="价格">
            <el-input-number v-model="form.price" :precision="2" :min="0.01" :step="0.01" />
          </el-form-item>
          <el-form-item label="数量">
            <el-input-number v-model="form.quantity" :precision="0" :min="1" :step="100" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="placing" @click="placeOrder">下单</el-button>
            <el-button @click="loadOrders">刷新</el-button>
          </el-form-item>
        </el-form>
        <el-alert v-if="msg" :title="msg" :type="ok ? 'success' : 'error'" show-icon :closable="false" />
      </el-card>

      <el-card>
        <template #header><span>订单列表</span></template>
        <el-table :data="orders" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="code" label="代码" width="100" />
          <el-table-column prop="name" label="名称" width="140" />
          <el-table-column prop="side" label="方向" width="90" />
          <el-table-column prop="orderType" label="类型" width="90" />
          <el-table-column prop="price" label="价格" width="100" align="right">
            <template #default="{row}">{{ fmt(row.price) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="100" align="right" />
          <el-table-column prop="filledQuantity" label="已成" width="100" align="right" />
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column prop="createdAt" label="时间" min-width="160" />
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../api'

const token = computed(() => localStorage.getItem('auth_token') || '')
const placing = ref(false)
const orders = ref([])
const msg = ref('')
const ok = ref(false)

const form = reactive({
  symbol: '600519',
  side: 'BUY',
  orderType: 'LIMIT',
  price: 100,
  quantity: 100
})

const authHeader = () => ({ Authorization: `Bearer ${token.value}` })

const loadOrders = async () => {
  if (!token.value) return
  try {
    const res = await api.get('/orders', { headers: authHeader() })
    orders.value = res.data || []
  } catch (e) {
    msg.value = e?.response?.data?.message || '读取订单失败'
    ok.value = false
  }
}

const placeOrder = async () => {
  msg.value = ''
  placing.value = true
  try {
    await api.post('/orders/place', {
      symbol: form.symbol,
      side: form.side,
      orderType: form.orderType,
      price: form.price,
      quantity: form.quantity
    }, { headers: authHeader() })
    ok.value = true
    msg.value = '下单成功'
    await loadOrders()
  } catch (e) {
    ok.value = false
    msg.value = e?.response?.data?.message || '下单失败'
  } finally {
    placing.value = false
  }
}

const fmt = (v) => Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

onMounted(loadOrders)
</script>

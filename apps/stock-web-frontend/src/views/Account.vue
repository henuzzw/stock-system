<template>
  <div>
    <!-- 未登录提示 -->
    <el-alert v-if="!token" title="请先登录" type="warning" show-icon :closable="false">
      <template #default>
        <router-link to="/login">登录</router-link> 后查看账户信息
      </template>
    </el-alert>

    <template v-else>
      <!-- 账户摘要卡片 -->
      <el-row :gutter="16" style="margin-bottom: 20px;">
        <el-col :span="8">
          <el-card shadow="hover">
            <template #header>初始资金</template>
            <div style="font-size: 24px; font-weight: 600; color: #409eff;">
              {{ formatMoney(summary?.initialCash) }}
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover">
            <template #header>当前现金</template>
            <div style="font-size: 24px; font-weight: 600; color: #67c23a;">
              {{ formatMoney(summary?.cashBalance) }}
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover">
            <template #header>持仓市值</template>
            <div style="font-size: 24px; font-weight: 600; color: #e6a23c;">
              {{ formatMoney(summary?.positionsMarketValue) }}
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 持仓列表 -->
      <el-card>
        <template #header>
          <span>持仓 ({{ positions.length }})</span>
        </template>

        <el-table :data="positions" stripe v-if="positions.length > 0">
          <el-table-column prop="symbolCode" label="代码" width="100" />
          <el-table-column prop="symbolName" label="名称" width="120" />
          <el-table-column prop="quantity" label="持仓数量" width="120" align="right">
            <template #default="{ row }">{{ formatQty(row.quantity) }}</template>
          </el-table-column>
          <el-table-column prop="availableQuantity" label="可用数量" width="120" align="right">
            <template #default="{ row }">{{ formatQty(row.availableQuantity) }}</template>
          </el-table-column>
          <el-table-column prop="avgCost" label="成本价" width="100" align="right">
            <template #default="{ row }">{{ formatMoney(row.avgCost) }}</template>
          </el-table-column>
          <el-table-column label="最新价" width="100" align="right">
            <template #default="{ row }">
              <span :style="{ color: row.priceChange >= 0 ? '#67c23a' : '#f56c6c' }">
                {{ formatMoney(row.currentPrice) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="浮动盈亏" align="right">
            <template #default="{ row }">
              <span :style="{ color: row.pnl >= 0 ? '#67c23a' : '#f56c6c' }">
                {{ formatMoney(row.pnl) }}
              </span>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-else description="暂无持仓" />
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../api'

const token = computed(() => localStorage.getItem('auth_token') || '')
const summary = ref(null)
const positions = ref([])
const loading = ref(false)

const load = async () => {
  if (!token.value) return
  loading.value = true
  try {
    const [sumRes, posRes] = await Promise.all([
      api.get('/account/summary', { headers: { Authorization: `Bearer ${token.value}` } }),
      api.get('/account/positions', { headers: { Authorization: `Bearer ${token.value}` } })
    ])
    summary.value = sumRes.data
    positions.value = posRes.data || []
  } catch (err) {
    console.error('加载账户信息失败', err)
  } finally {
    loading.value = false
  }
}

const formatMoney = (v) => {
  if (v == null) return '-'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const formatQty = (v) => {
  if (v == null) return '-'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 4 })
}

onMounted(load)
</script>

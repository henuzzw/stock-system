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

const fmtDateTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false })
}

onMounted(loadLastRun)
</script>

<template>
  <el-container style="min-height: 100vh">
    <el-header class="header">
      <div>
        <div class="title">Stock Research Dashboard</div>
        <div class="sub">A-share signals · technical / valuation / candidates</div>
      </div>
      <div style="margin-left:auto; display:flex; gap:10px; flex-wrap: wrap; justify-content: flex-end; align-items:center;">
        <el-button type="primary" plain @click="$router.push('/')">Dashboard</el-button>
        <el-button type="primary" plain @click="$router.push('/symbols')">全量股票</el-button>
        <el-button type="primary" plain @click="$router.push('/account')">账户</el-button>
        <el-button type="primary" plain @click="$router.push('/orders')">订单</el-button>
        <el-button type="primary" plain @click="$router.push('/trades')">成交</el-button>
        <el-button type="primary" plain @click="$router.push('/status')">状态页</el-button>
        <el-button type="success" plain @click="$router.push('/register')">注册</el-button>
        <el-button type="warning" plain @click="$router.push('/login')">登录</el-button>
        <span v-if="currentUser" style="color:#666; font-size:12px;">当前用户：{{ currentUser.username }}</span>
      </div>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'

const currentUser = computed(() => {
  try {
    const raw = localStorage.getItem('auth_user')
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
})
</script>

<style scoped>
.header {
  display: flex;
  align-items: baseline;
  gap: 16px;
  padding: 16px 24px;
  border-bottom: 1px solid #eee;
}
.title {
  font-size: 20px;
  font-weight: 600;
}
.sub {
  color: #888;
  font-size: 13px;
}
</style>

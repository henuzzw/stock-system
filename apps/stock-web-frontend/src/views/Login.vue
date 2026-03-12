<template>
  <div style="max-width: 420px; margin: 0 auto;">
    <el-card>
      <template #header>
        <div style="font-weight: 600;">登录</div>
      </template>

      <el-form :model="form" label-position="top" @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submit">登录</el-button>
          <el-button @click="loadMe" :disabled="!token">读取当前用户</el-button>
        </el-form-item>
      </el-form>

      <el-alert v-if="message" :title="message" :type="ok ? 'success' : 'error'" show-icon :closable="false" />

      <div v-if="currentUser" style="margin-top: 16px; color: #666; font-size: 13px;">
        当前登录：{{ currentUser.username }}（ID: {{ currentUser.id }}）
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import api from '../api'

const form = reactive({ username: '', password: '' })
const submitting = ref(false)
const message = ref('')
const ok = ref(false)
const currentUser = ref(null)

const token = computed(() => localStorage.getItem('auth_token') || '')

const submit = async () => {
  message.value = ''
  ok.value = false
  submitting.value = true
  try {
    const res = await api.post('/auth/login', form)
    localStorage.setItem('auth_token', res.data.token)
    localStorage.setItem('auth_user', JSON.stringify(res.data.user))
    currentUser.value = res.data.user
    ok.value = true
    message.value = '登录成功'
  } catch (err) {
    message.value = err?.response?.data?.message || '登录失败'
  } finally {
    submitting.value = false
  }
}

const loadMe = async () => {
  message.value = ''
  try {
    const res = await api.get('/auth/me', {
      headers: { Authorization: `Bearer ${token.value}` }
    })
    currentUser.value = res.data
    ok.value = true
    message.value = '已读取当前用户'
  } catch (err) {
    ok.value = false
    message.value = err?.response?.data?.message || '读取当前用户失败'
  }
}
</script>

<template>
  <div style="max-width: 420px; margin: 0 auto;">
    <el-card>
      <template #header>
        <div style="font-weight: 600;">注册账号</div>
      </template>

      <el-form :model="form" label-position="top" @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="至少 8 位" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submit">注册</el-button>
        </el-form-item>
      </el-form>

      <el-alert v-if="message" :title="message" :type="ok ? 'success' : 'error'" show-icon :closable="false" />
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()
const form = reactive({ username: '', password: '' })
const submitting = ref(false)
const message = ref('')
const ok = ref(false)

const submit = async () => {
  message.value = ''
  ok.value = false
  submitting.value = true
  const username = form.username
  const password = form.password
  try {
    const res = await api.post('/auth/register', { username, password })
    ok.value = !!res.data?.success
    message.value = res.data?.message || '注册成功'
    if (ok.value) {
      try {
        const loginRes = await api.post('/auth/login', { username, password })
        localStorage.setItem('auth_token', loginRes.data.token)
        localStorage.setItem('auth_user', JSON.stringify(loginRes.data.user))
        message.value = '注册并登录成功，正在跳转…'
        form.username = ''
        form.password = ''
        setTimeout(() => router.push('/'), 600)
      } catch {
        form.username = ''
        form.password = ''
        message.value = '注册成功，请前往登录'
        setTimeout(() => router.push('/login'), 800)
      }
    }
  } catch (err) {
    message.value = err?.response?.data?.message || '注册失败'
  } finally {
    submitting.value = false
  }
}
</script>

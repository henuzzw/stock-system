<template>
  <div>
    <el-row :gutter="12" style="margin-bottom: 12px">
      <el-col :span="12">
        <el-input v-model="q" placeholder="搜索代码/名称" clearable @keyup.enter="search" />
      </el-col>
      <el-col :span="12" style="text-align:right">
        <el-button type="primary" @click="search">搜索</el-button>
      </el-col>
    </el-row>

    <el-table :data="rows" style="width:100%" @row-click="go">
      <el-table-column type="index" label="#" width="50" />
      <el-table-column prop="code" label="代码" width="120" />
      <el-table-column prop="name" label="名称" width="200" />
      <el-table-column prop="market" label="市场" width="100" />
      <el-table-column prop="exchange" label="交易所" width="100" />
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getSymbols } from '../api'

const router = useRouter()
const q = ref('')
const rows = ref([])

const search = async () => {
  const res = await getSymbols(q.value, 500)
  rows.value = res.data
}

const go = (row) => {
  if (!row?.code) return
  router.push({ name: 'symbol', params: { code: row.code } })
}

onMounted(search)
</script>

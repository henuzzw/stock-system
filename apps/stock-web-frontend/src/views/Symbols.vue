<template>
  <div>
    <el-row :gutter="12" style="margin-bottom: 12px">
      <el-col :span="10">
        <el-input v-model="q" placeholder="搜索代码/名称（支持简称模糊）" clearable @keyup.enter="search" />
      </el-col>
      <el-col :span="14" style="text-align:right; display:flex; justify-content:flex-end; gap:12px;">
        <el-button type="primary" @click="search">搜索</el-button>
        <el-button type="success" @click="dialogVisible = true">手动添加股票</el-button>
      </el-col>
    </el-row>

    <el-table :data="rows" style="width:100%" @row-click="go">
      <el-table-column type="index" label="#" width="50" />
      <el-table-column prop="code" label="代码" width="120" />
      <el-table-column prop="name" label="名称" width="220" />
      <el-table-column prop="market" label="市场" width="100" />
      <el-table-column prop="exchange" label="交易所" width="100" />
    </el-table>

    <el-dialog v-model="dialogVisible" title="手动添加股票" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="代码">
          <el-input v-model="form.code" placeholder="如 601868" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="如 中国能建" />
        </el-form-item>
        <el-form-item label="市场">
          <el-select v-model="form.market" style="width:100%">
            <el-option label="CN" value="CN" />
            <el-option label="US" value="US" />
            <el-option label="HK" value="HK" />
          </el-select>
        </el-form-item>
        <el-form-item label="交易所">
          <el-select v-model="form.exchange" style="width:100%" allow-create filterable>
            <el-option label="SH" value="SH" />
            <el-option label="SZ" value="SZ" />
            <el-option label="BJ" value="BJ" />
            <el-option label="HKEX" value="HKEX" />
            <el-option label="NASDAQ" value="NASDAQ" />
            <el-option label="NYSE" value="NYSE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAdd">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { addSymbol, getSymbols } from '../api'

const router = useRouter()
const q = ref('')
const rows = ref([])
const dialogVisible = ref(false)
const submitting = ref(false)
const form = reactive({
  code: '',
  name: '',
  market: 'CN',
  exchange: 'SH',
  source: 'manual'
})

const search = async () => {
  const res = await getSymbols(q.value, 500)
  rows.value = res.data
}

const go = (row) => {
  if (!row?.code) return
  router.push({ name: 'symbol', params: { code: row.code } })
}

const resetForm = () => {
  form.code = ''
  form.name = ''
  form.market = 'CN'
  form.exchange = 'SH'
  form.source = 'manual'
}

const submitAdd = async () => {
  if (!form.code.trim() || !form.name.trim()) {
    ElMessage.error('代码和名称必填')
    return
  }
  submitting.value = true
  try {
    const res = await addSymbol({ ...form })
    const payload = res.data || {}
    ElMessage.success(payload.message || '添加成功')
    dialogVisible.value = false
    q.value = form.code.trim()
    resetForm()
    await search()
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || '添加失败')
  } finally {
    submitting.value = false
  }
}

onMounted(search)
</script>

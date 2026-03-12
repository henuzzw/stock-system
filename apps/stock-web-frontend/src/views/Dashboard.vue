<template>
  <div>
    <el-row :gutter="16" class="controls">
      <el-col :span="8">
        <el-date-picker v-model="pickedDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
      </el-col>
      <el-col :span="16" style="text-align: right">
        <el-button @click="pickedDate=''">今天</el-button>
        <el-button type="primary" @click="loadAll">刷新</el-button>
      </el-col>
    </el-row>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="技术低位 Top20" name="tech">
        <data-table :rows="tech" @row-click="goSymbol" @view-chart="goSymbol" />
      </el-tab-pane>
      <el-tab-pane label="估值便宜 Top20" name="val">
        <data-table :rows="val" @row-click="goSymbol" @view-chart="goSymbol" />
      </el-tab-pane>
      <el-tab-pane label="候选池 Left Top20" name="left">
        <el-row :gutter="12" style="margin-bottom: 10px">
          <el-col :span="6"><el-switch v-model="leftTriggered" active-text="仅触发" /></el-col>
          <el-col :span="8"><el-switch v-model="leftIntersection" active-text="仅交集(T&V)" /></el-col>
          <el-col :span="6">
            <el-select v-model="leftSort" style="width: 100%">
              <el-option label="按排名" value="rank" />
              <el-option label="按分数" value="score" />
            </el-select>
          </el-col>
          <el-col :span="4" style="text-align:right"><el-button @click="loadLeft">应用</el-button></el-col>
        </el-row>
        <candidates-table :rows="candLeft" @row-click="goSymbol" @view-chart="goSymbol" />
      </el-tab-pane>
      <el-tab-pane label="候选池 Right Top20" name="right">
        <el-row :gutter="12" style="margin-bottom: 10px">
          <el-col :span="6"><el-switch v-model="rightTriggered" active-text="仅触发" /></el-col>
          <el-col :span="8"><el-switch v-model="rightIntersection" active-text="仅交集(T&V)" /></el-col>
          <el-col :span="6">
            <el-select v-model="rightSort" style="width: 100%">
              <el-option label="按排名" value="rank" />
              <el-option label="按分数" value="score" />
            </el-select>
          </el-col>
          <el-col :span="4" style="text-align:right"><el-button @click="loadRight">应用</el-button></el-col>
        </el-row>
        <candidates-table :rows="candRight" @row-click="goSymbol" @view-chart="goSymbol" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getTechnicalTop, getValuationTop, getCandidates } from '../api'
import DataTable from '../components/DataTable.vue'
import CandidatesTable from '../components/CandidatesTable.vue'

const router = useRouter()
const activeTab = ref('tech')
const pickedDate = ref('')
const tech = ref([])
const val = ref([])
const candLeft = ref([])
const candRight = ref([])

const leftTriggered = ref(false)
const leftIntersection = ref(false)
const leftSort = ref('rank')

const rightTriggered = ref(false)
const rightIntersection = ref(false)
const rightSort = ref('rank')

const loadLeft = async () => {
  const date = pickedDate.value || undefined
  const res = await getCandidates(date, 'left', {
    triggered: leftTriggered.value,
    intersection: leftIntersection.value,
    sort: leftSort.value,
    limit: 20
  })
  candLeft.value = res.data
}

const loadRight = async () => {
  const date = pickedDate.value || undefined
  const res = await getCandidates(date, 'right', {
    triggered: rightTriggered.value,
    intersection: rightIntersection.value,
    sort: rightSort.value,
    limit: 20
  })
  candRight.value = res.data
}

const loadAll = async () => {
  const date = pickedDate.value || undefined
  const [t, v] = await Promise.all([
    getTechnicalTop(date),
    getValuationTop(date)
  ])
  tech.value = t.data
  val.value = v.data
  await Promise.all([loadLeft(), loadRight()])
}

const goSymbol = (row) => {
  if (!row?.code) return
  router.push({ name: 'symbol', params: { code: row.code } })
}

onMounted(loadAll)
</script>

<style scoped>
.controls { margin-bottom: 12px; }
</style>

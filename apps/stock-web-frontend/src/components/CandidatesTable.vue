<template>
  <el-table :data="rows" style="width: 100%" @row-click="$emit('row-click', $event)">
    <el-table-column type="index" label="#" width="50" />
    <el-table-column prop="code" label="代码" width="100" />
    <el-table-column prop="name" label="名称" width="140" />
    <el-table-column label="左侧分" width="160">
      <template #default="{ row }">
        <score-bar :value="row.left_signal_score" />
      </template>
    </el-table-column>
    <el-table-column label="右侧分" width="160">
      <template #default="{ row }">
        <score-bar :value="row.right_signal_score" />
      </template>
    </el-table-column>
    <el-table-column prop="left_triggered" label="左侧触发" width="90">
      <template #default="{ row }">
        <el-tag :type="row.left_triggered ? 'success' : 'info'">{{ row.left_triggered ? '是' : '否' }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="right_triggered" label="右侧触发" width="90">
      <template #default="{ row }">
        <el-tag :type="row.right_triggered ? 'success' : 'info'">{{ row.right_triggered ? '是' : '否' }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="in_technical_top20" label="技术Top" width="80" />
    <el-table-column prop="in_valuation_top20" label="估值Top" width="80" />
    <el-table-column prop="rank_left" label="Left Rank" width="90" />
    <el-table-column prop="rank_right" label="Right Rank" width="90" />
    <el-table-column label="K线图" width="110" fixed="right">
      <template #default="{ row }">
        <el-button type="primary" link @click.stop="$emit('view-chart', row)">查看</el-button>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup>
import ScoreBar from './ScoreBar.vue'

defineEmits(['row-click', 'view-chart'])
defineProps({ rows: { type: Array, default: () => [] } })
</script>

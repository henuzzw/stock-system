<template>
  <el-table :data="rows" style="width: 100%" @row-click="$emit('row-click', $event)">
    <el-table-column type="index" label="#" width="50" />
    <el-table-column prop="code" label="代码" width="100" />
    <el-table-column prop="name" label="名称" width="140" />
    <el-table-column label="评分" width="160">
      <template #default="{ row }">
        <score-bar :value="row.score" />
      </template>
    </el-table-column>
    <el-table-column prop="rank" label="排名" width="80" />
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

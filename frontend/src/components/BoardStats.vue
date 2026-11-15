<script setup>
/**
 * 汇总指标条：展示总数、各列数量、逾期数量与完成率。
 * 边界：纯 props 渲染；汇总口径由后端 summary 接口统一给出，前端不自算。
 */
import { computed } from 'vue'

const props = defineProps({
  /** 汇总指标对象；null 表示尚未加载。 */
  summary: { type: Object, default: null },
})

/** 各列数量，按看板列顺序展示。 */
const columns = computed(() => {
  const source = props.summary
  if (!source) {
    return []
  }
  return [
    { key: 'todo', label: '待办', value: source.todoCount },
    { key: 'doing', label: '进行中', value: source.doingCount },
    { key: 'review', label: '待验收', value: source.reviewCount },
    { key: 'done', label: '已完成', value: source.doneCount },
  ]
})
</script>

<template>
  <div class="stats">
    <div class="stats__item">
      <span class="stats__label">任务总数</span>
      <span class="stats__value">{{ summary ? summary.total : '—' }}</span>
    </div>

    <div v-for="item in columns" :key="item.key" class="stats__item">
      <span class="stats__label">{{ item.label }}</span>
      <span class="stats__value">{{ item.value }}</span>
    </div>

    <div class="stats__item">
      <span class="stats__label">逾期</span>
      <span :class="['stats__value', { 'stats__value--warn': summary && summary.overdueCount > 0 }]">
        {{ summary ? summary.overdueCount : '—' }}
      </span>
    </div>

    <div class="stats__item">
      <span class="stats__label">完成率</span>
      <span class="stats__value">{{ summary ? `${summary.completionRate}%` : '—' }}</span>
    </div>
  </div>
</template>

<style scoped>
.stats {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.stats__item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 84px;
  padding: 8px 14px;
  border: 1px solid #e3e8ef;
  border-radius: 8px;
  background: #ffffff;
}

.stats__label {
  color: #8a93a3;
  font-size: 12px;
}

.stats__value {
  color: #1f2937;
  font-size: 16px;
  font-weight: 600;
}

.stats__value--warn {
  color: #b42318;
}
</style>

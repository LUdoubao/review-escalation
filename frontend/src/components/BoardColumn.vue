<script setup>
/**
 * 看板单列：渲染列标题、数量与卡片列表，并把卡片事件按列上下文转发给页面。
 * 边界：不排序、不筛选、不请求；数据顺序即后端返回的列内序号顺序。
 * 展示细节：列头同时给出「已展示条数 / 该列总数」，超出部分由后端截断并在此提示。
 */
import TaskCard from './TaskCard.vue'

defineProps({
  /** 列数据，结构为 { status, label, total, tasks }。 */
  column: { type: Object, required: true },
})

const emit = defineEmits(['card-move', 'card-edit', 'card-remove'])

/**
 * 转发卡片移动事件，附带任务主键与目标状态，由页面决定是否连同位置一起提交。
 *
 * @param {object} task 被移动的卡片
 * @param {string} targetStatus 目标状态编码
 * @returns {void}
 */
function onCardMove(task, targetStatus) {
  emit('card-move', task.id, targetStatus)
}
</script>

<template>
  <section class="column">
    <header class="column__head">
      <h3 class="column__title">{{ column.label }}</h3>
      <span class="column__count">
        {{ column.tasks.length }}<template v-if="column.total > column.tasks.length"> / {{ column.total }}</template> 条
      </span>
    </header>

    <div class="column__body">
      <TaskCard
        v-for="task in column.tasks"
        :key="task.id"
        :task="task"
        @move="(target) => onCardMove(task, target)"
        @edit="emit('card-edit', task)"
        @remove="emit('card-remove', task)"
      />

      <p v-if="!column.tasks.length" class="column__empty">该列暂无任务</p>
    </div>
  </section>
</template>

<style scoped>
.column {
  display: flex;
  flex-direction: column;
  min-height: 220px;
  padding: 12px;
  border: 1px solid #e3e8ef;
  border-radius: 10px;
  background: #f9fbfe;
}

.column__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 10px;
}

.column__title {
  margin: 0;
  font-size: 14px;
  color: #1f2937;
}

.column__count {
  color: #8a93a3;
  font-size: 12px;
}

.column__body {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  gap: 10px;
}

.column__empty {
  margin: 0;
  padding: 18px 0;
  border: 1px dashed #d7dee9;
  border-radius: 8px;
  color: #a3aab7;
  font-size: 12px;
  text-align: center;
}
</style>

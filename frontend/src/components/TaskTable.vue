<script setup>
/**
 * 列表视图：以表格形式展示当前页任务，提供与看板一致的编辑、移动、删除入口。
 * 边界：只渲染传入的 items；排序、筛选、分页都由页面与 store 负责。
 */
import { STATUS_OPTIONS } from '../stores/boardStore.js'
import { formatDate } from '../utils/date.js'

defineProps({
  /** 当前页任务列表。 */
  items: { type: Array, default: () => [] },
})

const emit = defineEmits(['edit', 'remove', 'move'])

/**
 * 处理表格行内的目标列选择。
 *
 * @param {object} task 当前行任务
 * @param {Event} event 变更事件
 * @returns {void}
 */
function onMoveChange(task, event) {
  const target = event.target.value
  event.target.value = ''
  if (target) {
    emit('move', task.id, target)
  }
}
</script>

<template>
  <div class="table-wrap">
    <table class="table">
      <thead>
        <tr>
          <th scope="col">标题</th>
          <th scope="col">负责人</th>
          <th scope="col">优先级</th>
          <th scope="col">状态</th>
          <th scope="col">截止日期</th>
          <th scope="col">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="task in items" :key="task.id">
          <td class="table__title">
            {{ task.title }}
            <span v-if="task.tag" class="table__tag">{{ task.tag }}</span>
          </td>
          <td>{{ task.assignee || '未指派' }}</td>
          <td>{{ task.priorityLabel || task.priority }}</td>
          <td>{{ task.statusLabel || task.status }}</td>
          <td :class="{ 'table__due--overdue': task.overdue }">{{ formatDate(task.dueDate) }}</td>
          <td class="table__ops">
            <span v-if="task.terminal" class="table__locked">已完成 · 不可移动</span>
            <select v-else class="table__move" :aria-label="`移动任务 ${task.title}`" @change="onMoveChange(task, $event)">
              <option value="">移动…</option>
              <option
                v-for="item in STATUS_OPTIONS.filter((option) => option.status !== task.status)"
                :key="item.status"
                :value="item.status"
              >
                {{ item.label }}
              </option>
            </select>
            <button type="button" class="table__link" @click="emit('edit', task)">编辑</button>
            <button type="button" class="table__link table__link--danger" @click="emit('remove', task)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>

    <p v-if="!items.length" class="table__empty">当前筛选条件下没有任务</p>
  </div>
</template>

<style scoped>
.table-wrap {
  overflow-x: auto;
  border: 1px solid #e3e8ef;
  border-radius: 10px;
  background: #ffffff;
}

.table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.table th,
.table td {
  padding: 10px 12px;
  border-bottom: 1px solid #edf0f5;
  text-align: left;
  color: #475467;
}

.table th {
  background: #f9fbfe;
  color: #6b7484;
  font-weight: 600;
  white-space: nowrap;
}

.table tbody tr:last-child td {
  border-bottom: none;
}

.table__title {
  color: #1f2937;
}

.table__tag {
  margin-left: 6px;
  padding: 0 6px;
  border-radius: 8px;
  background: #eef2f7;
  color: #6b7484;
  font-size: 11px;
}

.table__due--overdue {
  color: #b42318;
}

.table__ops {
  display: flex;
  gap: 6px;
  align-items: center;
}

.table__move {
  height: 26px;
  border: 1px solid #d7dee9;
  border-radius: 5px;
  background: #ffffff;
  color: #475467;
  font-size: 12px;
}

.table__locked {
  color: #8a93a3;
  font-size: 12px;
}

.table__link {
  padding: 0;
  border: none;
  background: none;
  color: #2f6feb;
  font-size: 12px;
  cursor: pointer;
}

.table__link--danger {
  color: #b42318;
}

.table__empty {
  margin: 0;
  padding: 28px 0;
  color: #8a93a3;
  font-size: 13px;
  text-align: center;
}
</style>

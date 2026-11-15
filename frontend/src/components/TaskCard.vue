<script setup>
/**
 * 看板卡片：展示单个任务的关键信息与操作入口。
 * 边界：纯 props 渲染 + 事件上抛；不发起请求、不修改传入对象。
 * 可访问性：操作按钮带 aria-label，逾期日期同时用文字「逾期」标注，不单靠颜色区分。
 */
import { computed } from 'vue'
import { STATUS_OPTIONS } from '../stores/boardStore.js'
import { formatDate } from '../utils/date.js'

const props = defineProps({
  /** 任务出参，字段见 src/api/tasks.js 的注释。 */
  task: { type: Object, required: true },
})

const emit = defineEmits(['move', 'edit', 'remove'])

/** 可移动的目标列，排除当前所在列，避免出现「移动到当前列」的无意义选项。 */
const moveTargets = computed(() => STATUS_OPTIONS.filter((item) => item.status !== props.task.status))

/**
 * 是否逾期，直接采用后端派生的 overdue 字段。
 * 不在前端用日期重新推算：前后端对「截止日当天是否算逾期」和时区口径必须只有一个来源。
 */
const overdue = computed(() => Boolean(props.task.overdue))

/** 优先级角标文案，优先使用后端返回的显示名。 */
const priorityLabel = computed(() => props.task.priorityLabel || props.task.priority)

/**
 * 处理目标列选择：把目标状态上抛后立即复位下拉，避免二次选择时显示上一次的值。
 *
 * @param {Event} event 变更事件
 * @returns {void}
 */
function onMoveChange(event) {
  const target = event.target.value
  event.target.value = ''
  if (target) {
    emit('move', target)
  }
}
</script>

<template>
  <article class="card">
    <header class="card__head">
      <span :class="['card__priority', `card__priority--${task.priority.toLowerCase()}`]">{{ priorityLabel }}</span>
      <span v-if="task.tag" class="card__tag">{{ task.tag }}</span>
    </header>

    <h4 class="card__title">{{ task.title }}</h4>
    <p v-if="task.description" class="card__desc">{{ task.description }}</p>

    <dl class="card__meta">
      <div class="card__meta-row">
        <dt>负责人</dt>
        <dd>{{ task.assignee || '未指派' }}</dd>
      </div>
      <div class="card__meta-row">
        <dt>截止</dt>
        <dd :class="{ 'card__due--overdue': overdue }">
          {{ formatDate(task.dueDate) }}
          <span v-if="overdue" class="card__overdue-mark">逾期</span>
        </dd>
      </div>
    </dl>

    <footer class="card__foot">
      <!-- 终态任务不再提供移动入口：后端会以 409 拒绝，前端提前隐藏可避免用户白试一次。 -->
      <span v-if="task.terminal" class="card__locked">已完成 · 不可移动</span>
      <select v-else class="card__move" aria-label="移动到其他列" @change="onMoveChange">
        <option value="">移动到…</option>
        <option v-for="item in moveTargets" :key="item.status" :value="item.status">{{ item.label }}</option>
      </select>
      <button class="card__action" type="button" :aria-label="`编辑任务 ${task.title}`" @click="emit('edit', task)">
        编辑
      </button>
      <button class="card__action card__action--danger" type="button" :aria-label="`删除任务 ${task.title}`" @click="emit('remove', task)">
        删除
      </button>
    </footer>
  </article>
</template>

<style scoped>
.card {
  padding: 12px 12px 10px;
  border: 1px solid #e3e8ef;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.04);
}

.card__head {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 6px;
}

.card__priority {
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 11px;
  line-height: 18px;
}

.card__priority--low { background: #eef2f7; color: #5b6577; }
.card__priority--medium { background: #eef3fe; color: #2f6feb; }
.card__priority--high { background: #fff5e6; color: #b26a00; }
.card__priority--urgent { background: #fdecea; color: #b42318; }

.card__tag {
  padding: 1px 8px;
  border: 1px solid #e3e8ef;
  border-radius: 10px;
  color: #6b7484;
  font-size: 11px;
  line-height: 16px;
}

.card__title {
  margin: 0 0 6px;
  font-size: 14px;
  line-height: 1.5;
  color: #1f2937;
  word-break: break-word;
}

.card__desc {
  margin: 0 0 8px;
  color: #6b7484;
  font-size: 12px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card__meta {
  margin: 0 0 8px;
}

.card__meta-row {
  display: flex;
  gap: 8px;
  font-size: 12px;
  line-height: 20px;
}

.card__meta-row dt {
  flex: 0 0 44px;
  color: #8a93a3;
}

.card__meta-row dd {
  margin: 0;
  color: #475467;
}

.card__due--overdue {
  color: #b42318;
}

.card__overdue-mark {
  margin-left: 6px;
  padding: 0 4px;
  border-radius: 3px;
  background: #fdecea;
  font-size: 11px;
}

.card__foot {
  display: flex;
  gap: 6px;
  align-items: center;
  padding-top: 8px;
  border-top: 1px dashed #edf0f5;
}

.card__move {
  flex: 1 1 auto;
  height: 26px;
  border: 1px solid #d7dee9;
  border-radius: 5px;
  background: #ffffff;
  color: #475467;
  font-size: 12px;
}

.card__locked {
  flex: 1 1 auto;
  color: #8a93a3;
  font-size: 12px;
  line-height: 26px;
}

.card__action {
  height: 26px;
  padding: 0 8px;
  border: 1px solid #d7dee9;
  border-radius: 5px;
  background: #ffffff;
  color: #475467;
  font-size: 12px;
  cursor: pointer;
}

.card__action:hover {
  border-color: #2f6feb;
  color: #2f6feb;
}

.card__action--danger:hover {
  border-color: #b42318;
  color: #b42318;
}
</style>

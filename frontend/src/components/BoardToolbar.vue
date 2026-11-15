<script setup>
/**
 * 看板工具条：关键字检索、状态外的两个筛选下拉、视图切换与新建入口。
 * 边界：不持有筛选状态，只把用户输入通过 change-filter 事件上抛给页面；
 * 关键字输入做 250ms 防抖，避免逐字符触发请求。
 */
import { ref, watch } from 'vue'
import { PRIORITY_OPTIONS } from '../stores/boardStore.js'
import { debounce } from '../utils/debounce.js'

const props = defineProps({
  /** 当前筛选条件，结构为 { keyword, assignee, priority }。 */
  filters: { type: Object, required: true },
  /** 负责人候选列表。 */
  assignees: { type: Array, default: () => [] },
  /** 当前视图：board / list。 */
  view: { type: String, default: 'board' },
})

const emit = defineEmits(['change-filter', 'reset', 'create', 'change-view'])

/** 输入框本地值，与 props 单向同步，保证清空筛选时输入框同时被重置。 */
const keyword = ref(props.filters.keyword || '')

watch(
  () => props.filters.keyword,
  (value) => {
    keyword.value = value || ''
  },
)

/** 防抖后的关键字上抛，避免每个按键都触发一次列表/看板请求。 */
const emitKeyword = debounce((value) => {
  emit('change-filter', { keyword: value })
}, 250)

/**
 * 处理关键字输入。
 *
 * @param {Event} event 输入事件
 * @returns {void}
 */
function onKeywordInput(event) {
  keyword.value = event.target.value
  emitKeyword(keyword.value)
}

/**
 * 处理负责人下拉变更。
 *
 * @param {Event} event 变更事件
 * @returns {void}
 */
function onAssigneeChange(event) {
  emit('change-filter', { assignee: event.target.value })
}

/**
 * 处理优先级下拉变更。
 *
 * @param {Event} event 变更事件
 * @returns {void}
 */
function onPriorityChange(event) {
  emit('change-filter', { priority: event.target.value })
}
</script>

<template>
  <div class="toolbar">
    <div class="toolbar__filters">
      <input
        class="toolbar__input"
        type="search"
        :value="keyword"
        placeholder="按标题或标签检索"
        aria-label="按标题或标签检索"
        @input="onKeywordInput"
      />

      <select class="toolbar__select" :value="filters.assignee" aria-label="按负责人筛选" @change="onAssigneeChange">
        <option value="">全部负责人</option>
        <option v-for="name in assignees" :key="name" :value="name">{{ name }}</option>
      </select>

      <select class="toolbar__select" :value="filters.priority" aria-label="按优先级筛选" @change="onPriorityChange">
        <option value="">全部优先级</option>
        <option v-for="item in PRIORITY_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</option>
      </select>

      <button class="toolbar__reset" type="button" @click="$emit('reset')">清空筛选</button>
    </div>

    <div class="toolbar__actions">
      <div class="toolbar__switch" role="group" aria-label="视图切换">
        <button
          type="button"
          :class="['toolbar__switch-btn', { 'toolbar__switch-btn--on': view === 'board' }]"
          @click="$emit('change-view', 'board')"
        >
          看板
        </button>
        <button
          type="button"
          :class="['toolbar__switch-btn', { 'toolbar__switch-btn--on': view === 'list' }]"
          @click="$emit('change-view', 'list')"
        >
          列表
        </button>
      </div>

      <button class="toolbar__create" type="button" @click="$emit('create')">新建任务</button>
    </div>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.toolbar__filters,
.toolbar__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.toolbar__input,
.toolbar__select {
  height: 32px;
  padding: 0 10px;
  border: 1px solid #d7dee9;
  border-radius: 6px;
  background: #ffffff;
  color: #1f2937;
  font-size: 13px;
}

.toolbar__input {
  width: 200px;
}

.toolbar__reset,
.toolbar__switch-btn {
  height: 32px;
  padding: 0 12px;
  border: 1px solid #d7dee9;
  border-radius: 6px;
  background: #ffffff;
  color: #475467;
  font-size: 13px;
  cursor: pointer;
}

.toolbar__reset:hover,
.toolbar__switch-btn:hover {
  border-color: #2f6feb;
  color: #2f6feb;
}

.toolbar__switch {
  display: flex;
  overflow: hidden;
  border-radius: 6px;
}

.toolbar__switch-btn {
  border-radius: 0;
}

.toolbar__switch-btn--on {
  border-color: #2f6feb;
  background: #eef3fe;
  color: #2f6feb;
}

.toolbar__create {
  height: 32px;
  padding: 0 16px;
  border: 1px solid #2f6feb;
  border-radius: 6px;
  background: #2f6feb;
  color: #ffffff;
  font-size: 13px;
  cursor: pointer;
}

.toolbar__create:hover {
  background: #245bc9;
}
</style>

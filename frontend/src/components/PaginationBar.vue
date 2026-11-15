<script setup>
/**
 * 分页条：展示总数与页码，并把翻页意图上抛给页面。
 * 边界：不自行计算页码，页码与总页数由 store 提供；本组件只做区间渲染与禁用态。
 */
import { computed } from 'vue'

const props = defineProps({
  /** 当前页码，从 1 开始。 */
  page: { type: Number, default: 1 },
  /** 总页数，至少为 1。 */
  totalPages: { type: Number, default: 1 },
  /** 命中总条数，用于「共 N 条」文案。 */
  totalItems: { type: Number, default: 0 },
  /** 每页条数。 */
  size: { type: Number, default: 5 },
})

const emit = defineEmits(['change'])

/** 页码序列：最多展示 5 个连续页码，围绕当前页居中。 */
const pages = computed(() => {
  const total = Math.max(1, props.totalPages)
  const windowSize = 5
  let start = Math.max(1, props.page - Math.floor(windowSize / 2))
  const end = Math.min(total, start + windowSize - 1)
  start = Math.max(1, end - windowSize + 1)
  const result = []
  for (let index = start; index <= end; index += 1) {
    result.push(index)
  }
  return result
})

/** 当前页展示区间的起始序号，用于「第 X-Y 条」。 */
const rangeStart = computed(() => (props.totalItems === 0 ? 0 : (props.page - 1) * props.size + 1))

/** 当前页展示区间的结束序号。 */
const rangeEnd = computed(() => Math.min(props.totalItems, props.page * props.size))
</script>

<template>
  <div class="pager">
    <p class="pager__summary">共 {{ totalItems }} 条 · 第 {{ rangeStart }}-{{ rangeEnd }} 条 · 共 {{ totalPages }} 页</p>

    <div class="pager__actions">
      <button class="pager__btn" type="button" :disabled="page <= 1" @click="emit('change', page - 1)">上一页</button>

      <button
        v-for="item in pages"
        :key="item"
        type="button"
        :class="['pager__btn', { 'pager__btn--on': item === page }]"
        @click="emit('change', item)"
      >
        {{ item }}
      </button>

      <button class="pager__btn" type="button" :disabled="page >= totalPages" @click="emit('change', page + 1)">
        下一页
      </button>
    </div>
  </div>
</template>

<style scoped>
.pager {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
}

.pager__summary {
  margin: 0;
  color: #8a93a3;
  font-size: 12px;
}

.pager__actions {
  display: flex;
  gap: 6px;
}

.pager__btn {
  min-width: 32px;
  height: 28px;
  padding: 0 10px;
  border: 1px solid #d7dee9;
  border-radius: 5px;
  background: #ffffff;
  color: #475467;
  font-size: 12px;
  cursor: pointer;
}

.pager__btn:hover:not(:disabled) {
  border-color: #2f6feb;
  color: #2f6feb;
}

.pager__btn:disabled {
  color: #b9c0cc;
  cursor: not-allowed;
}

.pager__btn--on {
  border-color: #2f6feb;
  background: #2f6feb;
  color: #ffffff;
}
</style>

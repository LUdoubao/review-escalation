<script setup>
/**
 * 统一状态提示组件：把 loading / error / empty 三种非数据态收敛到一处渲染，
 * 避免每个视图各自拼一份提示文案导致口径不一致。
 * 边界：只做展示，重试动作由父组件通过 retry 事件决定。
 */
defineProps({
  /** 是否加载中；优先级最高。 */
  loading: { type: Boolean, default: false },
  /** 错误文案；非空时展示错误态并出现重试入口。 */
  error: { type: String, default: '' },
  /** 是否空数据；仅在非加载、无错误时生效。 */
  empty: { type: Boolean, default: false },
  /** 加载态文案。 */
  loadingText: { type: String, default: '正在加载…' },
  /** 空态文案。 */
  emptyText: { type: String, default: '暂无数据' },
})

defineEmits(['retry'])
</script>

<template>
  <div class="hint" aria-live="polite">
    <p v-if="loading" class="hint__text hint__text--loading">{{ loadingText }}</p>

    <div v-else-if="error" class="hint__error" role="alert">
      <p class="hint__text hint__text--error">{{ error }}</p>
      <button class="hint__retry" type="button" @click="$emit('retry')">重试</button>
    </div>

    <p v-else-if="empty" class="hint__text hint__text--empty">{{ emptyText }}</p>
  </div>
</template>

<style scoped>
.hint {
  min-height: 20px;
}

.hint__text {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
}

.hint__text--loading { color: #8a6d1f; }
.hint__text--error { color: #b42318; }
.hint__text--empty { color: #8a93a3; }

.hint__error {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hint__retry {
  padding: 3px 12px;
  border: 1px solid #b42318;
  border-radius: 5px;
  background: #ffffff;
  color: #b42318;
  font-size: 12px;
  cursor: pointer;
}

.hint__retry:hover {
  background: #fdf3f2;
}
</style>

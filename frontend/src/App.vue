<script setup>
/**
 * 基线页面容器：持有联通性请求的三态（loading / error / result）并驱动 StatusCard 渲染。
 * 领域：前后端联调自检；不引入路由、store 或组件间通信，保持状态流单向下行。
 */
import { onMounted, ref } from 'vue'
import { fetchHello } from './api/hello.js'
import StatusCard from './components/StatusCard.vue'

/** 请求进行中标记；同时用于禁用按钮，避免并发请求造成旧响应覆盖新响应。 */
const loading = ref(false)
/** 最近一次请求的错误文案，空串表示无错误。 */
const error = ref('')
/** 最近一次成功响应，null 表示尚无数据。 */
const result = ref(null)

/**
 * 发起一次联通性请求并同步三态。
 * 失败时清空 result，避免页面同时残留上一次成功数据又显示错误，造成状态矛盾。
 */
async function loadHello() {
  loading.value = true
  error.value = ''

  try {
    result.value = await fetchHello()
  } catch (cause) {
    result.value = null
    // fetch 或上层可能抛出非 Error 值，统一转成可展示文案，防止模板渲染出 [object Object]。
    error.value = cause instanceof Error ? cause.message : '未知错误'
  } finally {
    // finally 保证网络异常时按钮也能恢复可用，不把页面永久锁在 loading 态。
    loading.value = false
  }
}

// 页面加载即自动请求一次，使「打开页面即可看到返回结果」成为基线的最小验收动作。
onMounted(loadHello)
</script>

<template>
  <main class="page">
    <header>
      <h1>前后端联调基线</h1>
      <p class="subtitle">Vue 3 + Vite · Spring Boot 3 · GET /api/hello</p>
    </header>

    <button class="refresh" type="button" :disabled="loading" @click="loadHello">
      {{ loading ? '请求中…' : '重新请求' }}
    </button>

    <StatusCard :loading="loading" :error="error" :result="result" />
  </main>
</template>

<style scoped>
.page {
  max-width: 720px;
  margin: 0 auto;
  padding: 56px 24px;
}

h1 {
  margin: 0 0 8px;
  font-size: 26px;
  color: #1f2937;
}

.subtitle {
  margin: 0;
  color: #6b7484;
  font-size: 14px;
}

.refresh {
  margin-top: 28px;
  padding: 9px 20px;
  border: 1px solid #2f6feb;
  border-radius: 6px;
  background: #2f6feb;
  color: #ffffff;
  font-size: 14px;
  cursor: pointer;
}

.refresh:hover:not(:disabled) {
  background: #245bc9;
}

.refresh:disabled {
  border-color: #b9c6dc;
  background: #b9c6dc;
  cursor: not-allowed;
}
</style>
